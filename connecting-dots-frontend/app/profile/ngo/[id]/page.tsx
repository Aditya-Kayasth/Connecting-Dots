'use client'

import { useEffect, useState } from 'react'
import { useParams, useRouter } from 'next/navigation'
import { apiRequest } from '@/lib/api-client'

type NGO = {
  id: string
  organizationName: string
  domain: string
  contactNumber?: string
  preferredLanguage?: string
  location?: string
  isVerified?: boolean
  user?: {
    id: string
    email: string
  }
}

type Problem = {
  id: string
  title: string
  description: string
  domain: string
  status: string
}

export default function NgoPublicProfile() {
  const params = useParams()
  const router = useRouter()
  const id = params.id as string

  const [ngo, setNgo] = useState<NGO | null>(null)
  const [problems, setProblems] = useState<Problem[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!id) return

    const fetchNgoData = async () => {
      try {
        const data = await apiRequest<NGO>(`/api/v1/core/profiles/ngo/${id}`)
        setNgo(data)

        // Fetch all problems and filter for this NGO
        const problemsPage = await apiRequest<{ content: Problem[] }>('/api/v1/core/problem-statements?status=')
        const allProblems = Array.isArray(problemsPage?.content) ? problemsPage.content : []
        const ngoProblems = allProblems.filter((p: any) => p.ngoProfile?.id === id)
        setProblems(ngoProblems)
      } catch (err: any) {
        console.error('Failed to load NGO profile:', err)
        setError(err.message || 'Failed to load profile details.')
      } finally {
        setLoading(false)
      }
    }

    fetchNgoData()
  }, [id])

  if (loading) {
    return (
      <main className="page-shell">
        <div className="profile-content" style={{ padding: '4rem', textAlign: 'center' }}>
          <div className="empty-state">Loading NGO profile...</div>
        </div>
      </main>
    )
  }

  if (error || !ngo) {
    return (
      <main className="page-shell">
        <div className="profile-content" style={{ padding: '4rem', textAlign: 'center' }}>
          <div className="empty-state" style={{ color: '#ef4444' }}>
            Error: {error || 'NGO profile not found.'}
          </div>
          <button className="outline-button" onClick={() => router.push('/')} style={{ marginTop: '1.5rem' }}>
            Back to explore
          </button>
        </div>
      </main>
    )
  }

  return (
    <main className="page-shell">
      <div className="profile-content">
        <a className="back-link" href="/">← Back to explore</a>
          
          <section className="profile-hero settings-hero">
            <div className="profile-avatar dynamic-avatar" style={{ background: 'var(--green-color-muted, #10b981)' }}>
              {(ngo.organizationName || 'N').split(' ').map(w => w[0]).join('').slice(0, 2).toUpperCase()}
            </div>
            <div>
              <span className="eyebrow" style={{ color: 'var(--green-color, #10b981)' }}>
                NGO Partner {ngo.isVerified && '· VERIFIED'}
              </span>
              <h1>{ngo.organizationName}</h1>
              <p className="muted">Primary focus area: {ngo.domain}</p>
            </div>
          </section>

          <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr', gap: '2.5rem', marginTop: '2rem' }}>
            <div>
              <div className="section-heading" style={{ marginBottom: '1.5rem' }}>
                <div>
                  <span className="eyebrow">Opportunities</span>
                  <h2>Problem statements posted</h2>
                </div>
                <span className="count-pill">{problems.length} listed</span>
              </div>

              <div className="problem-grid" style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
                {problems.length === 0 ? (
                  <div className="empty-state" style={{ border: '1px dashed rgba(255,255,255,0.1)', padding: '2rem', borderRadius: '8px' }}>
                    No problem statements listed by this NGO yet.
                  </div>
                ) : (
                  problems.map(p => (
                    <article className="problem-card" key={p.id} style={{ border: '1px solid rgba(255,255,255,0.06)' }}>
                      <div className="card-meta">
                        <span>{p.domain}</span>
                        <span className={`status-badge status-${p.status.toLowerCase()}`}>{p.status}</span>
                      </div>
                      <h3>{p.title}</h3>
                      <p style={{ fontSize: '0.9rem', lineHeight: '1.5', margin: '0.75rem 0' }}>{p.description}</p>
                    </article>
                  ))
                )}
              </div>
            </div>

            <aside>
              <div style={{ padding: '1.5rem', background: 'rgba(255,255,255,0.03)', border: '1px solid rgba(255,255,255,0.06)', borderRadius: '8px' }}>
                <h3 style={{ fontSize: '1.1rem', marginBottom: '1.25rem' }}>Organization Details</h3>
                
                <div style={{ marginBottom: '1rem' }}>
                  <span className="eyebrow" style={{ fontSize: '0.75rem' }}>Focus domain</span>
                  <p style={{ margin: '0.25rem 0 0 0', fontWeight: 'bold' }}>{ngo.domain}</p>
                </div>

                <div style={{ marginBottom: '1rem' }}>
                  <span className="eyebrow" style={{ fontSize: '0.75rem' }}>Contact phone</span>
                  <p style={{ margin: '0.25rem 0 0 0', color: 'rgba(255,255,255,0.8)' }}>{ngo.contactNumber || 'No contact provided'}</p>
                </div>

                {ngo.user?.email && (
                  <div style={{ marginBottom: '1rem' }}>
                    <span className="eyebrow" style={{ fontSize: '0.75rem' }}>Contact email</span>
                    <p style={{ margin: '0.25rem 0 0 0', color: 'rgba(255,255,255,0.8)' }}>{ngo.user.email}</p>
                  </div>
                )}

                <div style={{ marginBottom: '1rem' }}>
                  <span className="eyebrow" style={{ fontSize: '0.75rem' }}>Location</span>
                  <p style={{ margin: '0.25rem 0 0 0', color: 'rgba(255,255,255,0.8)' }}>{ngo.location || 'Global Community'}</p>
                </div>

                <div>
                  <span className="eyebrow" style={{ fontSize: '0.75rem' }}>Preferred language</span>
                  <p style={{ margin: '0.25rem 0 0 0', textTransform: 'capitalize', color: 'rgba(255,255,255,0.8)' }}>
                    {ngo.preferredLanguage === 'sw' ? 'Swahili' : ngo.preferredLanguage === 'hi' ? 'Hindi' : ngo.preferredLanguage === 'mr' ? 'Marathi' : 'English'}
                  </p>
                </div>
              </div>
            </aside>
          </div>
        </div>
      </main>
  )
}
