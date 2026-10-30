import { useState } from 'react'
import { CalendarPlus, Clock, CreditCard, FileText, Receipt, Star } from 'lucide-react'
import ReceiptModal from './ReceiptModal'
import { downloadLessonInvite, googleCalendarLink } from '../lib/calendar'
import Avatar from './Avatar'
import StatusBadge from './StatusBadge'
import { formatMoney, formatTime } from '../lib/format'

export default function BookingCard({ booking, perspective, actions }) {
  const [showReceipt, setShowReceipt] = useState(false)
  const upcoming = ['PENDING', 'ACCEPTED'].includes(booking.status) && new Date(booking.endDateTime) > new Date()
  const start = new Date(booking.startDateTime)
  const otherName = perspective === 'TUTOR' ? booking.studentName : booking.tutorName
  const otherLabel = perspective === 'TUTOR' ? 'Student' : 'Tutor'

  return (
    <article className={`booking-card-row status-${booking.status.toLowerCase()}`}>
      <div className="date-block" aria-hidden="true">
        <span className="date-month">{start.toLocaleDateString('en-IN', { month: 'short' })}</span>
        <span className="date-day">{start.getDate()}</span>
        <span className="date-week">{start.toLocaleDateString('en-IN', { weekday: 'short' })}</span>
      </div>

      <div className="booking-body">
        <div className="booking-title">
          <h3>{booking.subject}</h3>
          <div className="badges">
            <StatusBadge status={booking.status} />
            {booking.status !== 'REJECTED' && booking.status !== 'CANCELLED' && (
              <StatusBadge status={booking.paymentStatus} kind="payment" />
            )}
          </div>
        </div>
        <p className="booking-person">
          <Avatar name={otherName} size={24} /> <span className="muted">{otherLabel}:</span> <strong>{otherName}</strong>
        </p>
        <p className="booking-meta">
          <span><Clock size={14} /> {formatTime(booking.startDateTime)} - {formatTime(booking.endDateTime)} ({booking.durationMinutes} min)</span>
          <span><CreditCard size={14} /> {formatMoney(booking.totalAmount)}</span>
          {booking.paymentId && <span className="muted small">Ref {booking.paymentId}</span>}
        </p>
        {booking.notes && <p className="booking-notes"><FileText size={14} /> {booking.notes}</p>}
        {booking.reviewed && <p className="reviewed-note"><Star size={14} className="star-on" /> You reviewed this lesson</p>}
        {(upcoming || booking.paymentStatus === 'PAID') && (
          <div className="booking-links">
            {upcoming && (
              <>
                <button type="button" className="link-btn" onClick={() => downloadLessonInvite(booking)}><CalendarPlus size={14} /> Add to calendar (.ics)</button>
                <a className="link-btn" href={googleCalendarLink(booking)} target="_blank" rel="noopener noreferrer">Google Calendar</a>
              </>
            )}
            {booking.paymentStatus === 'PAID' && (
              <button type="button" className="link-btn" onClick={() => setShowReceipt(true)}><Receipt size={14} /> {perspective === 'TUTOR' ? 'Payment receipt' : 'View receipt'}</button>
            )}
          </div>
        )}
      </div>

      {actions && <div className="booking-actions">{actions}</div>}
      {showReceipt && <ReceiptModal booking={booking} onClose={() => setShowReceipt(false)} />}
    </article>
  )
}
