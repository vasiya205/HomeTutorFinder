import { useEffect, useMemo, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import {
  ArrowRight, BadgeCheck, BellRing, BookOpen, CalendarCheck, CreditCard, GraduationCap, MailCheck,
  MapPin, Search, ShieldCheck, Sparkles, Star, Wallet,
} from 'lucide-react'
import TutorCard from '../components/TutorCard'
import Avatar from '../components/Avatar'
import { SkeletonCards } from '../components/Feedback'
import { api, searchTutorsWithRatings } from '../lib/api'
import { formatMoneyWhole } from '../lib/format'
import { useAuth } from '../context/AuthContext'

const steps = [
  { icon: Search, title: 'Search', text: 'Filter tutors by subject, city, teaching mode and hourly rate.' },
  { icon: CalendarCheck, title: 'Request a lesson', text: 'Pick a date and time. The tutor gets an instant email.' },
  { icon: CreditCard, title: 'Pay securely', text: 'Once accepted, pay through Razorpay checkout in seconds.' },
  { icon: Star, title: 'Learn & review', text: 'Attend the lesson and rate your tutor to help others.' },
]

const features = [
  { icon: BadgeCheck, title: 'Qualified tutors', text: 'Every profile shows qualifications, experience and real student reviews.' },
  { icon: Wallet, title: 'Transparent pricing', text: 'Hourly rates are shown upfront and the fee is calculated automatically.' },
  { icon: MailCheck, title: 'Email updates', text: 'Confirmations, acceptances and payment receipts land in your inbox.' },
  { icon: ShieldCheck, title: 'Secure by design', text: 'JWT-protected accounts, BCrypt passwords and server-verified payments.' },
]

export default function HomePage() {
  const navigate = useNavigate()
  const { user } = useAuth()
  const [tutors, setTutors] = useState(null)
  const [subjects, setSubjects] = useState([])
  const [query, setQuery] = useState({ subject: '', location: '' })

  useEffect(() => {
    searchTutorsWithRatings({}).then(setTutors).catch(() => setTutors([]))
    api.subjects().then(setSubjects).catch(() => setSubjects([]))
  }, [])

  const featured = useMemo(() => (tutors || [])
    .slice()
    .sort((a, b) => b.averageRating - a.averageRating || b.reviewCount - a.reviewCount)
    .slice(0, 3), [tutors])

  const stats = useMemo(() => {
    const list = tutors || []
    const rated = list.filter((tutor) => tutor.reviewCount > 0)
    const avg = rated.length ? rated.reduce((sum, t) => sum + t.averageRating, 0) / rated.length : 0
    return { tutors: list.length, rating: avg ? avg.toFixed(1) : '-', cities: new Set(list.map((t) => t.location)).size }
  }, [tutors])

  const search = (event) => {
    event.preventDefault()
    const params = new URLSearchParams()
    if (query.subject) params.set('subject', query.subject)
    if (query.location) params.set('location', query.location)
    navigate(`/tutors?${params.toString()}`)
  }

  const heroTutor = featured[0]

  return (
    <>
      <section className="hero">
        <div className="container hero-grid">
          <div className="hero-copy">
            <span className="eyebrow-pill"><Sparkles size={14} /> Home &amp; online tutoring made simple</span>
            <h1>Find the right tutor, <span className="gradient-text">right at your doorstep.</span></h1>
            <p className="lead">
              Compare qualified tutors, book a lesson in a few clicks, pay securely and get every update by email.
            </p>

            <form className="hero-search" onSubmit={search}>
              <label className="hero-field">
                <BookOpen size={18} />
                <input list="subject-options" placeholder="Subject, e.g. Mathematics" value={query.subject}
                  onChange={(e) => setQuery({ ...query, subject: e.target.value })} aria-label="Subject" />
                <datalist id="subject-options">{subjects.map((s) => <option key={s} value={s} />)}</datalist>
              </label>
              <label className="hero-field">
                <MapPin size={18} />
                <input placeholder="City, e.g. Hyderabad" value={query.location}
                  onChange={(e) => setQuery({ ...query, location: e.target.value })} aria-label="Location" />
              </label>
              <button className="btn btn-primary btn-lg" type="submit"><Search size={18} /> Search</button>
            </form>

            {subjects.length > 0 && (
              <div className="subject-chips">
                <span className="muted small">Popular:</span>
                {subjects.slice(0, 6).map((subject) => (
                  <Link key={subject} className="chip chip-soft" to={`/tutors?subject=${encodeURIComponent(subject)}`}>{subject}</Link>
                ))}
              </div>
            )}

            <dl className="hero-stats">
              <div><dt>{stats.tutors || '-'}</dt><dd>Verified tutors</dd></div>
              <div><dt>{stats.rating}<Star size={16} className="star-on" /></dt><dd>Average rating</dd></div>
              <div><dt>{stats.cities || '-'}</dt><dd>Cities covered</dd></div>
            </dl>
          </div>

          <div className="hero-visual" aria-hidden="true">
            <div className="blob blob-1" />
            <div className="blob blob-2" />
            <div className="float-card float-tutor">
              <div className="float-row">
                <Avatar name={heroTutor?.fullName || 'Priya Sharma'} size={52} />
                <div>
                  <strong>{heroTutor?.fullName || 'Priya Sharma'}</strong>
                  <span className="muted small">{heroTutor?.subject || 'Mathematics'} &middot; {heroTutor?.location || 'Hyderabad'}</span>
                </div>
              </div>
              <div className="float-row space">
                <span className="stars-inline"><Star size={14} className="star-on" /> {heroTutor?.averageRating ? heroTutor.averageRating.toFixed(1) : '4.9'}</span>
                <strong className="price-sm">{formatMoneyWhole(heroTutor?.hourlyRate || 800)}<span>/hr</span></strong>
              </div>
              <div className="float-btn">Request lesson</div>
            </div>
            <div className="float-card float-notify">
              <span className="notify-icon success"><CalendarCheck size={18} /></span>
              <div>
                <strong>Lesson accepted</strong>
                <span className="muted small">Tomorrow, 5:00 PM &middot; 1 hour</span>
              </div>
            </div>
            <div className="float-card float-mail">
              <span className="notify-icon"><BellRing size={18} /></span>
              <div>
                <strong>Payment receipt sent</strong>
                <span className="muted small">{formatMoneyWhole(heroTutor?.hourlyRate || 800)} paid via Razorpay</span>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section className="section">
        <div className="container">
          <div className="section-head">
            <div>
              <p className="eyebrow">Top rated</p>
              <h2>Tutors students love</h2>
            </div>
            <Link to="/tutors" className="btn btn-ghost">View all tutors <ArrowRight size={16} /></Link>
          </div>
          {tutors === null ? <SkeletonCards count={3} /> : (
            <div className="tutor-grid">
              {featured.map((tutor) => <TutorCard key={tutor.id} tutor={tutor} />)}
            </div>
          )}
          {tutors?.length === 0 && (
            <p className="muted center">Tutors will appear here once the backend is running.</p>
          )}
        </div>
      </section>

      <section className="section section-tint" id="how-it-works">
        <div className="container">
          <div className="section-head center-head">
            <p className="eyebrow">How it works</p>
            <h2>From search to lesson in four steps</h2>
          </div>
          <div className="steps">
            {steps.map(({ icon: Icon, title, text }, index) => (
              <div className="step" key={title}>
                <span className="step-num">{index + 1}</span>
                <span className="step-icon"><Icon size={22} /></span>
                <h3>{title}</h3>
                <p>{text}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      <section className="section">
        <div className="container">
          <div className="section-head center-head">
            <p className="eyebrow">Why Home Tutor Finder</p>
            <h2>Built for students, parents and tutors</h2>
          </div>
          <div className="features">
            {features.map(({ icon: Icon, title, text }) => (
              <div className="feature" key={title}>
                <span className="feature-icon"><Icon size={22} /></span>
                <h3>{title}</h3>
                <p>{text}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {!user && (
        <section className="section">
          <div className="container">
            <div className="cta-banner">
              <div>
                <h2><GraduationCap size={28} /> Are you a tutor?</h2>
                <p>Create a free profile, set your own hourly rate and start receiving lesson requests by email.</p>
              </div>
              <div className="cta-actions">
                <Link to="/register?role=TUTOR" className="btn btn-white btn-lg">Become a tutor</Link>
                <Link to="/register" className="btn btn-outline-white btn-lg">I&apos;m a student</Link>
              </div>
            </div>
          </div>
        </section>
      )}
    </>
  )
}
