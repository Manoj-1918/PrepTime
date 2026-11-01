import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Bar, BarChart, CartesianGrid, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { api } from '../api.js';

export default function Analytics() {
  const [a, setA] = useState(null);
  const [err, setErr] = useState('');
  useEffect(() => { api('/analytics').then(setA).catch((e) => setErr(e.message)); }, []);
  if (!a) return <p className="muted pad">{err || 'Loading...'}</p>;
  if (a.attempts === 0) {
    return (
      <div className="page"><h1>Performance</h1>
        <div className="card empty"><p>Take a topic quiz or a mock test and your results will show up here.</p>
          <Link className="btn" to="/mock">Start a mock test</Link></div></div>
    );
  }
  return (
    <div className="page">
      <h1>Performance</h1>
      <div className="card stats">
        <div><strong>{a.overallAccuracy}%</strong><span>overall accuracy</span></div>
        <div><strong>{a.attempts}</strong><span>quizzes taken</span></div>
        <div><strong>{a.answered}</strong><span>questions answered</span></div>
      </div>
      <section className="card">
        <h2>Accuracy by topic</h2>
        <ResponsiveContainer width="100%" height={Math.max(160, a.topics.length * 34 + 30)}>
          <BarChart data={a.topics} layout="vertical" margin={{ left: 10, right: 20 }}>
            <CartesianGrid horizontal={false} stroke="#e3e7ee" />
            <XAxis type="number" domain={[0, 100]} unit="%" />
            <YAxis type="category" dataKey="topicName" width={190} tick={{ fontSize: 13 }} />
            <Tooltip formatter={(v) => v + '%'} />
            <Bar dataKey="accuracy" fill="#3b5bdb" radius={[0, 4, 4, 0]} />
          </BarChart>
        </ResponsiveContainer>
      </section>
      <section className="card">
        <h2>Last quizzes</h2>
        <ResponsiveContainer width="100%" height={220}>
          <LineChart data={a.trend} margin={{ left: 0, right: 20, top: 10 }}>
            <CartesianGrid stroke="#e3e7ee" />
            <XAxis dataKey="label" /><YAxis domain={[0, 100]} unit="%" />
            <Tooltip formatter={(v) => v + '%'} />
            <Line type="monotone" dataKey="percent" stroke="#3b5bdb" strokeWidth={2} dot />
          </LineChart>
        </ResponsiveContainer>
      </section>
      <section className="card">
        <h2>Weak topics</h2>
        {a.weakTopics.length === 0
          ? <p className="muted">None right now. A topic counts as weak when your accuracy is under 60% after at least 3 answers.</p>
          : a.weakTopics.map((w) => (
            <p key={w.topicId}><Link to={`/topics/${w.topicId}?tab=quiz`}>{w.topicName}</Link> <span className="muted">{w.accuracy}% · {w.subjectName}</span></p>
          ))}
      </section>
    </div>
  );
}
