import { useState } from 'react'
import Modal from './Modal'
import { StarInput } from './StarRating'
import { Spinner } from './Feedback'
import { api } from '../lib/api'

const hints = ['', 'Poor', 'Fair', 'Good', 'Very good', 'Excellent']

export default function ReviewModal({ booking, onClose, onSaved }) {
  const [rating, setRating] = useState(5)
  const [comment, setComment] = useState('')
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')

  const submit = async (event) => {
    event.preventDefault()
    setSaving(true)
    setError('')
    try {
      await api.createReview({ bookingId: booking.id, rating, comment })
      onSaved()
    } catch (saveError) {
      setError(saveError.message)
      setSaving(false)
    }
  }

  return (
    <Modal title={`Review your lesson with ${booking.tutorName}`} onClose={onClose}>
      <form onSubmit={submit} className="form-grid">
        <div className="center">
          <StarInput value={rating} onChange={setRating} />
          <p className="muted small">{hints[rating]}</p>
        </div>
        <label className="field">
          <span>Your feedback</span>
          <textarea rows={4} value={comment} onChange={(e) => setComment(e.target.value)} maxLength={1000}
            placeholder="What did you like? Would you recommend this tutor?" required minLength={3} />
        </label>
        {error && <p className="field-error">{error}</p>}
        <button type="submit" className="btn btn-primary btn-block" disabled={saving}>
          {saving ? <Spinner label="Submitting..." /> : 'Submit review'}
        </button>
      </form>
    </Modal>
  )
}
