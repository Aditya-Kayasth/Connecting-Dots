import { useEffect, useState } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import apiClient from '../api/client'

// ── Types ──────────────────────────────────────────────────────────────────────

type TechCategory = 'SOFTWARE_WEB' | 'DATA_SCIENCE_ML' | 'IOT_HARDWARE' | 'PROCESS_AUTOMATION'

interface LiveProblem {
  id: string
  ngoName: string
  rawDescription: string
  structuredProblem: string
  techCategory: TechCategory | string
  status: string
}

// ── Style maps ─────────────────────────────────────────────────────────────────

const CATEGORY_STYLES: Record<string, { badge: string; border: string; dot: string; hover: string }> = {
  SOFTWARE_WEB: {
    badge: 'bg-blue-100 text-blue-800',
    border: 'border-blue-200',
    dot: 'bg-blue-500',
    hover: 'hover:bg-blue-600',
  },
  DATA_SCIENCE_ML: {
    badge: 'bg-purple-100 text-purple-800',
    border: 'border-purple-200',
    dot: 'bg-purple-500',
    hover: 'hover:bg-purple-600',
  },
  IOT_HARDWARE: {
    badge: 'bg-green-100 text-green-800',
    border: 'border-green-200',
    dot: 'bg-green-500',
    hover: 'hover:bg-green-600',
  },
  PROCESS_AUTOMATION: {
    badge: 'bg-orange-100 text-orange-800',
    border: 'border-orange-200',
    dot: 'bg-orange-500',
    hover: 'hover:bg-orange-600',
  },
}

const DEFAULT_STYLE = {
  badge: 'bg-gray-100 text-gray-600',
  border: 'border-gray-200',
  dot: 'bg-gray-400',
  hover: 'hover:bg-gray-600',
}

const FILTER_OPTIONS = ['ALL', 'SOFTWARE_WEB', 'DATA_SCIENCE_ML', 'IOT_HARDWARE', 'PROCESS_AUTOMATION'] as const
type FilterOption = typeof FILTER_OPTIONS[number]

// ── Component ──────────────────────────────────────────────────────────────────

