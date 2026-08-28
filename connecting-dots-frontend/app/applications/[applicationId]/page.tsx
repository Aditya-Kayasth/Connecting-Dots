'use client'

import { useEffect, useState, use } from 'react'
import { apiRequest, apiPost } from '@/lib/api-client'

interface MessageItem {
  id?: string
  senderId?: string
  author?: string
  text?: string
  content?: string
  createdAt?: string
  time?: string
}

export default function ApplicationDetail({ params }: { params: Promise<{ applicationId: string }> }) {
  const resolvedParams = use(params)
  const id = resolvedParams.applicationId || 'app-441'

  const [messageText, setMessageText] = useState('')
  const [messages, setMessages] = useState<MessageItem[]>([
    { author: 'Open Hands Kenya', text: 'We are excited to work with you on this project.', time: '10:42' },
    { author: 'Alex Morgan', text: 'Thank you. I will share an initial project outline tomorrow.', time: '11:08' },
  ])

  useEffect(() => {
    const poll = async () => {
      try {
        const fetched = await apiRequest<MessageItem[]>(`/api/v1/core/applications/${id}/messages`)
        if (Array.isArray(fetched) && fetched.length > 0) {
          const normalized = fetched.map(m => ({
            id: m.id,
            author: m.senderId === 'self' ? 'You' : (m.author || 'NGO Partner'),
            text: m.content || m.text || '',
            time: m.createdAt ? new Date(m.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : (m.time || 'Just now')
          }))
          setMessages(normalized)
        }
      } catch {
        /* Keep existing thread */
      }
    }

    poll()
    const timer = window.setInterval(poll, 5000)
    return () => window.clearInterval(timer)
  }, [id])

  async function send() {
    if (!messageText.trim()) return
    const textToSend = messageText.trim()
    const optimisticMessage: MessageItem = { author: 'Alex Morgan', text: textToSend, time: 'Just now' }
    setMessages((current) => [...current, optimisticMessage])
    setMessageText('')

    try {
      await apiPost(`/api/v1/core/applications/${id}/messages`, { content: textToSend })
    } catch {
      /* Keep optimistic message visible */
    }
  }

  return (
    <main className="workspace-shell">
      <nav className="topbar">
        <a className="brand" href="/">connecting<span>dots</span></a>
        <div className="nav-links">
          <a href="/contributor">Contributor workspace</a>
          <a href="/ngo">NGO workspace</a>
          <a href="/profile">Profile</a>
        </div>
      </nav>

      <div className="detail-shell">
        <a className="back-link" href="/contributor">← Back to applications</a>
        
        <div className="detail-header">
          <div>
            <span className="eyebrow">Application detail · {id}</span>
            <h1>Build a volunteer matching portal</h1>
            <p>Open Hands Kenya · Civic tech · Status <span className="status-badge status-accepted">ACCEPTED</span></p>
          </div>
          <div className="detail-avatar avatar avatar-green">OH</div>
        </div>

        <div className="detail-grid">
          <section className="context-card">
            <span className="eyebrow">Problem context</span>
            <h2>A better way for local volunteers to find community work.</h2>
            <p>Open Hands Kenya needs a lightweight matching portal where volunteers can discover, apply to, and follow projects in their neighborhoods.</p>
            <div className="owner-block">
              <div className="avatar avatar-gold">OH</div>
              <div>
                <strong>Open Hands Kenya</strong>
                <small>NGO owner · Nairobi, Kenya</small>
              </div>
            </div>
          </section>

          <section className="thread-card">
            <div className="section-heading">
              <div>
                <span className="eyebrow">Private thread</span>
                <h2>Application messages</h2>
              </div>
              <span className="section-count">Scoped to {id}</span>
            </div>

            <div className="message-list">
              {messages.map((m, i) => (
                <div className={`message ${m.author === 'Alex Morgan' || m.author === 'You' ? 'message-self' : ''}`} key={m.id || i}>
                  <strong>{m.author}</strong>
                  <p>{m.text}</p>
                  <small>{m.time}</small>
                </div>
              ))}
            </div>

            <div className="message-compose">
              <textarea
                value={messageText}
                onChange={(e) => setMessageText(e.target.value)}
                placeholder="Write a message to the NGO..."
                aria-label="Message"
              />
              <button className="primary-button" onClick={send}>Send message</button>
              <small>Messages refresh by polling this application only.</small>
            </div>
          </section>
        </div>
      </div>
    </main>
  )
}
