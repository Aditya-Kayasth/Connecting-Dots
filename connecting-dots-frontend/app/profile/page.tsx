'use client'

import { useState } from 'react'
import { Pencil, Check, X, Globe, Mail, MapPin, Award } from 'lucide-react'
import ReviewsList from "@/components/reviews-list"
import { apiPut } from "@/lib/api-client"

export default function ProfilePage() {
  const [profile, setProfile] = useState({
    name: 'Maya Chen',
    title: 'Product designer and frontend engineer',
    location: 'Nairobi, Kenya',
    skills: 'Next.js, React, Tailwind CSS, TypeScript, UX Research',
    portfolioUrl: 'https://github.com/mayachen',
    preferredLanguage: 'English (en)',
  })

  const [isEditing, setIsEditing] = useState(false)
  const [editForm, setEditForm] = useState(profile)
  const [saveMessage, setSaveMessage] = useState('')

  const getInitials = (fullName: string) => {
    if (!fullName || !fullName.trim()) return 'CD'
    const parts = fullName.trim().split(' ')
    if (parts.length >= 2) return `${parts[0][0]}${parts[parts.length - 1][0]}`.toUpperCase()
    return fullName.slice(0, 2).toUpperCase()
  }

  const handleSave = async () => {
    setProfile(editForm)
    setIsEditing(false)
    setSaveMessage('Profile updated successfully.')
    setTimeout(() => setSaveMessage(''), 3000)

    try {
      await apiPut('/api/v1/core/profiles/contributor/me', editForm)
    } catch {
      /* Fallback to local state */
    }
  }

  return (
    <main className="page-shell">
      <nav className="topbar">
        <a className="brand" href="/">connecting<span>dots</span></a>
        <div className="flex items-center gap-4 text-xs font-semibold">
          <a className="text-link" href="/ngo">NGO Workspace</a>
          <a className="text-link" href="/contributor">Contributor Workspace</a>
          <a className="text-link" href="/admin">Admin Dashboard</a>
        </div>
      </nav>

      <div className="profile-content">
        <div className="flex items-center justify-between">
          <a className="back-link" href="/">← Back to dashboard</a>
          {!isEditing ? (
            <button
              onClick={() => {
                setEditForm(profile)
                setIsEditing(true)
              }}
              className="flex items-center gap-1.5 px-3 py-1.5 text-xs font-semibold bg-teal-50 dark:bg-teal-950/60 text-teal-700 dark:text-teal-300 border border-teal-200 dark:border-teal-800 rounded-lg hover:bg-teal-100 transition-colors"
            >
              <Pencil className="w-3.5 h-3.5" /> Edit Profile Details
            </button>
          ) : (
            <div className="flex items-center gap-2">
              <button
                onClick={handleSave}
                className="flex items-center gap-1 px-3 py-1.5 text-xs font-semibold bg-teal-600 text-white rounded-lg hover:bg-teal-700 transition-colors shadow-sm"
              >
                <Check className="w-3.5 h-3.5" /> Save
              </button>
              <button
                onClick={() => setIsEditing(false)}
                className="flex items-center gap-1 px-3 py-1.5 text-xs font-semibold bg-slate-200 dark:bg-slate-800 text-slate-700 dark:text-slate-300 rounded-lg hover:bg-slate-300 transition-colors"
              >
                <X className="w-3.5 h-3.5" /> Cancel
              </button>
            </div>
          )}
        </div>

        {saveMessage && (
          <div className="p-3 text-xs bg-emerald-50 text-emerald-800 border border-emerald-200 rounded-lg font-medium">
            {saveMessage}
          </div>
        )}

        <section className="profile-hero">
          <div className="profile-avatar">{getInitials(profile.name)}</div>
          <div className="w-full">
            <span className="eyebrow">Technical contributor</span>
            
            {!isEditing ? (
              <>
                <h1 className="flex items-center gap-2">{profile.name}</h1>
                <p className="muted flex items-center gap-2 mt-1">
                  <MapPin className="w-3.5 h-3.5 text-slate-400" /> {profile.title} · {profile.location}
                </p>
                <div className="mt-3 flex flex-wrap items-center gap-4 text-xs text-slate-600 dark:text-slate-400">
                  <span className="flex items-center gap-1"><Award className="w-3.5 h-3.5 text-teal-600" /> {profile.skills}</span>
                  <a href={profile.portfolioUrl} target="_blank" rel="noreferrer" className="flex items-center gap-1 text-teal-600 hover:underline">
                    <Globe className="w-3.5 h-3.5" /> Portfolio
                  </a>
                </div>
              </>
            ) : (
              <div className="mt-2 space-y-3 max-w-lg">
                <div>
                  <label className="block text-xs font-semibold text-slate-600 dark:text-slate-400 mb-1">Full Name</label>
                  <input
                    className="w-full px-3 py-1.5 text-sm bg-white dark:bg-slate-800 text-slate-900 dark:text-white border border-slate-300 dark:border-slate-700 rounded-md focus:ring-2 focus:ring-teal-500"
                    value={editForm.name}
                    onChange={(e) => setEditForm({ ...editForm, name: e.target.value })}
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-600 dark:text-slate-400 mb-1">Title & Location</label>
                  <div className="grid grid-cols-2 gap-2">
                    <input
                      className="px-3 py-1.5 text-sm bg-white dark:bg-slate-800 text-slate-900 dark:text-white border border-slate-300 dark:border-slate-700 rounded-md"
                      value={editForm.title}
                      onChange={(e) => setEditForm({ ...editForm, title: e.target.value })}
                      placeholder="Title"
                    />
                    <input
                      className="px-3 py-1.5 text-sm bg-white dark:bg-slate-800 text-slate-900 dark:text-white border border-slate-300 dark:border-slate-700 rounded-md"
                      value={editForm.location}
                      onChange={(e) => setEditForm({ ...editForm, location: e.target.value })}
                      placeholder="Location"
                    />
                  </div>
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-600 dark:text-slate-400 mb-1">Skills Summary</label>
                  <input
                    className="w-full px-3 py-1.5 text-sm bg-white dark:bg-slate-800 text-slate-900 dark:text-white border border-slate-300 dark:border-slate-700 rounded-md"
                    value={editForm.skills}
                    onChange={(e) => setEditForm({ ...editForm, skills: e.target.value })}
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-600 dark:text-slate-400 mb-1">Portfolio URL</label>
                  <input
                    className="w-full px-3 py-1.5 text-sm bg-white dark:bg-slate-800 text-slate-900 dark:text-white border border-slate-300 dark:border-slate-700 rounded-md"
                    value={editForm.portfolioUrl}
                    onChange={(e) => setEditForm({ ...editForm, portfolioUrl: e.target.value })}
                  />
                </div>
              </div>
            )}

            <div className="mt-4">
              <span className="status verified">TRUSTED CONTRIBUTOR</span>
            </div>
          </div>
        </section>

        <ReviewsList />
      </div>
    </main>
  )
}
