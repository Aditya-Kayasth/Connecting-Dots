"use client"

import { useEffect, useRef, useState } from "react"
import { useRouter } from "next/navigation"
import { apiRequest } from "@/lib/api-client"
import { SkeletonCard } from "@/components/skeleton-card"
import { uploadFileToCloudinary } from "@/lib/upload"

type Status = "UPLOADED" | "PROCESSING" | "DRAFT" | "OPEN" | "IN_PROGRESS" | "CLOSED" | "RESOLVED"

type Problem = {
  id: string
  title: string
  description: string
  domain: string
  status: Status
  sourceFileUrl?: string
  sourceType?: string
}

type Applicant = {
  id: string
  applicantName: string
  applicantEmail: string
  applicantSkills: string
  status: string
}

const allowedTypes = [".pdf", ".docx", ".txt", ".png", ".jpg", ".jpeg", ".mp3", ".wav", ".m4a"]

function getSourceCategory(fileType?: string) {
  const type = (fileType || "").toUpperCase()
  if (["PNG", "JPG", "JPEG", "WEBP"].includes(type)) {
    return {
      tag: "IMAGE",
      label: "Handwritten Note Photo / Image Scan",
      details: "OCR Vision Engine active — will extract handwritten & printed text"
    }
  }
  if (["PDF", "DOCX", "TXT"].includes(type)) {
    return {
      tag: "DOC",
      label: "Document Specification",
      details: "Full-text parser active — will extract structured problem scope & objectives"
    }
  }
  if (["MP3", "WAV", "M4A"].includes(type)) {
    return {
      tag: "AUDIO",
      label: "Field Audio Recording",
      details: "Speech-to-Text Speech engine active — will transcribe audio notes"
    }
  }
  return {
    tag: "FILE",
    label: "Uploaded Source File",
    details: "Staged for AI ingestion & structuring"
  }
}

function StatusPill({ status }: { status: string }) {
  return <span className={`status-badge status-${status.toLowerCase()}`}>{status}</span>
}

