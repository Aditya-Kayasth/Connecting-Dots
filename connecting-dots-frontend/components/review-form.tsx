"use client"

import { useState } from "react"
import { apiRequest } from "@/lib/api-client"

export function ReviewForm({ applicationId = "app-2048", recipientName = "Maya Chen" }: { applicationId?: string; recipientName?: string }) {
  const [rating, setRating] = useState(0)
  const [comment, setComment] = useState("")
  const [submitted, setSubmitted] = useState(false)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState("")

  async function submitReview(event: React.FormEvent) {
    event.preventDefault()
    if (!rating || !comment.trim()) { setError("Choose a rating and add a short comment."); return }
    setBusy(true); setError("")
    try { await apiRequest("/api/v1/core/reviews", { method: "POST", body: { applicationId, rating, comment: comment.trim() } }); setSubmitted(true) }
    catch (err) { setError(err instanceof Error ? err.message : "Unable to submit review.") }
    finally { setBusy(false) }
  }

  if (submitted) return <div className="success-panel"><span className="eyebrow">Review submitted</span><h2>Thank you for sharing your experience.</h2><p>Your feedback helps the Connecting Dots community build trust.</p></div>

  return <form onSubmit={submitReview} className="review-form">
    <div><span className="eyebrow">Completed application</span><h2>How was working with {recipientName}?</h2><p className="muted">Your review will appear on their public profile.</p></div>
    <fieldset><legend className="field-label">Your rating</legend><div className="rating-row">{[1,2,3,4,5].map((value) => <button key={value} type="button" aria-label={`${value} star${value > 1 ? "s" : ""}`} aria-pressed={rating === value} className={`star-button ${rating >= value ? "selected" : ""}`} onClick={() => setRating(value)}>★</button>)}</div></fieldset>
    <label className="field-label" htmlFor="review-comment">Comment<textarea id="review-comment" value={comment} onChange={(event) => setComment(event.target.value)} placeholder="Share a thoughtful note about the collaboration..." rows={5} /></label>
    {error && <p className="form-error" role="alert">{error}</p>}
    <button className="primary-button" disabled={busy} type="submit">{busy ? "Submitting..." : "Submit review"}</button>
  </form>
} 

export default ReviewForm

function StarIcon() { return null }
