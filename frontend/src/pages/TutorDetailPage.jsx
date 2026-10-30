import { useEffect, useMemo, useState } from 'react'
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom'
import {
  ArrowLeft, BadgeCheck, BookOpen, Briefcase, CalendarDays, Clock, ExternalLink, GraduationCap, MapPin,
  MessageSquareQuote, MonitorSmartphone, Send,
} from 'lucide-react'
import Avatar from '../components/Avatar'
import { StarRating } from '../components/StarRating'
import { EmptyState, Spinner } from '../components/Feedback'
import { api } from '../lib/api'
import { combineDateTime, formatDate, formatMoney, formatMoneyWhole, teachingModeLabel, toLocalInputValue } from '../lib/format'
import { resourcesForSubject } from '../lib/resources'
import { useAuth } from '../context/AuthContext'
import { useToast } from '../context/ToastContext'

const timeSlots = Array.from({ length: 29 }, (_, i) => {
  const minutes = 7 * 60 + i * 30
  return `${String(Math.floor(minutes / 60)).padStart(2, '0')}:${String(minutes % 60).padStart(2, '0')}`
})
const durations = [[30, '30 minutes'], [60, '1 hour'], [90, '1.5 hours'], [120, '2 hours']]

const slotLabel = (slot) => {
  const [h, m] = slot.split(':').map(Number)
  const suffix = h >= 12 ? 'PM' : 'AM'
  return `${((h + 11) % 12) + 1}:${String(m).padStart(2, '0')} ${suffix}`
}

