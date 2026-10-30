import { useState } from 'react'
import { Link } from 'react-router-dom'
import { ArrowLeft, Mail, MailCheck, Send } from 'lucide-react'
import AuthLayout from '../components/AuthLayout'
import { Spinner } from '../components/Feedback'
import { api } from '../lib/api'

export default function ForgotPasswordPage() {
  const [email, setEmail] = useState('')
  const [sent, setSent] = useState(false)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const submit = async (event) => {
    event.preventDefault()
    setError('')
    setLoading(true)
    try {
      await api.forgotPassword(email.trim())
      setSent(true)
    } catch (requestError) {
      setError(requestError.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <AuthLayout title="Forgot your password?" subtitle="We'll email you a secure link to choose a new one.">
      {sent ? (
        <div className="success-panel">
          <MailCheck size={44} />
          <h3>Check your inbox</h3>
          <p>If an account exists for <strong>{email}</strong>, a reset link is on its way. It expires in 30 minutes.</p>
          <Link to="/login" className="btn btn-primary btn-block">Back to sign in</Link>
        </div>
      ) : (
        <form onSubmit={submit} className="form-grid">
          <label className="field">
            <span>Email address</span>
            <div className="input-icon">
              <Mail size={18} />
              <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} placeholder="you@example.com" required />
            </div>
          </label>
          {error && <p className="field-error">{error}</p>}
          <button type="submit" className="btn btn-primary btn-block btn-lg" disabled={loading}>
            {loading ? <Spinner label="Sending..." /> : <><Send size={17} /> Send reset link</>}
          </button>
        </form>
      )}
      <p className="auth-switch"><Link to="/login"><ArrowLeft size={14} /> Back to sign in</Link></p>
    </AuthLayout>
  )
}
