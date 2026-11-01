import { useState } from 'react';
import { Navigate } from 'react-router-dom';
import { useAuth } from '../auth.jsx';

export default function Login() {
  const { user, authenticate } = useAuth();
  const [mode, setMode] = useState('login');
  const [f, setF] = useState({ name: '', email: '', password: '' });
  const [err, setErr] = useState('');
  const [busy, setBusy] = useState(false);
  if (user) return <Navigate to="/" replace />;

  const set = (k) => (e) => setF({ ...f, [k]: e.target.value });
  const submit = async (e) => {
    e.preventDefault();
    setBusy(true); setErr('');
    try { await authenticate(mode === 'login' ? '/auth/login' : '/auth/register', f); }
    catch (x) { setErr(x.message); } finally { setBusy(false); }
  };

  return (
    <div className="login">
      <div className="login-copy">
        <h1>PrepTime</h1>
        <p>Tell us how many days you have. We build the plan, you tick off the days, and if you miss one the rest of the plan moves with you.</p>
      </div>
      <form className="card login-form" onSubmit={submit}>
        <h2>{mode === 'login' ? 'Log in' : 'Create your account'}</h2>
        {mode === 'register' && (<label>Name<input value={f.name} onChange={set('name')} required /></label>)}
        <label>Email<input type="email" value={f.email} onChange={set('email')} required /></label>
        <label>Password<input type="password" value={f.password} onChange={set('password')} minLength={6} required /></label>
        {err && <p className="error">{err}</p>}
        <button className="btn" disabled={busy}>{mode === 'login' ? 'Log in' : 'Create account'}</button>
        <button type="button" className="linkish" onClick={() => { setMode(mode === 'login' ? 'register' : 'login'); setErr(''); }}>
          {mode === 'login' ? 'New here? Create an account' : 'Already have an account? Log in'}
        </button>
      </form>
    </div>
  );
}
