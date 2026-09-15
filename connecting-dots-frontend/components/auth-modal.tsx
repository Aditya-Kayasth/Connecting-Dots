'use client'
import { useEffect, useState } from 'react'
import { apiPost } from '@/lib/api-client'

export default function AuthModal({
  open,
  onClose,
  onSuccess
}: {
  open: boolean
  onClose: () => void
  onSuccess: (token: string, role: string, userId: string, email: string) => void
}) {
  const [mode, setMode] = useState<'signin' | 'register'>('signin')
  const [role, setRole] = useState<'CONTRIBUTOR' | 'NGO'>('CONTRIBUTOR')
  const [form, setForm] = useState({
    name: '',
    organizationName: '',
    domain: 'Education & Technology',
    email: '',
    password: '',
    contactNumber: '',
    location: '',
    preferredLanguage: 'en',
    title: '',
    skillsSummary: '',
    portfolioUrl: ''
  })
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    if (open) {
      document.body.classList.add('modal-open')
    } else {
      document.body.classList.remove('modal-open')
    }
    return () => document.body.classList.remove('modal-open')
  }, [open])

  if (!open) return null

  const change = (key: keyof typeof form) => (
    e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>
  ) => setForm({ ...form, [key]: e.target.value })

  function validatePhone(phone: string): boolean {
    if (!phone || !phone.trim()) return true
    return /^[0-9]{10}$/.test(phone.trim())
  }

  async function handleQuickLogin(demoEmail: string, demoPass: string) {
    setError('')
    setLoading(true)
    setMode('signin')
    setForm(f => ({ ...f, email: demoEmail, password: demoPass }))

    try {
      const data = await apiPost<any>('/api/v1/core/auth/login', {
        email: demoEmail,
        password: demoPass
      })

      const token = data.token || data.accessToken || data.data?.token
      if (!token) throw new Error('No session token returned')

      onSuccess(
        token,
        data.role || data.data?.role || 'CONTRIBUTOR',
        data.userId || '',
        demoEmail
      )
      onClose()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Unable to sign in with demo credentials.')
    } finally {
      setLoading(false)
    }
  }

  async function submit(e: React.FormEvent) {
    e.preventDefault()
    setError('')

    if (mode === 'register' && form.contactNumber && !validatePhone(form.contactNumber)) {
      setError('Phone number must be exactly 10 numeric digits (e.g. 9876543210).')
      return
    }

    setLoading(true)
    try {
      const payload =
        mode === 'signin'
          ? { email: form.email, password: form.password }
          : {
              role,
              fullName: form.name,
              organizationName: form.organizationName,
              primaryDomain: form.domain,
              email: form.email,
              password: form.password,
              contactNumber: form.contactNumber,
              location: form.location || (role === 'NGO' ? 'Global Community' : 'Community Member'),
              preferredLanguage: form.preferredLanguage || 'en',
              title: form.title || 'Technical Contributor',
              skillsSummary: form.skillsSummary,
              portfolioUrl: form.portfolioUrl
            }

      const data = await apiPost<any>(
        mode === 'signin' ? '/api/v1/core/auth/login' : '/api/v1/core/auth/register',
        payload
      )

      const token = data.token || data.accessToken || data.data?.token
      if (!token) throw new Error('No session token returned')

      onSuccess(
        token,
        mode === 'signin' ? data.role || data.data?.role || 'CONTRIBUTOR' : role,
        data.userId || '',
        form.email
      )
      onClose()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Unable to authenticate right now.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div 
      className="modal-backdrop" 
      role="dialog" 
      aria-modal="true" 
      aria-labelledby="auth-title"
      onClick={onClose}
    >
      <div className="auth-modal" onClick={(e) => e.stopPropagation()}>
        <button className="close-button auth-close" onClick={onClose} aria-label="Close authentication dialog">
          ×
        </button>
        <span className="eyebrow">Connecting Dots access</span>
        <h2 id="auth-title">{mode === 'signin' ? 'Welcome back.' : 'Join the community.'}</h2>

        {/* Quick Demo Login Choices */}
        <div style={{ marginBottom: '1.25rem', padding: '0.85rem', background: 'var(--card)', border: '1px solid var(--line)', borderRadius: '6px' }}>
          <span className="eyebrow" style={{ fontSize: '0.75rem', marginBottom: '0.5rem', display: 'block' }}>
            Quick Demo Login (One-Click Test Account)
          </span>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '0.5rem' }}>
            <button
              type="button"
              className="outline-button"
              disabled={loading}
              style={{ padding: '0.45rem 0.5rem', fontSize: '0.75rem', fontWeight: 600, textAlign: 'center' }}
              onClick={() => handleQuickLogin('ngo_test@connectingdots.org', 'password123')}
            >
              Test NGO
            </button>
            <button
              type="button"
              className="outline-button"
              disabled={loading}
              style={{ padding: '0.45rem 0.5rem', fontSize: '0.75rem', fontWeight: 600, textAlign: 'center' }}
              onClick={() => handleQuickLogin('contributor_test@connectingdots.org', 'password123')}
            >
              Test Contributor
            </button>
            <button
              type="button"
              className="outline-button"
              disabled={loading}
              style={{ padding: '0.45rem 0.5rem', fontSize: '0.75rem', fontWeight: 600, textAlign: 'center' }}
              onClick={() => handleQuickLogin('admin@connectingdots.org', 'Admin@1234')}
            >
              Admin
            </button>
          </div>
        </div>

        <div className="auth-tabs">
          <button
            type="button"
            className={mode === 'signin' ? 'auth-tab active' : 'auth-tab'}
            onClick={() => {
              setMode('signin')
              setError('')
            }}
          >
            Sign in
          </button>
          <button
            type="button"
            className={mode === 'register' ? 'auth-tab active' : 'auth-tab'}
            onClick={() => {
              setMode('register')
              setError('')
            }}
          >
            Create account
          </button>
        </div>

        <form onSubmit={submit} className="auth-form">
          {mode === 'register' && (
            <>
              <div className="role-switcher">
                <button
                  type="button"
                  className={role === 'CONTRIBUTOR' ? 'role-choice active' : 'role-choice'}
                  onClick={() => setRole('CONTRIBUTOR')}
                >
                  Technical Contributor
                </button>
                <button
                  type="button"
                  className={role === 'NGO' ? 'role-choice active' : 'role-choice'}
                  onClick={() => setRole('NGO')}
                >
                  NGO Organization
                </button>
              </div>

              {role === 'CONTRIBUTOR' ? (
                <>
                  <label>
                    Full Name *
                    <input value={form.name} onChange={change('name')} placeholder="e.g. Alex Morgan" required />
                  </label>
                  <label>
                    Role / Professional Title
                    <input value={form.title} onChange={change('title')} placeholder="e.g. Senior Data Engineer" />
                  </label>
                  <label>
                    Skills Summary
                    <input
                      value={form.skillsSummary}
                      onChange={change('skillsSummary')}
                      placeholder="e.g. Python, React, PostgreSQL, AI"
                    />
                  </label>
                  <label>
                    Portfolio or GitHub URL
                    <input
                      type="url"
                      value={form.portfolioUrl}
                      onChange={change('portfolioUrl')}
                      placeholder="https://github.com/username"
                    />
                  </label>
                </>
              ) : (
                <>
                  <label>
                    Organization Name *
                    <input
                      value={form.organizationName}
                      onChange={change('organizationName')}
                      placeholder="e.g. Green Earth Alliance"
                      required
                    />
                  </label>
                  <label>
                    Primary Focus Domain *
                    <select value={form.domain} onChange={change('domain')}>
                      <option>Education & Technology</option>
                      <option>Healthcare & Wellness</option>
                      <option>Environment & Sustainability</option>
                      <option>Community Development</option>
                      <option>Poverty Alleviation</option>
                      <option>Financial Inclusion</option>
                      <option>Web/Software Development</option>
                      <option>Others</option>
                    </select>
                  </label>
                </>
              )}

              <label>
                Contact Number (10 digits)
                <input
                  type="tel"
                  value={form.contactNumber}
                  onChange={change('contactNumber')}
                  placeholder="e.g. 9876543210"
                  maxLength={10}
                />
              </label>

              <label>
                Location / Region
                <input
                  value={form.location}
                  onChange={change('location')}
                  placeholder={role === 'NGO' ? 'e.g. Seattle, WA, USA' : 'e.g. San Francisco, CA'}
                />
              </label>

              <label>
                Preferred Language
                <select value={form.preferredLanguage} onChange={change('preferredLanguage')}>
                  <option value="en">English (default)</option>
                  <option value="hi">Hindi (हिन्दी)</option>
                  <option value="mr">Marathi (मराठी)</option>
                  <option value="sw">Swahili (Kiswahili)</option>
                </select>
              </label>
            </>
          )}

          <label>
            Email Address *
            <input type="email" value={form.email} onChange={change('email')} required />
          </label>
          <label>
            Password *
            <input type="password" value={form.password} onChange={change('password')} required />
          </label>

          {error && (
            <p className="form-error" role="alert">
              {error}
            </p>
          )}

          <button className="primary-button auth-submit" disabled={loading}>
            {loading ? 'Connecting…' : mode === 'signin' ? 'Sign in' : 'Create account'}
          </button>
        </form>
      </div>
    </div>
  )
}
