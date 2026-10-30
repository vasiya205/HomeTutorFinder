import { Star } from 'lucide-react'

export function StarRating({ value = 0, count, size = 15, showValue = true }) {
  const rounded = Math.round(value * 2) / 2
  return (
    <span className="stars" aria-label={count ? `${value} out of 5 from ${count} reviews` : 'No reviews yet'}>
      {[1, 2, 3, 4, 5].map((star) => (
        <Star key={star} size={size} className={rounded >= star - 0.25 ? 'star-on' : 'star-off'} />
      ))}
      {showValue && (
        <span className="stars-text">
          {count ? <><strong>{Number(value).toFixed(1)}</strong> ({count})</> : 'New'}
        </span>
      )}
    </span>
  )
}

export function StarInput({ value, onChange }) {
  return (
    <div className="star-input" role="radiogroup" aria-label="Rating">
      {[1, 2, 3, 4, 5].map((star) => (
        <button
          key={star}
          type="button"
          role="radio"
          aria-checked={value === star}
          aria-label={`${star} star${star > 1 ? 's' : ''}`}
          className={value >= star ? 'active' : ''}
          onClick={() => onChange(star)}
        >
          <Star size={30} />
        </button>
      ))}
    </div>
  )
}
