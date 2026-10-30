import { useState } from 'react'
import { Eye, EyeOff } from 'lucide-react'

export default function PasswordInput({ id, value, onChange, autoComplete = 'current-password', placeholder, required = true }) {
  const [visible, setVisible] = useState(false)
  return (
    <div className="input-with-action">
      <input
        id={id}
        type={visible ? 'text' : 'password'}
        value={value}
        onChange={onChange}
        autoComplete={autoComplete}
        placeholder={placeholder}
        required={required}
      />
      <button type="button" className="input-action" onClick={() => setVisible((v) => !v)}
        aria-label={visible ? 'Hide password' : 'Show password'}>
        {visible ? <EyeOff size={18} /> : <Eye size={18} />}
      </button>
    </div>
  )
}

export function passwordStrength(password) {
  let score = 0
  if (password.length >= 8) score++
  if (/[A-Za-z]/.test(password) && /\d/.test(password)) score++
  if (/[A-Z]/.test(password) && /[a-z]/.test(password)) score++
  if (/[^A-Za-z0-9]/.test(password) || password.length >= 12) score++
  return { score, label: ['Too short', 'Weak', 'Fair', 'Good', 'Strong'][score] }
}
