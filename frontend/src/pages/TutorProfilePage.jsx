import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { Eye, Save } from 'lucide-react'
import TutorCard from '../components/TutorCard'
import { Spinner } from '../components/Feedback'
import { api } from '../lib/api'
import { useAuth } from '../context/AuthContext'
import { useToast } from '../context/ToastContext'

const empty = { qualification: '', subject: '', location: '', experienceYears: 1, hourlyRate: 500, teachingMode: 'BOTH', bio: '' }
const suggestions = ['Mathematics', 'Physics', 'Chemistry', 'Biology', 'English', 'Programming', 'Hindi', 'Telugu', 'Accountancy', 'Economics']

export default function TutorProfilePage() {
  const { user } = useAuth()
  const toast = useToast()
  const [form, setForm] = useState(empty)
  const [saved, setSaved] = useState(null)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [errors, setErrors] = useState({})

  useEffect(() => {
    api.myTutorProfile()
      .then((profile) => {
        if (profile) {
          setSaved(profile)
          setForm({
            qualification: profile.qualification, subject: profile.subject, location: profile.location,
            experienceYears: profile.experienceYears, hourlyRate: Number(profile.hourlyRate),
            teachingMode: profile.teachingMode, bio: profile.bio,
          })
        }
      })
      .catch((error) => toast.error(error.message))
      .finally(() => setLoading(false))
  }, [toast])

  const set = (key, number = false) => (event) =>
    setForm({ ...form, [key]: number ? (event.target.value === '' ? '' : Number(event.target.value)) : event.target.value })

  const submit = async (event) => {
    event.preventDefault()
    setSaving(true)
    setErrors({})
    try {
      const profile = await api.saveTutorProfile(form)
      setSaved(profile)
      toast.success(saved ? 'Profile updated.' : 'Profile published! Students can now find and book you.')
    } catch (error) {
      setErrors(error.fieldErrors || {})
      toast.error(error.message)
    } finally {
      setSaving(false)
    }
  }

  if (loading) return <div className="page-center"><Spinner size={28} label="Loading your profile..." /></div>

  const preview = {
    id: saved?.id || 0, userId: user.id, fullName: user.fullName, qualification: form.qualification || 'Your qualification',
    subject: form.subject || 'Subject', location: form.location || 'City', experienceYears: form.experienceYears || 0,
    hourlyRate: form.hourlyRate || 0, teachingMode: form.teachingMode, averageRating: 0, reviewCount: 0,
  }
  const err = (key) => errors[key] && <small className="field-error">{errors[key]}</small>

  return (
    <div className="container page-pad">
      <div className="dash-head">
        <div>
          <p className="eyebrow">Tutor profile</p>
          <h1>{saved ? 'Edit your public profile' : 'Publish your tutor profile'}</h1>
          <p className="muted">This is what students see when they search. A detailed bio gets more requests.</p>
        </div>
        {saved && <Link to={`/tutors/${saved.id}`} className="btn btn-ghost"><Eye size={16} /> View public page</Link>}
      </div>

      <div className="profile-editor">
        <form className="card form-grid" onSubmit={submit}>
          <div className="field-row">
            <label className="field">
              <span>Main subject</span>
              <input list="subject-suggestions" value={form.subject} onChange={set('subject')} required maxLength={80} placeholder="e.g. Mathematics" />
              <datalist id="subject-suggestions">{suggestions.map((s) => <option key={s} value={s} />)}</datalist>
              {err('subject')}
            </label>
            <label className="field">
              <span>City / area</span>
              <input value={form.location} onChange={set('location')} required maxLength={80} placeholder="e.g. Hyderabad" />
              {err('location')}
            </label>
          </div>
          <label className="field">
            <span>Qualification</span>
            <input value={form.qualification} onChange={set('qualification')} required maxLength={150} placeholder="e.g. M.Sc. Mathematics, B.Ed." />
            {err('qualification')}
          </label>
          <div className="field-row three">
            <label className="field">
              <span>Experience (years)</span>
              <input type="number" min={0} max={60} value={form.experienceYears} onChange={set('experienceYears', true)} required />
              {err('experienceYears')}
            </label>
            <label className="field">
              <span>Hourly rate (₹)</span>
              <input type="number" min={100} max={10000} step={50} value={form.hourlyRate} onChange={set('hourlyRate', true)} required />
              {err('hourlyRate')}
            </label>
            <label className="field">
              <span>Teaching mode</span>
              <select value={form.teachingMode} onChange={set('teachingMode')}>
                <option value="BOTH">Home &amp; online</option>
                <option value="HOME">Home tuition only</option>
                <option value="ONLINE">Online only</option>
              </select>
            </label>
          </div>
          <label className="field">
            <span>About you <small className="muted">({form.bio.length}/2000)</small></span>
            <textarea rows={6} value={form.bio} onChange={set('bio')} required minLength={20} maxLength={2000}
              placeholder="Your teaching style, classes and boards you cover, achievements of your students..." />
            {err('bio')}
          </label>
          <button type="submit" className="btn btn-primary btn-lg" disabled={saving}>
            {saving ? <Spinner label="Saving..." /> : <><Save size={17} /> {saved ? 'Save changes' : 'Publish profile'}</>}
          </button>
        </form>

        <aside className="profile-preview">
          <p className="eyebrow">Live preview</p>
          <TutorCard tutor={preview} />
          <p className="muted small">Your name and email come from your account ({user.email}).</p>
        </aside>
      </div>
    </div>
  )
}
