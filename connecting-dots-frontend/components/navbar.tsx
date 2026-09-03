'use client'

import { useEffect, useState } from 'react'
import { useRouter, usePathname } from 'next/navigation'
import AuthModal from './auth-modal'
import { setAuthSession } from '@/lib/api-client'

type Session = { token: string; role: string; email: string }

export default function Navbar() {
  const [session, setSession] = useState<Session | null>(null)
  const [authOpen, setAuthOpen] = useState(false)
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
    window.dispatchEvent(new StorageEvent('storage'))
    
    // Redirect back to landing page on logout
    router.push('/')
  }

  const role = session?.role

  const isActive = (path: string) => {
    return pathname === path ? 'active-nav' : ''
  }

  return (
    <>
      <nav className="topbar">
        <a className="brand" href="/">
          connecting<span>dots</span>
        </a>
        <div className="nav-links">
          {!session ? (
            <>
              <a href="#problems">Problems</a>
              <a href="#ngos">NGOs</a>
              <a href="#contributors">Contributors</a>
              <button className="primary-button nav-signin" onClick={() => setAuthOpen(true)}>
                Sign in
              </button>
            </>
          ) : (
            <>
              <a className={isActive('/')} href="/">Explore</a>
              {role === 'NGO' && (
                <a className={isActive('/ngo')} href="/ngo">NGO Workspace</a>
              )}
              {role === 'CONTRIBUTOR' && (
                <a className={isActive('/contributor')} href="/contributor">My Applications</a>
              )}
              {role === 'ADMIN' && (
                <a className={isActive('/admin')} href="/admin">Admin Dashboard</a>
              )}
              {role !== 'ADMIN' && (
                <a className={isActive('/profile')} href="/profile">My Profile</a>
              )}
              <button className="outline-button" onClick={signOut}>
                Sign out ({session.email})
              </button>
            </>
          )}
        </div>
      </nav>
      <AuthModal open={authOpen} onClose={() => setAuthOpen(false)} onSuccess={success} />
    </>
  )
}
