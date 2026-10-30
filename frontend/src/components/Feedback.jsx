import { AlertCircle, Inbox, Loader2 } from 'lucide-react'

export function Spinner({ size = 18, label }) {
  return (
    <span className="spinner-wrap">
      <Loader2 size={size} className="spin" aria-hidden="true" />
      {label && <span>{label}</span>}
    </span>
  )
}

export function EmptyState({ icon: Icon = Inbox, title, children, action }) {
  return (
    <div className="empty-state">
      <span className="empty-icon"><Icon size={26} /></span>
      <h3>{title}</h3>
      {children && <p>{children}</p>}
      {action}
    </div>
  )
}

export function ErrorBanner({ message, onRetry }) {
  if (!message) return null
  return (
    <div className="alert alert-error" role="alert">
      <AlertCircle size={18} />
      <span>{message}</span>
      {onRetry && <button type="button" className="link-btn" onClick={onRetry}>Try again</button>}
    </div>
  )
}

export function SkeletonCards({ count = 6 }) {
  return (
    <div className="tutor-grid">
      {Array.from({ length: count }).map((_, index) => (
        <div className="card skeleton-card" key={index}>
          <div className="skeleton skeleton-circle" />
          <div className="skeleton skeleton-line" style={{ width: '70%' }} />
          <div className="skeleton skeleton-line" style={{ width: '45%' }} />
          <div className="skeleton skeleton-line" style={{ width: '90%' }} />
          <div className="skeleton skeleton-line" style={{ width: '60%' }} />
        </div>
      ))}
    </div>
  )
}

export function StatCard({ icon: Icon, label, value, hint, tone = 'indigo' }) {
  return (
    <div className={`stat-card tone-${tone}`}>
      <span className="stat-icon"><Icon size={20} /></span>
      <div>
        <p className="stat-label">{label}</p>
        <p className="stat-value">{value}</p>
        {hint && <p className="stat-hint">{hint}</p>}
      </div>
    </div>
  )
}
