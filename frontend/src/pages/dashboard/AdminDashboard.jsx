import { useCallback, useEffect, useMemo, useState } from 'react'
import {
  BarChart3, CalendarRange, LineChart, Eye, GraduationCap, IndianRupee, Mail, MailCheck, RefreshCw, Search, Send,
  ShieldCheck, UserRound, Users,
} from 'lucide-react'
import Avatar from '../../components/Avatar'
import Analytics from '../../components/Analytics'
import Modal from '../../components/Modal'
import StatusBadge from '../../components/StatusBadge'
import { EmptyState, ErrorBanner, Spinner, StatCard } from '../../components/Feedback'
import { api } from '../../lib/api'
import { formatDate, formatDateTime, formatMoney, formatMoneyWhole, timeAgo } from '../../lib/format'
import { useAuth } from '../../context/AuthContext'
import { useToast } from '../../context/ToastContext'

const tabs = [
  ['overview', 'Overview', BarChart3],
  ['analytics', 'Analytics', LineChart],
  ['users', 'Users', Users],
  ['bookings', 'Bookings', CalendarRange],
  ['emails', 'Email log', Mail],
]

export default function AdminDashboard() {
  const { user } = useAuth()
  const toast = useToast()
  const [tab, setTab] = useState('overview')
  const [data, setData] = useState({ users: null, bookings: null, stats: null, emails: null, mail: null })
  const [error, setError] = useState('')
  const [busy, setBusy] = useState('')
  const [userQuery, setUserQuery] = useState('')
  const [preview, setPreview] = useState(null)
  const [testTo, setTestTo] = useState('')

  const load = useCallback(async () => {
    setError('')
    const results = await Promise.allSettled([
      api.adminUsers(), api.adminBookings(), api.adminBookingStats(), api.adminEmails(), api.adminMailStatus(),
    ])
    const [users, bookings, stats, emails, mail] = results.map((r) => (r.status === 'fulfilled' ? r.value : []))
    setData({ users, bookings, stats: results[2].status === 'fulfilled' ? stats : null, emails, mail: results[4].status === 'fulfilled' ? mail : null })
    const failed = results.find((r) => r.status === 'rejected')
    if (failed) setError(`Some data could not be loaded: ${failed.reason.message}`)
  }, [])
  useEffect(() => { load() }, [load])

  const counts = useMemo(() => {
    const users = data.users || []
    return {
      students: users.filter((u) => u.role === 'STUDENT').length,
      tutors: users.filter((u) => u.role === 'TUTOR').length,
    }
  }, [data.users])

  const filteredUsers = useMemo(() => {
    const q = userQuery.trim().toLowerCase()
    return (data.users || []).filter((u) => !q || u.fullName.toLowerCase().includes(q) || u.email.toLowerCase().includes(q))
  }, [data.users, userQuery])

  const toggleUser = async (target) => {
    setBusy(`user-${target.id}`)
    try {
      const updated = await api.setUserEnabled(target.id, !target.enabled)
      setData((d) => ({ ...d, users: d.users.map((u) => (u.id === updated.id ? updated : u)) }))
      toast.success(`${updated.fullName} is now ${updated.enabled ? 'enabled' : 'disabled'}.`)
    } catch (actionError) {
      toast.error(actionError.message)
    } finally {
      setBusy('')
    }
  }

  const cancelBooking = async (booking) => {
    if (!window.confirm(`Cancel booking #${booking.id} (${booking.subject})? Both participants will be emailed.`)) return
    setBusy(`booking-${booking.id}`)
    try {
      const updated = await api.adminCancelBooking(booking.id)
      setData((d) => ({ ...d, bookings: d.bookings.map((b) => (b.id === updated.id ? updated : b)) }))
      toast.success(`Booking #${booking.id} cancelled.`)
    } catch (actionError) {
      toast.error(actionError.message)
    } finally {
      setBusy('')
    }
  }

  const openEmail = async (email) => {
    setPreview({ ...email, loading: true })
    try {
      setPreview(await api.adminEmail(email.id))
    } catch (previewError) {
      toast.error(previewError.message)
      setPreview(null)
    }
  }

  const sendTest = async (event) => {
    event.preventDefault()
    setBusy('test')
    try {
      await api.adminSendTestEmail(testTo.trim())
      toast.success(`Test email queued for ${testTo}. Refreshing the log...`)
      setTimeout(load, 2500)
    } catch (sendError) {
      toast.error(sendError.message)
    } finally {
      setBusy('')
    }
  }

  const loading = data.users === null

  return (
    <div className="container page-pad dashboard">
      <div className="dash-head">
        <div>
          <p className="eyebrow"><ShieldCheck size={14} /> Administration</p>
          <h1>Platform overview</h1>
          <p className="muted">Signed in as {user.fullName}. Manage accounts, bookings and email delivery.</p>
        </div>
        <button type="button" className="btn btn-ghost" onClick={load}><RefreshCw size={16} /> Refresh</button>
      </div>

      <ErrorBanner message={error} />

      <div className="tabs tabs-pill" role="tablist">
        {tabs.map(([key, label, Icon]) => (
          <button key={key} type="button" role="tab" aria-selected={tab === key} className={tab === key ? 'active' : ''} onClick={() => setTab(key)}>
            <Icon size={16} /> {label}
          </button>
        ))}
      </div>

      {loading ? <div className="panel-loading"><Spinner size={24} label="Loading admin data..." /></div> : (
        <>
          {tab === 'overview' && (
            <>
              <div className="stats-grid">
                <StatCard icon={UserRound} label="Students" value={counts.students} tone="indigo" />
                <StatCard icon={GraduationCap} label="Tutors" value={counts.tutors} tone="green" />
                <StatCard icon={CalendarRange} label="Bookings" value={data.stats?.totalBookings ?? '-'}
                  hint={data.stats ? `${data.stats.pending} pending · ${data.stats.completed} completed` : ''} tone="amber" />
                <StatCard icon={IndianRupee} label="Revenue (paid)" value={formatMoneyWhole(data.stats?.totalRevenue ?? 0)}
                  hint={data.stats ? `${data.stats.paidBookings} paid lessons` : ''} tone="violet" />
              </div>

              <div className="admin-grid">
                <section className="card">
                  <h2 className="card-title"><MailCheck size={20} /> Email delivery</h2>
                  {data.mail ? (
                    <>
                      <p className={`mail-status ${data.mail.smtpConfigured ? 'ok' : 'warn'}`}>
                        {data.mail.smtpConfigured
                          ? <>SMTP connected - sending from <strong>{data.mail.fromAddress}</strong></>
                          : <>SMTP not configured - emails are saved and visible in the app. Add <code>MAIL_USERNAME</code> and <code>MAIL_PASSWORD</code> to <code>.env</code> to deliver them.</>}
                      </p>
                      <div className="mini-stats">
                        <div><strong>{data.mail.total}</strong><span>Total</span></div>
                        <div><strong>{data.mail.sent}</strong><span>Delivered</span></div>
                        <div><strong>{data.mail.logged}</strong><span>Logged</span></div>
                        <div><strong>{data.mail.failed}</strong><span>Failed</span></div>
                      </div>
                    </>
                  ) : <p className="muted">Notification service is not reachable.</p>}
                  <form className="inline-form" onSubmit={sendTest}>
                    <input type="email" required placeholder="Send a test email to..." value={testTo} onChange={(e) => setTestTo(e.target.value)} />
                    <button type="submit" className="btn btn-primary" disabled={busy === 'test'}>
                      {busy === 'test' ? <Spinner size={14} /> : <Send size={16} />} Send test
                    </button>
                  </form>
                </section>

                <section className="card">
                  <h2 className="card-title"><CalendarRange size={20} /> Latest bookings</h2>
                  <ul className="activity-list">
                    {(data.bookings || []).slice(0, 6).map((booking) => (
                      <li key={booking.id}>
                        <Avatar name={booking.studentName} size={34} />
                        <div>
                          <strong>{booking.studentName}</strong> booked <strong>{booking.tutorName}</strong>
                          <span className="muted small">{booking.subject} · {formatDate(booking.startDateTime)} · {formatMoney(booking.totalAmount)}</span>
                        </div>
                        <StatusBadge status={booking.status} />
                      </li>
                    ))}
                    {(data.bookings || []).length === 0 && <li className="muted">No bookings yet.</li>}
                  </ul>
                </section>
              </div>
            </>
          )}

          {tab === 'analytics' && <Analytics bookings={data.bookings || []} users={data.users || []} />}

          {tab === 'users' && (
            <section className="card panel">
              <div className="table-toolbar">
                <div className="search-bar small-search">
                  <Search size={16} />
                  <input placeholder="Search name or email" value={userQuery} onChange={(e) => setUserQuery(e.target.value)} />
                </div>
                <span className="muted small">{filteredUsers.length} users</span>
              </div>
              <div className="table-wrap">
                <table className="table">
                  <thead><tr><th>User</th><th>Role</th><th>Joined</th><th>Last sign-in</th><th>Status</th><th /></tr></thead>
                  <tbody>
                    {filteredUsers.map((account) => (
                      <tr key={account.id}>
                        <td>
                          <div className="cell-user">
                            <Avatar name={account.fullName} size={34} />
                            <div><strong>{account.fullName}</strong><span className="muted small">{account.email}</span></div>
                          </div>
                        </td>
                        <td><span className={`role-tag role-${account.role.toLowerCase()}`}>{account.role}</span></td>
                        <td>{formatDate(account.createdAt)}</td>
                        <td className="muted">{account.lastLoginAt ? timeAgo(account.lastLoginAt) : 'Never'}</td>
                        <td><span className={`badge ${account.enabled ? 'badge-completed' : 'badge-cancelled'}`}>{account.enabled ? 'Active' : 'Disabled'}</span></td>
                        <td className="right">
                          {account.id !== user.id && (
                            <button type="button" className={`btn btn-sm ${account.enabled ? 'btn-ghost danger' : 'btn-success'}`}
                              disabled={busy === `user-${account.id}`} onClick={() => toggleUser(account)}>
                              {account.enabled ? 'Disable' : 'Enable'}
                            </button>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </section>
          )}

          {tab === 'bookings' && (
            <section className="card panel">
              {(data.bookings || []).length === 0 ? <EmptyState title="No bookings yet" /> : (
                <div className="table-wrap">
                  <table className="table">
                    <thead><tr><th>#</th><th>Lesson</th><th>Student</th><th>Tutor</th><th>When</th><th>Fee</th><th>Status</th><th>Payment</th><th /></tr></thead>
                    <tbody>
                      {data.bookings.map((booking) => (
                        <tr key={booking.id}>
                          <td className="muted">{booking.id}</td>
                          <td><strong>{booking.subject}</strong></td>
                          <td>{booking.studentName}</td>
                          <td>{booking.tutorName}</td>
                          <td>{formatDateTime(booking.startDateTime)}</td>
                          <td>{formatMoney(booking.totalAmount)}</td>
                          <td><StatusBadge status={booking.status} /></td>
                          <td><StatusBadge status={booking.paymentStatus} kind="payment" /></td>
                          <td className="right">
                            {['PENDING', 'ACCEPTED'].includes(booking.status) && (
                              <button type="button" className="btn btn-ghost btn-sm danger" disabled={busy === `booking-${booking.id}`}
                                onClick={() => cancelBooking(booking)}>Cancel</button>
                            )}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </section>
          )}

          {tab === 'emails' && (
            <section className="card panel">
              {(data.emails || []).length === 0 ? (
                <EmptyState icon={Mail} title="No emails yet">Emails appear here as soon as users sign up, book or pay.</EmptyState>
              ) : (
                <div className="table-wrap">
                  <table className="table">
                    <thead><tr><th>When</th><th>Recipient</th><th>Subject</th><th>Type</th><th>Status</th><th /></tr></thead>
                    <tbody>
                      {data.emails.map((email) => (
                        <tr key={email.id}>
                          <td className="muted nowrap">{timeAgo(email.createdAt)}</td>
                          <td><strong>{email.recipientName || '-'}</strong><span className="muted small block">{email.recipientEmail}</span></td>
                          <td>{email.subject}</td>
                          <td><span className="type-tag">{email.type.replaceAll('_', ' ').toLowerCase()}</span></td>
                          <td title={email.note || ''}><StatusBadge status={email.status} /></td>
                          <td className="right">
                            <button type="button" className="icon-btn" onClick={() => openEmail(email)} aria-label="Preview email"><Eye size={17} /></button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </section>
          )}
        </>
      )}

      {preview && (
        <Modal title={preview.subject} onClose={() => setPreview(null)} width={720}>
          <p className="muted small">To {preview.recipientEmail} · {formatDateTime(preview.createdAt)} · {preview.status}{preview.note ? ` - ${preview.note}` : ''}</p>
          {preview.loading ? <Spinner label="Loading..." /> : (
            <iframe title="Email preview" className="email-frame" sandbox="" srcDoc={preview.htmlBody} />
          )}
        </Modal>
      )}
    </div>
  )
}