export default function ContributorDashboard() {
  const [problems, setProblems] = useState<LiveProblem[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [filter, setFilter] = useState<FilterOption>('ALL')
  const [offeredIds, setOfferedIds] = useState<Set<string>>(new Set())

  const fetchProblems = async () => {
    setLoading(true)
    setError(null)
    try {
      const { data } = await apiClient.get<LiveProblem[]>('/problems/open')
      setProblems(data)
    } catch {
      setError('Could not load problems. Make sure the backend is running and your .env is configured.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchProblems()
  }, [])

  const filtered = filter === 'ALL'
    ? problems
    : problems.filter(p => p.techCategory === filter)

  const handleOffer = (id: string) => setOfferedIds(prev => new Set(prev).add(id))

  return (
    <div className="bg-indigo-50 min-h-full">
      <div className="max-w-4xl mx-auto px-4 py-10 space-y-8">

        {/* Page header */}
        <div className="flex items-end justify-between gap-4 flex-wrap">
          <div>
            <h1 className="text-3xl font-bold text-gray-900">Project Feed</h1>
            <p className="text-gray-500 text-sm mt-1">
              Live NGO challenges from the database — offer your skills where they matter most.
            </p>
          </div>
          <div className="flex items-center gap-3">
            {!loading && !error && (
              <span className="text-sm font-semibold text-indigo-600 bg-indigo-100 px-3 py-1.5 rounded-full">
                {problems.length} open
              </span>
            )}
            <button
              onClick={fetchProblems}
              disabled={loading}
              className="text-xs font-medium text-indigo-500 hover:text-indigo-700 border border-indigo-200 hover:border-indigo-400 px-3 py-1.5 rounded-full transition-colors disabled:opacity-40"
            >
              ↻ Refresh
            </button>
          </div>
        </div>

        {/* Category filter pills */}
        <div className="flex flex-wrap gap-2">
          {FILTER_OPTIONS.map(opt => (
            <button
              key={opt}
              onClick={() => setFilter(opt)}
              className={`text-xs font-semibold px-3 py-1.5 rounded-full border transition-colors ${
                filter === opt
                  ? 'bg-indigo-600 text-white border-indigo-600'
                  : 'bg-white text-gray-500 border-gray-200 hover:border-indigo-300 hover:text-indigo-600'
              }`}
            >
              {opt === 'ALL' ? 'All Categories' : opt.replace(/_/g, ' ')}
            </button>
          ))}
        </div>

        {/* ── Loading ── */}
        {loading && (
          <div className="flex flex-col items-center justify-center py-24 gap-4">
            <div className="w-9 h-9 border-4 border-indigo-200 border-t-indigo-600 rounded-full animate-spin" />
            <p className="text-sm text-gray-400">Fetching live problems…</p>
          </div>
        )}

        {/* ── Error ── */}
        <AnimatePresence>
          {error && !loading && (
            <motion.div
              initial={{ opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0 }}
              className="bg-red-50 border border-red-200 rounded-2xl px-5 py-4 flex items-start gap-3"
            >
              <span className="text-red-400 text-lg flex-shrink-0">⚠</span>
              <div>
                <p className="text-sm font-semibold text-red-700">Failed to load problems</p>
                <p className="text-xs text-red-500 mt-0.5">{error}</p>
                <button
                  onClick={fetchProblems}
                  className="mt-2 text-xs font-semibold text-red-600 hover:text-red-800 underline underline-offset-2"
                >
                  Try again
                </button>
              </div>
            </motion.div>
          )}
        </AnimatePresence>

        {/* ── Empty state ── */}
        {!loading && !error && problems.length === 0 && (
          <motion.div
            initial={{ opacity: 0 }} animate={{ opacity: 1 }}
            className="text-center py-24 space-y-3"
          >
            <p className="text-5xl">🔍</p>
            <p className="text-gray-600 font-medium">No open problems found.</p>
            <p className="text-gray-400 text-sm">
              Check back later, or submit a new problem from the NGO Dashboard.
            </p>
          </motion.div>
        )}

        {/* ── Empty filtered state ── */}
        {!loading && !error && problems.length > 0 && filtered.length === 0 && (
          <div className="text-center py-16 text-gray-400">
            <p className="text-sm">No problems in this category yet.</p>
          </div>
        )}

        {/* ── Problem cards ── */}
        {!loading && !error && (
          <div className="space-y-4">
            {filtered.map((problem, i) => {
              const style = CATEGORY_STYLES[problem.techCategory] ?? DEFAULT_STYLE
              const isSolved = problem.status !== 'OPEN'
              const hasOffered = offeredIds.has(problem.id)

              return (
                <motion.div
                  key={problem.id}
                  initial={{ opacity: 0, y: 16 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ delay: i * 0.05, duration: 0.3 }}
                  className={`bg-white rounded-2xl border p-5 transition-all ${
                    isSolved
                      ? 'border-gray-100 opacity-50 grayscale'
                      : `${style.border} shadow-sm`
                  }`}
                >
                  {/* Top row */}
                  <div className="flex items-start justify-between gap-3 mb-3">
                    <div className="flex items-center gap-2 flex-wrap">
                      <span className={`w-2.5 h-2.5 rounded-full flex-shrink-0 ${isSolved ? 'bg-gray-300' : style.dot}`} />
                      <span className="font-semibold text-gray-800 text-sm">{problem.ngoName}</span>
                    </div>
                    <span className={`text-xs font-semibold px-2.5 py-1 rounded-full flex-shrink-0 ${
                      isSolved ? 'bg-gray-100 text-gray-400' : style.badge
                    }`}>
                      {problem.techCategory.replace(/_/g, ' ')}
                    </span>
                  </div>

                  {/* Structured problem */}
                  <p className={`text-sm leading-relaxed mb-4 ${isSolved ? 'text-gray-400' : 'text-gray-700'}`}>
                    {problem.structuredProblem}
                  </p>

                  {/* Footer */}
                  <div className="flex items-center justify-between gap-3">
                    {/* Status pill */}
                    {isSolved ? (
                      <span className="inline-flex items-center gap-1.5 text-xs font-semibold px-3 py-1.5 rounded-full bg-emerald-100 text-emerald-700">
                        <span className="w-1.5 h-1.5 rounded-full bg-emerald-500" />
                        Completed
                      </span>
                    ) : (
                      <span className="inline-flex items-center gap-1.5 text-xs font-semibold px-3 py-1.5 rounded-full bg-amber-100 text-amber-700">
                        <span className="w-1.5 h-1.5 rounded-full bg-amber-400 animate-pulse" />
                        Open
                      </span>
                    )}

                    {/* Action */}
                    {isSolved ? (
                      <span className="text-xs font-semibold px-4 py-1.5 rounded-full bg-emerald-500 text-white">
                        ✓ Completed
                      </span>
                    ) : hasOffered ? (
                      <span className="text-xs font-semibold px-4 py-1.5 rounded-full bg-indigo-100 text-indigo-700">
                        Help Offered ✓
                      </span>
                    ) : (
                      <button
                        onClick={() => handleOffer(problem.id)}
                        className={`text-xs font-semibold px-4 py-1.5 rounded-full bg-indigo-500 text-white transition-colors ${style.hover}`}
                      >
                        Offer Help
                      </button>
                    )}
                  </div>

                  {/* UUID footer */}
                  <p className="text-xs text-gray-300 font-mono mt-3 truncate">{problem.id}</p>
                </motion.div>
              )
            })}
          </div>
        )}

      </div>
    </div>
  )
}
