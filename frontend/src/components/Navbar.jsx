import { useEffect, useRef, useState } from 'react'
import { Link, NavLink, useLocation, useNavigate } from 'react-router-dom'
import { Bell, ChevronDown, LayoutDashboard, LogOut, Menu, Moon, Search, Sun, UserRound, X } from 'lucide-react'
import { useTheme } from '../lib/theme'
import { useAuth } from '../context/AuthContext'
import Logo from './Logo'
import Avatar from './Avatar'

const roleLabel = { STUDENT: 'Student', TUTOR: 'Tutor', ADMIN: 'Administrator' }

export default function Navbar() {
  const { user, logout } = useAuth()
  const [open, setOpen] = useState(false)
  const [menuOpen, setMenuOpen] = useState(false)
  const menuRef = useRef(null)
  const [theme, toggleTheme] = useTheme()
  const location = useLocation()
  const navigate = useNavigate()

  useEffect(() => {
    setOpen(false)
    setMenuOpen(false)
  }, [location.pathname])

  useEffect(() => {
    const onClick = (event) => {
      if (menuRef.current && !menuRef.current.contains(event.target)) setMenuOpen(false)
    }
    document.addEventListener('mousedown', onClick)
    return () => document.removeEventListener('mousedown', onClick)
  }, [])

  const handleLogout = () => {
    logout()
    navigate('/')
  }

  return (
    <header className="navbar">
      <div className="container navbar-inner">
        <Logo />

        <nav className={`nav-links ${open ? 'open' : ''}`} aria-label="Main">
          <NavLink to="/tutors"><Search size={16} /> Find tutors</NavLink>
          {!user && <a href="/#how-it-works">How it works</a>}
          {!user && <Link to="/register?role=TUTOR">Become a tutor</Link>}
          {user && <NavLink to="/dashboard"><LayoutDashboard size={16} /> Dashboard</NavLink>}
          {user?.role === 'TUTOR' && <NavLink to="/tutor-profile"><UserRound size={16} /> My profile</NavLink>}
          {user && <NavLink to="/notifications"><Bell size={16} /> Inbox</NavLink>}
          {!user && (
            <div className="nav-auth-mobile">
              <Link className="btn btn-ghost" to="/login">Sign in</Link>
              <Link className="btn btn-primary" to="/register">Get started</Link>
            </div>
          )}
          {user && (
            <button type="button" className="nav-logout-mobile" onClick={handleLogout}>
              <LogOut size={16} /> Sign out
            </button>
          )}
        </nav>

        <div className="nav-actions">
          <button type="button" className="icon-btn theme-toggle" onClick={toggleTheme}
            aria-label={theme === 'dark' ? 'Switch to light mode' : 'Switch to dark mode'} title="Toggle dark mode">
            {theme === 'dark' ? <Sun size={19} /> : <Moon size={19} />}
          </button>
          {user ? (
            <div className="user-menu" ref={menuRef}>
              <button type="button" className="user-chip" onClick={() => setMenuOpen((v) => !v)} aria-expanded={menuOpen}>
                <Avatar name={user.fullName} size={34} />
                <span className="user-chip-text">
                  <strong>{user.fullName.split(' ')[0]}</strong>
                  <small>{roleLabel[user.role]}</small>
                </span>
                <ChevronDown size={16} />
              </button>
              {menuOpen && (
                <div className="dropdown">
                  <div className="dropdown-head">
                    <strong>{user.fullName}</strong>
                    <span>{user.email}</span>
                  </div>
                  <Link to="/dashboard"><LayoutDashboard size={16} /> Dashboard</Link>
                  <Link to="/notifications"><Bell size={16} /> Email inbox</Link>
                  {user.role === 'TUTOR' && <Link to="/tutor-profile"><UserRound size={16} /> Tutor profile</Link>}
                  <button type="button" onClick={handleLogout}><LogOut size={16} /> Sign out</button>
                </div>
              )}
            </div>
          ) : (
            <div className="nav-auth">
              <Link className="btn btn-ghost" to="/login">Sign in</Link>
              <Link className="btn btn-primary" to="/register">Get started</Link>
            </div>
          )}
          <button type="button" className="icon-btn nav-toggle" onClick={() => setOpen((v) => !v)}
            aria-label={open ? 'Close menu' : 'Open menu'}>
            {open ? <X size={22} /> : <Menu size={22} />}
          </button>
        </div>
      </div>
    </header>
  )
}
