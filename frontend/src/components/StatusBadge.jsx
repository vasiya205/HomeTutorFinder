const labels = {
  PENDING: 'Pending', ACCEPTED: 'Accepted', REJECTED: 'Rejected', COMPLETED: 'Completed', CANCELLED: 'Cancelled',
  UNPAID: 'Unpaid', PAID: 'Paid', SENT: 'Sent', LOGGED: 'Logged', FAILED: 'Failed', QUEUED: 'Queued',
}

export default function StatusBadge({ status, kind = 'status' }) {
  const key = kind === 'payment' && status === 'PENDING' ? 'PAYMENT_PENDING' : status
  const text = kind === 'payment' && status === 'PENDING' ? 'Payment started' : labels[status] || status
  return <span className={`badge badge-${String(key).toLowerCase()}`}>{text}</span>
}
