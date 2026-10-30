import { Link } from 'react-router-dom'
import { Compass } from 'lucide-react'
import { EmptyState } from '../components/Feedback'

export default function NotFoundPage() {
  return (
    <div className="container page-pad">
      <EmptyState icon={Compass} title="Page not found" action={<Link to="/" className="btn btn-primary">Go home</Link>}>
        The page you are looking for does not exist or has moved.
      </EmptyState>
    </div>
  )
}
