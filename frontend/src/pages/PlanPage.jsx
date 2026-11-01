import { useEffect, useState } from 'react';
import { api, fmtDate } from '../api.js';
import TaskRow from '../components/TaskRow.jsx';

const iso = (d) => new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString().slice(0, 10);

export default function PlanPage() {
  const [data, setData] = useState(null);
  const [form, setForm] = useState({ totalDays: 90, hoursPerDay: 3, startDate: iso(new Date()) });
  const [showForm, setShowForm] = useState(false);
  const [msg, setMsg] = useState('');
  const [err, setErr] = useState('');
  const [busy, setBusy] = useState(false);

  const load = () => api('/plan/current').then(setData).catch((e) => setErr(e.message));
  useEffect(() => { load(); }, []);

  const create = async (e) => {
    e.preventDefault();
    setBusy(true); setErr(''); setMsg('');
    try {
      const r = await api('/plan', { method: 'POST', body: { totalDays: +form.totalDays, hoursPerDay: +form.hoursPerDay, startDate: form.startDate } });
      setData(r); setShowForm(false);
      if (r.overflow > 0) setMsg(`${r.overflow} tasks did not fit. Add more days or hours for full coverage.`);
    } catch (x) { setErr(x.message); } finally { setBusy(false); }
  };
  const replan = async () => {
    setBusy(true); setErr('');
    try { const r = await api('/plan/reschedule', { method: 'POST' }); setMsg(r.message); await load(); }
    catch (x) { setErr(x.message); } finally { setBusy(false); }
  };
  const toggle = async (t) => {
    await api(`/plan/tasks/${t.id}/status`, { method: 'POST', body: { status: t.status === 'DONE' ? 'PENDING' : 'DONE' } });
    load();
  };

  if (!data) return <p className="muted pad">{err || 'Loading...'}</p>;

  const planForm = (
    <form className="card form" onSubmit={create}>
      <h2>{data.plan ? 'Start a new plan' : 'Create your plan'}</h2>
      <label>Number of days<input type="number" min="7" max="365" value={form.totalDays} onChange={(e) => setForm({ ...form, totalDays: e.target.value })} /></label>
      <label>Hours per day<input type="number" min="0.5" max="16" step="0.5" value={form.hoursPerDay} onChange={(e) => setForm({ ...form, hoursPerDay: e.target.value })} /></label>
      <label>Start date<input type="date" value={form.startDate} onChange={(e) => setForm({ ...form, startDate: e.target.value })} /></label>
      {data.plan && <p className="muted">Your current plan will be archived.</p>}
      <button className="btn" disabled={busy}>{busy ? 'Building plan...' : 'Build plan'}</button>
    </form>
  );

  if (!data.plan) return <div className="page"><h1>My plan</h1>{err && <p className="error">{err}</p>}{planForm}</div>;

  // group tasks into weeks (7 days from plan start)
  const start = new Date(data.plan.startDate + 'T00:00:00');
  const weeks = {};
  data.tasks.forEach((t) => {
    const w = Math.floor((new Date(t.date + 'T00:00:00') - start) / 86400000 / 7);
    ((weeks[w] ||= {})[t.date] ||= []).push(t);
  });
  const currentWeek = Math.max(0, Math.floor((new Date(data.today + 'T00:00:00') - start) / 86400000 / 7));

  return (
    <div className="page">
      <header className="row between">
        <h1>My plan</h1>
        <div className="row">
          <button className="btn ghost" onClick={replan} disabled={busy}>Replan missed days{data.missedCount ? ` (${data.missedCount})` : ''}</button>
          <button className="btn ghost" onClick={() => setShowForm(!showForm)}>New plan</button>
        </div>
      </header>
      <p className="muted">{data.plan.totalDays} days · {data.plan.hoursPerDay} h/day · ends {fmtDate(data.endDate)} · version {data.plan.version}</p>
      {msg && <div className="banner">{msg}</div>}
      {err && <p className="error">{err}</p>}
      {showForm && planForm}
      {Object.keys(weeks).map(Number).sort((a, b) => a - b).map((w) => (
        <details key={w} open={w === currentWeek} className="week">
          <summary>Week {w + 1}</summary>
          {Object.keys(weeks[w]).sort().map((date) => (
            <div className={'day' + (date === data.today ? ' today' : '')} key={date}>
              <h4>{fmtDate(date)}{date === data.today && ' · today'}</h4>
              {weeks[w][date].map((t) => <TaskRow key={t.id} t={t} today={data.today} onToggle={toggle} />)}
            </div>
          ))}
        </details>
      ))}
    </div>
  );
}
