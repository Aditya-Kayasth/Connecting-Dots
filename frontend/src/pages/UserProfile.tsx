import { useEffect, useState } from 'react'
import { motion } from 'framer-motion'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import apiClient from '../api/client'

interface Problem {
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

const STATUS_COLORS: Record<string, string> = {
  OPEN: 'bg-amber-100 text-amber-700',
  TAKEN: 'bg-indigo-100 text-indigo-700',
  SOLVED: 'bg-emerald-100 text-emerald-700',
}

export default function UserProfile() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const [problems, setProblems] = useState<Problem[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (!user) { navigate('/login'); return }
    const endpoint = user.role === 'NGO' ? '/problems/my' : '/problems/claimed'
    apiClient.get<Problem[]>(endpoint)
      .then(r => setProblems(r.data))
      .catch(() => setProblems([]))
      .finally(() => setLoading(false))
  }, [user, navigate])

  const handleLogout = () => {
    logout()
    navigate('/')
  }

  if (!user) return null

  const roleColor = user.role === 'NGO' ? 'bg-amber-100 text-amber-700' : 'bg-indigo-100 text-indigo-700'

  return (
    <div className="bg-gray-50 min-h-full">
      <div className="max-w-3xl mx-auto px-4 py-10 space-y-8">

        {/* Profile card */}
        <motion.div
          initial={{ opacity: 0, y: 16 }}
          animate={{ opacity: 1, y: 0 }}
          className="bg-white rounded-3xl border border-gray-100 shadow-sm p-6 flex items-center justify-between gap-4 flex-wrap"
        >
          <div className="flex items-center gap-4">
            <div className="w-14 h-14 rounded-2xl bg-amber-100 flex items-center justify-center text-2xl font-bold text-amber-600">
              {user.name.charAt(0).toUpperCase()}
            </div>
            <div>
              <h1 className="text-xl font-bold text-gray-900">{user.name}</h1>
              <p className="text-sm text-gray-500">{user.email}</p>
              <span className={`inline-block mt-1 text-xs font-semibold px-2.5 py-0.5 rounded-full ${roleColor}`}>
                {user.role}
              </span>
            </div>
          </div>
          <button
            onClick={handleLogout}
            className="text-sm font-semibold text-red-500 hover:text-red-700 border border-red-200 hover:border-red-400 px-4 py-2 rounded-xl transition-colors"
          >
            Sign Out
          </button>
        </motion.div>

        {/* Problems list */}
        <section>
          <h2 className="text-lg font-semibold text-gray-800 mb-4">
            {user.role === 'NGO' ? 'Your Submitted Problems' : 'Problems You Claimed'}
          </h2>

          {loading ? (
            <div className="flex justify-center py-16">
              <div className="w-8 h-8 border-4 border-amber-200 border-t-amber-500 rounded-full animate-spin" />
            </div>
          ) : problems.length === 0 ? (
            <p className="text-sm text-gray-400 text-center py-12">
              {user.role === 'NGO'
                ? 'No problems submitted yet. Head to the dashboard to submit one.'
                : 'No problems claimed yet. Browse the project feed to offer help.'}
            </p>
          ) : (
            <div className="space-y-3">
              {problems.map((p, i) => (
                <motion.div
                  key={p.id}
                  initial={{ opacity: 0, y: 10 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ delay: i * 0.05 }}
                  className="bg-white rounded-2xl border border-gray-100 shadow-sm p-5 space-y-2"
                >
                  <div className="flex items-center gap-2 flex-wrap">
                    <span className="font-semibold text-gray-800 text-sm">{p.ngoName}</span>
                    <span className={`text-xs font-semibold px-2 py-0.5 rounded-full ${CATEGORY_COLORS[p.techCategory] ?? 'bg-gray-100 text-gray-600'}`}>
                      {p.techCategory.replace(/_/g, ' ')}
                    </span>
                    <span className={`text-xs font-semibold px-2 py-0.5 rounded-full ${STATUS_COLORS[p.status] ?? 'bg-gray-100 text-gray-600'}`}>
                      {p.status}
                    </span>
                  </div>
                  <p className="text-sm text-gray-600 leading-relaxed line-clamp-2">{p.structuredProblem}</p>
                </motion.div>
              ))}
            </div>
          )}
        </section>

      </div>
    </div>
  )
}
