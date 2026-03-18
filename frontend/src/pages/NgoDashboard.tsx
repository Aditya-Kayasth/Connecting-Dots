import { useState } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import axios from 'axios'
import { useProjects } from '../context/ProjectContext'

interface ProblemResult {
  id: string
  ngoName: string
  structuredProblem: string
  techCategory: string
  status: string
}

const CATEGORY_COLORS: Record<string, string> = {
  SOFTWARE_WEB: 'bg-blue-100 text-blue-800',
  DATA_SCIENCE_ML: 'bg-purple-100 text-purple-800',
  IOT_HARDWARE: 'bg-green-100 text-green-800',
  PROCESS_AUTOMATION: 'bg-orange-100 text-orange-800',
}

export default function NgoDashboard() {
  const { projects, markAsSolved } = useProjects()

  // AI submit form state
  const [ngoName, setNgoName] = useState('')
  const [rawDescription, setRawDescription] = useState('')
  const [loading, setLoading] = useState(false)
  const [result, setResult] = useState<ProblemResult | null>(null)
  const [error, setError] = useState<string | null>(null)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setLoading(true)
    setError(null)
    setResult(null)
    try {
      const { data } = await axios.post<ProblemResult>(
        'http://localhost:8080/api/v1/problems/submit',
        { ngoName, rawDescription }
      )
      setResult(data)
      setNgoName('')
      setRawDescription('')
    } catch {
      setError('Something went wrong. Make sure the backend is running.')
    } finally {
      setLoading(false)
    }
  }

  const openCount = projects.filter(p => p.status === 'OPEN').length
  const solvedCount = projects.filter(p => p.status === 'SOLVED').length

  return (
    <div className="bg-amber-50 min-h-full">
      <div className="max-w-4xl mx-auto px-4 py-10 space-y-10">

        {/* Page header */}
        <div>
          <h1 className="text-3xl font-bold text-gray-900">NGO Dashboard</h1>
          <p className="text-gray-500 text-sm mt-1">
            Manage your active projects and submit new challenges for AI structuring.
          </p>
        </div>

        {/* Stats row */}
        <div className="grid grid-cols-2 sm:grid-cols-3 gap-4">
          <StatCard label="Total Projects" value={projects.length} color="text-gray-800" />
          <StatCard label="Open" value={openCount} color="text-amber-600" />
          <StatCard label="Solved" value={solvedCount} color="text-emerald-600" />
        </div>

        {/* ── Active Projects ── */}
        <section>
          <h2 className="text-lg font-semibold text-gray-800 mb-4">Active Projects</h2>
          <div className="space-y-3">
            {projects.map((project, i) => {
              const isSolved = project.status === 'SOLVED'
              return (
                <motion.div
                  key={project.id}
                  initial={{ opacity: 0, y: 12 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ delay: i * 0.05, duration: 0.3 }}
                  className={`bg-white rounded-2xl border p-5 transition-all ${
                    isSolved
                      ? 'border-gray-100 opacity-60'
                      : 'border-amber-100 shadow-sm'
                  }`}
                >
                  <div className="flex items-start justify-between gap-4">
                    {/* Left */}
                    <div className="flex-1 min-w-0 space-y-1.5">
                      <div className="flex items-center gap-2 flex-wrap">
                        <span className="font-semibold text-gray-800 text-sm">
                          {project.ngoName}
                        </span>
                        <span className="text-xs text-gray-400">{project.state}</span>
                        <span className={`text-xs font-semibold px-2 py-0.5 rounded-full ${
                          CATEGORY_COLORS[project.techCategory] ?? 'bg-gray-100 text-gray-600'
                        }`}>
                          {project.techCategory.replace(/_/g, ' ')}
                        </span>
                      </div>
                      <p className="text-sm text-gray-600 leading-relaxed line-clamp-2">
                        {project.structuredProblem}
                      </p>
                    </div>

                    {/* Right — status / action */}
                    <div className="flex-shrink-0">
                      {isSolved ? (
                        <span className="inline-flex items-center gap-1 text-xs font-semibold px-3 py-1.5 rounded-full bg-emerald-100 text-emerald-700">
                          ✓ Solved
                        </span>
                      ) : (
                        <button
                          onClick={() => markAsSolved(project.id)}
                          className="text-xs font-semibold px-3 py-1.5 rounded-full border border-amber-300 text-amber-700 hover:bg-amber-500 hover:text-white hover:border-amber-500 transition-colors"
                        >
                          Mark Solved
                        </button>
                      )}
                    </div>
                  </div>
                </motion.div>
              )
            })}
          </div>
        </section>

        {/* ── AI Submit Form ── */}
        <section>
          <h2 className="text-lg font-semibold text-gray-800 mb-4">Submit a New Problem</h2>
          <div className="bg-white rounded-2xl shadow-sm border border-amber-100 p-6 space-y-5">
            <form onSubmit={handleSubmit} className="space-y-5">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1.5">NGO Name</label>
                <input
                  type="text"
                  value={ngoName}
                  onChange={e => setNgoName(e.target.value)}
                  required
                  placeholder="e.g. Green Earth Foundation"
                  className="w-full border border-gray-200 rounded-xl px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-amber-400 transition"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1.5">Raw Description</label>
                <textarea
                  value={rawDescription}
                  onChange={e => setRawDescription(e.target.value)}
                  required
                  rows={4}
                  placeholder="Describe your problem in plain language. The AI will structure it."
                  className="w-full border border-gray-200 rounded-xl px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-amber-400 transition resize-none"
                />
              </div>
              <button
                type="submit"
                disabled={loading}
                className="w-full bg-amber-500 hover:bg-amber-600 disabled:bg-amber-300 text-white font-semibold py-2.5 rounded-xl transition-colors text-sm"
              >
                {loading ? 'Analysing with AI…' : 'Submit Problem'}
              </button>
            </form>

            <AnimatePresence>
              {error && (
                <motion.div
                  initial={{ opacity: 0, y: 6 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0 }}
                  className="bg-red-50 border border-red-200 text-red-700 rounded-xl px-4 py-3 text-sm"
                >
                  {error}
                </motion.div>
              )}
            </AnimatePresence>

            <AnimatePresence>
              {result && (
                <motion.div
                  initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0 }}
                  className="bg-emerald-50 border border-emerald-100 rounded-xl p-4 space-y-2"
                >
                  <div className="flex items-center gap-2">
                    <span className="text-emerald-500">✓</span>
                    <span className="font-semibold text-gray-800 text-sm">Problem Structured Successfully</span>
                  </div>
                  <p className="text-sm text-gray-700">{result.structuredProblem}</p>
                  <div className="flex gap-2 flex-wrap">
                    <span className={`text-xs font-semibold px-2.5 py-1 rounded-full ${CATEGORY_COLORS[result.techCategory] ?? 'bg-gray-100 text-gray-700'}`}>
                      {result.techCategory.replace(/_/g, ' ')}
                    </span>
                    <span className="text-xs font-semibold px-2.5 py-1 rounded-full bg-emerald-100 text-emerald-700">
                      {result.status}
                    </span>
                  </div>
                </motion.div>
              )}
            </AnimatePresence>
          </div>
        </section>

      </div>
    </div>
  )
}

// ── Stat card ──────────────────────────────────────────────────────────────────

function StatCard({ label, value, color }: { label: string; value: number; color: string }) {
  return (
    <div className="bg-white rounded-2xl border border-gray-100 shadow-sm px-5 py-4">
      <p className="text-xs text-gray-400 uppercase tracking-widest mb-1">{label}</p>
      <p className={`text-3xl font-bold ${color}`}>{value}</p>
    </div>
  )
}
