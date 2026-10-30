import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { CalendarCheck, Check, CheckCircle2, Clock3, Hourglass, IndianRupee, Inbox, UserRoundPen, X, XCircle } from 'lucide-react'
import BookingCard from '../../components/BookingCard'
import { EmptyState, ErrorBanner, Spinner, StatCard } from '../../components/Feedback'
import { api } from '../../lib/api'
import { formatMoneyWhole } from '../../lib/format'
import { useAuth } from '../../context/AuthContext'
import { useToast } from '../../context/ToastContext'

const tabs = [['requests', 'New requests'], ['upcoming', 'Upcoming'], ['history', 'History']]

export default function TutorDashboard() {
  const { user } = useAuth()
  const toast = useToast()
  const [bookings, setBookings] = useState(null)
  const [profile, setProfile] = useState(undefined)
  const [error, setError] = useState('')
  const [tab, setTab] = useState('requests')
  const [busy, setBusy] = useState(null)

  const load = useCallback(() => {
    setError('')
    api.incomingBookings().then(setBookings).catch((loadError) => {
      setError(loadError.message)
      setBookings([])
    })
    api.myTutorProfile().then((p) => setProfile(p || null)).catch(() => setProfile(null))
  }, [])
  useEffect(load, [load])

  const groups = useMemo(() => {
    const list = bookings || []
    return {
      requests: list.filter((b) => b.status === 'PENDING').sort((a, b) => new Date(a.startDateTime) - new Date(b.startDateTime)),
      upcoming: list.filter((b) => b.status === 'ACCEPTED').sort((a, b) => new Date(a.startDateTime) - new Date(b.startDateTime)),
      history: list.filter((b) => !['PENDING', 'ACCEPTED'].includes(b.status)),
    }
  }, [bookings])

  const earnings = (bookings || []).filter((b) => b.paymentStatus === 'PAID').reduce((sum, b) => sum + Number(b.totalAmount), 0)

  const change = async (booking, status, message) => {
    if (status === 'CANCELLED' && !window.confirm(`Cancel the lesson with ${booking.studentName}?`)) return
    setBusy(`${booking.id}-${status}`)
    try {
      const updated = await api.updateBookingStatus(booking.id, status)
      setBookings((list) => list.map((item) => (item.id === updated.id ? updated : item)))
      toast.success(message)
    } catch (actionError) {
      toast.error(actionError.message)
    } finally {
      setBusy(null)
    }
  }

  const isBusy = (booking, status) => busy === `${booking.id}-${status}`

  const actionsFor = (booking) => {
    if (booking.status === 'PENDING') {
      return [
        <button key="accept" type="button" className="btn btn-success btn-sm" disabled={Boolean(busy)}
          onClick={() => change(booking, 'ACCEPTED', `Accepted - ${booking.studentName} has been emailed to complete payment.`)}>
          {isBusy(booking, 'ACCEPTED') ? <Spinner size={14} /> : <Check size={15} />} Accept
        </button>,
        <button key="reject" type="button" className="btn btn-ghost btn-sm" disabled={Boolean(busy)}
          onClick={() => change(booking, 'REJECTED', 'Request declined. The student has been notified.')}>
          <X size={15} /> Decline
        </button>,
      ]
    }
    if (booking.status === 'ACCEPTED' && booking.paymentStatus === 'PAID') {
      return [
        <button key="complete" type="button" className="btn btn-primary btn-sm" disabled={Boolean(busy)}
          onClick={() => change(booking, 'COMPLETED', 'Lesson marked as completed. The student was asked for a review.')}>
          {isBusy(booking, 'COMPLETED') ? <Spinner size={14} /> : <CheckCircle2 size={15} />} Mark completed
        </button>,
      ]
    }
    if (booking.status === 'ACCEPTED') {
      return [
        <span key="wait" className="waiting-note"><Hourglass size={15} /> Waiting for payment</span>,
        <button key="cancel" type="button" className="btn btn-ghost btn-sm" disabled={Boolean(busy)}
          onClick={() => change(booking, 'CANCELLED', 'Lesson cancelled. The student has been notified.')}>
          <XCircle size={15} /> Cancel
        </button>,
      ]
    }
    return null
  }

  return (
    <div className="container page-pad dashboard">
      <div className="dash-head">
        <div>
          <p className="eyebrow">Tutor dashboard</p>
          <h1>Welcome back, {user.fullName.split(' ')[0]}</h1>
          <p className="muted">Respond to lesson requests and keep track of your students and earnings.</p>
        </div>
        <Link to="/tutor-profile" className="btn btn-ghost"><UserRoundPen size={17} /> Edit profile</Link>
      </div>

      {profile === null && (
        <div className="alert alert-warning">
          <UserRoundPen size={18} />
          <span><strong>Your profile is not published yet.</strong> Students can only find and book you after you complete it.</span>
          <Link to="/tutor-profile" className="btn btn-primary btn-sm">Complete profile</Link>
        </div>
      )}

      <div className="stats-grid">
        <StatCard icon={Inbox} label="New requests" value={groups.requests.length} tone="amber" />
        <StatCard icon={CalendarCheck} label="Upcoming lessons" value={groups.upcoming.length} tone="indigo" />
        <StatCard icon={Clock3} label="Completed lessons" value={(bookings || []).filter((b) => b.status === 'COMPLETED').length} tone="green" />
        <StatCard icon={IndianRupee} label="Earnings received" value={formatMoneyWhole(earnings)} tone="violet" />
      </div>

      <ErrorBanner message={error} onRetry={load} />

      <section className="card panel">
        <div className="tabs" role="tablist">
          {tabs.map(([key, label]) => (
            <button key={key} role="tab" type="button" aria-selected={tab === key} className={tab === key ? 'active' : ''}
              onClick={() => setTab(key)}>
              {label} <span className="tab-count">{groups[key].length}</span>
            </button>
          ))}
        </div>
        {bookings === null ? <div className="panel-loading"><Spinner label="Loading requests..." /></div>
          : groups[tab].length === 0 ? (
            <EmptyState icon={Inbox} title={tab === 'requests' ? 'No new requests' : 'Nothing here yet'}>
              {tab === 'requests' ? 'New lesson requests appear here and are also sent to your email.' : 'Accepted and completed lessons will show up here.'}
            </EmptyState>
          ) : (
            <div className="booking-list">
              {groups[tab].map((booking) => (
                <BookingCard key={booking.id} booking={booking} perspective="TUTOR" actions={actionsFor(booking)} />
              ))}
            </div>
          )}
      </section>
    </div>
  )
}
