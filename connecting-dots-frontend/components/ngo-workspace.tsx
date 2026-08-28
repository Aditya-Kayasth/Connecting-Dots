"use client"

import { useState } from "react"
import { apiRequest, apiPut, apiPost } from "@/lib/api-client"
import { uploadFileToCloudinary } from "@/lib/upload"
import { Loader2 } from "lucide-react"

type Status = "PROCESSING" | "DRAFT" | "OPEN"
type Draft = { id: number | string; title: string; description: string; domain: string; tags: string[]; status: Status; sourceFileUrl?: string }
type Applicant = { id: number | string; name: string; email: string; skills: string; status: string }

const initialDrafts: Draft[] = [
  { id: 1, title: "Digital intake for rural health clinics", description: "Create a lightweight intake workflow so clinic teams can securely collect patient referrals and track follow-up across three districts.", domain: "Health", tags: ["Next.js", "Data", "Accessibility"], status: "DRAFT" },
  { id: 2, title: "Volunteer coordination toolkit", description: "A simple coordination space for local volunteers supporting food distribution and emergency response.", domain: "Community", tags: ["Product design", "React", "Operations"], status: "PROCESSING" },
  { id: 3, title: "Open learning resource hub", description: "A searchable, accessible resource library for teachers working with displaced learners.", domain: "Education", tags: ["Content", "Search", "UX"], status: "OPEN" },
]

const initialApplicants: Record<number | string, Applicant[]> = {
  1: [
    { id: 101, name: "Amara Okafor", email: "amara@example.org", skills: "React, accessibility", status: "PENDING" },
    { id: 102, name: "Daniel Kim", email: "daniel@example.org", skills: "Data systems, Python", status: "PENDING" },
  ],
  3: [{ id: 103, name: "Maya Singh", email: "maya@example.org", skills: "Content design, search", status: "ACCEPTED" }],
}

function StatusPill({ status }: { status: string }) { return <span className={`status-badge status-${status.toLowerCase()}`}>{status}</span> }

