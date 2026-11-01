import { useEffect, useState } from 'react';
import { useParams, useSearchParams } from 'react-router-dom';
import ReactMarkdown from 'react-markdown';
import { api } from '../api.js';
import Quiz from '../components/Quiz.jsx';
import Chat from '../components/Chat.jsx';

function Problems({ topicId }) {
  const [list, setList] = useState(null);
  useEffect(() => { api(`/topics/${topicId}/dsa`).then(setList); }, [topicId]);
  const toggle = async (p) => {
    await api(`/dsa/${p.id}/toggle`, { method: 'POST' });
    setList(list.map((x) => (x.id === p.id ? { ...x, solved: !x.solved } : x)));
  };
  if (!list) return <p className="muted">Loading...</p>;
  if (list.length === 0) return <p className="muted">No practice problems for this topic.</p>;
  return (
    <div>
      <p className="muted">{list.filter((p) => p.solved).length} of {list.length} solved. Open a problem on LeetCode, then tick it here.</p>
      {list.map((p) => (
        <div key={p.id} className={'task' + (p.solved ? ' done' : '')}>
          <input type="checkbox" checked={p.solved} onChange={() => toggle(p)} aria-label={`Mark ${p.title} solved`} />
          <div className="task-main">
            <a href={p.url} target="_blank" rel="noreferrer">{p.title}</a>
            <span className="muted">{p.difficulty.toLowerCase()} · {p.sheet === 'BLIND75' ? 'Blind 75' : 'frequently asked'}</span>
          </div>
        </div>
      ))}
    </div>
  );
}

function QuizTab({ topicId }) {
  const [qs, setQs] = useState(null);
  const [round, setRound] = useState(0);
  useEffect(() => { setQs(null); api(`/quiz/topic/${topicId}?count=10`).then(setQs); }, [topicId, round]);
  if (!qs) return <p className="muted">Loading...</p>;
  if (qs.length === 0) return <p className="muted">No questions for this topic yet.</p>;
  return (
    <div>
      <Quiz key={round} questions={qs} topicId={+topicId} />
      <button className="btn ghost" onClick={() => setRound(round + 1)}>New set of questions</button>
    </div>
  );
}

export default function TopicPage() {
  const { id } = useParams();
  const [sp, setSp] = useSearchParams();
  const [t, setT] = useState(null);
  const [err, setErr] = useState('');
  const tab = sp.get('tab') || 'read';
  useEffect(() => { setT(null); api(`/topics/${id}`).then(setT).catch((e) => setErr(e.message)); }, [id]);
  if (!t) return <p className="muted pad">{err || 'Loading...'}</p>;

  const tabs = [['read', 'Read'], ['quiz', 'Quiz'], ...(t.problemCount > 0 ? [['dsa', 'Problems']] : []), ['ask', 'Ask AI']];
  return (
    <div className="page">
      <p className="muted">{t.subjectName}</p>
      <h1>{t.name}</h1>
      <div className="tabs" role="tablist">
        {tabs.map(([k, label]) => (
          <button key={k} role="tab" aria-selected={tab === k} className={tab === k ? 'tab on' : 'tab'} onClick={() => setSp({ tab: k })}>{label}</button>
        ))}
      </div>
      <div className="card">
        {tab === 'read' && t.articles.map((a) => (
          <article key={a.id} className="prose"><h2>{a.title}</h2><ReactMarkdown>{a.content}</ReactMarkdown></article>
        ))}
        {tab === 'quiz' && <QuizTab topicId={id} />}
        {tab === 'dsa' && <Problems topicId={id} />}
        {tab === 'ask' && <Chat topicId={+id} />}
      </div>
    </div>
  );
}
