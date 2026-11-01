import { useEffect, useState } from 'react';
import { api } from '../api.js';
import Quiz from '../components/Quiz.jsx';

export default function Mock() {
  const [qs, setQs] = useState(null);
  const [round, setRound] = useState(0);
  useEffect(() => { setQs(null); api('/quiz/mock?count=15').then(setQs); }, [round]);
  return (
    <div className="page">
      <h1>Mock test</h1>
      <p className="muted">15 mixed questions from every subject. Results feed your performance page.</p>
      <div className="card">
        {!qs ? <p className="muted">Loading...</p> : <Quiz key={round} questions={qs} />}
      </div>
      <button className="btn ghost" onClick={() => setRound(round + 1)}>New mock test</button>
    </div>
  );
}
