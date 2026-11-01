import { useState } from 'react';
import ReactMarkdown from 'react-markdown';
import { api } from '../api.js';

/** Doubt-solving chat. Messages go Spring Boot -> Python service -> Gemini. */
export default function Chat({ topicId }) {
  const [msgs, setMsgs] = useState([]);
  const [text, setText] = useState('');
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState('');

  const send = async () => {
    const message = text.trim();
    if (!message || busy) return;
    const history = msgs;
    setMsgs([...history, { role: 'user', content: message }]);
    setText(''); setBusy(true); setErr('');
    try {
      const r = await api('/chat', { method: 'POST', body: { topicId, message, history } });
      setMsgs((m) => [...m, { role: 'ai', content: r.answer }]);
    } catch (e) { setErr(e.message); } finally { setBusy(false); }
  };

  return (
    <div className="chat">
      {msgs.length === 0 && <p className="muted">Ask anything about this topic, for example "explain with an example" or "what is the time complexity?".</p>}
      {msgs.map((m, i) => (
        <div key={i} className={'bubble ' + m.role}>
          {m.role === 'ai' ? <ReactMarkdown>{m.content}</ReactMarkdown> : m.content}
        </div>
      ))}
      {busy && <div className="bubble ai muted">Thinking...</div>}
      {err && <p className="error">{err}</p>}
      <div className="row">
        <input value={text} placeholder="Type your doubt" maxLength={1000}
               onChange={(e) => setText(e.target.value)} onKeyDown={(e) => e.key === 'Enter' && send()} />
        <button className="btn" onClick={send} disabled={busy}>Ask</button>
      </div>
    </div>
  );
}
