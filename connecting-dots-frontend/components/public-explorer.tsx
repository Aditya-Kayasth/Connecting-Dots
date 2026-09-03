'use client'

import { useEffect, useMemo, useState } from 'react'
import { useRouter } from 'next/navigation'
import { SkeletonCard } from '@/components/skeleton-card'
import { apiRequest, getAuthRole, getAuthToken } from '@/lib/api-client'

type Problem = {
  id: string
  title: string
  description: string
  domain: string
  status: string
  ngoProfile?: {
    id: string
    organizationName: string
  }
}

type NGO = {
  id: string
  organizationName: string
  domain: string
  contactNumber?: string
  preferredLanguage?: string
  isVerified?: boolean
}

type Contributor = {
  id: string
  firstName: string
  lastName: string
  skillsSummary?: string
  portfolioUrl?: string
  title?: string
  location?: string
}

function Badge({ status }: { status: string }) {
  return <span className={`status-badge status-${status.toLowerCase()}`}>{status}</span>
}

export default function PublicExplorer() {
  const router = useRouter()
  
  const [problems, setProblems] = useState<Problem[]>([])
  const [ngos, setNgos] = useState<NGO[]>([])
  const [contributors, setContributors] = useState<Contributor[]>([])
  
  const [domain, setDomain] = useState('All domains')
  const [loading, setLoading] = useState(true)
  const [selectedNgoName, setSelectedNgoName] = useState<string | null>(null)
  
  const [problemDetail, setProblemDetail] = useState<Problem | null>(null)

  // Auth & applying states
  const [role, setRole] = useState<string | null>(null)
  const [token, setToken] = useState<string | null>(null)
  const [contributorProfile, setContributorProfile] = useState<any | null>(null)
  const [appliedIds, setAppliedIds] = useState<Set<string>>(new Set())
  const [applying, setApplying] = useState(false)
  const [mounted, setMounted] = useState(false)

  useEffect(() => {
    setMounted(true)
    setRole(getAuthRole())
    setToken(getAuthToken())
  }, [])

  useEffect(() => {
    Promise.all([
      apiRequest<any>('/api/v1/core/problem-statements?status=OPEN'),
      apiRequest<NGO[]>('/api/v1/core/profiles/ngos'),
      apiRequest<Contributor[]>('/api/v1/core/profiles/contributors')
    ])
      .then(([problemsPage, ngosData, contributorsData]) => {
        const probs = Array.isArray(problemsPage?.content) ? problemsPage.content : []
        setProblems(probs)
        setNgos(Array.isArray(ngosData) ? ngosData : [])
        setContributors(Array.isArray(contributorsData) ? contributorsData : [])
      })
      .catch((err) => console.error('Failed to load explorer directories:', err))
      .finally(() => setLoading(false))
  }, [])

  // Fetch logged-in contributor's details and their applications
  useEffect(() => {
    if (!mounted || !token || role !== 'CONTRIBUTOR') return

    const fetchContributorApplications = async () => {
      try {
        const profile = await apiRequest<any>('/api/v1/core/profiles/contributor/me')
        setContributorProfile(profile)
        if (profile?.id) {
          const apps = await apiRequest<any[]>(`/api/v1/core/applications/contributor/${profile.id}`)
          if (Array.isArray(apps)) {
            setAppliedIds(new Set(apps.map(a => a.problemId)))
          }
        }
      } catch (err) {
        console.error('Failed to load contributor applications in public explorer:', err)
      }
    }

    fetchContributorApplications()
  }, [mounted, token, role])

  const filteredProblems = useMemo(() => {
    return problems.filter(p => {
      const matchDomain = domain === 'All domains' || p.domain === domain
      const ngoName = p.ngoProfile?.organizationName || 'NGO Partner'
      const matchNgo = !selectedNgoName || ngoName === selectedNgoName
      return matchDomain && matchNgo
    })
  }, [problems, domain, selectedNgoName])

  const domains = useMemo(() => {
    const list = new Set(problems.map(p => p.domain))
    return ['All domains', ...Array.from(list)]
  }, [problems])

  const handleApply = async (problemId: string) => {
    if (!contributorProfile?.id) return
    setApplying(true)
    try {
      await apiRequest('/api/v1/core/applications', {
        method: 'POST',
        body: {
          problemId,
          contributorProfileId: contributorProfile.id
        }
      })
      setAppliedIds(prev => new Set([...prev, problemId]))
      alert('Application submitted successfully!')
    } catch (err: any) {
      console.error('Failed to apply:', err)
      alert(err.message || 'Failed to submit application. Please try again.')
    } finally {
      setApplying(false)
    }
  }

  if (!mounted) return null

  return (
    <div className="explorer">
      {/* 01 / Problems Section */}
      <section className="explorer-section" id="problems">
        <div className="section-heading">
          <div>
            <span className="eyebrow">01 / Technical opportunities</span>
            <h2>Problem statements</h2>
          </div>
          
          <div className="filter-controls">
            {selectedNgoName && (
              <button 
                className="filter-pill active" 
                onClick={() => setSelectedNgoName(null)}
              >
                NGO: {selectedNgoName} ✕
              </button>
            )}
            <select 
              value={domain} 
              onChange={e => setDomain(e.target.value)}
              className="domain-select"
            >
              {domains.map(d => (
                <option key={d} value={d}>{d}</option>
              ))}
            </select>
          </div>
        </div>

        {loading ? (
          <div className="problem-grid">
            <SkeletonCard />
            <SkeletonCard />
          </div>
        ) : filteredProblems.length === 0 ? (
          <div className="empty-state">No matching problem statements found.</div>
        ) : (
          <div className="problem-grid">
            {filteredProblems.map(p => (
              <article 
                className="problem-card clickable-card" 
                key={p.id}
                onClick={() => setProblemDetail(p)}
              >
                <div className="card-meta">
                  <span>{p.ngoProfile?.organizationName || 'NGO Partner'}</span>
                  <Badge status={p.status} />
                </div>
                <h3>{p.title}</h3>
                <p>{p.description.substring(0, 140)}...</p>
                <div className="card-footer">
                  <span className="domain-tag">{p.domain}</span>
                  <span className="text-link">Read details →</span>
                </div>
              </article>
            ))}
          </div>
        )}
      </section>

      {/* 02 / NGOs Section */}
      <section className="explorer-section" id="ngos">
        <div className="section-heading">
          <div>
            <span className="eyebrow">02 / Community partners</span>
            <h2>NGO directory</h2>
          </div>
          <span className="section-count">{ngos.length} organizations</span>
        </div>
        
        {loading ? (
          <div className="directory-grid">
            <SkeletonCard />
            <SkeletonCard />
          </div>
        ) : ngos.length === 0 ? (
          <div className="empty-state">No NGO profiles created yet.</div>
        ) : (
          <div className="directory-grid">
            {ngos.map(o => {
              const isSelected = selectedNgoName === o.organizationName
              return (
                <article 
                  className={`directory-card clickable-card ${isSelected ? 'directory-selected' : ''}`} 
                  key={o.id} 
                  onClick={() => router.push(`/profile/ngo/${o.id}`)}
                >
                  <div className="avatar avatar-green">
                    {(o.organizationName || 'N').split(' ').map(w => w[0]).join('').slice(0, 2).toUpperCase()}
                  </div>
                  <div>
                    <div className="card-meta">
                      <h3>{o.organizationName}</h3>
                      {o.isVerified && <span className="status-badge status-open" style={{ padding: '2px 6px', fontSize: '0.7rem' }}>VERIFIED</span>}
                    </div>
                    <p>{o.domain}</p>
                    <div className="directory-detail">{o.contactNumber || 'No contact provided'}</div>
                  </div>
                </article>
              )
            })}
          </div>
        )}
      </section>

      {/* 03 / Contributors Section */}
      <section className="explorer-section" id="contributors">
        <div className="section-heading">
          <div>
            <span className="eyebrow">03 / Technical community</span>
            <h2>Contributors</h2>
          </div>
          <span className="section-count">{contributors.length} profiles</span>
        </div>
        
        {loading ? (
          <div className="directory-grid">
            <SkeletonCard />
            <SkeletonCard />
          </div>
        ) : contributors.length === 0 ? (
          <div className="empty-state">No contributor profiles created yet.</div>
        ) : (
          <div className="directory-grid">
            {contributors.map(c => {
              const fullName = `${c.firstName || ''} ${c.lastName || ''}`.trim() || 'Contributor'
              const skills = c.skillsSummary ? c.skillsSummary.split(',').map(s => s.trim()) : []
              return (
                <article 
                  className="directory-card clickable-card" 
                  key={c.id} 
                  onClick={() => router.push(`/profile/contributor/${c.id}`)}
                >
                  <div className="avatar avatar-gold">
                    {fullName.split(' ').map(w => w[0]).join('').slice(0, 2).toUpperCase()}
                  </div>
                  <div>
                    <div className="card-meta">
                      <h3>{fullName}</h3>
                      <span className="availability">Available</span>
                    </div>
                    <p>{c.title || 'Technical Contributor'} · {c.location || 'Community Member'}</p>
                    <div className="tag-row" style={{ marginTop: '0.5rem' }}>
                      {skills.slice(0, 4).map(s => <span key={s}>#{s}</span>)}
                    </div>
                  </div>
                </article>
              )
            })}
          </div>
        )}
      </section>

      {/* Problem Drawer Modal */}
      {problemDetail && (
        <div className="modal-backdrop" role="dialog" aria-modal="true">
          <section className="detail-drawer">
            <button className="close-button" onClick={() => setProblemDetail(null)} aria-label="Close problem">×</button>
            <span className="eyebrow" style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', flexWrap: 'wrap' }}>
              <strong>{problemDetail.ngoProfile?.organizationName || 'NGO Partner'}</strong>
              {problemDetail.ngoProfile?.id && (
                <a 
                  href={`/profile/ngo/${problemDetail.ngoProfile.id}`}
                  className="text-link"
                  style={{ fontSize: '0.85rem' }}
                >
                  (View NGO Profile)
                </a>
              )}
              <span>· {problemDetail.domain}</span>
            </span>
            <h2>{problemDetail.title}</h2>
            <p style={{ marginTop: '1.5rem', marginBottom: '1.5rem', lineHeight: '1.6' }}>{problemDetail.description}</p>
            <Badge status={problemDetail.status} />

            <div style={{ marginTop: '2rem' }}>
              {!token ? (
                <button 
                  className="primary-button" 
                  onClick={() => {
                    setProblemDetail(null)
                    window.dispatchEvent(new CustomEvent('dots:open-auth'))
                  }}
                >
                  Sign in to apply →
                </button>
              ) : role === 'CONTRIBUTOR' ? (
                appliedIds.has(problemDetail.id) ? (
                  <button className="primary-button" disabled style={{ opacity: 0.6, cursor: 'not-allowed' }}>
                    Applied ✓
                  </button>
                ) : (
                  <button 
                    className="primary-button" 
                    disabled={applying}
                    onClick={() => handleApply(problemDetail.id)}
                  >
                    {applying ? 'Applying...' : 'Apply to this problem →'}
                  </button>
                )
              ) : null}
            </div>
          </section>
        </div>
      )}
    </div>
  )
}
