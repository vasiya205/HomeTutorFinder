import { Link } from 'react-router-dom'
import { BadgeCheck, Briefcase, MapPin, MonitorSmartphone } from 'lucide-react'
import Avatar from './Avatar'
import { StarRating } from './StarRating'
import { formatMoneyWhole, teachingModeLabel } from '../lib/format'

export default function TutorCard({ tutor }) {
  return (
    <article className="card tutor-card">
      <div className="tutor-card-head">
        <Avatar name={tutor.fullName} size={56} />
        <div className="tutor-card-title">
          <h3>
            <Link to={`/tutors/${tutor.id}`} className="stretched-link">{tutor.fullName}</Link>
            <BadgeCheck size={16} className="verified" aria-label="Verified profile" />
          </h3>
          <span className="chip chip-primary">{tutor.subject}</span>
        </div>
      </div>
      <p className="tutor-qualification">{tutor.qualification}</p>
      <ul className="tutor-meta">
        <li><MapPin size={15} /> {tutor.location}</li>
        <li><Briefcase size={15} /> {tutor.experienceYears} yrs experience</li>
        <li><MonitorSmartphone size={15} /> {teachingModeLabel[tutor.teachingMode] || 'Home & online'}</li>
      </ul>
      <div className="tutor-card-foot">
        <StarRating value={tutor.averageRating} count={tutor.reviewCount} />
        <p className="price">{formatMoneyWhole(tutor.hourlyRate)}<span>/hr</span></p>
      </div>
    </article>
  )
}
