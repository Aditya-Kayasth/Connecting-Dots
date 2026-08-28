'use client'

import { useMemo, useState } from 'react'

const problems = [
  { id: '7dd308c2-237a-4099-bf2d-5fc99c55e5c4', ngoName: "Aditya's Tech Rescue", title: 'Digitize Student Records & Dropout Early Warning AI', description: 'We tutor underprivileged students in Nagpur. We need a web dashboard to digitize attendance and quiz scores, with machine learning to predict students at risk of dropping out before exams.', domain: 'Education', status: 'OPEN', techCategory: 'DATA_SCIENCE_ML' },
  { id: '7e3eab66-8db2-479f-adfc-6638f037e9ca', ngoName: 'Shiksha Foundation', title: 'Rural School Attendance Tracking Web Portal', description: 'Our rural schools lose paper attendance rosters. We require a simple mobile-friendly web application for teachers to mark daily attendance offline and sync when connected.', domain: 'Education', status: 'OPEN', techCategory: 'SOFTWARE_WEB' },
  { id: 'eb3e6159-f856-48db-bb52-993f414a99ba', ngoName: 'Payaar Foundation', title: 'Animal Rescue Staff & Medicine Reimbursement Management', description: 'We manage field volunteers treating injured animals. We need a portal to track field staff tasks and automate medicine purchase reimbursement requests.', domain: 'Animal Welfare', status: 'OPEN', techCategory: 'SOFTWARE_WEB' },
]

const ngos = [
  { organizationName: "Aditya's Tech Rescue", domain: 'Education & Literacy', contactNumber: '+91 98765 43210', preferredLanguage: 'en' },
  { organizationName: 'Shiksha Foundation', domain: 'Rural Education', contactNumber: '+91 98123 45678', preferredLanguage: 'hi' },
  { organizationName: 'Payaar Foundation', domain: 'Animal Welfare', contactNumber: '+91 97654 32109', preferredLanguage: 'mr' },
]

const contributors = [
  { firstName: 'Aarav', lastName: 'Sharma', skillsSummary: 'Next.js, React, Tailwind CSS, TypeScript, Node.js', portfolioUrl: 'https://github.com/aaravsharma', availability: 'Open to projects' },
  { firstName: 'Meera', lastName: 'Iyer', skillsSummary: 'Python, Data Science, Machine Learning, SQL', portfolioUrl: 'https://github.com/meera-iyer', availability: 'Open to projects' },
  { firstName: 'Kabir', lastName: 'Patel', skillsSummary: 'UX Research, Figma, Product Design, Accessibility', portfolioUrl: 'https://behance.net/kabirpatel', availability: 'Open to projects' },
]

function StatusBadge({ status }: { status: string }) { return <span className={`status-badge status-${status.toLowerCase()}`}>{status.replace('_', ' ')}</span> }

export default function PublicExplorer() {
  const [domain, setDomain] = useState('All domains')
  const filtered = useMemo(() => domain === 'All domains' ? problems : problems.filter((problem) => problem.domain === domain), [domain])
  return <div className="explorer">
    <header className="explorer-head"><div><span className="eyebrow">Public directory</span><h1>Find a problem worth solving.</h1><p>Browse open opportunities, meet the organizations behind them, and discover contributors building for good.</p></div><div className="read-only-note"><span className="note-dot" /> Read-only browsing<br /><small>Sign in when you are ready to contribute.</small></div></header>
    <section className="explorer-section" id="problems"><div className="section-heading"><div><span className="eyebrow">01 / Open opportunities</span><h2>Problem statements</h2></div><select aria-label="Filter problems by domain" value={domain} onChange={(event) => setDomain(event.target.value)}><option>All domains</option><option>Education</option><option>Animal Welfare</option></select></div><div className="problem-grid">{filtered.map((problem) => <article className="problem-card" key={problem.id}><div className="card-meta"><span>{problem.ngoName}</span><StatusBadge status={problem.status} /></div><h3>{problem.title}</h3><p>{problem.description}</p><div className="card-footer"><span>{problem.domain}</span><span>{problem.techCategory.replace('_', ' / ')}</span></div></article>)}</div></section>
    <section className="explorer-section" id="ngos"><div className="section-heading"><div><span className="eyebrow">02 / Community partners</span><h2>NGO directory</h2></div><span className="section-count">{ngos.length} organizations</span></div><div className="directory-grid">{ngos.map((ngo) => <article className="directory-card" key={ngo.organizationName}><div className="avatar avatar-green">{ngo.organizationName.charAt(0)}</div><div><h3>{ngo.organizationName}</h3><p>{ngo.domain}</p><div className="directory-detail">{ngo.contactNumber} <span>·</span> {ngo.preferredLanguage.toUpperCase()}</div></div></article>)}</div></section>
    <section className="explorer-section" id="contributors"><div className="section-heading"><div><span className="eyebrow">03 / Technical community</span><h2>Contributor directory</h2></div><span className="section-count">{contributors.length} contributors</span></div><div className="directory-grid">{contributors.map((person) => <article className="directory-card" key={person.portfolioUrl}><div className="avatar avatar-gold">{person.firstName.charAt(0)}{person.lastName.charAt(0)}</div><div><div className="card-meta"><h3>{person.firstName} {person.lastName}</h3><span className="availability">● {person.availability}</span></div><p>{person.skillsSummary}</p><a className="text-link" href={person.portfolioUrl} target="_blank" rel="noreferrer">View portfolio →</a></div></article>)}</div></section>
  </div>
}
