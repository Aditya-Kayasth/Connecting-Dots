import { Suspense } from "react"
import ReviewForm from "@/components/review-form"

export default function ReviewPage() {
  return (
    <main className="page-shell">
      <div className="narrow-content">
        <a className="back-link" href="/">← Back to dashboard</a>
        <Suspense fallback={<div>Loading review form...</div>}>
          <ReviewForm />
        </Suspense>
      </div>
    </main>
  )
}
