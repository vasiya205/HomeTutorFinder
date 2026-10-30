import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { LogIn, Mail, ShieldCheck, UserRound, GraduationCap } from 'lucide-react'
import AuthLayout from '../components/AuthLayout'
import PasswordInput from '../components/PasswordInput'
import { Spinner } from '../components/Feedback'
import { useAuth } from '../context/AuthContext'
import { useToast } from '../context/ToastContext'

const demoAccounts = [
  { label: 'Student', email: 'student@hometutor.demo', icon: UserRound },
  { label: 'Tutor', email: 'priya@hometutor.demo', icon: GraduationCap },
  { label: 'Admin', email: 'admin@hometutor.demo', icon: ShieldCheck },
]

export default function LoginPage() {
  const { login } = useAuth()
  const toast = useToast()
  const navigate = useNavigate()
  const location = useLocation()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const signIn = async (emailValue, passwordValue) => {
    setError('')
    setLoading(true)
    try {
      const user = await login(emailValue, passwordValue)
      toast.success(`Welcome back, ${user.fullName.split(' ')[0]}!`)
      navigate(location.state?.from || '/dashboard', { replace: true })
    } catch (loginError) {
      setError(loginError.message)
    } finally {
      setLoading(false)
    }
  }

  const submit = (event) => {
    event.preventDefault()
    signIn(email, password)
  }

  return (
    <AuthLayout title="Welcome back" subtitle="Sign in to manage your lessons and bookings.">
      <form onSubmit={submit} className="form-grid">
        <label className="field">
          <span>Email address</span>
          <div className="input-icon">
            <Mail size={18} />
            <input id="email" type="email" value={email} onChange={(e) => setEmail(e.target.value)}
              autoComplete="email" placeholder="you@example.com" required />
          </div>
        </label>
        <label className="field">
          <span className="field-label-row">
            Password
            <Link to="/forgot-password" className="small">Forgot password?</Link>
          </span>
          <PasswordInput id="password" value={password} onChange={(e) => setPassword(e.target.value)} placeholder="Your password" />
        </label>
        {error && <p className="field-error" role="alert">{error}</p>}
        <button type="submit" className="btn btn-primary btn-block btn-lg" disabled={loading}>
          {loading ? <Spinner label="Signing in..." /> : <><LogIn size={18} /> Sign in</>}
        </button>
      </form>

      <div className="divider"><span>or try a demo account</span></div>
      <div className="demo-accounts">
        {demoAccounts.map(({ label, email: demoEmail, icon: Icon }) => (
          <button key={label} type="button" className="demo-account" disabled={loading}
            onClick={() => signIn(demoEmail, 'Demo@123')}>
            <Icon size={18} />
            <span><strong>{label}</strong><small>{demoEmail}</small></span>
          </button>
        ))}
      </div>
      <p className="muted small center">Demo password: <code>Demo@123</code></p>

      <p className="auth-switch">New to Home Tutor Finder? <Link to="/register">Create an account</Link></p>
    </AuthLayout>
  )
}
