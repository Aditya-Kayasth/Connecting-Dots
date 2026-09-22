'use client'

import { useEffect, useState } from 'react'
import { useRouter, usePathname } from 'next/navigation'
import AuthModal from './auth-modal'
import { setAuthSession } from '@/lib/api-client'

type Session = { token: string; role: string; email: string }

export default function Navbar() {
  const [session, setSession] = useState<Session | null>(null)
  const [authOpen, setAuthOpen] = useState(false)
  const [mobileOpen, setMobileOpen] = useState(false)
  const router = useRouter()
  const pathname = usePathname()

  const readSession = () => {
    const token = sessionStorage.getItem('auth_token')
    const role = sessionStorage.getItem('auth_role')
    const email = sessionStorage.getItem('auth_email')
    setSession(token && role ? { token, role, email: email || '' } : null)
  }

  useEffect(() => {
    readSession()

    const handleStorage = () => readSession()
    const handleOpenAuth = () => setAuthOpen(true)

    window.addEventListener('storage', handleStorage)
    window.addEventListener('dots:open-auth', handleOpenAuth)

    return () => {
      window.removeEventListener('storage', handleStorage)
      window.removeEventListener('dots:open-auth', handleOpenAuth)
    }
  }, [])

  function success(token: string, role: string, userId: string, email: string) {
    const formattedRole = role.toUpperCase()
    setAuthSession(token, formattedRole, userId, email)
    setSession({ token, role: formattedRole, email })
    setMobileOpen(false)
    
    // Auto-redirection upon successful sign-in/registration
    if (formattedRole === 'NGO') {
      router.push('/ngo')
    } else if (formattedRole === 'CONTRIBUTOR') {
      router.push('/contributor')
    } else if (formattedRole === 'ADMIN') {
      router.push('/admin')
    }
  }

  function signOut() {
    sessionStorage.removeItem('auth_token')
    sessionStorage.removeItem('auth_role')
    sessionStorage.removeItem('auth_email')
    sessionStorage.removeItem('auth_user_id')
    setSession(null)
    setMobileOpen(false)
    window.dispatchEvent(new StorageEvent('storage'))
    
    // Redirect back to landing page on logout
    router.push('/')
  }

  const role = session?.role

  const isActive = (path: string) => {
    return pathname === path ? 'active-nav' : ''
  }

  const truncateEmail = (emailStr: string) => {
    if (!emailStr) return ''
    if (emailStr.length > 18) return emailStr.slice(0, 15) + '…'
    return emailStr
  }

  return (
    <>
      <nav className="topbar">
        <a className="brand" href="/">
          connecting<span>dots</span>
        </a>

        {/* Mobile Hamburger Toggle Button */}
        <button 
          className="mobile-menu-toggle"
          onClick={() => setMobileOpen(!mobileOpen)}
          aria-label="Toggle Navigation Menu"
          aria-expanded={mobileOpen}
        >
          {mobileOpen ? '✕' : '☰'}
        </button>

        <div className={`nav-links ${mobileOpen ? 'mobile-nav-open' : ''}`}>
          {!session ? (
            <>
              <a href="#problems" onClick={() => setMobileOpen(false)}>Problems</a>
              <a href="#ngos" onClick={() => setMobileOpen(false)}>NGOs</a>
              <a href="#contributors" onClick={() => setMobileOpen(false)}>Contributors</a>
              <button className="primary-button nav-signin" onClick={() => { setMobileOpen(false); setAuthOpen(true); }}>
                Sign in
              </button>
            </>
          ) : (
            <>
              <a className={isActive('/')} href="/" onClick={() => setMobileOpen(false)}>Explore</a>
              {role === 'NGO' && (
                <a className={isActive('/ngo')} href="/ngo" onClick={() => setMobileOpen(false)}>NGO Workspace</a>
              )}
              {role === 'CONTRIBUTOR' && (
                <a className={isActive('/contributor')} href="/contributor" onClick={() => setMobileOpen(false)}>My Applications</a>
              )}
              {role === 'ADMIN' && (
                <a className={isActive('/admin')} href="/admin" onClick={() => setMobileOpen(false)}>Admin Dashboard</a>
              )}
              {role !== 'ADMIN' && (
                <a className={isActive('/profile')} href="/profile" onClick={() => setMobileOpen(false)}>My Profile</a>
              )}
              <button className="outline-button nav-signout" onClick={signOut}>
                Sign out <span className="nav-email">({truncateEmail(session.email)})</span>
              </button>
            </>
          )}
        </div>
      </nav>
      <AuthModal open={authOpen} onClose={() => setAuthOpen(false)} onSuccess={success} />
    </>
  )
}
