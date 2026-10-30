import { useAuth } from '../context/AuthContext'
import StudentDashboard from './dashboard/StudentDashboard'
import TutorDashboard from './dashboard/TutorDashboard'
import AdminDashboard from './dashboard/AdminDashboard'

export default function DashboardPage() {
  const { user } = useAuth()
  if (user.role === 'ADMIN') return <AdminDashboard />
  if (user.role === 'TUTOR') return <TutorDashboard />
  return <StudentDashboard />
}
