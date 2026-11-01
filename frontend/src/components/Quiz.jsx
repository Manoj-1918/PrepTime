import { useState } from 'react';
import { api } from '../api.js';

/** Shows questions, submits the answers and then reveals the right answers with explanations. */
export default function Quiz({ questions, topicId = null, onFinish }) {
  const [picked, setPicked] = useState({});
  const [result, setResult] = useState(null);
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState('');

  const submit = async () => {
    setBusy(true); setErr('');
    try {
      const r = await api('/quiz/submit', {
        method: 'POST',
        body: { topicId, answers: questions.map((q) => ({ questionId: q.id, selectedIndex: picked[q.id] ?? null })) },
      });
      setResult(r);
      onFinish && onFinish(r);
    } catch (e) { setErr(e.message); } finally { setBusy(false); }
  };

  const byId = result ? Object.fromEntries(result.results.map((r) => [r.questionId, r])) : {};
  const answered = Object.keys(picked).length;

  return (
    <div className="quiz">
      {result && (
        <div className="score">
          <strong>{result.score} / {result.total}</strong>
          <span>{Math.round((100 * result.score) / result.total)}% correct. Wrong answers are explained below.</span>
        </div>
      )}
      {questions.map((q, i) => {
        const r = byId[q.id];
        return (
          <div className="q" key={q.id}>
            <p className="qtext"><span className="qn">{i + 1}.</span> {q.text}</p>
            <div className="opts">
              {q.options.map((o, idx) => {
                let cls = 'opt';
                if (!result && picked[q.id] === idx) cls += ' picked';
                if (r && idx === r.correctIndex) cls += ' right';
                if (r && idx === r.selectedIndex && !r.correct) cls += ' wrong';
                return (
                  <button key={idx} className={cls} disabled={!!result}
                          onClick={() => setPicked({ ...picked, [q.id]: idx })}>{o}</button>
                );
              })}
            </div>
            {r && <p className="why">{r.correct ? 'Correct. ' : 'Not quite. '}{r.explanation}</p>}
          </div>
        );
      })}
      {err && <p className="error">{err}</p>}
      {!result && (
        <button className="btn" disabled={busy || answered === 0} onClick={submit}>
          {busy ? 'Checking...' : `Submit answers (${answered}/${questions.length})`}
        </button>
      )}
    </div>
  );
}
