import { Link } from 'react-router-dom'
import { Mail, ShieldCheck, CreditCard } from 'lucide-react'
import Logo from './Logo'

export default function Footer() {
  return (
    <footer className="footer">
      <div className="container footer-grid">
        <div>
          <Logo light />
          <p className="footer-about">
            Connecting students with qualified home and online tutors. Book lessons, pay securely and stay updated by email.
          </p>
        </div>
        <div>
          <h4>Platform</h4>
          <Link to="/tutors">Find tutors</Link>
          <Link to="/register?role=TUTOR">Become a tutor</Link>
          <Link to="/login">Sign in</Link>
        </div>
        <div>
          <h4>Trust &amp; safety</h4>
          <span><ShieldCheck size={15} /> JWT secured accounts</span>
          <span><CreditCard size={15} /> Razorpay payments</span>
          <span><Mail size={15} /> Email notifications</span>
        </div>
      </div>
      <div className="container footer-bottom">
        <span>&copy; {new Date().getFullYear()} Home Tutor Finder</span>
        <span>React &middot; Spring Boot microservices &middot; PostgreSQL</span>
      </div>
    </footer>
  )
}
