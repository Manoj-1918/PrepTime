"""PrepTime AI service: a small FastAPI app that wraps Gemini through LangChain.

Spring Boot is the only client. Two endpoints:
  POST /chat          doubt-solving tutor for a topic
  POST /plan-summary  short, friendly explanation of a study plan (or why it was re-planned)
"""
import os
from typing import List, Optional

from fastapi import FastAPI, HTTPException
from langchain_core.output_parsers import StrOutputParser
from langchain_core.prompts import ChatPromptTemplate, MessagesPlaceholder
from langchain_google_genai import ChatGoogleGenerativeAI
from pydantic import BaseModel, Field

MODEL = os.getenv("GEMINI_MODEL", "gemini-2.5-flash")

app = FastAPI(title="PrepTime AI service")
_llm = None


def get_llm() -> ChatGoogleGenerativeAI:
    """Create the Gemini client lazily so the service can start even before a key is set."""
    global _llm
    if _llm is None:
        key = os.getenv("GEMINI_API_KEY")
        if not key:
            raise HTTPException(status_code=503, detail="GEMINI_API_KEY is not set")
        _llm = ChatGoogleGenerativeAI(
            model=MODEL, google_api_key=key, temperature=0.4, max_retries=2, timeout=45
        )
    return _llm


CHAT_PROMPT = ChatPromptTemplate.from_messages([
    ("system",
     "You are PrepTime's tutor for engineering students preparing for campus placements. "
     "Current subject: {subject}. Current topic: {topic}. "
     "Explain clearly and briefly (under 200 words unless the student asks for more), "
     "use a small example or code snippet when it helps, and format with Markdown. "
     "If the question is unrelated to computer science or placement preparation, politely steer back."),
    MessagesPlaceholder("history"),
    ("human", "{message}"),
])

SUMMARY_PROMPT = ChatPromptTemplate.from_messages([
    ("system",
     "You write short, encouraging study-plan notes for a student. Plain text, 3 or 4 sentences, no lists, no headings."),
    ("human",
     "Plan: {total_days} days, {hours_per_day} hours per day.\n"
     "Topic order (first topics): {topic_order}\n"
     "Weak topics from quizzes: {weak_topics}\n"
     "Event: {reason}\n"
     "Explain the strategy of this plan. If the event is 'replanned', say that missed work was moved to the "
     "remaining days. If there are weak topics, say they get extra practice."),
])


class Turn(BaseModel):
    role: str
    content: str


class ChatIn(BaseModel):
    topic: str
    subject: str = "Computer Science"
    message: str = Field(max_length=1000)
    history: List[Turn] = []


class SummaryIn(BaseModel):
    totalDays: int
    hoursPerDay: float
    topicOrder: List[str] = []
    weakTopics: List[str] = []
    reason: Optional[str] = "created"


@app.get("/health")
def health():
    return {"status": "ok", "model": MODEL, "key_set": bool(os.getenv("GEMINI_API_KEY"))}


@app.post("/chat")
def chat(body: ChatIn):
    history = [("human" if t.role == "user" else "ai", t.content) for t in body.history]
    chain = CHAT_PROMPT | get_llm() | StrOutputParser()
    try:
        answer = chain.invoke({
            "subject": body.subject, "topic": body.topic,
            "history": history, "message": body.message,
        })
    except HTTPException:
        raise
    except Exception as e:  # quota, network, safety block, ...
        raise HTTPException(status_code=502, detail=f"Gemini request failed: {e}")
    return {"answer": answer}


@app.post("/plan-summary")
def plan_summary(body: SummaryIn):
    chain = SUMMARY_PROMPT | get_llm() | StrOutputParser()
    try:
        text = chain.invoke({
            "total_days": body.totalDays, "hours_per_day": body.hoursPerDay,
            "topic_order": ", ".join(body.topicOrder) or "n/a",
            "weak_topics": ", ".join(body.weakTopics) or "none yet",
            "reason": body.reason,
        })
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=502, detail=f"Gemini request failed: {e}")
    return {"summary": text.strip()}
