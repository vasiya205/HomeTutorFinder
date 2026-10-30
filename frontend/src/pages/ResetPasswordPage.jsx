import { useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { KeyRound } from 'lucide-react'
import AuthLayout from '../components/AuthLayout'
import PasswordInput, { passwordStrength } from '../components/PasswordInput'
import { Spinner } from '../components/Feedback'
import { api } from '../lib/api'
import { useToast } from '../context/ToastContext'

export default function ResetPasswordPage() {
  const [params] = useSearchParams()
  const token = params.get('token') || ''
  const navigate = useNavigate()
  const toast = useToast()
  const [password, setPassword] = useState('')
  const [confirm, setConfirm] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const strength = passwordStrength(password)

  const submit = async (event) => {
    event.preventDefault()
    if (password !== confirm) {
      setError('The two passwords do not match.')
      return
    }
    setError('')
    setLoading(true)
    try {
      const response = await api.resetPassword(token, password)
      toast.success(response.message)
      navigate('/login', { replace: true })
    } catch (resetError) {
      setError(resetError.message)
    } finally {
      setLoading(false)
    }
  }

  if (!token) {
    return (
      <AuthLayout title="Invalid link" subtitle="This password reset link is incomplete.">
        <Link to="/forgot-password" className="btn btn-primary btn-block">Request a new link</Link>
      </AuthLayout>
    )
  }

  return (
    <AuthLayout title="Choose a new password" subtitle="Use at least 8 characters with a letter and a number.">
      <form onSubmit={submit} className="form-grid">
        <label className="field">
          <span>New password</span>
          <PasswordInput value={password} onChange={(e) => setPassword(e.target.value)} autoComplete="new-password" />
          {password && (
            <span className={`strength strength-${strength.score}`}>
              <span className="strength-bar"><span style={{ width: `${strength.score * 25}%` }} /></span>
              {strength.label}
            </span>
          )}
        </label>
        <label className="field">
          <span>Confirm password</span>
          <PasswordInput value={confirm} onChange={(e) => setConfirm(e.target.value)} autoComplete="new-password" />
        </label>
        {error && <p className="field-error">{error}</p>}
        <button type="submit" className="btn btn-primary btn-block btn-lg" disabled={loading}>
          {loading ? <Spinner label="Saving..." /> : <><KeyRound size={17} /> Update password</>}
        </button>
      </form>
    </AuthLayout>
  )
}
