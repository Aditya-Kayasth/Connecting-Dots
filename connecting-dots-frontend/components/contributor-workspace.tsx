'use client'

import { useMemo, useState } from 'react'

const problems = [
  { id: 'prob-104', title: 'Build a volunteer matching portal', org: 'Open Hands Kenya', domain: 'Civic tech', desc: 'Create a lightweight way for local volunteers to discover and commit to community projects.', tags: ['Next.js', 'UX', 'API'] },
  { id: 'prob-098', title: 'Make our food bank data accessible', org: 'The Pantry Project', domain: 'Data', desc: 'Turn monthly spreadsheets into a clear public dashboard for donors and partner organizations.', tags: ['Data viz', 'React', 'CSV'] },
  { id: 'prob-091', title: 'Digitize an after-school curriculum', org: 'Bright Futures Collective', domain: 'Education', desc: 'Help teachers share and adapt a modular literacy curriculum across three learning centers.', tags: ['Content', 'CMS', 'Design'] },
]

const applications = [
  { id: 'app-441', problem: 'Build a volunteer matching portal', org: 'Open Hands Kenya', status: 'ACCEPTED', date: 'Aug 19, 2026' },
  { id: 'app-424', problem: 'Make our food bank data accessible', org: 'The Pantry Project', status: 'PENDING', date: 'Aug 14, 2026' },
  { id: 'app-402', problem: 'Community health SMS reminders', org: 'Care Circle Uganda', status: 'REJECTED', date: 'Aug 04, 2026' },
]

export default function ContributorWorkspace() {
  const [applied, setApplied] = useState<string[]>(['prob-104'])
  const [filter, setFilter] = useState('All domains')
  const filtered = useMemo(() => filter === 'All domains' ? problems : problems.filter((p) => p.domain === filter), [filter])
  return <main className="workspace-shell"><header className="workspace-header"><div><span className="eyebrow">Contributor workspace</span><h1>Put your skills where they can move something forward.</h1><p>Browse open problems, follow your applications, and build a reputation through work that matters.</p></div><div className="workspace-user"><div className="avatar avatar-green">AM</div><div><strong>Alex Morgan</strong><small>Product engineer · Nairobi</small></div></div></header><div className="contributor-layout"><section><div className="section-heading"><div><span className="eyebrow">Open problems</span><h2>Find a meaningful next project</h2></div><select value={filter} onChange={(e) => setFilter(e.target.value)} aria-label="Filter by domain"><option>All domains</option><option>Civic tech</option><option>Data</option><option>Education</option></select></div><div className="problem-grid">{filtered.map((p) => <article className="problem-card" key={p.id}><div className="card-meta"><span>{p.domain}</span><span className="status-badge status-open">OPEN</span></div><h3>{p.title}</h3><p>{p.desc}</p><div className="tag-row">{p.tags.map((tag) => <span key={tag}>{tag}</span>)}</div><div className="card-footer"><span>{p.org}</span>{applied.includes(p.id) ? <span className="published-note">Applied</span> : <button className="text-link button-link" onClick={() => setApplied([...applied, p.id])}>Apply →</button>}</div></article>)}</div><div className="section-heading dashboard-section-heading"><div><span className="eyebrow">Your activity</span><h2>My applications</h2></div><span className="section-count">{applications.length} total</span></div><div className="application-list">{applications.map((a) => <a className="application-row" href={`/applications/${a.id}`} key={a.id}><div><strong>{a.problem}</strong><small>{a.org} · Applied {a.date}</small></div><span className={`status-badge status-${a.status.toLowerCase()}`}>{a.status}</span><span className="row-arrow">→</span></a>)}</div></section><aside className="reputation-card"><span className="eyebrow">Reputation</span><div className="reputation-score">4.8<span>/5</span></div><p>Based on 12 completed projects and 9 reviews from NGO partners.</p><div className="reputation-line"><span>Projects completed</span><strong>12</strong></div><div className="reputation-line"><span>Hours contributed</span><strong>248</strong></div><div className="reputation-line"><span>Community rank</span><strong>Top 8%</strong></div><a className="text-link" href="/profile">View public profile →</a></aside></div></main>
}
