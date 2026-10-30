import { useEffect, useState } from 'react'
import { Inbox, Mail, RefreshCw } from 'lucide-react'
import StatusBadge from '../components/StatusBadge'
import { EmptyState, ErrorBanner, Spinner } from '../components/Feedback'
import { api } from '../lib/api'
import { formatDateTime, timeAgo } from '../lib/format'
import { useAuth } from '../context/AuthContext'

export default function NotificationsPage() {
  const { user } = useAuth()
  const [emails, setEmails] = useState(null)
  const [selected, setSelected] = useState(null)
  const [error, setError] = useState('')

  const load = () => {
    setError('')
    api.myEmails()
      .then((list) => {
        setEmails(list)
        setSelected((current) => list.find((e) => e.id === current?.id) || list[0] || null)
      })
      .catch((loadError) => {
        setError(loadError.message)
        setEmails([])
      })
  }
  useEffect(load, [])

  return (
    <div className="container page-pad">
      <div className="dash-head">
        <div>
          <p className="eyebrow"><Mail size={14} /> Email inbox</p>
          <h1>Your notifications</h1>
          <p className="muted">Every email we sent to <strong>{user.email}</strong> - confirmations, updates and receipts.</p>
        </div>
        <button type="button" className="btn btn-ghost" onClick={load}><RefreshCw size={16} /> Refresh</button>
      </div>

      <ErrorBanner message={error} onRetry={load} />

      {emails === null ? <div className="panel-loading"><Spinner label="Loading inbox..." /></div>
        : emails.length === 0 ? (
          <div className="card"><EmptyState icon={Inbox} title="No emails yet">Booking updates and receipts will appear here.</EmptyState></div>
        ) : (
          <div className="inbox">
            <ul className="inbox-list card">
              {emails.map((email) => (
                <li key={email.id}>
                  <button type="button" className={selected?.id === email.id ? 'active' : ''} onClick={() => setSelected(email)}>
                    <span className="inbox-subject">{email.subject}</span>
                    <span className="inbox-meta">
                      <span>{timeAgo(email.createdAt)}</span>
                      <StatusBadge status={email.status} />
                    </span>
                  </button>
                </li>
              ))}
            </ul>
            {selected && (
              <section className="inbox-view card">
                <header>
                  <h2>{selected.subject}</h2>
                  <p className="muted small">
                    From Home Tutor Finder · {formatDateTime(selected.createdAt)}
                    {selected.status === 'SENT' ? ' · delivered to your inbox' : selected.note ? ` · ${selected.note}` : ''}
                  </p>
                </header>
                <iframe title="Email content" className="email-frame" sandbox="" srcDoc={selected.htmlBody} />
              </section>
            )}
          </div>
        )}
    </div>
  )
}
