'use client'

import { useEffect, useState } from 'react'
import { useParams, useRouter } from 'next/navigation'
import { apiRequest } from '@/lib/api-client'
import ReviewsList from '@/components/reviews-list'

type Contributor = {
  id: string
  firstName: string
  lastName: string
  skillsSummary?: string
  portfolioUrl?: string
  title?: string
  location?: string
  preferredLanguage?: string
  completedProjects?: number
  user?: {
    id: string
    email: string
  }
}

export default function ContributorPublicProfile() {
  const params = useParams()
  const router = useRouter()
  const id = params.id as string

  const [contributor, setContributor] = useState<Contributor | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!id) return

    const fetchContributorData = async () => {
      try {
        const data = await apiRequest<Contributor>(`/api/v1/core/profiles/contributor/${id}`)
        setContributor(data)
      } catch (err: any) {
        console.error('Failed to load contributor profile:', err)
        setError(err.message || 'Failed to load profile details.')
      } finally {
        setLoading(false)
      }
    }

    fetchContributorData()
  }, [id])

  if (loading) {
    return (
      <main className="page-shell">
        <div className="profile-content" style={{ padding: '4rem', textAlign: 'center' }}>
          <div className="empty-state">Loading contributor profile...</div>
        </div>
      </main>
    )
  }

  if (error || !contributor) {
    return (
      <main className="page-shell">
        <div className="profile-content" style={{ padding: '4rem', textAlign: 'center' }}>
          <div className="empty-state" style={{ color: '#ef4444' }}>
            Error: {error || 'Contributor profile not found.'}
          </div>
          <button className="outline-button" onClick={() => router.push('/')} style={{ marginTop: '1.5rem' }}>
            Back to explore
          </button>
        </div>
      </main>
    )
  }

  const fullName = `${contributor.firstName || ''} ${contributor.lastName || ''}`.trim() || 'Contributor'
  const skills = contributor.skillsSummary ? contributor.skillsSummary.split(',').map(s => s.trim()) : []

  return (
    <main className="page-shell">
      <div className="profile-content">
        <a className="back-link" href="/">← Back to explore</a>
          
          <section className="profile-hero settings-hero">
            <div className="profile-avatar dynamic-avatar" style={{ background: 'var(--gold-color-muted, #f59e0b)' }}>
              {fullName.split(' ').map(w => w[0]).join('').slice(0, 2).toUpperCase()}
            </div>
            <div>
              <span className="eyebrow" style={{ color: 'var(--gold-color, #eab308)' }}>Technical Contributor</span>
              <h1>{fullName}</h1>
              {contributor.user?.email && (
                <p className="muted">{contributor.user.email}</p>
              )}
            </div>
          </section>

          <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr', gap: '2.5rem', marginTop: '2rem' }}>
            <div>
              <ReviewsList userId={contributor.user?.id} />
            </div>

            <aside>
              <div style={{ padding: '1.5rem', background: 'rgba(255,255,255,0.03)', border: '1px solid rgba(255,255,255,0.06)', borderRadius: '8px', marginBottom: '1.5rem' }}>
                <h3 style={{ fontSize: '1.1rem', marginBottom: '1.25rem' }}>Skills & Portfolio</h3>
                
                <div style={{ marginBottom: '1.25rem' }}>
                  <span className="eyebrow" style={{ fontSize: '0.75rem', display: 'block', marginBottom: '0.5rem' }}>Skills summary</span>
                  <div className="tag-row" style={{ flexWrap: 'wrap' }}>
                    {skills.length === 0 ? (
                      <span className="muted" style={{ fontSize: '0.9rem' }}>No skills listed yet.</span>
                    ) : (
                      skills.map(s => <span key={s}>#{s}</span>)
                    )}
                  </div>
                </div>

                <div>
                  <span className="eyebrow" style={{ fontSize: '0.75rem' }}>Preferred language</span>
                  <p style={{ margin: '0.25rem 0 0 0', textTransform: 'capitalize', color: 'rgba(255,255,255,0.8)' }}>
                    {contributor.preferredLanguage === 'sw' ? 'Swahili' : contributor.preferredLanguage === 'hi' ? 'Hindi' : contributor.preferredLanguage === 'mr' ? 'Marathi' : 'English'}
                  </p>
                </div>
              </div>

              <div style={{ padding: '1.5rem', background: 'rgba(255,255,255,0.03)', border: '1px solid rgba(255,255,255,0.06)', borderRadius: '8px' }}>
                <h3 style={{ fontSize: '1.1rem', marginBottom: '1rem' }}>Reputation Statistics</h3>
                <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
                  <div style={{ background: 'rgba(255,255,255,0.05)', borderRadius: '6px', padding: '0.75rem 1.25rem', textAlign: 'center' }}>
                    <span className="eyebrow" style={{ fontSize: '0.7rem' }}>Completed</span>
                    <p style={{ margin: '0.25rem 0 0 0', fontWeight: 'bold', fontSize: '1.5rem' }}>{contributor.completedProjects || 0}</p>
                  </div>
                </div>
              </div>
            </aside>
          </div>
        </div>
      </main>
  )
}
