import { CalendarCheck, CreditCard, MailCheck, Star } from 'lucide-react'
import Logo from './Logo'

export default function AuthLayout({ title, subtitle, children }) {
  return (
    <div className="auth-page">
      <aside className="auth-aside">
        <Logo light />
        <div className="auth-aside-copy">
          <h2>Learning that comes to you.</h2>
          <p>Verified tutors, transparent hourly rates and instant email updates for every booking.</p>
          <ul>
            <li><Star size={18} /> Ratings from real students</li>
            <li><CalendarCheck size={18} /> Request lessons in seconds</li>
            <li><CreditCard size={18} /> Secure Razorpay checkout</li>
            <li><MailCheck size={18} /> Email confirmations &amp; receipts</li>
          </ul>
        </div>
        <p className="auth-aside-foot">Trusted by students and parents across India</p>
      </aside>
      <main className="auth-main">
        <div className="auth-card">
          <h1>{title}</h1>
          {subtitle && <p className="auth-subtitle">{subtitle}</p>}
          {children}
        </div>
      </main>
    </div>
  )
}
