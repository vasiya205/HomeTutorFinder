import { Link } from 'react-router-dom'
import { GraduationCap } from 'lucide-react'

export default function Logo({ light = false }) {
  return (
    <Link to="/" className={`logo ${light ? 'logo-light' : ''}`} aria-label="Home Tutor Finder home">
      <span className="logo-mark"><GraduationCap size={20} strokeWidth={2.2} /></span>
      <span className="logo-text">Home<span>Tutor</span>Finder</span>
    </Link>
  )
}
