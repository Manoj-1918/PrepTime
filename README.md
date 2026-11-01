# PrepTime

A placement-preparation website for CSE students. The user chooses how many days and hours per day they have;
PrepTime builds a study plan covering Aptitude, DSA, DBMS, OS, CN and System Design, and **automatically re-plans
when days are missed**. It also has articles, MCQ quizzes, a DSA problem sheet with LeetCode links, a performance
analyzer, mock tests and an AI doubt-solving tutor.

## Run it (Docker)

1. Install Docker Desktop.
2. Copy `.env.example` to `.env` and paste your free Gemini API key (https://aistudio.google.com/apikey).
   The app also runs without a key: everything works except the AI tutor and the AI plan note.
3. From this folder run:

       docker compose up --build

4. Open http://localhost:3000, create an account and build a plan.

The first build downloads dependencies and takes a few minutes. Seed content (subjects, articles, questions,
DSA problems) loads automatically the first time the backend starts.

To reset everything: `docker compose down -v`.

## Architecture

    React (nginx)  ->  Spring Boot  ->  PostgreSQL
                          |
                          +-> Python FastAPI (LangChain) -> Gemini

| Folder | What it does |
|---|---|
| `frontend/` | React + Vite. Pages: Dashboard, My plan, Subjects/Topics, Performance, Mock test, Login |
| `backend/` | Spring Boot 3 (Java 17). REST API, auth, scheduler, quiz and analytics logic, JPA/PostgreSQL |
| `ai-service/` | FastAPI + LangChain. `/chat` (tutor) and `/plan-summary` (plan note), both using Gemini |
| `docker-compose.yml` | db, ai-service, backend, frontend |

The browser only talks to Spring Boot (nginx proxies `/api`). Spring Boot is the only caller of the AI service,
so the Gemini key never reaches the browser. If Gemini fails or the quota is used up, the plan still works with a
built-in text note and the chat shows a friendly error.

## How the scheduler works (backend/.../service/PlanService.java)

1. Every topic has an estimated number of hours. The total time you have (days x hours, minus revision days) is
   compared with it to decide how much practice each topic gets.
2. Topics are ordered with "stride scheduling": subjects with a higher weight (DSA 40, Aptitude 15,
   System Design 15, others 10) appear more often.
3. Each topic becomes tasks: Read, Practice (DSA only) and Quiz, split across days by your daily hours.
4. Every 7th day and the last day are revision + mock test days.
5. **Reschedule**: pending tasks from past days become *missed*. They are put first in the queue, followed by all
   future tasks, plus one short re-quiz for each weak topic (quiz accuracy under 60% after 3+ answers). All of it is
   re-spread over the remaining days, shrunk if needed to fit. The old missed tasks are kept as history.
   The dashboard runs this automatically when it finds missed tasks.

## Database (PostgreSQL, tables created automatically by Hibernate)

`users`, `auth_token`, `subject`, `topic`, `article`, `question` (options as JSONB), `dsa_problem`, `study_plan`,
`plan_task`, `quiz_attempt`, `attempt_answer`, `user_dsa_progress`.

## Add your own content

Edit the files in `backend/src/main/resources/seed/*.json` (same structure as the existing ones: subject -> topics ->
articles, questions, problems). The seed only runs on an empty database, so run `docker compose down -v` and start
again after changing it. Articles are Markdown. A DSA problem only needs its LeetCode `slug`.

## Run without Docker (development)

- Database: `docker compose up db` (add `ports: ["5432:5432"]` to the db service) or any local PostgreSQL with
  database/user/password `preptime`.
- AI service: `cd ai-service && pip install -r requirements.txt && GEMINI_API_KEY=... uvicorn main:app --port 8000`
- Backend: `cd backend && mvn spring-boot:run`
- Frontend: `cd frontend && npm install && npm run dev` (http://localhost:5173, proxies `/api` to port 8080)

## Notes

- Auth is intentionally simple: email + password (BCrypt), the server hands out a random token stored in the DB,
  and the browser sends it as `Authorization: Bearer <token>`.
- Entities use public fields and plain id columns instead of JPA relations to keep the code short and easy to read.
- Change the model with `GEMINI_MODEL` in `.env` if your free key offers a different one.
