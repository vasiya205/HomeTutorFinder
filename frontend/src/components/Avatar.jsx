import { avatarGradient, initials } from '../lib/format'

export default function Avatar({ name, size = 44, className = '' }) {
  return (
    <span
      className={`avatar ${className}`}
      style={{ width: size, height: size, fontSize: Math.round(size * 0.38), background: avatarGradient(name) }}
      aria-hidden="true"
    >
      {initials(name)}
    </span>
  )
}
