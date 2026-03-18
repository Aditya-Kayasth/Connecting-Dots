import { useState } from 'react'
import { motion } from 'framer-motion'

type TechSkill = 'Frontend' | 'Backend' | 'Data Science' | 'AI/ML' | ''

interface ContributorFormState {
  fullName: string
  primaryTechSkill: TechSkill
  githubUrl: string
}

const INITIAL: ContributorFormState = {
  fullName: '',
  primaryTechSkill: '',
  githubUrl: '',
}

const SKILL_OPTIONS: Exclude<TechSkill, ''>[] = ['Frontend', 'Backend', 'Data Science', 'AI/ML']

const SKILL_COLORS: Record<string, string> = {
  Frontend: 'text-sky-600',
  Backend: 'text-emerald-600',
  'Data Science': 'text-purple-600',
  'AI/ML': 'text-indigo-600',
}

export default function ContributorRegister() {
  const [form, setForm] = useState<ContributorFormState>(INITIAL)
  const [submitted, setSubmitted] = useState(false)

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    setForm(prev => ({ ...prev, [e.target.name]: e.target.value }))
  }

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    console.log('[ContributorRegister] payload:', form)
    setSubmitted(true)
  }

  return (
    <div className="min-h-[calc(100vh-3.5rem)] flex items-center justify-center px-4 py-16 bg-indigo-50">
      <motion.div
        initial={{ opacity: 0, y: 24 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.45, ease: 'easeOut' }}
        className="w-full max-w-lg"
      >
        {/* Card */}
        <div className="bg-white rounded-3xl shadow-xl border border-indigo-100 overflow-hidden">

          {/* Card header stripe */}
          <div className="h-2 bg-gradient-to-r from-indigo-500 to-violet-500" />

          <div className="px-8 py-10">
            {/* Heading */}
            <div className="mb-8">
              <span className="inline-flex items-center gap-1.5 text-xs font-semibold tracking-widest uppercase text-indigo-600 bg-indigo-50 px-3 py-1 rounded-full mb-4">
                <span className="w-1.5 h-1.5 rounded-full bg-indigo-500" />
                Contributor Registration
              </span>
              <h1 className="text-2xl font-bold text-gray-900 leading-tight">
                Join the Cause
              </h1>
              <p className="text-gray-500 text-sm mt-2">
                Lend your skills to NGOs that need them most. Browse open problems and make a real-world impact.
              </p>
            </div>

            {submitted ? (
              <motion.div
                initial={{ opacity: 0, scale: 0.95 }}
                animate={{ opacity: 1, scale: 1 }}
                className="text-center py-8 space-y-3"
              >
                <div className="w-14 h-14 rounded-full bg-indigo-100 flex items-center justify-center mx-auto text-2xl">
                  🚀
                </div>
                <h2 className="font-semibold text-gray-800 text-lg">You're In!</h2>
                <p className="text-gray-500 text-sm">
                  Check the console for the payload. Backend integration coming soon.
                </p>
                <button
                  onClick={() => { setForm(INITIAL); setSubmitted(false) }}
                  className="mt-4 text-sm text-indigo-600 hover:text-indigo-700 font-medium underline underline-offset-2"
                >
                  Register another
                </button>
              </motion.div>
            ) : (
              <form onSubmit={handleSubmit} className="space-y-5">

                {/* Full Name */}
                <div>
                  <label htmlFor="fullName" className="block text-sm font-medium text-gray-700 mb-1.5">
                    Full Name <span className="text-red-400">*</span>
                  </label>
                  <input
                    id="fullName"
                    name="fullName"
                    type="text"
                    required
                    value={form.fullName}
                    onChange={handleChange}
                    placeholder="e.g. Arjun Sharma"
                    className="w-full border border-gray-200 rounded-xl px-4 py-2.5 text-sm text-gray-800 placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-indigo-400 focus:border-transparent transition"
                  />
                </div>

                {/* Primary Tech Skill */}
                <div>
                  <label htmlFor="primaryTechSkill" className="block text-sm font-medium text-gray-700 mb-1.5">
                    Primary Tech Skill <span className="text-red-400">*</span>
                  </label>
                  <div className="relative">
                    <select
                      id="primaryTechSkill"
                      name="primaryTechSkill"
                      required
                      value={form.primaryTechSkill}
                      onChange={handleChange}
                      className={`w-full border border-gray-200 rounded-xl px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-400 focus:border-transparent transition appearance-none bg-white ${
                        form.primaryTechSkill
                          ? SKILL_COLORS[form.primaryTechSkill] + ' font-medium'
                          : 'text-gray-400'
                      }`}
                    >
                      <option value="" disabled>Select your primary skill…</option>
                      {SKILL_OPTIONS.map(skill => (
                        <option key={skill} value={skill} className="text-gray-800 font-normal">
                          {skill}
                        </option>
                      ))}
                    </select>
                    {/* Custom chevron */}
                    <span className="pointer-events-none absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 text-xs">
                      ▾
                    </span>
                  </div>
                </div>

                {/* GitHub URL */}
                <div>
                  <label htmlFor="githubUrl" className="block text-sm font-medium text-gray-700 mb-1.5">
                    GitHub Profile URL <span className="text-red-400">*</span>
                  </label>
                  <div className="relative">
                    <span className="absolute left-3.5 top-1/2 -translate-y-1/2 text-gray-400 text-sm select-none">
                      github.com/
                    </span>
                    <input
                      id="githubUrl"
                      name="githubUrl"
                      type="text"
                      required
                      value={form.githubUrl}
                      onChange={handleChange}
                      placeholder="your-username"
                      className="w-full border border-gray-200 rounded-xl pl-[6.5rem] pr-4 py-2.5 text-sm text-gray-800 placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-indigo-400 focus:border-transparent transition"
                    />
                  </div>
                </div>

                <button
                  type="submit"
                  className="w-full mt-2 bg-gradient-to-r from-indigo-500 to-violet-500 hover:from-indigo-600 hover:to-violet-600 text-white font-semibold py-3 rounded-xl transition-all shadow-md hover:shadow-lg text-sm tracking-wide"
                >
                  Join the Cause
                </button>

              </form>
            )}
          </div>
        </div>

        <p className="text-center text-xs text-gray-400 mt-5">
          Already registered?{' '}
          <a href="/contributor" className="text-indigo-600 hover:underline font-medium">
            Go to Contributor Dashboard
          </a>
        </p>
      </motion.div>
    </div>
  )
}
