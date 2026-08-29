export function SkeletonCard() {
  return <div className="skeleton-card" aria-hidden="true"><span /><span /><span /><span /></div>
}

export function SkeletonTable({ rows = 4 }: { rows?: number }) {
  return <div className="skeleton-table" aria-hidden="true">{Array.from({ length: rows }).map((_, index) => <div className="skeleton-table-row" key={index}><span /><span /><span /><span /></div>)}</div>
}

export default SkeletonCard
