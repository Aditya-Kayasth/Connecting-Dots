import { useState } from 'react'
import { motion } from 'framer-motion'
import { useProjects, TechCategory } from '../context/ProjectContext'

const CATEGORY_STYLES: Record<TechCategory, { badge: string; border: string; dot: string; activeBg: string }> = {
  SOFTWARE_WEB: {
    badge: 'bg-blue-100 text-blue-800',
    border: 'border-blue-200',
    dot: 'bg-blue-500',
    activeBg: 'hover:bg-blue-600',
  },
  DATA_SCIENCE_ML: {
    badge: 'bg-purple-100 text-purple-800',
    border: 'border-purple-200',
    dot: 'bg-purple-500',
    activeBg: 'hover:bg-purple-600',
  },
  IOT_HARDWARE: {
    badge: 'bg-green-100 text-green-800',
    border: 'border-green-200',
    dot: 'bg-green-500',
    activeBg: 'hover:bg-green-600',
  },
  PROCESS_AUTOMATION: {
    badge: 'bg-orange-100 text-orange-800',
    border: 'border-orange-200',
    dot: 'bg-orange-500',
    activeBg: 'hover:bg-orange-600',
  },
}

const FILTER_OPTIONS = ['ALL', 'SOFTWARE_WEB', 'DATA_SCIENCE_ML', 'IOT_HARDWARE', 'PROCESS_AUTOMATION'] as const
type FilterOption = typeof FILTER_OPTIONS[number]

export default function ContributorDashboard() {
  const { projects } = useProjects()
  const [filter, setFilter] = useState<FilterOption>('ALL')
  const [offeredIds, setOfferedIds] = useState<Set<string>>(new Set())

  const filtered = filter === 'ALL'
    ? projects
    : projects.filter(p => p.techCategory === filter)

  const openCount = projects.filter(p => p.status === 'OPEN').length

  const handleOffer = (id: string) => {
    setOfferedIds(prev => new Set(prev).add(id))
  }

  return (
    <div className="bg-indigo-50 min-h-full">
      <div className="max-w-4xl mx-auto px-4 py-10 space-y-8">

        {/* Page header */}
        <div className="flex items-end justify-between gap-4 flex-wrap">
          <div>
            <h1 className="text-3xl font-bold text-gray-900">Project Feed</h1>
            <p className="text-gray-500 text-sm mt-1">
              Browse NGO challenges and offer your skills where they matter most.
            </p>
          </div>
          <span className="text-sm font-semibold text-indigo-600 bg-indigo-100 px-3 py-1.5 rounded-full">
            {openCount} open
          </span>
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

        {/* Cards */}
        <div className="space-y-4">
          {filtered.length === 0 && (
            <div className="text-center py-20 text-gray-400">
              <p className="text-4xl mb-3">🔍</p>
              <p className="text-sm">No projects in this category yet.</p>
            </div>
          )}

          {filtered.map((project, i) => {
            const style = CATEGORY_STYLES[project.techCategory]
            const isSolved = project.status === 'SOLVED'
            const hasOffered = offeredIds.has(project.id)

            return (
              <motion.div
                key={project.id}
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
                    <span className="font-semibold text-gray-800 text-sm">{project.ngoName}</span>
                    <span className="text-xs text-gray-400">{project.state}</span>
                  </div>
                  <span className={`text-xs font-semibold px-2.5 py-1 rounded-full flex-shrink-0 ${
                    isSolved ? 'bg-gray-100 text-gray-400' : style.badge
                  }`}>
                    {project.techCategory.replace(/_/g, ' ')}
                  </span>
                </div>

                {/* Description */}
                <p className={`text-sm leading-relaxed mb-4 ${isSolved ? 'text-gray-400' : 'text-gray-700'}`}>
                  {project.structuredProblem}
                </p>

                {/* Footer */}
                <div className="flex items-center justify-between gap-3">
                  {/* Status badge */}
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

                  {/* Action button */}
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
                      onClick={() => handleOffer(project.id)}
                      className={`text-xs font-semibold px-4 py-1.5 rounded-full bg-indigo-500 text-white transition-colors ${style.activeBg}`}
                    >
                      Offer Help
                    </button>
                  )}
                </div>
              </motion.div>
            )
          })}
        </div>

      </div>
    </div>
  )
}
