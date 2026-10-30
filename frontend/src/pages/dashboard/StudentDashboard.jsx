import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { CalendarClock, CheckCircle2, CreditCard, Search, Star, Wallet, XCircle } from 'lucide-react'
import BookingCard from '../../components/BookingCard'
import ReviewModal from '../../components/ReviewModal'
import { usePayment } from '../../components/Payment'
import { EmptyState, ErrorBanner, Spinner, StatCard } from '../../components/Feedback'
import { api } from '../../lib/api'
import { formatMoney, formatMoneyWhole } from '../../lib/format'
import { useAuth } from '../../context/AuthContext'
import { useToast } from '../../context/ToastContext'

const tabs = [
  ['action', 'Needs action'],
  ['upcoming', 'Upcoming'],
  ['past', 'Past'],
  ['all', 'All'],
]

export default function StudentDashboard() {
  const { user } = useAuth()
  const toast = useToast()
  const [bookings, setBookings] = useState(null)
  const [error, setError] = useState('')
  const [tab, setTab] = useState('upcoming')
  const [busyId, setBusyId] = useState(null)
  const [reviewing, setReviewing] = useState(null)

  const replace = useCallback((updated) => {
    setBookings((list) => list.map((item) => (item.id === updated.id ? { ...item, ...updated } : item)))
  }, [])
  const { pay, payingId, modal: paymentModal } = usePayment(replace)

  const load = useCallback(() => {
    setError('')
    api.myBookings().then(setBookings).catch((loadError) => {
      setError(loadError.message)
      setBookings([])
    })
  }, [])
  useEffect(load, [load])

  const groups = useMemo(() => {
    const now = Date.now()
    const list = bookings || []
    return {
      action: list.filter((b) => b.status === 'ACCEPTED' && b.paymentStatus !== 'PAID'
        || (b.status === 'COMPLETED' && !b.reviewed)),
      upcoming: list.filter((b) => ['PENDING', 'ACCEPTED'].includes(b.status) && new Date(b.endDateTime) > now)
        .sort((a, b) => new Date(a.startDateTime) - new Date(b.startDateTime)),
      past: list.filter((b) => !['PENDING', 'ACCEPTED'].includes(b.status) || new Date(b.endDateTime) <= now),
      all: list,
    }
  }, [bookings])

  useEffect(() => {
    if (bookings && groups.action.length > 0 && tab === 'upcoming' && groups.upcoming.length === 0) setTab('action')
  }, [bookings]) // eslint-disable-line react-hooks/exhaustive-deps

  const spent = (bookings || []).filter((b) => b.paymentStatus === 'PAID').reduce((sum, b) => sum + Number(b.totalAmount), 0)

  const cancel = async (booking) => {
    if (!window.confirm(`Cancel your ${booking.subject} lesson with ${booking.tutorName}?`)) return
    setBusyId(booking.id)
    try {
      replace(await api.updateBookingStatus(booking.id, 'CANCELLED'))
      toast.info('Lesson cancelled. The tutor has been notified by email.')
    } catch (actionError) {
      toast.error(actionError.message)
    } finally {
      setBusyId(null)
    }
  }

  const actionsFor = (booking) => {
    const buttons = []
    if (booking.status === 'ACCEPTED' && booking.paymentStatus !== 'PAID') {
      buttons.push(
        <button key="pay" type="button" className="btn btn-primary btn-sm" disabled={payingId === booking.id} onClick={() => pay(booking)}>
          {payingId === booking.id ? <Spinner size={14} /> : <CreditCard size={15} />} Pay {formatMoney(booking.totalAmount)}
        </button>,
      )
    }
    if (booking.status === 'COMPLETED' && !booking.reviewed) {
      buttons.push(
        <button key="review" type="button" className="btn btn-accent btn-sm" onClick={() => setReviewing(booking)}>
          <Star size={15} /> Leave review
        </button>,
      )
    }
    if ((booking.status === 'PENDING' || booking.status === 'ACCEPTED') && booking.paymentStatus !== 'PAID') {
      buttons.push(
        <button key="cancel" type="button" className="btn btn-ghost btn-sm" disabled={busyId === booking.id} onClick={() => cancel(booking)}>
          <XCircle size={15} /> Cancel
        </button>,
      )
    }
    if (booking.status === 'ACCEPTED' && booking.paymentStatus === 'PAID') {
      buttons.push(<span key="ok" className="confirmed-note"><CheckCircle2 size={15} /> Confirmed</span>)
    }
    return buttons.length ? buttons : null
  }

  const visible = groups[tab]

  return (
    <div className="container page-pad dashboard">
      <div className="dash-head">
        <div>
          <p className="eyebrow">Student dashboard</p>
          <h1>Hi {user.fullName.split(' ')[0]}, ready to learn?</h1>
          <p className="muted">Track your lesson requests, pay for accepted lessons and review your tutors.</p>
        </div>
        <Link to="/tutors" className="btn btn-primary"><Search size={17} /> Find a tutor</Link>
      </div>

      <div className="stats-grid">
        <StatCard icon={CalendarClock} label="Upcoming lessons" value={groups.upcoming.length} tone="indigo" />
        <StatCard icon={CreditCard} label="Awaiting payment"
          value={(bookings || []).filter((b) => b.status === 'ACCEPTED' && b.paymentStatus !== 'PAID').length} tone="amber" />
        <StatCard icon={CheckCircle2} label="Completed" value={(bookings || []).filter((b) => b.status === 'COMPLETED').length} tone="green" />
        <StatCard icon={Wallet} label="Total paid" value={formatMoneyWhole(spent)} tone="violet" />
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
        {bookings === null ? <div className="panel-loading"><Spinner label="Loading your lessons..." /></div>
          : visible.length === 0 ? (
            <EmptyState icon={CalendarClock} title={tab === 'action' ? 'Nothing needs your attention' : 'No lessons here yet'}
              action={<Link to="/tutors" className="btn btn-primary">Browse tutors</Link>}>
              Find a tutor you like and request your first lesson.
            </EmptyState>
          ) : (
            <div className="booking-list">
              {visible.map((booking) => (
                <BookingCard key={booking.id} booking={booking} perspective="STUDENT" actions={actionsFor(booking)} />
              ))}
            </div>
          )}
      </section>

      {paymentModal}
      {reviewing && (
        <ReviewModal booking={reviewing} onClose={() => setReviewing(null)} onSaved={() => {
          replace({ ...reviewing, reviewed: true })
          setReviewing(null)
          toast.success('Thanks for your review!')
        }} />
      )}
    </div>
  )
}
