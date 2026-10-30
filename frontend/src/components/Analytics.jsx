import { useMemo } from 'react'
import { BarChart3, PieChart, TrendingUp, Trophy } from 'lucide-react'
import { formatMoneyWhole } from '../lib/format'

const STATUS_COLORS = {
  PENDING: '#f59e0b', ACCEPTED: '#6366f1', COMPLETED: '#10b981', REJECTED: '#f43f5e', CANCELLED: '#94a3b8',
}

/** Admin analytics computed in the browser from the bookings list (no extra API needed). */
export default function Analytics({ bookings = [], users = [] }) {
  const stats = useMemo(() => {
    const paid = bookings.filter((b) => b.paymentStatus === 'PAID')
    const revenueByTutor = {}
    paid.forEach((b) => { revenueByTutor[b.tutorName] = (revenueByTutor[b.tutorName] || 0) + Number(b.totalAmount) })
    const bySubject = {}
    bookings.forEach((b) => { bySubject[b.subject] = (bySubject[b.subject] || 0) + 1 })
    const byStatus = {}
    bookings.forEach((b) => { byStatus[b.status] = (byStatus[b.status] || 0) + 1 })

    const days = []
    for (let offset = -13; offset <= 7; offset++) {
      const d = new Date()
      d.setHours(0, 0, 0, 0)
      d.setDate(d.getDate() + offset)
      days.push({ date: d, count: 0, today: offset === 0 })
    }
    bookings.forEach((b) => {
      const start = new Date(b.startDateTime)
      start.setHours(0, 0, 0, 0)
      const slot = days.find((day) => day.date.getTime() === start.getTime())
      if (slot && b.status !== 'CANCELLED' && b.status !== 'REJECTED') slot.count++
    })

    const decided = bookings.filter((b) => b.status !== 'PENDING').length
    const accepted = bookings.filter((b) => ['ACCEPTED', 'COMPLETED'].includes(b.status)).length
    const revenue = paid.reduce((sum, b) => sum + Number(b.totalAmount), 0)
    return {
      revenueByTutor: Object.entries(revenueByTutor).sort((a, b) => b[1] - a[1]),
      bySubject: Object.entries(bySubject).sort((a, b) => b[1] - a[1]),
      byStatus: Object.entries(byStatus),
      days,
      acceptance: decided ? Math.round((accepted / decided) * 100) : 0,
      avgValue: paid.length ? revenue / paid.length : 0,
      paidShare: bookings.length ? Math.round((paid.length / bookings.length) * 100) : 0,
      tutors: users.filter((u) => u.role === 'TUTOR').length,
      students: users.filter((u) => u.role === 'STUDENT').length,
    }
  }, [bookings, users])

  const maxRevenue = Math.max(1, ...stats.revenueByTutor.map(([, v]) => v))
  const maxSubject = Math.max(1, ...stats.bySubject.map(([, v]) => v))
  const maxDay = Math.max(1, ...stats.days.map((d) => d.count))
  const total = stats.byStatus.reduce((sum, [, v]) => sum + v, 0) || 1
  let angle = 0
  const donut = stats.byStatus.map(([status, value]) => {
    const from = angle
    angle += (value / total) * 360
    return `${STATUS_COLORS[status] || '#64748b'} ${from}deg ${angle}deg`
  }).join(', ')

  return (
    <div className="analytics">
      <div className="kpi-row">
        <div className="kpi"><span>Acceptance rate</span><strong>{stats.acceptance}%</strong><small>of answered requests accepted</small></div>
        <div className="kpi"><span>Average lesson value</span><strong>{formatMoneyWhole(stats.avgValue)}</strong><small>per paid lesson</small></div>
        <div className="kpi"><span>Paid bookings</span><strong>{stats.paidShare}%</strong><small>of all bookings</small></div>
        <div className="kpi"><span>Students per tutor</span><strong>{stats.tutors ? (stats.students / stats.tutors).toFixed(1) : '-'}</strong><small>{stats.students} students · {stats.tutors} tutors</small></div>
      </div>

      <div className="chart-grid">
        <section className="card chart-card wide">
          <h2 className="card-title"><TrendingUp size={20} /> Lessons per day <span className="muted small">last 14 days → next 7 days</span></h2>
          <div className="day-chart">
            {stats.days.map((day) => (
              <div key={day.date.toISOString()} className={`day-col ${day.today ? 'today' : ''} ${day.date > new Date() ? 'future' : ''}`}
                title={`${day.date.toDateString()}: ${day.count} lesson(s)`}>
                <span className="day-count">{day.count || ''}</span>
                <span className="day-bar" style={{ height: `${(day.count / maxDay) * 100}%` }} />
                <span className="day-label">{day.date.getDate()}</span>
              </div>
            ))}
          </div>
        </section>

        <section className="card chart-card">
          <h2 className="card-title"><Trophy size={20} /> Revenue by tutor</h2>
          {stats.revenueByTutor.length === 0 ? <p className="muted">No paid lessons yet.</p> : (
            <ul className="hbar-list">
              {stats.revenueByTutor.map(([name, value]) => (
                <li key={name}>
                  <span className="hbar-label">{name}</span>
                  <span className="hbar-track"><span className="hbar-fill" style={{ width: `${(value / maxRevenue) * 100}%` }} /></span>
                  <strong>{formatMoneyWhole(value)}</strong>
                </li>
              ))}
            </ul>
          )}
        </section>

        <section className="card chart-card">
          <h2 className="card-title"><PieChart size={20} /> Booking status</h2>
          <div className="donut-wrap">
            <div className="donut" style={{ background: `conic-gradient(${donut || '#e2e8f0 0deg 360deg'})` }}>
              <div className="donut-hole"><strong>{bookings.length}</strong><span>bookings</span></div>
            </div>
            <ul className="legend">
              {stats.byStatus.map(([status, value]) => (
                <li key={status}><span className="dot" style={{ background: STATUS_COLORS[status] }} />{status.toLowerCase()} <strong>{value}</strong></li>
              ))}
            </ul>
          </div>
        </section>

        <section className="card chart-card wide">
          <h2 className="card-title"><BarChart3 size={20} /> Bookings by subject</h2>
          <div className="vbar-chart">
            {stats.bySubject.map(([subject, value]) => (
              <div key={subject} className="vbar">
                <span className="vbar-value">{value}</span>
                <span className="vbar-fill" style={{ height: `${(value / maxSubject) * 100}%` }} />
                <span className="vbar-label">{subject}</span>
              </div>
            ))}
          </div>
        </section>
      </div>
    </div>
  )
}
