import { useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { GraduationCap, Mail, UserPlus, UserRound } from 'lucide-react'
import AuthLayout from '../components/AuthLayout'
import PasswordInput, { passwordStrength } from '../components/PasswordInput'
import { Spinner } from '../components/Feedback'
import { useAuth } from '../context/AuthContext'
import { useToast } from '../context/ToastContext'

const roles = [
  { value: 'STUDENT', title: 'I want to learn', text: 'Find tutors and book lessons', icon: UserRound },
  { value: 'TUTOR', title: 'I want to teach', text: 'Create a profile and get students', icon: GraduationCap },
]

export default function RegisterPage() {
  const { register } = useAuth()
  const toast = useToast()
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const [form, setForm] = useState({
    fullName: '', email: '', password: '', role: params.get('role') === 'TUTOR' ? 'TUTOR' : 'STUDENT',
  })
  const [errors, setErrors] = useState({})
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const strength = passwordStrength(form.password)

  const set = (key) => (event) => setForm({ ...form, [key]: event.target.value })

  const submit = async (event) => {
    event.preventDefault()
    setError('')
    setErrors({})
    setLoading(true)
    try {
      const user = await register({ ...form, fullName: form.fullName.trim(), email: form.email.trim() })
      toast.success('Account created! A welcome email is on its way.')
      navigate(user.role === 'TUTOR' ? '/tutor-profile' : '/tutors', { replace: true })
    } catch (registerError) {
      setError(registerError.message)
      setErrors(registerError.fieldErrors || {})
    } finally {
      setLoading(false)
    }
  }

  return (
    <AuthLayout title="Create your account" subtitle="It takes less than a minute.">
      <form onSubmit={submit} className="form-grid">
        <div className="role-picker" role="radiogroup" aria-label="Account type">
          {roles.map(({ value, title, text, icon: Icon }) => (
            <button key={value} type="button" role="radio" aria-checked={form.role === value}
              className={`role-option ${form.role === value ? 'active' : ''}`}
              onClick={() => setForm({ ...form, role: value })}>
              <Icon size={22} />
              <strong>{title}</strong>
              <small>{text}</small>
            </button>
          ))}
        </div>
        <label className="field">
          <span>Full name</span>
          <input value={form.fullName} onChange={set('fullName')} autoComplete="name" placeholder="e.g. Sneha Patel" required minLength={2} maxLength={120} />
          {errors.fullName && <small className="field-error">{errors.fullName}</small>}
        </label>
        <label className="field">
          <span>Email address</span>
          <div className="input-icon">
            <Mail size={18} />
            <input type="email" value={form.email} onChange={set('email')} autoComplete="email" placeholder="you@example.com" required />
          </div>
          {errors.email && <small className="field-error">{errors.email}</small>}
        </label>
        <label className="field">
          <span>Password</span>
          <PasswordInput value={form.password} onChange={set('password')} autoComplete="new-password" placeholder="At least 8 characters, with a number" />
          {form.password && (
            <span className={`strength strength-${strength.score}`}>
              <span className="strength-bar"><span style={{ width: `${strength.score * 25}%` }} /></span>
              {strength.label}
            </span>
          )}
          {errors.password && <small className="field-error">{errors.password}</small>}
        </label>
        {error && !Object.keys(errors).length && <p className="field-error" role="alert">{error}</p>}
        <button type="submit" className="btn btn-primary btn-block btn-lg" disabled={loading}>
          {loading ? <Spinner label="Creating account..." /> : <><UserPlus size={18} /> Create account</>}
        </button>
        <p className="muted small center">Your password is stored securely with BCrypt hashing.</p>
      </form>
      <p className="auth-switch">Already have an account? <Link to="/login">Sign in</Link></p>
    </AuthLayout>
  )
}
