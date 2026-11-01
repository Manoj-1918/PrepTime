import { Link } from 'react-router-dom';
import { fmtMin } from '../api.js';

const TYPE = { READ: 'Read', QUIZ: 'Quiz', PRACTICE: 'Practice', REVISION: 'Revision' };

export function taskLink(t) {
  if (!t.topicId) return '/mock';
  const tab = t.type === 'PRACTICE' ? 'dsa' : t.type === 'QUIZ' ? 'quiz' : 'read';
  return `/topics/${t.topicId}?tab=${tab}`;
}

export default function TaskRow({ t, onToggle, today }) {
  const done = t.status === 'DONE';
  const overdue = !done && t.date < today;
  return (
    <div className={'task' + (done ? ' done' : '') + (overdue ? ' overdue' : '')}>
      <input type="checkbox" checked={done} onChange={() => onToggle(t)} aria-label={`Mark "${t.title}" as done`} />
      <div className="task-main">
        <Link to={taskLink(t)}>{t.title}</Link>
        <span className="muted">{TYPE[t.type]} · {fmtMin(t.minutes)}{overdue ? ' · overdue' : ''}</span>
      </div>
    </div>
  );
}