export default function TutorDetailPage() {
  const { id } = useParams()
  const { user } = useAuth()
  const toast = useToast()
  const navigate = useNavigate()
  const location = useLocation()
  const [tutor, setTutor] = useState(null)
  const [reviews, setReviews] = useState([])
  const [error, setError] = useState('')
  const tomorrow = useMemo(() => {
    const date = new Date()
    date.setDate(date.getDate() + 1)
    return toLocalInputValue(date)
  }, [])
  const [form, setForm] = useState({ date: tomorrow, time: '17:00', duration: 60, subject: '', notes: '' })
  const [submitting, setSubmitting] = useState(false)
  const [formError, setFormError] = useState('')

  useEffect(() => {
    let active = true
    setTutor(null)
    setError('')
    api.tutor(id)
      .then((profile) => {
        if (!active) return
        setTutor(profile)
        setForm((current) => ({ ...current, subject: profile.subject }))
        return api.tutorReviews(profile.userId).then((list) => active && setReviews(list)).catch(() => {})
      })
      .catch((loadError) => active && setError(loadError.status === 404 ? 'This tutor profile does not exist.' : loadError.message))
    return () => {
      active = false
    }
  }, [id])

  const rating = useMemo(() => {
    if (!reviews.length) return { value: 0, count: 0 }
    return { value: reviews.reduce((sum, r) => sum + r.rating, 0) / reviews.length, count: reviews.length }
  }, [reviews])

  const fee = tutor ? (Number(tutor.hourlyRate) * form.duration) / 60 : 0
  const resources = tutor ? resourcesForSubject(tutor.subject).slice(0, 4) : []

  const submit = async (event) => {
    event.preventDefault()
    setFormError('')
    const start = new Date(`${form.date}T${form.time}:00`)
    if (start <= new Date()) {
      setFormError('Please choose a time in the future.')
      return
    }
    const end = new Date(start.getTime() + form.duration * 60000)
    const pad = (n) => String(n).padStart(2, '0')
    const endTime = `${pad(end.getHours())}:${pad(end.getMinutes())}`
    const endDate = toLocalInputValue(end)
    setSubmitting(true)
    try {
      await api.createBooking({
        tutorUserId: tutor.userId,
        subject: form.subject || tutor.subject,
        startDateTime: combineDateTime(form.date, form.time),
        endDateTime: combineDateTime(endDate, endTime),
        notes: form.notes,
      })
      toast.success(`Request sent to ${tutor.fullName}. You'll get an email when they respond.`)
      navigate('/dashboard')
    } catch (bookingError) {
      setFormError(bookingError.message)
    } finally {
      setSubmitting(false)
    }
  }

  if (error) {
    return (
      <div className="container page-pad">
        <EmptyState title="Tutor not available" action={<Link className="btn btn-primary" to="/tutors">Browse tutors</Link>}>
          {error}
        </EmptyState>
      </div>
    )
  }
  if (!tutor) return <div className="page-center"><Spinner size={28} label="Loading profile..." /></div>

  return (
    <div className="container page-pad">
      <Link to="/tutors" className="back-link"><ArrowLeft size={16} /> All tutors</Link>

      <div className="detail-layout">
        <div className="detail-main">
          <section className="card profile-hero">
            <Avatar name={tutor.fullName} size={96} />
            <div className="profile-hero-body">
              <div className="profile-title">
                <h1>{tutor.fullName}</h1>
                <span className="verified-pill"><BadgeCheck size={15} /> Verified</span>
              </div>
              <p className="profile-qual"><GraduationCap size={17} /> {tutor.qualification}</p>
              <div className="profile-chips">
                <span className="chip chip-primary">{tutor.subject}</span>
                <span className="chip"><MapPin size={14} /> {tutor.location}</span>
                <span className="chip"><Briefcase size={14} /> {tutor.experienceYears} years</span>
                <span className="chip"><MonitorSmartphone size={14} /> {teachingModeLabel[tutor.teachingMode]}</span>
              </div>
              <StarRating value={rating.value} count={rating.count} size={17} />
            </div>
          </section>

          <section className="card">
            <h2 className="card-title">About {tutor.fullName.split(' ')[0]}</h2>
            <p className="bio">{tutor.bio}</p>
          </section>

          <section className="card">
            <h2 className="card-title"><MessageSquareQuote size={20} /> Student reviews {rating.count > 0 && <span className="muted">({rating.count})</span>}</h2>
            {reviews.length === 0 ? (
              <p className="muted">No reviews yet - be the first to learn with {tutor.fullName.split(' ')[0]}!</p>
            ) : (
              <ul className="review-list">
                {reviews.map((review) => (
                  <li key={review.id} className="review">
                    <Avatar name={review.studentName} size={38} />
                    <div>
                      <div className="review-head">
                        <strong>{review.studentName}</strong>
                        <StarRating value={review.rating} showValue={false} size={13} />
                        <span className="muted small">{formatDate(review.createdAt)}</span>
                      </div>
                      <p>{review.comment}</p>
                    </div>
                  </li>
                ))}
              </ul>
            )}
          </section>

          {resources.length > 0 && (
            <section className="card">
              <h2 className="card-title"><BookOpen size={20} /> Recommended reading for {tutor.subject}</h2>
              <div className="resource-grid">
                {resources.map((resource) => (
                  <a key={resource.title} className="resource" href={resource.resourceUrl} target="_blank" rel="noopener noreferrer">
                    <span className="resource-cover">{resource.subject}</span>
                    <span className="resource-body">
                      <strong>{resource.title}</strong>
                      <span className="muted small">{resource.author} &middot; {resource.accessLabel}</span>
                      <span className="resource-link">{resource.resourceLinkLabel} <ExternalLink size={13} /></span>
                    </span>
                  </a>
                ))}
              </div>
            </section>
          )}
        </div>

        <aside className="detail-side">
          <form className="card booking-card" onSubmit={submit}>
            <div className="booking-price">
              <p className="price">{formatMoneyWhole(tutor.hourlyRate)}<span>/hour</span></p>
              <StarRating value={rating.value} count={rating.count} />
            </div>

            {user?.role !== 'STUDENT' ? (
              <div className="booking-locked">
                {user ? (
                  <p className="muted">Only student accounts can request lessons. You are signed in as a {user.role.toLowerCase()}.</p>
                ) : (
                  <>
                    <p className="muted">Sign in as a student to request a lesson with {tutor.fullName.split(' ')[0]}.</p>
                    <Link className="btn btn-primary btn-block" to="/login" state={{ from: location.pathname }}>Sign in to book</Link>
                    <Link className="btn btn-ghost btn-block" to="/register">Create a free account</Link>
                  </>
                )}
              </div>
            ) : (
              <>
                <label className="field">
                  <span><CalendarDays size={15} /> Date</span>
                  <input type="date" min={toLocalInputValue(new Date())} value={form.date} required
                    onChange={(e) => setForm({ ...form, date: e.target.value })} />
                </label>
                <div className="field-row">
                  <label className="field">
                    <span><Clock size={15} /> Start time</span>
                    <select value={form.time} onChange={(e) => setForm({ ...form, time: e.target.value })}>
                      {timeSlots.map((slot) => <option key={slot} value={slot}>{slotLabel(slot)}</option>)}
                    </select>
                  </label>
                  <label className="field">
                    <span>Duration</span>
                    <select value={form.duration} onChange={(e) => setForm({ ...form, duration: Number(e.target.value) })}>
                      {durations.map(([value, label]) => <option key={value} value={value}>{label}</option>)}
                    </select>
                  </label>
                </div>
                <label className="field">
                  <span>Topic</span>
                  <input value={form.subject} maxLength={80} onChange={(e) => setForm({ ...form, subject: e.target.value })} />
                </label>
                <label className="field">
                  <span>Message for the tutor <small className="muted">(optional)</small></span>
                  <textarea rows={3} maxLength={1000} value={form.notes} placeholder="Class, syllabus, topics you need help with..."
                    onChange={(e) => setForm({ ...form, notes: e.target.value })} />
                </label>
                <div className="fee-line">
                  <span>{formatMoneyWhole(tutor.hourlyRate)} &times; {form.duration / 60} h</span>
                  <strong>{formatMoney(fee)}</strong>
                </div>
                {formError && <p className="field-error">{formError}</p>}
                <button type="submit" className="btn btn-primary btn-block btn-lg" disabled={submitting}>
                  {submitting ? <Spinner label="Sending request..." /> : <><Send size={17} /> Request lesson</>}
                </button>
                <p className="muted small center">You pay only after the tutor accepts.</p>
              </>
            )}
          </form>
        </aside>
      </div>
    </div>
  )
}
