import { Navigate, useLocation } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { Spinner } from './Feedback'

export default function ProtectedRoute({ roles, children }) {
  const { user, checking } = useAuth()
  const location = useLocation()

  if (checking && !user) {
    return <div className="page-center"><Spinner size={28} label="Checking your session..." /></div>
  }
  if (!user) {
    return <Navigate to="/login" replace state={{ from: location.pathname + location.search }} />
  }
  if (roles && !roles.includes(user.role)) {
    return <Navigate to="/dashboard" replace />
  }
  return children
}
