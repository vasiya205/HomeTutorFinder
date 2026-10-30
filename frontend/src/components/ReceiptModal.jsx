import { GraduationCap, Printer } from 'lucide-react'
import Modal from './Modal'
import { formatDateTime, formatMoney, formatTime, formatDate } from '../lib/format'

/** Printable invoice for a paid lesson. "Print / Save as PDF" uses the browser's print dialog. */
export default function ReceiptModal({ booking, onClose }) {
  const invoiceNo = `HTF-${new Date(booking.paidAt || booking.updatedAt || Date.now()).getFullYear()}-${String(booking.id).padStart(5, '0')}`
  return (
    <Modal title="Payment receipt" onClose={onClose} width={620}
      footer={<>
        <button type="button" className="btn btn-ghost" onClick={onClose}>Close</button>
        <button type="button" className="btn btn-primary" onClick={() => window.print()}><Printer size={16} /> Print / Save as PDF</button>
      </>}>
      <div className="receipt" id="printable-receipt">
        <div className="receipt-head">
          <div className="receipt-brand"><span className="logo-mark"><GraduationCap size={18} /></span><strong>Home Tutor Finder</strong></div>
          <div className="receipt-meta">
            <span className="receipt-paid">PAID</span>
            <p><strong>Invoice</strong> {invoiceNo}</p>
            <p className="muted small">Issued {formatDateTime(booking.paidAt || booking.updatedAt)}</p>
          </div>
        </div>
        <div className="receipt-parties">
          <div><p className="muted small">Billed to</p><strong>{booking.studentName}</strong></div>
          <div><p className="muted small">Tutor</p><strong>{booking.tutorName}</strong></div>
        </div>
        <table className="receipt-table">
          <thead><tr><th>Description</th><th>Rate</th><th>Duration</th><th className="right">Amount</th></tr></thead>
          <tbody>
            <tr>
              <td><strong>{booking.subject} lesson</strong><span className="muted small block">{formatDate(booking.startDateTime)}, {formatTime(booking.startDateTime)} - {formatTime(booking.endDateTime)}</span></td>
              <td>{formatMoney(booking.hourlyRate)}/hr</td>
              <td>{booking.durationMinutes} min</td>
              <td className="right">{formatMoney(booking.totalAmount)}</td>
            </tr>
          </tbody>
          <tfoot><tr><td colSpan={3}>Total paid</td><td className="right">{formatMoney(booking.totalAmount)}</td></tr></tfoot>
        </table>
        <div className="receipt-foot">
          <p><span className="muted">Payment ID</span> <code>{booking.paymentId}</code></p>
          <p><span className="muted">Method</span> {booking.paymentMode === 'RAZORPAY' ? 'Razorpay (test mode)' : 'Demo checkout'}</p>
          <p><span className="muted">Booking</span> #{booking.id}</p>
        </div>
        <p className="muted small center">Thank you for learning with Home Tutor Finder. This is a computer-generated receipt; no real money was charged in test/demo mode.</p>
      </div>
    </Modal>
  )
}
