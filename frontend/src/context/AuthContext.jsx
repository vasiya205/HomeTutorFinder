import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import { api, getStoredToken, storeToken } from '../lib/api'

const AuthContext = createContext(null)
const USER_KEY = 'htf.user'

function readStoredUser() {
  try {
    const raw = localStorage.getItem(USER_KEY)
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

function writeStoredUser(user) {
  try {
    if (user) localStorage.setItem(USER_KEY, JSON.stringify(user))
    else localStorage.removeItem(USER_KEY)
  } catch {
    /* ignore */
  }
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => (getStoredToken() ? readStoredUser() : null))
  const [checking, setChecking] = useState(() => Boolean(getStoredToken()))

  const applySession = useCallback((session) => {
    storeToken(session.token)
    writeStoredUser(session.user)
    setUser(session.user)
    return session.user
  }, [])

  const logout = useCallback(() => {
    storeToken(null)
    writeStoredUser(null)
    setUser(null)
  }, [])

  // On start-up, confirm the stored JWT is still valid (not expired, account not disabled).
  useEffect(() => {
    if (!getStoredToken()) return
    let active = true
    api.me()
      .then((fresh) => {
        if (!active) return
        writeStoredUser(fresh)
        setUser(fresh)
      })
      .catch((error) => {
        if (active && (error.status === 401 || error.status === 403 || error.status === 404)) logout()
      })
      .finally(() => active && setChecking(false))
    return () => {
      active = false
    }
  }, [logout])

  // Any API call that comes back 401 signs the user out.
  useEffect(() => {
    const handler = () => logout()
    window.addEventListener('htf:unauthorized', handler)
    return () => window.removeEventListener('htf:unauthorized', handler)
  }, [logout])

  const value = useMemo(() => ({
    user,
    checking,
    isAuthenticated: Boolean(user),
    login: async (email, password) => applySession(await api.login(email.trim(), password)),
    register: async (payload) => applySession(await api.register(payload)),
    logout,
  }), [user, checking, applySession, logout])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) throw new Error('useAuth must be used inside <AuthProvider>')
  return context
}
