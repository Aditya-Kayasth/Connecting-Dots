import { useState } from 'react'
import { motion } from 'framer-motion'
import { Link, useNavigate } from 'react-router-dom'
import apiClient from '../api/client'
import { useAuth } from '../context/AuthContext'

export default function Register() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [role, setRole] = useState<'NGO' | 'CONTRIBUTOR'>('NGO')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setLoading(true)
    setError(null)
    try {
      const { data } = await apiClient.post('/auth/register', { name, email, password, role })
      login({ id: data.id, email: data.email, name: data.name, role: data.role, token: data.token })
      navigate(data.role === 'NGO' ? '/ngo' : '/contributor')
    } catch (err: any) {
      setError(err?.response?.data?.message ?? 'Registration failed. Please try again.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-amber-50 to-orange-100 flex items-center justify-center px-4">
      <motion.div
        initial={{ opacity: 0, y: 24 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.4 }}
        className="w-full max-w-md bg-white rounded-3xl shadow-xl p-8 space-y-6"
      >
        <div className="text-center space-y-1">
          <div className="flex justify-center gap-1.5 mb-3">
            <span className="w-3 h-3 rounded-full bg-amber-500" />
            <span className="w-3 h-3 rounded-full bg-orange-400" />
          </div>
          <h1 className="text-2xl font-bold text-gray-900">Create an account</h1>
          <p className="text-sm text-gray-500">Join Connecting-Dots as an NGO or Contributor</p>
        </div>

        {/* Role toggle */}
        <div className="flex rounded-xl border border-gray-200 overflow-hidden">
          {(['NGO', 'CONTRIBUTOR'] as const).map(r => (
            <button
              key={r}
              type="button"
              onClick={() => setRole(r)}
              className={`flex-1 py-2 text-sm font-semibold transition-colors ${
                role === r
                  ? 'bg-amber-500 text-white'
                  : 'bg-white text-gray-500 hover:bg-amber-50'
              }`}
            >
              {r === 'NGO' ? 'NGO' : 'Contributor'}
            </button>
          ))}
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1.5">Full Name</label>
            <input
              type="text" value={name} onChange={e => setName(e.target.value)} required
              placeholder="Your name or organisation"
              className="w-full border border-gray-200 rounded-xl px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-amber-400 transition"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1.5">Email</label>
            <input
              type="email" value={email} onChange={e => setEmail(e.target.value)} required
              placeholder="you@example.com"
              className="w-full border border-gray-200 rounded-xl px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-amber-400 transition"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1.5">Password</label>
            <input
              type="password" value={password} onChange={e => setPassword(e.target.value)} required
              minLength={8}
              placeholder="Min. 8 characters"
              className="w-full border border-gray-200 rounded-xl px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-amber-400 transition"
            />
          </div>

          {error && (
            <p className="text-sm text-red-600 bg-red-50 border border-red-200 rounded-xl px-4 py-2.5">{error}</p>
          )}

          <button
            type="submit" disabled={loading}
            className="w-full bg-amber-500 hover:bg-amber-600 disabled:bg-amber-300 text-white font-semibold py-2.5 rounded-xl transition-colors text-sm"
          >
            {loading ? (
              <span className="flex items-center justify-center gap-2">
                <span className="w-4 h-4 border-2 border-white/40 border-t-white rounded-full animate-spin" />
                Creating account…
              </span>
            ) : 'Create Account'}
          </button>
        </form>

        <p className="text-center text-sm text-gray-500">
          Already have an account?{' '}
          <Link to="/login" className="text-amber-600 font-semibold hover:underline">Sign in</Link>
        </p>
      </motion.div>
    </div>
  )
}
