import { useEffect, useState } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import apiClient from '../api/client'

interface LiveProblem {
  id: string
  ngoName: string
  rawDescription: string
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
  const [projects, setProjects] = useState<LiveProblem[]>([])
  const [fetchLoading, setFetchLoading] = useState(true)
  const [solvingId, setSolvingId] = useState<string | null>(null)

  // shared result/error state for both forms
  const [result, setResult] = useState<LiveProblem | null>(null)
  const [error, setError] = useState<string | null>(null)

  // text form state
  const [ngoName, setNgoName] = useState('')
  const [rawDescription, setRawDescription] = useState('')
  const [submitLoading, setSubmitLoading] = useState(false)

  // pdf upload state
  const [pdfNgoName, setPdfNgoName] = useState('')
  const [pdfFile, setPdfFile] = useState<File | null>(null)
  const [pdfLoading, setPdfLoading] = useState(false)

  const fetchProblems = async () => {
    setFetchLoading(true)
    try {
      const { data } = await apiClient.get<LiveProblem[]>('/problems/open')
      setProjects(data)
    } catch {
      // silently fail — stats will just show 0
    } finally {
      setFetchLoading(false)
    }
  }

  useEffect(() => { fetchProblems() }, [])

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setSubmitLoading(true)
    setError(null)
    setResult(null)
    try {
      const { data } = await apiClient.post<LiveProblem>('/problems/submit', {
        ngoName,
        rawDescription,
      })
      setResult(data)
      setNgoName('')
      setRawDescription('')
      fetchProblems()
    } catch {
      setError('Something went wrong. Make sure the backend is running and your .env is configured.')
    } finally {
      setSubmitLoading(false)
    }
  }

  const handlePdfUpload = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!pdfFile) return
    setPdfLoading(true)
    setError(null)
    setResult(null)
    try {
      const formData = new FormData()
      formData.append('file', pdfFile)
      formData.append('ngoName', pdfNgoName)
      const { data } = await apiClient.post<LiveProblem>('/problems/upload', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      })
      setResult(data)
      setPdfNgoName('')
      setPdfFile(null)
      // reset the file input
      const fileInput = document.getElementById('pdf-input') as HTMLInputElement
      if (fileInput) fileInput.value = ''
      fetchProblems()
    } catch {
      setError('PDF upload failed. Ensure the file is a valid PDF and the backend is running.')
    } finally {
      setPdfLoading(false)
    }
  }

  const handleMarkSolved = async (id: string) => {
    setSolvingId(id)
    try {
      await apiClient.patch(`/problems/${id}/status?status=SOLVED`)
      await fetchProblems()
    } catch {
      // no-op — button will re-enable
    } finally {
      setSolvingId(null)
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

          {fetchLoading ? (
            <div className="flex items-center justify-center py-16">
              <div className="w-8 h-8 border-4 border-amber-200 border-t-amber-500 rounded-full animate-spin" />
            </div>
          ) : projects.length === 0 ? (
            <p className="text-sm text-gray-400 text-center py-12">
              No projects yet. Submit your first problem below.
            </p>
          ) : (
            <div className="space-y-3">
              {projects.map((project, i) => {
                const isSolved = project.status === 'SOLVED'
                const isPatching = solvingId === project.id
                return (
                  <motion.div
                    key={project.id}
                    initial={{ opacity: 0, y: 12 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ delay: i * 0.05, duration: 0.3 }}
                    className={`bg-white rounded-2xl border p-5 transition-all ${
                      isSolved ? 'border-gray-100 opacity-60' : 'border-amber-100 shadow-sm'
                    }`}
                  >
                    <div className="flex items-start justify-between gap-4">
                      <div className="flex-1 min-w-0 space-y-1.5">
                        <div className="flex items-center gap-2 flex-wrap">
                          <span className="font-semibold text-gray-800 text-sm">{project.ngoName}</span>
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

                      <div className="flex-shrink-0">
                        {isSolved ? (
                          <span className="inline-flex items-center gap-1 text-xs font-semibold px-3 py-1.5 rounded-full bg-emerald-100 text-emerald-700">
                            ✓ Solved
                          </span>
                        ) : (
                          <button
                            onClick={() => handleMarkSolved(project.id)}
                            disabled={isPatching}
                            className="inline-flex items-center gap-1.5 text-xs font-semibold px-3 py-1.5 rounded-full border border-amber-300 text-amber-700 hover:bg-amber-500 hover:text-white hover:border-amber-500 disabled:opacity-50 transition-colors"
                          >
                            {isPatching ? (
                              <>
                                <span className="w-3 h-3 border-2 border-amber-400/40 border-t-amber-600 rounded-full animate-spin" />
                                Saving…
                              </>
                            ) : 'Mark Solved'}
                          </button>
                        )}
                      </div>
                    </div>
                  </motion.div>
                )
              })}
            </div>
          )}
        </section>

        {/* ── PDF Upload ── */}
        <section>
          <h2 className="text-lg font-semibold text-gray-800 mb-4">Upload a Document (PDF)</h2>
          <div className="bg-white rounded-2xl shadow-sm border border-amber-100 p-6">
            <form onSubmit={handlePdfUpload} className="space-y-5">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1.5">NGO Name</label>
                <input
                  type="text"
                  value={pdfNgoName}
                  onChange={e => setPdfNgoName(e.target.value)}
                  required
                  placeholder="e.g. Green Earth Foundation"
                  className="w-full border border-gray-200 rounded-xl px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-amber-400 transition"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1.5">PDF Document</label>
                <label className="flex flex-col items-center justify-center w-full h-28 border-2 border-dashed border-amber-200 rounded-xl cursor-pointer bg-amber-50 hover:bg-amber-100 transition-colors">
                  <div className="flex flex-col items-center gap-1 pointer-events-none">
                    <span className="text-2xl">📄</span>
                    <span className="text-sm text-gray-500">
                      {pdfFile ? pdfFile.name : 'Click to select a PDF'}
                    </span>
                    {pdfFile && (
                      <span className="text-xs text-gray-400">
                        {(pdfFile.size / 1024).toFixed(1)} KB
                      </span>
                    )}
                  </div>
                  <input
                    id="pdf-input"
                    type="file"
                    accept=".pdf"
                    required
                    className="hidden"
                    onChange={e => setPdfFile(e.target.files?.[0] ?? null)}
                  />
                </label>
              </div>
              <button
                type="submit"
                disabled={pdfLoading || !pdfFile}
                className="w-full bg-amber-500 hover:bg-amber-600 disabled:bg-amber-300 text-white font-semibold py-2.5 rounded-xl transition-colors text-sm"
              >
                {pdfLoading ? (
                  <span className="flex items-center justify-center gap-2">
                    <span className="w-4 h-4 border-2 border-white/40 border-t-white rounded-full animate-spin" />
                    Extracting & analyzing PDF…
                  </span>
                ) : 'Upload & Analyze PDF'}
              </button>
            </form>
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
                disabled={submitLoading}
                className="w-full bg-amber-500 hover:bg-amber-600 disabled:bg-amber-300 text-white font-semibold py-2.5 rounded-xl transition-colors text-sm"
              >
                {submitLoading ? (
                  <span className="flex items-center justify-center gap-2">
                    <span className="w-4 h-4 border-2 border-white/40 border-t-white rounded-full animate-spin" />
                    AI is analyzing your problem…
                  </span>
                ) : 'Submit Problem'}
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

function StatCard({ label, value, color }: { label: string; value: number; color: string }) {
  return (
    <div className="bg-white rounded-2xl border border-gray-100 shadow-sm px-5 py-4">
      <p className="text-xs text-gray-400 uppercase tracking-widest mb-1">{label}</p>
      <p className={`text-3xl font-bold ${color}`}>{value}</p>
    </div>
  )
}
