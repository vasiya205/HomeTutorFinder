const inr = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 2 })
const inrWhole = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 })

export const formatMoney = (value) => (value == null ? '-' : inr.format(Number(value)))
export const formatMoneyWhole = (value) => (value == null ? '-' : inrWhole.format(Number(value)))

export function formatDate(value) {
  if (!value) return '-'
  return new Date(value).toLocaleDateString('en-IN', { weekday: 'short', day: 'numeric', month: 'short', year: 'numeric' })
}

export function formatTime(value) {
  if (!value) return ''
  return new Date(value).toLocaleTimeString('en-IN', { hour: 'numeric', minute: '2-digit' })
}

export function formatDateTime(value) {
  if (!value) return '-'
  return `${formatDate(value)}, ${formatTime(value)}`
}

export function timeAgo(value) {
  if (!value) return ''
  const seconds = Math.round((Date.now() - new Date(value).getTime()) / 1000)
  if (seconds < 60) return 'just now'
  const minutes = Math.round(seconds / 60)
  if (minutes < 60) return `${minutes} min ago`
  const hours = Math.round(minutes / 60)
  if (hours < 24) return `${hours} h ago`
  const days = Math.round(hours / 24)
  if (days < 30) return `${days} d ago`
  return formatDate(value)
}

export const initials = (name = '') =>
  name.trim().split(/\s+/).slice(0, 2).map((part) => part[0]?.toUpperCase() || '').join('') || '?'

const palettes = [
  ['#4f46e5', '#7c3aed'], ['#0ea5e9', '#2563eb'], ['#10b981', '#0d9488'], ['#f59e0b', '#ea580c'],
  ['#ec4899', '#db2777'], ['#8b5cf6', '#6366f1'], ['#14b8a6', '#0891b2'],
]
export function avatarGradient(seed = '') {
  let hash = 0
  for (const char of String(seed)) hash = (hash * 31 + char.charCodeAt(0)) >>> 0
  const [from, to] = palettes[hash % palettes.length]
  return `linear-gradient(135deg, ${from}, ${to})`
}

export const teachingModeLabel = { HOME: 'Home tuition', ONLINE: 'Online', BOTH: 'Home & online' }

export const toLocalInputValue = (date) => {
  const pad = (n) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

/** Builds "2026-10-31T17:00:00" (local time, which is what the backend expects). */
export const combineDateTime = (date, time) => `${date}T${time}:00`
