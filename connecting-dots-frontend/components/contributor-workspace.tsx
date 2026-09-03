'use client'

import { useMemo, useState, useEffect } from 'react'
import { useRouter } from 'next/navigation'
import { apiRequest, getAuthUserId } from '@/lib/api-client'

type Problem = {
  id: string
  title: string
  description: string
  domain: string
  ngoProfile?: {
    organizationName: string
  }
}

type Application = {
  id: string
  problemId: string
  status: string
  date?: string
  problemTitle?: string
  ngoName?: string
}

export default function ContributorWorkspace() {
  const [profile, setProfile] = useState<any>(null)
  const [problems, setProblems] = useState<Problem[]>([])
  const [applications, setApplications] = useState<Application[]>([])
  const [filter, setFilter] = useState('All domains')
  const [loading, setLoading] = useState(true)
  const [reviews, setReviews] = useState<any[]>([])
  const [expandedProblemIds, setExpandedProblemIds] = useState<Set<string>>(new Set())

  const router = useRouter()

  const toggleExpand = (problemId: string) => {
    setExpandedProblemIds((prev) => {
      const next = new Set(prev)
      if (next.has(problemId)) {
        next.delete(problemId)
      } else {
        next.add(problemId)
      }
      return next
    })
  }

  const withdrawApplication = async (appId: string) => {
    if (!confirm('Are you sure you want to withdraw/cancel this application?')) return
    try {
      await apiRequest(`/api/v1/core/applications/${appId}/status`, {
        method: 'PUT',
        body: { status: 'WITHDRAWN' }
      })
      alert('✓ Application withdrawn successfully.')
      fetchWorkspaceData()
    } catch (err) {
      console.error('Failed to withdraw application:', err)
      alert('Failed to withdraw application.')
    }
  }

  useEffect(() => {
    if (typeof window !== 'undefined') {
      const token = sessionStorage.getItem('auth_token')
      const role = sessionStorage.getItem('auth_role')
      if (!token || role !== 'CONTRIBUTOR') {
        router.push('/')
      }
    }
  }, [router])

  const fetchWorkspaceData = async () => {
    try {
      // 1. Fetch contributor profile
      const prof = await apiRequest<any>('/api/v1/core/profiles/contributor/me')
      setProfile(prof)

      if (prof && prof.id) {
        // 2. Fetch problems, applications, and reviews in parallel
        const uid = getAuthUserId()
        const [problemsData, appsData, reviewsData] = await Promise.all([
          apiRequest<{ content: Problem[] }>('/api/v1/core/problem-statements?status=OPEN'),
          apiRequest<Application[]>(`/api/v1/core/applications/contributor/${prof.id}`),
          uid ? apiRequest<any[]>(`/api/v1/core/users/${uid}/reviews`) : Promise.resolve([])
        ])

        setProblems(Array.isArray(problemsData?.content) ? problemsData.content : [])
        setApplications(Array.isArray(appsData) ? appsData : [])
        setReviews(Array.isArray(reviewsData) ? reviewsData : [])
      }
    } catch (err) {
      console.error('Failed to fetch workspace data:', err)
    } finally {
      setLoading(false)
    }
  }

  const avgRating = useMemo(() => {
    if (!reviews || reviews.length === 0) return '5.0'
    const total = reviews.reduce((acc, r) => acc + r.rating, 0)
    return (total / reviews.length).toFixed(1)
  }, [reviews])

  useEffect(() => {
    fetchWorkspaceData()
  }, [])

  const appliedIds = useMemo(() => {
    return new Set(applications.map((app) => app.problemId))
  }, [applications])

  const filtered = useMemo(() => {
    return filter === 'All domains'
      ? problems
      : problems.filter((p) => p.domain === filter)
  }, [filter, problems])

  // Get distinct domains for the dropdown
  const domains = useMemo(() => {
    const set = new Set(problems.map((p) => p.domain).filter(Boolean))
    return ['All domains', ...Array.from(set)]
  }, [problems])

  const applyToProblem = async (problemId: string) => {
    if (!profile) return
    try {
      await apiRequest('/api/v1/core/applications', {
        method: 'POST',
        body: {
          problemId,
          contributorProfileId: profile.id
        }
      })
      alert('✓ Application submitted successfully!')
      // Refresh applications list
      const appsData = await apiRequest<Application[]>(`/api/v1/core/applications/contributor/${profile.id}`)
      setApplications(Array.isArray(appsData) ? appsData : [])
    } catch (err) {
      console.error('Failed to apply:', err)
      alert(err instanceof Error ? err.message : 'Failed to submit application.')
    }
  }

  const initials = profile
    ? `${profile.firstName?.[0] || ''}${profile.lastName?.[0] || ''}`.toUpperCase()
    : 'CD'

  const fullName = profile
    ? `${profile.firstName || ''} ${profile.lastName || ''}`
    : 'Guest Contributor'

  if (loading) {
    return (
      <main className="workspace-shell">
        <div className="empty-state">Loading contributor workspace...</div>
      </main>
    )
  }

  return (
    <main className="workspace-shell">
      <header className="workspace-header">
        <div>
          <span className="eyebrow">Contributor workspace</span>
          <h1>Put your skills where they can move something forward.</h1>
          <p>Browse open problems, follow your applications, and build a reputation through work that matters.</p>
        </div>
        <div className="workspace-user">
          <div className="avatar avatar-green">{initials || 'CD'}</div>
          <div>
            <strong>{fullName}</strong>
            <small>{profile?.title || 'Technical Contributor'}</small>
          </div>
        </div>
      </header>

      <div className="contributor-layout">
        <section>
          <div className="section-heading">
            <div>
              <span className="eyebrow">Open problems</span>
              <h2>Find a meaningful next project</h2>
            </div>
            <select value={filter} onChange={(e) => setFilter(e.target.value)} aria-label="Filter by domain">
              {domains.map((d) => (
                <option key={d}>{d}</option>
              ))}
            </select>
          </div>

          <div
            className="problem-grid"
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fill, minmax(320px, 1fr))',
              gap: '1.5rem'
            }}
          >
            {filtered.length === 0 ? (
              <div className="empty-state">No open problems found matching your filters.</div>
            ) : (
              filtered.map((p) => {
                const isExpanded = expandedProblemIds.has(p.id)
                return (
                  <article
                    className="problem-card"
                    key={p.id}
                    style={{
                      display: 'flex',
                      flexDirection: 'column',
                      justifyContent: 'space-between',
                      minHeight: '260px'
                    }}
                  >
                    <div>
                      <div className="card-meta">
                        <span>{p.domain}</span>
                        <span className="status-badge status-open">OPEN</span>
                      </div>
                      <h3 style={{ fontSize: '1.1rem', marginBottom: '0.5rem', lineHeight: '1.4' }}>{p.title}</h3>
                      <p
                        style={
                          isExpanded
                            ? { color: 'var(--muted)', fontSize: '0.9rem', lineHeight: '1.5', whiteSpace: 'pre-wrap' }
                            : {
                                display: '-webkit-box',
                                WebkitLineClamp: 3,
                                WebkitBoxOrient: 'vertical',
                                overflow: 'hidden',
                                textOverflow: 'ellipsis',
                                color: 'var(--muted)',
                                fontSize: '0.9rem',
                                lineHeight: '1.5'
                              }
                        }
                      >
                        {p.description}
                      </p>
                      <button
                        onClick={() => toggleExpand(p.id)}
                        style={{
                          background: 'none',
                          border: 'none',
                          color: '#22c55e',
                          fontSize: '0.8rem',
                          cursor: 'pointer',
                          padding: 0,
                          marginTop: '0.4rem',
                          fontWeight: 600,
                          display: 'inline-block'
                        }}
                      >
                        {isExpanded ? 'Collapse ↑' : 'Read full brief ↓'}
                      </button>
                    </div>
                    <div>
                      <div className="tag-row" style={{ marginTop: '0.75rem', marginBottom: '1rem' }}>
                        <span>#{p.domain}</span>
                      </div>
                      <div className="card-footer" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                        <span style={{ fontSize: '0.85rem', fontWeight: 500 }}>{p.ngoProfile?.organizationName || 'Connecting Dots NGO'}</span>
                        {appliedIds.has(p.id) ? (
                          <span className="published-note" style={{ color: '#22c55e', fontWeight: 600 }}>Applied ✓</span>
                        ) : (
                          <button className="primary-button" style={{ padding: '0.35rem 0.85rem', fontSize: '0.85rem' }} onClick={() => applyToProblem(p.id)}>
                            Apply →
                          </button>
                        )}
                      </div>
                    </div>
                  </article>
                )
              })
            )}
          </div>

          <div className="section-heading dashboard-section-heading">
            <div>
              <span className="eyebrow">Your activity</span>
              <h2>My applications</h2>
            </div>
            <span className="section-count">{applications.length} total</span>
          </div>

          <div className="application-list">
            {applications.length === 0 ? (
              <div className="empty-state">You have not submitted any applications yet.</div>
            ) : (
              applications.map((a) => (
                <div className="application-row" key={a.id} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <a href={`/applications/${a.id}`} style={{ textDecoration: 'none', color: 'inherit', flexGrow: 1 }}>
                    <div>
                      <strong>{a.problemTitle || 'Civic Tech Solution'}</strong>
                      <small>{a.ngoName || 'NGO Partner'} · Application ID: {a.id.slice(0, 8)}</small>
                    </div>
                  </a>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                    <span className={`status-badge status-${a.status.toLowerCase()}`}>{a.status}</span>
                    {(a.status === 'PENDING' || a.status === 'ACCEPTED') && (
                      <button
                        className="outline-button"
                        style={{ padding: '0.25rem 0.6rem', fontSize: '0.75rem', color: '#ef4444', borderColor: 'rgba(239, 68, 68, 0.4)' }}
                        onClick={(e) => {
                          e.preventDefault()
                          e.stopPropagation()
                          withdrawApplication(a.id)
                        }}
                      >
                        Withdraw
                      </button>
                    )}
                    <a href={`/applications/${a.id}`} className="row-arrow" style={{ textDecoration: 'none' }}>→</a>
                  </div>
                </div>
              ))
            )}
          </div>
        </section>

        <aside className="reputation-card">
          <span className="eyebrow">Contributor Profile</span>
          {reviews.length > 0 ? (
            <>
              <div className="reputation-score">{avgRating}<span>/5</span></div>
              <p>Based on {reviews.length} review{reviews.length > 1 ? 's' : ''} from NGO partners.</p>
            </>
          ) : (
            <div style={{ margin: '1rem 0' }}>
              <strong style={{ fontSize: '1.2rem', display: 'block' }}>Verified Member</strong>
              <small className="muted">No reviews received yet.</small>
            </div>
          )}
          <div className="reputation-line">
            <span>Projects completed</span>
            <strong>{profile?.completedProjects || 0}</strong>
          </div>
          <div className="reputation-line">
            <span>Preferred language</span>
            <strong>{profile?.preferredLanguage?.toUpperCase() || 'EN'}</strong>
          </div>
          <div className="reputation-line">
            <span>Location</span>
            <strong>{profile?.location || 'Community Member'}</strong>
          </div>
          <a className="text-link" href={profile?.id ? `/profile/contributor/${profile.id}` : "/profile"} style={{ marginTop: '1rem', display: 'inline-block' }}>
            View public profile →
          </a>
        </aside>
      </div>
    </main>
  )
}
