"use client"

import { useEffect, useState } from "react"
import { apiRequest } from "@/lib/api-client"

type Review = { id: string; rating: number; comment: string; authorName?: string; createdAt?: string }
const fallback: Review[] = [{ id: "1", rating: 5, comment: "Maya translated a complex data problem into a clear, maintainable dashboard. She was thoughtful, communicative, and generous with her time.", authorName: "Amina Okafor", createdAt: "2 weeks ago" }, { id: "2", rating: 5, comment: "An excellent collaborator from kickoff to handoff. Our team felt supported throughout.", authorName: "David Mensah", createdAt: "1 month ago" }]

export function ReviewsList({ userId = "contributor-2048" }: { userId?: string }) {
 const [reviews, setReviews] = useState<Review[]>(fallback)
 useEffect(() => { apiRequest<Review[]>(`/api/v1/core/users/${userId}/reviews`).then(setReviews).catch(() => {}) }, [userId])
 return <section className="reviews-section"><div className="section-heading"><div><span className="eyebrow">Community feedback</span><h2>Reviews received</h2></div><span className="count-pill">{reviews.length} reviews</span></div><div className="reviews-stack">{reviews.map((review) => <article className="review-card" key={review.id}><div className="review-meta"><div className="avatar">{(review.authorName || "A").slice(0,1)}</div><div><strong>{review.authorName || "Community member"}</strong><p className="muted">{review.createdAt || "Recently"}</p></div><span className="stars">{"★".repeat(review.rating)}<span className="stars-muted">{"★".repeat(5-review.rating)}</span></span></div><p>{review.comment}</p></article>)}</div></section>
}

export default ReviewsList

function StarIcon() { return null }
