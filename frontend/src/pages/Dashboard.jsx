import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api, fmtDate } from '../api.js';
import TaskRow from '../components/TaskRow.jsx';

export function PlanStrip({ strip }) {
  return (
    <div className="strip" role="img" aria-label="Day by day plan progress">
      {strip.map((d) => (
        <span key={d.day} className={`cell ${d.state}${d.revision ? ' rev' : ''}`}
              title={`Day ${d.day} · ${fmtDate(d.date)}${d.revision ? ' · revision' : ''}`} />
      ))}
    </div>
  );
}

export default function Dashboard() {
  const [d, setD] = useState(null);
  const [note, setNote] = useState('');
  const [err, setErr] = useState('');

  const load = () => api('/dashboard').then(setD).catch((e) => setErr(e.message));

  useEffect(() => {
    (async () => {
      try {
        let x = await api('/dashboard');
        if (x.hasPlan && x.missedCount > 0) {          // missed days -> replan automatically
          try {
            const r = await api('/plan/reschedule', { method: 'POST' });
            setNote(r.message);
            x = await api('/dashboard');
          } catch (e) { setNote(e.message); }   // e.g. the plan has already ended
        }
        setD(x);
      } catch (e) { setErr(e.message); }
    })();
  }, []);

  const toggle = async (t) => {
    await api(`/plan/tasks/${t.id}/status`, { method: 'POST', body: { status: t.status === 'DONE' ? 'PENDING' : 'DONE' } });
    load();
  };

  if (err) return <p className="error pad">{err}</p>;
  if (!d) return <p className="muted pad">Loading...</p>;

  if (!d.hasPlan) {
    return (
      <div className="page">
        <h1>Welcome to PrepTime</h1>
        <div className="card empty">
          <h2>You do not have a plan yet</h2>
          <p>Pick how many days you have and how many hours you can study each day. We will schedule every subject for you.</p>
          <Link className="btn" to="/plan">Create my plan</Link>
        </div>
      </div>
    );
  }

  const open = d.todayTasks.filter((t) => t.status !== 'DONE').length;
  return (
    <div className="page">
      <header className="hero">
        <div>
          <h1>Day {d.dayNumber} <span className="of">of {d.plan.totalDays}</span></h1>
          <p className="muted">{open === 0 ? 'Everything for today is done.' : `${open} task${open === 1 ? '' : 's'} left today.`}</p>
        </div>
        <div className="bar-wrap" aria-label="Plan progress">
          <div className="bar"><div style={{ width: d.progress.percent + '%' }} /></div>
          <span>{d.progress.percent}% of the plan finished</span>
        </div>
      </header>

      {note && <div className="banner">{note}</div>}

      <PlanStrip strip={d.strip} />
      <div className="legend muted">
        <span><i className="cell done" /> done</span><span><i className="cell missed" /> missed</span>
        <span><i className="cell today" /> today</span><span><i className="cell upcoming" /> coming up</span>
        <span><i className="cell upcoming rev" /> revision day</span>
      </div>

      <div className="cols">
        <section className="card">
          <h2>Today</h2>
          {d.todayTasks.length === 0 && <p className="muted">No tasks today. Use the time to revise a weak topic.</p>}
          {d.todayTasks.map((t) => <TaskRow key={t.id} t={t} today={d.today} onToggle={toggle} />)}
        </section>
        <section className="side">
          <div className="card stats">
            <div><strong>{d.streak}</strong><span>day streak</span></div>
            <div><strong>{d.accuracy == null ? '-' : d.accuracy + '%'}</strong><span>quiz accuracy</span></div>
            <div><strong>{d.dsaSolved}/{d.dsaTotal}</strong><span>DSA solved</span></div>
          </div>
          {d.plan.aiSummary && (<div className="card"><h3>Plan note</h3><p>{d.plan.aiSummary}</p></div>)}
          {d.weakTopics.length > 0 && (
            <div className="card">
              <h3>Needs more practice</h3>
              {d.weakTopics.map((w) => (
                <p key={w.topicId}><Link to={`/topics/${w.topicId}?tab=quiz`}>{w.topicName}</Link> <span className="muted">{w.accuracy}%</span></p>
              ))}
            </div>
          )}
        </section>
      </div>
    </div>
  );
}
