import { Routes, Route, Navigate, NavLink, Outlet } from 'react-router-dom';
import { useAuth } from './auth.jsx';
import Login from './pages/Login.jsx';
import Dashboard from './pages/Dashboard.jsx';
import PlanPage from './pages/PlanPage.jsx';
import Subjects from './pages/Subjects.jsx';
import TopicPage from './pages/TopicPage.jsx';
import Analytics from './pages/Analytics.jsx';
import Mock from './pages/Mock.jsx';

function Shell() {
  const { user, logout } = useAuth();
  const link = ({ isActive }) => (isActive ? 'nav active' : 'nav');
  return (
    <div className="shell">
      <aside>
        <div className="brand">PrepTime</div>
        <nav>
          <NavLink to="/" end className={link}>Dashboard</NavLink>
          <NavLink to="/plan" className={link}>My plan</NavLink>
          <NavLink to="/subjects" className={link}>Subjects</NavLink>
          <NavLink to="/analytics" className={link}>Performance</NavLink>
          <NavLink to="/mock" className={link}>Mock test</NavLink>
        </nav>
        <div className="who">
          <span>{user.name}</span>
          <button className="linkish" onClick={logout}>Log out</button>
        </div>
      </aside>
      <main><Outlet /></main>
    </div>
  );
}

function Guard() {
  const { user, ready } = useAuth();
  if (!ready) return <p className="muted pad">Loading...</p>;
  return user ? <Shell /> : <Navigate to="/login" replace />;
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route element={<Guard />}>
        <Route path="/" element={<Dashboard />} />
        <Route path="/plan" element={<PlanPage />} />
        <Route path="/subjects" element={<Subjects />} />
        <Route path="/topics/:id" element={<TopicPage />} />
        <Route path="/analytics" element={<Analytics />} />
        <Route path="/mock" element={<Mock />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
