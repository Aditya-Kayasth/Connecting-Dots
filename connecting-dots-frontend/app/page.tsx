"use client"

import { useEffect, useState } from "react"
import PublicExplorer from "@/components/public-explorer"
import AuthModal from "@/components/auth-modal"
import { getSavedUser, logout } from "@/lib/api-client"

export default function Home() {
  const [user, setUser] = useState<{ email: string; role: string } | null>(null)
  const [isAuthOpen, setIsAuthOpen] = useState(false)

  useEffect(() => {
    setUser(getSavedUser())
  }, [])

  return (
    <main className="page-shell">
      <nav className="topbar">
        <a className="brand" href="/">
          connecting<span>dots</span>
        </a>
        <div className="nav-links">
          <a href="#problems">Problems</a>
          <a href="#ngos">NGOs</a>
          <a href="#contributors">Contributors</a>
          <a href="/profile">Profiles</a>
          {user?.role === "NGO" && <a href="/ngo">NGO workspace</a>}
          {user?.role === "CONTRIBUTOR" && <a href="/contributor">My applications</a>}
          {(user?.role === "ROLE_ADMIN" || user?.role === "ADMIN" || user?.email === "admin@connectingdots.org") && (
            <a href="/admin" className="font-semibold text-emerald-600 dark:text-emerald-400">Admin</a>
          )}

          {user ? (
            <button className="outline-button" onClick={() => { logout(); setUser(null); location.reload(); }}>
              Sign out ({user.email.split('@')[0]})
            </button>
          ) : (
            <button className="outline-button" onClick={() => setIsAuthOpen(true)}>
              Sign in
            </button>
          )}
        </div>
      </nav>

      <section className="hero">
        <div className="hero-copy">
          <span className="eyebrow">A community for meaningful work</span>
          <h1>Technology should connect people to possibility.</h1>
          <p>Connecting Dots brings NGOs with real-world problems together with technical contributors who want to make a difference.</p>
          <div className="hero-actions">
            <a className="primary-button" href="#problems">Explore open problems <span>↓</span></a>
            <a className="text-link" href="/review">Review a completed project</a>
          </div>
        </div>
        <div className="hero-note">
          <div className="note-mark">“</div>
          <p>Small teams. Shared skills. Lasting change.</p>
          <span>— Our community principle</span>
        </div>
      </section>

      <PublicExplorer />

      <AuthModal
        isOpen={isAuthOpen}
        onClose={() => setIsAuthOpen(false)}
        onSuccess={(u) => {
          setUser(u)
          setIsAuthOpen(false)
        }}
      />

      <footer>
        <span>© 2026 Connecting Dots</span>
        <span>Built for people who care.</span>
      </footer>
    </main>
  )
}
