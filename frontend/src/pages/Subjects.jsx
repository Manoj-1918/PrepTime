import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api.js';

export default function Subjects() {
  const [subjects, setSubjects] = useState(null);
  const [err, setErr] = useState('');
  useEffect(() => { api('/subjects').then(setSubjects).catch((e) => setErr(e.message)); }, []);
  if (!subjects) return <p className="muted pad">{err || 'Loading...'}</p>;
  return (
    <div className="page">
      <h1>Subjects</h1>
      {subjects.map((s) => (
        <section key={s.id} className="subject">
          <h2>{s.name}</h2>
          <p className="muted">{s.description}</p>
          <div className="topics">
            {s.topics.map((t) => (
              <Link key={t.id} to={`/topics/${t.id}`} className="topic card">
                <strong>{t.name}</strong>
                <span className="muted">
                  {t.questionCount} questions{t.problemCount > 0 ? ` · ${t.solvedCount}/${t.problemCount} problems solved` : ''}
                </span>
              </Link>
            ))}
          </div>
        </section>
      ))}
    </div>
  );
}