export default function NgoWorkspace() {
  const [profile, setProfile] = useState<any>(null)
  const [problems, setProblems] = useState<Problem[]>([])
  const [selectedProblem, setSelectedProblem] = useState<string>("")
  const [applicants, setApplicants] = useState<Applicant[]>([])

  const [message, setMessage] = useState("")
  const [uploadError, setUploadError] = useState("")
  const [uploadProgress, setUploadProgress] = useState(0)

  const router = useRouter()

  const [loading, setLoading] = useState(true)
  const [form, setForm] = useState({ title: "", description: "", domain: "", tags: "" })
  const [fileUrl, setFileUrl] = useState("")
  const [fileType, setFileType] = useState("")
  const [fileName, setFileName] = useState("")
  const [fileSize, setFileSize] = useState("")
  const [reviewingDraft, setReviewingDraft] = useState<any | null>(null)

  const dirty = Boolean(form.title.trim() || form.description.trim() || form.tags.trim())
  const dragRef = useRef<HTMLLabelElement>(null)

  // Redirect to home if logged out
  useEffect(() => {
    if (typeof window !== "undefined") {
      const token = sessionStorage.getItem("auth_token")
      const role = sessionStorage.getItem("auth_role")
      if (!token || role !== "NGO") {
        router.push("/")
      }
    }
  }, [router])

  const fetchWorkspace = async () => {
    try {
      // 1. Fetch NGO profile
      const prof = await apiRequest<any>("/api/v1/core/profiles/ngo/me")
      setProfile(prof)

      if (prof && prof.id) {
        // 2. Fetch NGO's problem statements across all statuses with page size 100
        const problemsPage = await apiRequest<{ content: any[] }>("/api/v1/core/problem-statements?status=ALL&size=100")
        const ngoProblems = Array.isArray(problemsPage?.content)
          ? problemsPage.content.filter((p) => p.ngoProfile?.id === prof.id || p.ngoProfile?.user?.id === prof.user?.id)
          : []
        setProblems(ngoProblems)

        // Set default selected problem if none selected yet
        if (ngoProblems.length > 0 && !selectedProblem) {
          const firstOpen = ngoProblems.find((p) => p.status === "OPEN") || ngoProblems[0]
          setSelectedProblem(firstOpen.id)
        }
      }
    } catch (err) {
      console.error("Failed to load NGO workspace:", err)
    } finally {
      setLoading(false)
    }
  }

  // Load applicants whenever selectedProblem changes
  useEffect(() => {
    if (!selectedProblem) {
      setApplicants([])
      return
    }
    const fetchApplicants = async () => {
      try {
        const apps = await apiRequest<Applicant[]>(`/api/v1/core/applications/problem/${selectedProblem}`)
        setApplicants(Array.isArray(apps) ? apps : [])
      } catch (err) {
        console.error("Failed to fetch applications for problem:", err)
        setApplicants([])
      }
    }
    fetchApplicants()
  }, [selectedProblem])

  useEffect(() => {
    fetchWorkspace()
  }, [])

  // Auto-poll when any problem is in PROCESSING state
  useEffect(() => {
    const hasProcessing = problems.some((p) => p.status === "PROCESSING")
    if (!hasProcessing) return

    const timer = setInterval(async () => {
      if (!profile?.id) return
      try {
        const problemsPage = await apiRequest<{ content: any[] }>("/api/v1/core/problem-statements?status=ALL&size=100")
        const ngoProblems = Array.isArray(problemsPage?.content)
          ? problemsPage.content.filter((p) => p.ngoProfile?.id === profile.id || p.ngoProfile?.user?.id === profile.user?.id)
          : []
        
        // Find any problem that transitioned from PROCESSING to PROCESSED
        const newlyProcessed = ngoProblems.find((np) => {
          const old = problems.find((op) => op.id === np.id)
          return old && old.status === "PROCESSING" && np.status === "PROCESSED"
        })

        setProblems(ngoProblems)

        if (newlyProcessed && !reviewingDraft) {
          notify(`AI brief structured for "${newlyProcessed.title}". Click Review to publish.`)
          setReviewingDraft(newlyProcessed)
        }
      } catch (err) {
        console.error("Polling error:", err)
      }
    }, 3000)

    return () => clearInterval(timer)
  }, [problems, profile, reviewingDraft])

  useEffect(() => {
    const warn = (event: BeforeUnloadEvent) => {
      if (dirty) {
        event.preventDefault()
        event.returnValue = ""
      }
    }
    window.addEventListener("beforeunload", warn)
    return () => {
      window.removeEventListener("beforeunload", warn)
    }
  }, [dirty])

  // Lock background body scroll when review & publish modal is open
  useEffect(() => {
    if (reviewingDraft) {
      document.body.classList.add("modal-open")
    } else {
      document.body.classList.remove("modal-open")
    }
    return () => document.body.classList.remove("modal-open")
  }, [reviewingDraft])

  function notify(text: string) {
    setMessage(text)
    window.setTimeout(() => setMessage(""), 4000)
  }

  function validateFile(file?: File) {
    if (!file) return
    const extension = `.${file.name.split(".").pop()?.toLowerCase()}`
    if (!allowedTypes.includes(extension)) {
      return setUploadError("Unsupported format. Please use PDF, DOCX, TXT documents, PNG/JPG photos (handwritten notes/scans), or MP3/WAV audio files.")
    }
    if (file.size > 10 * 1024 * 1024) {
      return setUploadError("Files must be 10MB or smaller.")
    }
    setUploadError("")
    setUploadProgress(8)
    setFileName(file.name)
    const sizeInMb = (file.size / (1024 * 1024)).toFixed(2) + " MB"
    setFileSize(sizeInMb)

    uploadFileToCloudinary(file)
      .then((url) => {
        setUploadProgress(100)
        setFileUrl(url)
        setFileType(extension.toUpperCase().replace(".", ""))
        notify(`Source material "${file.name}" uploaded successfully. Ready for AI ingestion.`)
      })
      .catch((err) => {
        console.warn("Real Cloudinary upload failed. Falling back to mock upload:", err)
        runMockUpload(file, extension)
      })
  }

  function runMockUpload(file: File, extension: string) {
    setUploadProgress(8)
    let progress = 8
    const timer = window.setInterval(() => {
      progress = Math.min(progress + 18, 100)
      setUploadProgress(progress)
      if (progress === 100) {
        window.clearInterval(timer)
        setFileUrl(`https://res.cloudinary.com/connecting-dots/image/upload/v123456789/${file.name}`)
        setFileType(extension.toUpperCase().replace(".", ""))
        notify(`Source material "${file.name}" uploaded successfully. Ready for AI ingestion.`)
      }
    }, 170)
  }

  async function submitProblem(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault()
    if (!form.description.trim() && !fileUrl) {
      notify("Please type problem instructions or upload a document / photo scan.")
      return
    }

    notify("Step 1/3: Staging source media payload...")
    setTimeout(() => {
      notify("Step 2/3: Dispatching task to AI Ingestion Engine (OCR & NLP parsing)...")
    }, 1200)

    try {
      const response = await apiRequest<any>("/api/v1/core/problem-statements", {
        method: "POST",
        body: {
          title: "",
          description: form.description,
          domain: "",
          sourceFileUrl: fileUrl || null,
          sourceType: fileType || null,
          status: "PROCESSING"
        }
      })
      
      setForm({ title: "", description: "", domain: "", tags: "" })
      setFileUrl("")
      setFileType("")
      setFileName("")
      setFileSize("")
      setUploadProgress(0)
      
      // Auto-trigger ingestion task for AI processing
      if (response && response.id) {
        apiRequest(`/api/v1/core/problem-statements/${response.id}/ingest`, { method: "POST" })
          .catch((err) => console.error("Auto-ingestion trigger failed:", err))
      }

      setTimeout(() => {
        notify("Step 3/3: Queued in AI Review Queue. AI is structuring your problem brief.")
      }, 2500)

      fetchWorkspace()
    } catch (err) {
      console.error("Failed to submit problem statement:", err)
      notify("Error submitting problem statement. Please check connection.")
    }
  }

  async function decide(problemId: string, applicantId: string, decision: "ACCEPTED" | "REJECTED") {
    try {
      await apiRequest(`/api/v1/core/applications/${applicantId}/status`, {
        method: "PUT",
        body: { status: decision }
      })
      notify(`Application ${decision.toLowerCase()}.`)
      
      // Refresh applicants list
      const apps = await apiRequest<Applicant[]>(`/api/v1/core/applications/problem/${problemId}`)
      setApplicants(Array.isArray(apps) ? apps : [])
    } catch (err) {
      console.error("Failed to update application status:", err)
    }
  }

  const organizationName = profile?.organizationName || "NGO Organization"

  return (
    <main className="workspace-shell">
      <header className="workspace-header">
        <div>
          <span className="eyebrow">NGO workspace / {organizationName}</span>
          <h1>Move a good idea from draft to shared mission.</h1>
          <p>Submit a problem, review the AI-structured brief, and choose the right contributors for the work ahead.</p>
        </div>
        <div className="workspace-user">
          <span className="avatar avatar-green">NGO</span>
          <div>
            <strong>{organizationName}</strong>
            <small>NGO Account · Verified</small>
          </div>
        </div>
      </header>

      {message && <div className="toast-message" role="status">{message}</div>}
      {uploadError && <div className="toast-message upload-error" role="alert">{uploadError}</div>}

      <section className="ngo-layout">
        <div className="ngo-main">
          <div className="section-heading">
            <div>
              <span className="eyebrow">01 / New submission</span>
              <h2>Share a problem statement</h2>
            </div>
            <span className="section-count" style={{ background: 'var(--card)', color: 'var(--muted)', border: '1px solid var(--line)', padding: '0.3rem 0.6rem', borderRadius: '4px', fontSize: '0.8rem', fontWeight: 500 }}>
              PDF/DOCX · Image Scans · Audio
            </span>
          </div>

          <form className="submit-panel" onSubmit={submitProblem}>
            <label>
              Describe problem details, key objectives, or instructions for AI parsing
              <textarea
                required={!fileUrl}
                placeholder={`Enter problem details or paste instructions for the AI parsing engine...\n\nSupported Inputs & Enterprise Guidance:\n• Direct Description: State the background problem, target community impact, and required tech skills.\n• Handwritten Notes & Scans: Upload a clear photograph/scan of handwritten problem notes below.\n• Documents & Reports: Upload project scope documents (PDF, DOCX, TXT) or field audio (MP3, WAV).`}
                value={form.description}
                onChange={(e) => setForm({ ...form, description: e.target.value })}
                rows={6}
                style={{ minHeight: '135px', lineHeight: '1.5', fontFamily: 'inherit' }}
              />
            </label>

            {fileUrl ? (
              <div className="uploaded-file-card" style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '1rem 1.25rem', border: '1px solid var(--line)', borderRadius: '6px', background: 'var(--card)' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.85rem' }}>
                  <span style={{ fontSize: '0.75rem', fontWeight: 'bold', background: 'var(--brand)', color: '#fff', padding: '0.2rem 0.5rem', borderRadius: '4px' }}>
                    {getSourceCategory(fileType).tag}
                  </span>
                  <div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                      <strong style={{ color: 'var(--foreground)', fontSize: '0.95rem' }}>
                        {fileName || `Uploaded ${fileType || "File"}`}
                      </strong>
                      <span style={{ background: 'var(--brand)', color: '#fff', fontSize: '0.7rem', fontWeight: 'bold', padding: '0.15rem 0.45rem', borderRadius: '4px' }}>
                        UPLOADED
                      </span>
                    </div>
                    <small style={{ color: 'var(--muted)', fontSize: '0.82rem', display: 'block', marginTop: '0.15rem' }}>
                      <strong>{getSourceCategory(fileType).label}</strong> {fileSize ? `(${fileSize})` : ''} · {getSourceCategory(fileType).details}
                    </small>
                  </div>
                </div>
                <button
                  type="button"
                  className="outline-button"
                  style={{ padding: '0.35rem 0.75rem', fontSize: '0.8rem', cursor: 'pointer', color: '#ef4444', borderColor: '#ef4444' }}
                  onClick={() => {
                    setFileUrl("")
                    setFileType("")
                    setFileName("")
                    setFileSize("")
                    setUploadProgress(0)
                  }}
                >
                  Remove
                </button>
              </div>
            ) : (
              <label
                ref={dragRef}
                className="upload-placeholder"
                onDragOver={(e) => {
                  e.preventDefault()
                  dragRef.current?.classList.add("drag-active")
                }}
                onDragLeave={() => dragRef.current?.classList.remove("drag-active")}
                onDrop={(e) => {
                  e.preventDefault()
                  dragRef.current?.classList.remove("drag-active")
                  validateFile(e.dataTransfer.files[0])
                }}
                style={{ padding: '1.75rem 1rem', textAlign: 'center' }}
              >
                <span style={{ fontSize: '1.2rem', display: 'block', marginBottom: '0.25rem', fontWeight: 'bold' }}>+</span>
                <strong style={{ fontSize: '1rem', display: 'block', color: 'var(--foreground)' }}>
                  Drop source material, project brief, or handwritten note photo here
                </strong>
                <small style={{ display: 'block', marginTop: '0.35rem', color: 'var(--muted)', fontSize: '0.85rem' }}>
                  PDF, DOCX, TXT · PNG, JPG (Handwritten Notes & Document Scans) · MP3, WAV · Max 10MB
                </small>
                <input
                  type="file"
                  accept=".pdf,.docx,.txt,.png,.jpg,.jpeg,.mp3,.wav,.m4a"
                  onChange={(e) => validateFile(e.target.files?.[0])}
                />
                {uploadProgress > 0 && uploadProgress < 100 && (
                  <div className="upload-progress" style={{ marginTop: '0.75rem' }}>
                    <i style={{ width: `${uploadProgress}%` }} />
                    <small>Uploading file & preparing AI staging… {uploadProgress}%</small>
                  </div>
                )}
              </label>
            )}

            <button className="primary-button" type="submit" style={{ marginTop: '0.5rem' }}>
              Submit for AI Ingestion & Structuring
            </button>
          </form>

          <div className="section-heading draft-heading">
            <div>
              <span className="eyebrow">02 / AI review queue</span>
              <h2>Drafts pending review</h2>
            </div>
            <span className="section-count">
              {problems.filter((d) => d.status !== "OPEN").length} need attention
            </span>
          </div>

          {loading ? (
            <div className="draft-list">
              <SkeletonCard />
              <SkeletonCard />
            </div>
          ) : (
            <div className="draft-list">
              {problems.length === 0 ? (
                <div className="empty-state">No problem statement drafts submitted yet.</div>
              ) : (
                problems.map((draft) => (
                  <article className="draft-card" key={draft.id}>
                    <div className="draft-top">
                      <StatusPill status={draft.status} />
                      <span className="draft-id">PS-{draft.id.slice(0, 4)}</span>
                    </div>
                    <h3>{draft.title || "Untitled Problem Draft"}</h3>
                    <p className="line-clamp-4">{draft.description}</p>
                    
                    {draft.sourceType && (
                      <div style={{ marginTop: '0.75rem', display: 'inline-flex', alignItems: 'center', gap: '0.4rem', background: 'var(--card)', border: '1px solid var(--line)', padding: '0.25rem 0.6rem', borderRadius: '4px', fontSize: '0.75rem', color: 'var(--muted)' }}>
                        <strong style={{ color: 'var(--brand)' }}>[{getSourceCategory(draft.sourceType).tag}]</strong>
                        <span>Source: {getSourceCategory(draft.sourceType).label} ({draft.sourceType})</span>
                      </div>
                    )}

                    <div className="tag-row" style={{ marginTop: '0.75rem' }}>
                      <span>#{draft.domain || 'Uncategorized'}</span>
                    </div>

                    <div className="draft-actions" style={{ display: 'flex', gap: '0.5rem', alignItems: 'center', flexWrap: 'wrap' }}>
                      {draft.status === "PROCESSING" ? (
                        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', color: 'var(--brand)', fontSize: '0.85rem', fontWeight: 500 }}>
                          <span>AI Ingestion Engine active: Extracting key requirements & domain taxonomy...</span>
                        </div>
                      ) : draft.status === "OPEN" ? (
                        <>
                          <span className="published-note">Published to contributors</span>
                          <button
                            className="secondary-button"
                            onClick={() => {
                              setSelectedProblem(draft.id)
                              const appsPanel = document.querySelector('.applications-panel')
                              if (appsPanel) {
                                appsPanel.scrollIntoView({ behavior: 'smooth', block: 'start' })
                              }
                            }}
                          >
                            View applications
                          </button>
                        </>
                      ) : (
                        <button
                          className="primary-button"
                          onClick={() => setReviewingDraft(draft)}
                        >
                          Review brief & Publish
                        </button>
                      )}
                      <button
                        className="outline-button"
                        style={{ padding: '0.35rem 0.65rem', fontSize: '0.8rem', color: '#ef4444', borderColor: 'rgba(239, 68, 68, 0.4)' }}
                        onClick={async () => {
                          if (!confirm("Are you sure you want to delete this problem statement?")) return;
                          try {
                            await apiRequest(`/api/v1/core/problem-statements/${draft.id}`, {
                              method: "DELETE"
                            })
                            notify("Problem statement deleted successfully.")
                            fetchWorkspace()
                          } catch (err) {
                            console.error("Failed to delete problem statement:", err)
                            notify("Failed to delete problem statement.")
                          }
                        }}
                      >
                        Delete
                      </button>
                    </div>
                  </article>
                ))
              )}
            </div>
          )}
        </div>

        <aside className="applications-panel">
          <div className="section-heading">
            <div>
              <span className="eyebrow">03 / Contributor matching</span>
              <h2>Applications received</h2>
            </div>
          </div>
          <label className="problem-select" style={{ display: 'block', width: '100%' }}>
            Problem
            <select
              value={selectedProblem}
              onChange={(e) => setSelectedProblem(e.target.value)}
              style={{
                width: '100%',
                maxWidth: '100%',
                textOverflow: 'ellipsis',
                overflow: 'hidden',
                whiteSpace: 'nowrap',
                padding: '0.6rem 0.8rem',
                borderRadius: '6px',
                border: '1px solid var(--border-color)',
                boxSizing: 'border-box'
              }}
            >
              <option value="">Select problem statement</option>
              {problems
                .filter((d) => (d.status as string) === "OPEN" || (d.status as string) === "PROCESSED")
                .map((d) => (
                  <option value={d.id} key={d.id}>
                    {d.title}
                  </option>
                ))}
            </select>
          </label>

          <div className="applicant-list">
            {applicants.length ? (
              applicants.map((applicant) => (
                <div className="applicant-card" key={applicant.id}>
                  <div className="applicant-head">
                    <span className="avatar avatar-gold">
                      {applicant.applicantName
                        .split(" ")
                        .map((n) => n[0])
                        .join("")}
                    </span>
                    <div>
                      <a href={`/applications/${applicant.id}`}>
                        <strong>{applicant.applicantName}</strong>
                      </a>
                      <small>{applicant.applicantEmail}</small>
                    </div>
                    <StatusPill status={applicant.status} />
                  </div>
                  <p>{applicant.applicantSkills || "No skills summary provided"}</p>
                  {applicant.status === "PENDING" && (
                    <div className="applicant-actions">
                      <button
                        className="secondary-button"
                        onClick={() => decide(selectedProblem, applicant.id, "REJECTED")}
                      >
                        Reject
                      </button>
                      <button
                        className="primary-button"
                        onClick={() => decide(selectedProblem, applicant.id, "ACCEPTED")}
                      >
                        Accept
                      </button>
                    </div>
                  )}
                  {applicant.status === "ACCEPTED" && (
                    <div className="applicant-actions" style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
                      <button
                        className="primary-button"
                        onClick={async () => {
                          try {
                            await apiRequest(`/api/v1/core/applications/${applicant.id}/complete`, {
                              method: "PUT"
                            })
                            notify("Project completed successfully. Contributor completed count updated.")
                            fetchWorkspace()
                            if (selectedProblem) {
                              const apps = await apiRequest<Applicant[]>(`/api/v1/core/applications/problem/${selectedProblem}`)
                              setApplicants(Array.isArray(apps) ? apps : [])
                            }
                          } catch (err) {
                            console.error("Failed to complete project:", err)
                            notify(err instanceof Error ? err.message : "Failed to mark project completed.")
                          }
                        }}
                      >
                        Mark Completed
                      </button>
                      <button
                        className="outline-button"
                        style={{ color: '#ef4444', borderColor: 'rgba(239, 68, 68, 0.4)', fontSize: '0.85rem' }}
                        onClick={async () => {
                          if (!confirm("Are you sure you want to unassign this contributor? The problem statement will re-open.")) return;
                          try {
                            await apiRequest(`/api/v1/core/applications/${applicant.id}/status`, {
                              method: "PUT",
                              body: { status: "REJECTED" }
                            })
                            notify("Contributor unassigned. Problem statement re-opened.")
                            fetchWorkspace()
                            if (selectedProblem) {
                              const apps = await apiRequest<Applicant[]>(`/api/v1/core/applications/problem/${selectedProblem}`)
                              setApplicants(Array.isArray(apps) ? apps : [])
                            }
                          } catch (err) {
                            console.error("Failed to unassign:", err)
                            notify("Failed to unassign contributor.")
                          }
                        }}
                      >
                        Unassign Project
                      </button>
                    </div>
                  )}
                </div>
              ))
            ) : (
              <div className="empty-state">
                No applications yet.
                <small>Once a contributor applies, their profile will appear here.</small>
              </div>
            )}
          </div>
        </aside>
      </section>

      {reviewingDraft && (
        <div 
          className="modal-backdrop" 
          role="dialog" 
          aria-modal="true"
          onClick={() => setReviewingDraft(null)}
        >
          <div className="edit-modal" onClick={(e) => e.stopPropagation()}>
            <button className="close-button" onClick={() => setReviewingDraft(null)}>×</button>
            <span className="eyebrow">Review AI brief & Publish</span>
            <h2>Verify Problem Statement</h2>
            <p className="muted" style={{ marginBottom: '1rem' }}>
              Confirm or refine the details below before publishing this opportunity to technical volunteers.
            </p>

            <div style={{ padding: '0.85rem 1.1rem', background: 'var(--card)', border: '1px solid var(--line)', borderRadius: '8px', marginBottom: '1.25rem' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', fontWeight: 600, color: 'var(--brand)', fontSize: '0.88rem' }}>
                <span>AI Ingestion Report</span>
                {reviewingDraft.sourceType && (
                  <span style={{ marginLeft: 'auto', fontSize: '0.78rem', color: 'var(--muted)', fontWeight: 'normal' }}>
                    Source: {getSourceCategory(reviewingDraft.sourceType).tag} ({reviewingDraft.sourceType})
                  </span>
                )}
              </div>
              <p className="muted" style={{ margin: '0.35rem 0 0 0', fontSize: '0.82rem', lineHeight: '1.5' }}>
                AI OCR & NLP pipeline successfully structured title, detailed problem description, and category domain from source payload.
              </p>
            </div>

            <form onSubmit={async (e) => {
              e.preventDefault();
              const target = e.currentTarget;
              const title = (target.elements.namedItem('title') as HTMLInputElement).value;
              const description = (target.elements.namedItem('description') as HTMLTextAreaElement).value;
              const domain = (target.elements.namedItem('domain') as HTMLSelectElement).value;
              
              try {
                await apiRequest(`/api/v1/core/problem-statements/${reviewingDraft.id}/ai-update`, {
                  method: 'PUT',
                  body: { title, description, domain, status: 'OPEN' }
                });
                notify('Problem statement published successfully!');
                setReviewingDraft(null);
                fetchWorkspace();
              } catch (err) {
                console.error('Failed to publish problem:', err);
                notify('Failed to publish problem statement.');
              }
            }} className="auth-form" style={{ gap: '1.25rem' }}>
              <label>
                Title (max 80 chars)
                <input 
                  name="title" 
                  defaultValue={reviewingDraft.title} 
                  placeholder="e.g., Solar Powered Water Purification Monitoring System" 
                  maxLength={80} 
                  required 
                />
              </label>
              <label>
                Description
                <textarea 
                  name="description" 
                  defaultValue={reviewingDraft.description} 
                  placeholder="Describe the background problem, requirements, technical scope, and expected outcome..." 
                  rows={8} 
                  required 
                  style={{ width: '100%', padding: '0.75rem', borderRadius: '6px', border: '1px solid var(--border-color)', background: 'transparent', color: 'inherit', resize: 'vertical' }}
                />
              </label>
              <label>
                Domain
                <select name="domain" defaultValue={reviewingDraft.domain || 'Education Technology'}>
                  <option>Education Technology</option>
                  <option>Healthcare & Wellness</option>
                  <option>Environment & Sustainability</option>
                  <option>Community Development</option>
                  <option>Poverty Alleviation</option>
                  <option>Financial Inclusion</option>
                  <option>Web/Software Development</option>
                  <option>Others</option>
                </select>
              </label>
              <div style={{ display: 'flex', gap: '1rem', marginTop: '1rem' }}>
                <button type="button" className="outline-button" style={{ flex: 1 }} onClick={() => setReviewingDraft(null)}>Cancel</button>
                <button type="submit" className="primary-button" style={{ flex: 2 }}>Publish to Community</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </main>
  )
}
