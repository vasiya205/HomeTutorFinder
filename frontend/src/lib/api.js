// Central API client. Every request goes through the API Gateway (/api/...).
// In development Vite proxies /api to http://localhost:8080 (see vite.config.js).
const API_BASE = (import.meta.env.VITE_API_BASE_URL || '').replace(/\/$/, '')
const TOKEN_KEY = 'htf.token'

export class ApiError extends Error {
  constructor(message, status, fieldErrors = {}) {
    super(message)
    this.status = status
    this.fieldErrors = fieldErrors
  }
}

export function getStoredToken() {
  try {
    return localStorage.getItem(TOKEN_KEY)
  } catch {
    return null
  }
}

export function storeToken(token) {
  try {
    if (token) localStorage.setItem(TOKEN_KEY, token)
    else localStorage.removeItem(TOKEN_KEY)
  } catch {
    /* storage unavailable (private mode) - keep the session in memory only */
  }
}

async function request(path, { method = 'GET', body, auth = true, signal } = {}) {
  const headers = { Accept: 'application/json' }
  if (body !== undefined) headers['Content-Type'] = 'application/json'
  const token = auth ? getStoredToken() : null
  if (token) headers.Authorization = `Bearer ${token}`

  let response
  try {
    response = await fetch(`${API_BASE}${path}`, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
      signal,
    })
  } catch (error) {
    if (error.name === 'AbortError') throw error
    throw new ApiError('Cannot reach the server. Make sure the backend services are running.', 0)
  }

  if (response.status === 204) return null
  const text = await response.text()
  let data = null
  if (text) {
    try {
      data = JSON.parse(text)
    } catch {
      data = null
    }
  }

  if (!response.ok) {
    if (response.status === 401 && token) {
      window.dispatchEvent(new Event('htf:unauthorized'))
    }
    const fallback = response.status >= 500 || response.status === 0
      ? 'The service is temporarily unavailable. Please try again in a moment.'
      : 'Request failed.'
    throw new ApiError(data?.message || fallback, response.status, data?.fieldErrors || {})
  }
  return data
}

const qs = (params) => {
  const search = new URLSearchParams()
  Object.entries(params || {}).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') search.append(key, value)
  })
  const text = search.toString()
  return text ? `?${text}` : ''
}

export const api = {
  // Auth
  login: (email, password) => request('/api/auth/login', { method: 'POST', body: { email, password }, auth: false }),
  register: (payload) => request('/api/auth/register', { method: 'POST', body: payload, auth: false }),
  me: () => request('/api/auth/me'),
  forgotPassword: (email) => request('/api/auth/forgot-password', { method: 'POST', body: { email }, auth: false }),
  resetPassword: (token, newPassword) =>
    request('/api/auth/reset-password', { method: 'POST', body: { token, newPassword }, auth: false }),

  // Tutors
  searchTutors: (params, signal) => request(`/api/tutors/search${qs(params)}`, { auth: false, signal }),
  subjects: () => request('/api/tutors/subjects', { auth: false }),
  tutor: (id) => request(`/api/tutors/${id}`, { auth: false }),
  myTutorProfile: () => request('/api/tutors/me'),
  saveTutorProfile: (payload) => request('/api/tutors/me', { method: 'PUT', body: payload }),

  // Bookings & payments
  createBooking: (payload) => request('/api/bookings', { method: 'POST', body: payload }),
  myBookings: () => request('/api/bookings/my'),
  incomingBookings: () => request('/api/bookings/incoming'),
  updateBookingStatus: (id, status) => request(`/api/bookings/${id}/status`, { method: 'PATCH', body: { status } }),
  createPaymentOrder: (id) => request(`/api/bookings/${id}/payments/order`, { method: 'POST', body: {} }),
  verifyPayment: (id, payload) => request(`/api/bookings/${id}/payments/verify`, { method: 'POST', body: payload }),

  // Reviews
  ratingSummary: () => request('/api/reviews/summary', { auth: false }),
  tutorReviews: (tutorUserId) => request(`/api/reviews/tutor/${tutorUserId}`, { auth: false }),
  createReview: (payload) => request('/api/reviews', { method: 'POST', body: payload }),

  // Notifications (emails)
  myEmails: () => request('/api/notifications/my'),

  // Admin
  adminUsers: () => request('/api/auth/admin/users'),
  setUserEnabled: (id, enabled) => request(`/api/auth/admin/users/${id}/enabled`, { method: 'PATCH', body: { enabled } }),
  adminBookings: () => request('/api/admin/bookings'),
  adminBookingStats: () => request('/api/admin/bookings/stats'),
  adminCancelBooking: (id) => request(`/api/admin/bookings/${id}/cancel`, { method: 'PATCH' }),
  adminEmails: () => request('/api/notifications/admin'),
  adminEmail: (id) => request(`/api/notifications/${id}`),
  adminMailStatus: () => request('/api/notifications/admin/status'),
  adminSendTestEmail: (to) => request('/api/notifications/admin/test', { method: 'POST', body: { to } }),
}

/** Fetches tutors plus their rating summary and merges them. */
export async function searchTutorsWithRatings(params, signal) {
  const [tutors, ratings] = await Promise.all([
    api.searchTutors(params, signal),
    api.ratingSummary().catch(() => []),
  ])
  const byTutor = new Map((ratings || []).map((rating) => [rating.tutorUserId, rating]))
  return tutors.map((tutor) => ({
    ...tutor,
    averageRating: byTutor.get(tutor.userId)?.averageRating ?? 0,
    reviewCount: byTutor.get(tutor.userId)?.reviewCount ?? 0,
  }))
}
