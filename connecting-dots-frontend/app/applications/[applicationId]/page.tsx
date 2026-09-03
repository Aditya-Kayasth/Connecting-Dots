'use client'

import { useEffect, useState } from 'react'
import { apiRequest, getAuthUserId, getAuthRole } from '@/lib/api-client'
import ReviewForm from '@/components/review-form'

type Message = { 
  id?: string; 
  senderId?: string; 
  author?: string; 
  text?: string; 
  content?: string;
  message?: string;
  createdAt?: string;
  timestamp?: string; 
  time?: string;
}

type Application = { 
  status: string; 
  problemTitle: string; 
  problemDescription: string; 
  ngoName: string; 
  applicantName: string; 
  applicantEmail: string; 
}

export default function ApplicationDetail({ params }: { params: Promise<{ applicationId: string }> }) {
  const [id, setId] = useState('')
  const [application, setApplication] = useState<Application>({ 
    status: 'ACCEPTED', 
    problemTitle: 'Build a volunteer matching portal', 
    problemDescription: 'Open Hands Kenya needs a lightweight matching portal where volunteers can discover, apply to, and follow projects in their neighborhoods.', 
    ngoName: 'Open Hands Kenya', 
    applicantName: 'Alex Morgan', 
    applicantEmail: 'alex@example.com' 
  })
  
  const [message, setMessage] = useState('')
  const [messages, setMessages] = useState<Message[]>([])
  const [loading, setLoading] = useState(true)
  const [reviewOpen, setReviewOpen] = useState(false)
  
  const [currentUserId, setCurrentUserId] = useState<string>('')
  const [currentUserName, setCurrentUserName] = useState<string>('')
  const [currentUserRole, setCurrentUserRole] = useState<string>('')

  // Resolve applicationId parameter
  useEffect(() => { 
    params.then(value => setId(value.applicationId)) 
  }, [params])

  // Fetch application context
  useEffect(() => { 
    if (!id) return
    apiRequest<Application>(`/api/v1/core/applications/${id}`)
      .then(setApplication)
      .catch((err) => console.error('Failed to load application context:', err)) 
  }, [id])

  // Resolve user identity details dynamically
  useEffect(() => {
    const uid = getAuthUserId() || ''
    const role = getAuthRole() || ''
    setCurrentUserId(uid)
    setCurrentUserRole(role)

    if (uid && role) {
      const endpoint = role === 'NGO' ? '/api/v1/core/profiles/ngo/me' : '/api/v1/core/profiles/contributor/me'
      apiRequest<any>(endpoint)
        .then(profile => {
          if (role === 'NGO') {
            setCurrentUserName(profile.organizationName || 'NGO Partner')
          } else {
            setCurrentUserName(`${profile.firstName || ''} ${profile.lastName || ''}`.trim() || 'Contributor')
          }
        })
        .catch(() => {
          setCurrentUserName(role === 'NGO' ? 'NGO Partner' : 'Contributor')
        })
    }
  }, [])

  // Poll for messages every 5 seconds
  useEffect(() => { 
    if (!id) return
    const poll = () => 
      apiRequest<Message[]>(`/api/v1/core/applications/${id}/messages`)
        .then(data => setMessages(Array.isArray(data) ? data : []))
        .catch(() => {
          // Fallback to legacy endpoint if needed
          return apiRequest<Message[]>(`/api/v1/core/messages/application/${id}`)
            .then(data => setMessages(Array.isArray(data) ? data : []))
            .catch(() => setMessages(current => current))
        })
        .finally(() => setLoading(false))
        
    poll()
    const timer = window.setInterval(poll, 5000)
    return () => window.clearInterval(timer) 
  }, [id])

  // Send message
  async function send() { 
    if (!message.trim()) return
    const text = message.trim()
    
    // Optimistic UI update
    const optimisticMessage: Message = { 
      author: currentUserName, 
      senderId: currentUserId, 
      content: text,
      text: text, 
      createdAt: new Date().toISOString() 
    }
    setMessages(current => [...current, optimisticMessage])
    setMessage('')
    
    try { 
      await apiRequest(`/api/v1/core/applications/${id}/messages`, { 
        method: 'POST', 
        body: { 
          content: text 
        } 
      }) 
      // Refresh messages list
      const refreshed = await apiRequest<Message[]>(`/api/v1/core/applications/${id}/messages`)
      if (Array.isArray(refreshed)) setMessages(refreshed)
    } catch (err) {
      console.error('Failed to post message:', err)
      alert(err instanceof Error ? err.message : 'Failed to send message.')
    } 
  }

  const backHref = currentUserRole === 'NGO' ? '/ngo' : '/contributor'

  return (
    <main className="workspace-shell">
      <div className="detail-shell">
        <a className="back-link" href={backHref}>← Back to workspace</a>
        
        <div className="detail-header">
          <div>
            <span className="eyebrow">Application detail · {id}</span>
            <h1>{application.problemTitle}</h1>
            <p>
              {application.ngoName} · Status{' '}
              <span className={`status-badge status-${application.status.toLowerCase()}`}>
                {application.status}
              </span>
            </p>
          </div>
          <div className="detail-avatar avatar avatar-green">
            {application.ngoName ? application.ngoName.split(' ').map(word => word[0]).join('').slice(0, 2).toUpperCase() : 'NGO'}
          </div>
        </div>

        <div className="detail-grid">
          {/* Left Panel: Context */}
          <section className="context-card">
            <span className="eyebrow">Problem context</span>
            <h2>{application.problemTitle}</h2>
            <p>{application.problemDescription}</p>
            <div className="owner-block">
              <div className="avatar avatar-gold">
                {application.ngoName ? application.ngoName.split(' ').map(word => word[0]).join('').slice(0, 2).toUpperCase() : 'NGO'}
              </div>
              <div>
                <strong>{application.ngoName}</strong>
                <small>NGO owner · application {id.slice(0, 8)}</small>
              </div>
            </div>
            {application.status === 'COMPLETED' && currentUserRole === 'CONTRIBUTOR' && (
              <button className="primary-button" onClick={() => setReviewOpen(true)} style={{ marginTop: '1.5rem' }}>
                Leave a Review →
              </button>
            )}
          </section>

          {/* Right Panel: Chat Thread */}
          <section className="thread-card">
            <div className="section-heading">
              <div>
                <span className="eyebrow">Private 1-on-1 thread</span>
                <h2>Application messages</h2>
              </div>
              <span className="section-count">Scoped to {id.slice(0, 8)}</span>
            </div>

            {loading ? (
              <div className="empty-state">Loading messages…</div>
            ) : messages.length === 0 ? (
              <div className="empty-state">
                Start the conversation...
                <small>Only messages for this application appear here.</small>
              </div>
            ) : (
              <div className="message-list">
                {messages.map((item, index) => { 
                  const self = item.senderId === currentUserId
                  const authorName = item.author || (self ? currentUserName : (currentUserRole === 'NGO' ? application.applicantName : application.ngoName))
                  const avatarInitials = authorName ? authorName.split(' ').map(word => word[0]).join('').slice(0, 2).toUpperCase() : 'CD'
                  const messageBody = item.content || item.text || item.message || '(Empty message)'
                  const rawTime = item.createdAt || item.timestamp
                  const formattedTime = rawTime 
                    ? new Date(rawTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) 
                    : (item.time || 'Recently')
                  
                  return (
                    <div className={`message ${self ? 'message-self' : ''}`} key={item.id || `msg-${index}`}>
                      <div className="message-author">
                        <span className="message-avatar">{avatarInitials}</span>
                        <strong>{authorName}</strong>
                      </div>
                      <p style={{ whiteSpace: 'pre-wrap', margin: '0.4rem 0', wordBreak: 'break-word' }}>{messageBody}</p>
                      <small style={{ opacity: 0.7 }}>{formattedTime}</small>
                    </div>
                  )
                })}
              </div>
            )}

            <div className="message-compose">
              <textarea 
                value={message} 
                onChange={e => setMessage(e.target.value)} 
                placeholder="Write a message to the partner..." 
                aria-label="Message" 
              />
              <button className="primary-button" onClick={send}>Send message</button>
              <small>Messages poll every 5 seconds for this application only.</small>
            </div>
          </section>
        </div>
      </div>

      {/* Review Modal Backdrop */}
      {reviewOpen && (
        <div className="modal-backdrop" role="dialog" aria-modal="true">
          <section className="edit-modal">
            <button className="close-button" onClick={() => setReviewOpen(false)} aria-label="Close review">×</button>
            <ReviewForm applicationId={id} recipientName={application.ngoName} />
          </section>
        </div>
      )}
    </main>
  )
}
