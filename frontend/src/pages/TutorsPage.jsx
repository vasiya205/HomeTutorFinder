import { useEffect, useMemo, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { RotateCcw, Search, SlidersHorizontal, Users } from 'lucide-react'
import TutorCard from '../components/TutorCard'
import { EmptyState, ErrorBanner, SkeletonCards } from '../components/Feedback'
import { api, searchTutorsWithRatings } from '../lib/api'
import { formatMoneyWhole } from '../lib/format'

const MAX_RATE = 2000

export default function TutorsPage() {
  const [params, setParams] = useSearchParams()
  const [subjects, setSubjects] = useState([])
  const [tutors, setTutors] = useState(null)
  const [error, setError] = useState('')
  const [reload, setReload] = useState(0)
  const [showFilters, setShowFilters] = useState(false)

  const filters = {
    q: params.get('q') || '',
    subject: params.get('subject') || '',
    location: params.get('location') || '',
    maxRate: Number(params.get('maxRate') || MAX_RATE),
    mode: params.get('mode') || '',
    minRating: Number(params.get('minRating') || 0),
    sort: params.get('sort') || 'experience',
  }
  const [keyword, setKeyword] = useState(filters.q)
  const [location, setLocation] = useState(filters.location)

  const update = (changes) => {
    const next = new URLSearchParams(params)
    Object.entries(changes).forEach(([key, value]) => {
      if (value === '' || value === null || value === undefined || (key === 'maxRate' && Number(value) >= MAX_RATE)
        || (key === 'minRating' && Number(value) === 0) || (key === 'sort' && value === 'experience')) next.delete(key)
      else next.set(key, value)
    })
    setParams(next, { replace: true })
  }

  // Debounce the free-text inputs so we do not query on every key stroke.
  useEffect(() => {
    const timer = setTimeout(() => {
      if (keyword !== filters.q || location !== filters.location) update({ q: keyword, location })
    }, 350)
    return () => clearTimeout(timer)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [keyword, location])

  useEffect(() => {
    api.subjects().then(setSubjects).catch(() => {})
  }, [])

  const serverSort = filters.sort === 'rating' ? 'experience' : filters.sort
  useEffect(() => {
    const controller = new AbortController()
    setTutors(null)
    setError('')
    searchTutorsWithRatings({
      q: filters.q, subject: filters.subject, location: filters.location,
      maxRate: filters.maxRate < MAX_RATE ? filters.maxRate : undefined, sort: serverSort,
    }, controller.signal)
      .then(setTutors)
      .catch((loadError) => {
        if (loadError.name !== 'AbortError') {
          setError(loadError.message)
          setTutors([])
        }
      })
    return () => controller.abort()
  }, [filters.q, filters.subject, filters.location, filters.maxRate, serverSort, reload])

  const visible = useMemo(() => {
    let list = (tutors || []).filter((tutor) =>
      (!filters.mode || tutor.teachingMode === filters.mode || tutor.teachingMode === 'BOTH')
      && tutor.averageRating >= filters.minRating)
    if (filters.sort === 'rating') {
      list = list.slice().sort((a, b) => b.averageRating - a.averageRating || b.reviewCount - a.reviewCount)
    }
    return list
  }, [tutors, filters.mode, filters.minRating, filters.sort])

  const reset = () => {
    setKeyword('')
    setLocation('')
    setParams(new URLSearchParams(), { replace: true })
  }

  const activeFilters = [filters.q, filters.subject, filters.location, filters.mode].filter(Boolean).length
    + (filters.maxRate < MAX_RATE ? 1 : 0) + (filters.minRating ? 1 : 0)

  return (
    <div className="page">
      <div className="page-hero">
        <div className="container">
          <p className="eyebrow">Find your learning match</p>
          <h1>Explore tutors</h1>
          <p className="lead">Filter by subject, city, price and teaching mode. Every profile shows real ratings.</p>
          <div className="search-bar">
            <Search size={18} />
            <input value={keyword} onChange={(e) => setKeyword(e.target.value)}
              placeholder="Search by name, subject or qualification..." aria-label="Search tutors" />
          </div>
        </div>
      </div>

      <div className="container listing">
        <aside className={`filters-panel card ${showFilters ? 'open' : ''}`}>
          <div className="filters-head">
            <h3><SlidersHorizontal size={18} /> Filters</h3>
            {activeFilters > 0 && <button type="button" className="link-btn" onClick={reset}><RotateCcw size={14} /> Reset</button>}
          </div>

          <label className="field">
            <span>Subject</span>
            <select value={filters.subject} onChange={(e) => update({ subject: e.target.value })}>
              <option value="">All subjects</option>
              {subjects.map((subject) => <option key={subject} value={subject}>{subject}</option>)}
            </select>
          </label>

          <label className="field">
            <span>City / area</span>
            <input value={location} onChange={(e) => setLocation(e.target.value)} placeholder="e.g. Hyderabad" />
          </label>

          <label className="field">
            <span>Max hourly rate <strong className="accent">{filters.maxRate >= MAX_RATE ? 'Any' : formatMoneyWhole(filters.maxRate)}</strong></span>
            <input type="range" min={200} max={MAX_RATE} step={50} value={filters.maxRate}
              onChange={(e) => update({ maxRate: e.target.value })} />
          </label>

          <fieldset className="field">
            <legend>Teaching mode</legend>
            <div className="segmented">
              {[['', 'Any'], ['HOME', 'Home'], ['ONLINE', 'Online']].map(([value, label]) => (
                <button key={label} type="button" className={filters.mode === value ? 'active' : ''}
                  onClick={() => update({ mode: value })}>{label}</button>
              ))}
            </div>
          </fieldset>

          <fieldset className="field">
            <legend>Minimum rating</legend>
            <div className="segmented">
              {[[0, 'Any'], [4, '4+'], [4.5, '4.5+']].map(([value, label]) => (
                <button key={label} type="button" className={filters.minRating === value ? 'active' : ''}
                  onClick={() => update({ minRating: value })}>{label}</button>
              ))}
            </div>
          </fieldset>
        </aside>

        <section className="results">
          <div className="results-bar">
            <p><strong>{tutors === null ? '...' : visible.length}</strong> {visible.length === 1 ? 'tutor' : 'tutors'} found</p>
            <div className="results-actions">
              <button type="button" className="btn btn-ghost btn-sm filters-toggle" onClick={() => setShowFilters((v) => !v)}>
                <SlidersHorizontal size={16} /> Filters {activeFilters > 0 && `(${activeFilters})`}
              </button>
              <label className="sort-select">
                <span className="sr-only">Sort by</span>
                <select value={filters.sort} onChange={(e) => update({ sort: e.target.value })}>
                  <option value="experience">Most experienced</option>
                  <option value="rating">Highest rated</option>
                  <option value="rate_asc">Price: low to high</option>
                  <option value="rate_desc">Price: high to low</option>
                  <option value="newest">Newest</option>
                </select>
              </label>
            </div>
          </div>

          <ErrorBanner message={error} onRetry={() => setReload((n) => n + 1)} />

          {tutors === null ? <SkeletonCards count={6} /> : visible.length === 0 ? (
            <EmptyState icon={Users} title="No tutors match these filters"
              action={<button type="button" className="btn btn-primary" onClick={reset}>Clear filters</button>}>
              Try a different subject or city, or raise the maximum rate.
            </EmptyState>
          ) : (
            <div className="tutor-grid">
              {visible.map((tutor) => <TutorCard key={tutor.id} tutor={tutor} />)}
            </div>
          )}
        </section>
      </div>
    </div>
  )
}