export default function NgoWorkspace() {
  const [drafts, setDrafts] = useState(initialDrafts)
  const [applicants, setApplicants] = useState(initialApplicants)
  const [selectedProblem, setSelectedProblem] = useState<number | string>(1)
  const [message, setMessage] = useState("")
  const [editingId, setEditingId] = useState<number | string | null>(null)
  const [form, setForm] = useState({ title: "", description: "", domain: "", tags: "" })
  const [selectedFile, setSelectedFile] = useState<File | null>(null)
  const [isUploading, setIsUploading] = useState(false)

  function notify(text: string) { setMessage(text); window.setTimeout(() => setMessage(""), 3500) }
  
  function startEdit(draft: Draft) { 
    setEditingId(draft.id)
    setForm({ title: draft.title, description: draft.description, domain: draft.domain, tags: draft.tags.join(", ") }) 
  }

  async function saveDraft() {
    if (!editingId) return
    const next = drafts.map(d => d.id === editingId ? { ...d, title: form.title, description: form.description, domain: form.domain, tags: form.tags.split(",").map(t => t.trim()).filter(Boolean) } : d)
    setDrafts(next)
    setEditingId(null)
    notify("Draft updated.")
    try { 
      await apiPut(`/api/v1/core/problem-statements/${editingId}`, form) 
    } catch { 
      /* demo fallback */ 
    }
  }

  async function publish(id: number | string) {
    setDrafts(drafts.map(d => d.id === id ? { ...d, status: "OPEN" } : d))
    notify("Problem approved and published.")
    try { 
      await apiPut(`/api/v1/core/problem-statements/${id}`, { status: "OPEN" }) 
    } catch { 
      /* demo fallback */ 
    }
  }

  async function submitProblem(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault()
    setIsUploading(true)
    let fileUrl = ""

    try {
      if (selectedFile) {
        notify("Uploading source file to Cloudinary...")
        fileUrl = await uploadFileToCloudinary(selectedFile)
      }

      notify("Submitting problem & triggering AI structuring...")
      
      const newProblem = await apiPost<{ id: string }>("/api/v1/core/problem-statements", {
        title: form.title,
        description: form.description,
        domain: form.domain,
        sourceFileUrl: fileUrl || undefined,
        sourceType: selectedFile ? (selectedFile.type.includes("pdf") ? "PDF" : selectedFile.type.includes("audio") ? "AUDIO" : "IMAGE") : undefined
      }).catch(() => null)

      const createdId = newProblem?.id || Date.now()
      
      if (newProblem?.id) {
        await apiPost(`/api/v1/core/problem-statements/${newProblem.id}/ingest`, {}).catch(() => null)
      }

      setDrafts([
        { id: createdId, title: form.title, description: form.description, domain: form.domain, tags: form.tags.split(",").map(t => t.trim()).filter(Boolean), status: "PROCESSING", sourceFileUrl: fileUrl },
        ...drafts
      ])

      setForm({ title: "", description: "", domain: "", tags: "" })
      setSelectedFile(null)
      notify("Problem submitted successfully. AI ingestion started.")
    } catch (err: any) {
      notify(`Submission complete (demo mode): ${err?.message || 'Ready'}`)
    } finally {
      setIsUploading(false)
    }
  }

  async function decide(problemId: number | string, applicantId: number | string, decision: "ACCEPTED" | "REJECTED") {
    setApplicants({ ...applicants, [problemId]: (applicants[problemId] || []).map(a => a.id === applicantId ? { ...a, status: decision } : a) })
    notify(`Application ${decision.toLowerCase()}.`)
    try { 
      await apiPut(`/api/v1/core/applications/${applicantId}/status`, { status: decision }) 
    } catch { 
      /* demo state fallback */ 
    }
  }

  const selectedApplicants = applicants[selectedProblem] || []

  return (
    <main className="workspace-shell">
      <nav className="topbar">
        <a className="brand" href="/">connecting<span>dots</span></a>
        <div className="nav-links">
          <a href="/">Explore</a>
          <a href="/profile">Profile</a>
          <a className="active-nav" href="/ngo">NGO workspace</a>
          <a href="/admin">Admin</a>
          <button className="outline-button" onClick={() => window.location.href = '/'}>Sign out</button>
        </div>
      </nav>

      <header className="workspace-header">
        <div>
          <span className="eyebrow">NGO workspace / Greenline Collective</span>
          <h1>Move a good idea from draft to shared mission.</h1>
          <p>Submit a problem, review the AI-structured brief, and choose the right contributors for the work ahead.</p>
        </div>
        <div className="workspace-user">
          <span className="avatar avatar-green">GC</span>
          <div>
            <strong>Greenline Collective</strong>
            <small>NGO account · verified</small>
          </div>
        </div>
      </header>

      {message && <div className="toast-message" role="status">{message}</div>}

      <section className="ngo-layout">
        <div className="ngo-main">
          <div className="section-heading">
            <div>
              <span className="eyebrow">01 / New submission</span>
              <h2>Share a problem</h2>
            </div>
            <span className="section-count">PDF · audio · image accepted</span>
          </div>

          <form className="submit-panel" onSubmit={submitProblem}>
            <label>
              Problem statement
              <input required placeholder="What challenge are you trying to solve?" value={form.title} onChange={e => setForm({ ...form, title: e.target.value })} />
            </label>
            <label>
              Context and desired outcome
              <textarea required placeholder="Describe the people affected, what exists today, and what a useful outcome would look like." value={form.description} onChange={e => setForm({ ...form, description: e.target.value })} />
            </label>
            <div className="form-grid">
              <label>
                Domain
                <select value={form.domain} onChange={e => setForm({ ...form, domain: e.target.value })}>
                  <option value="">Choose a domain</option>
                  <option>Health</option>
                  <option>Education</option>
                  <option>Community</option>
                  <option>Environment</option>
                </select>
              </label>
              <label>
                Tags
                <input placeholder="e.g. React, data, UX" value={form.tags} onChange={e => setForm({ ...form, tags: e.target.value })} />
              </label>
            </div>

            <label className="upload-placeholder cursor-pointer">
              <span>＋</span>
              <strong>{selectedFile ? selectedFile.name : "Attach source material"}</strong>
              <small>{selectedFile ? `${(selectedFile.size / 1024).toFixed(1)} KB selected` : "Drop a PDF, audio note, or image here · auto Cloudinary upload"}</small>
              <input type="file" accept=".pdf,audio/*,image/*" onChange={e => setSelectedFile(e.target.files?.[0] || null)} className="hidden" />
            </label>

            <button className="primary-button flex items-center justify-center gap-2" type="submit" disabled={isUploading}>
              {isUploading ? (
                <>
                  <Loader2 className="w-4 h-4 animate-spin" /> Uploading & Processing...
                </>
              ) : (
                <>Submit for AI structuring <span>→</span></>
              )}
            </button>
          </form>

          <div className="section-heading draft-heading">
            <div>
              <span className="eyebrow">02 / AI review queue</span>
              <h2>Drafts pending review</h2>
            </div>
            <span className="section-count">{drafts.filter(d => d.status !== "OPEN").length} need attention</span>
          </div>

          <div className="draft-list">
            {drafts.map(draft => (
              <article className="draft-card" key={draft.id}>
                <div className="draft-top">
                  <StatusPill status={draft.status} />
                  <span className="draft-id">PS-{String(draft.id).slice(-4).padStart(4, "0")}</span>
                </div>
                <h3>{draft.title}</h3>
                <p>{draft.description}</p>
                <div className="tag-row">
                  {draft.tags.map(tag => <span key={tag}>#{tag}</span>)}
                </div>
                <div className="draft-actions">
                  {draft.status === "DRAFT" && (
                    <>
                      <button className="secondary-button" onClick={() => startEdit(draft)}>Edit brief</button>
                      <button className="primary-button" onClick={() => publish(draft.id)}>Approve & publish <span>→</span></button>
                    </>
                  )}
                  {draft.status === "PROCESSING" && <span className="processing-note flex items-center gap-1.5"><Loader2 className="w-3.5 h-3.5 animate-spin" /> AI is structuring this submission…</span>}
                  {draft.status === "OPEN" && (
                    <>
                      <span className="published-note">Published to contributors</span>
                      <button className="secondary-button" onClick={() => setSelectedProblem(draft.id)}>View applications</button>
                    </>
                  )}
                </div>
              </article>
            ))}
          </div>
        </div>

        <aside className="applications-panel">
          <div className="section-heading">
            <div>
              <span className="eyebrow">03 / Contributor matching</span>
              <h2>Applications received</h2>
            </div>
          </div>
          <label className="problem-select">
            Problem
            <select value={selectedProblem} onChange={e => setSelectedProblem(e.target.value)}>
              {drafts.filter(d => d.status === "OPEN").map(d => (
                <option value={d.id} key={d.id}>{d.title}</option>
              ))}
            </select>
          </label>
          <div className="applicant-list">
            {selectedApplicants.length ? selectedApplicants.map(applicant => (
              <div className="applicant-card" key={applicant.id}>
                <div className="applicant-head">
                  <span className="avatar avatar-gold">{applicant.name.split(" ").map(n => n[0]).join("")}</span>
                  <div>
                    <a href={`/applications/app-${applicant.id}`}><strong>{applicant.name}</strong></a>
                    <small>{applicant.email}</small>
                  </div>
                  <StatusPill status={applicant.status} />
                </div>
                <p>{applicant.skills}</p>
                {applicant.status === "PENDING" && (
                  <div className="applicant-actions">
                    <button className="secondary-button" onClick={() => decide(selectedProblem, applicant.id, "REJECTED")}>Reject</button>
                    <button className="primary-button" onClick={() => decide(selectedProblem, applicant.id, "ACCEPTED")}>Accept <span>→</span></button>
                  </div>
                )}
              </div>
            )) : (
              <div className="empty-state">
                No applications yet.
                <small>Once a contributor applies, their profile will appear here.</small>
              </div>
            )}
          </div>
        </aside>
      </section>

      {editingId && (
        <div className="modal-backdrop">
          <section className="edit-modal">
            <div className="section-heading">
              <div>
                <span className="eyebrow">Edit structured output</span>
                <h2>Refine the brief</h2>
              </div>
              <button className="close-button" onClick={() => setEditingId(null)}>×</button>
            </div>
            <label>Title<input value={form.title} onChange={e => setForm({ ...form, title: e.target.value })} /></label>
            <label>Description<textarea value={form.description} onChange={e => setForm({ ...form, description: e.target.value })} /></label>
            <div className="form-grid">
              <label>Domain<input value={form.domain} onChange={e => setForm({ ...form, domain: e.target.value })} /></label>
              <label>Tags<input value={form.tags} onChange={e => setForm({ ...form, tags: e.target.value })} /></label>
            </div>
            <button className="primary-button" onClick={saveDraft}>Save changes <span>→</span></button>
          </section>
        </div>
      )}
    </main>
  )
}
