import { useState } from 'react'
import { motion } from 'framer-motion'

interface NgoFormState {
  ngoName: string
  missionStatement: string
  operatingRegion: string
}

const INITIAL: NgoFormState = {
  ngoName: '',
  missionStatement: '',
  operatingRegion: '',
}

export default function NgoRegister() {
  const [form, setForm] = useState<NgoFormState>(INITIAL)
  const [submitted, setSubmitted] = useState(false)

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    setForm(prev => ({ ...prev, [e.target.name]: e.target.value }))
  }

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    console.log('[NgoRegister] payload:', form)
    setSubmitted(true)
  }

  return (
    <div className="min-h-[calc(100vh-3.5rem)] flex items-center justify-center px-4 py-16 bg-amber-50">
      <motion.div
        initial={{ opacity: 0, y: 24 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.45, ease: 'easeOut' }}
        className="w-full max-w-lg"
      >
        {/* Card */}
        <div className="bg-white rounded-3xl shadow-xl border border-amber-100 overflow-hidden">

          {/* Card header stripe */}
          <div className="h-2 bg-gradient-to-r from-amber-400 to-orange-400" />

          <div className="px-8 py-10">
            {/* Heading */}
            <div className="mb-8">
              <span className="inline-flex items-center gap-1.5 text-xs font-semibold tracking-widest uppercase text-amber-600 bg-amber-50 px-3 py-1 rounded-full mb-4">
                <span className="w-1.5 h-1.5 rounded-full bg-amber-400" />
                NGO Registration
              </span>
              <h1 className="text-2xl font-bold text-gray-900 leading-tight">
                Register your Organization
              </h1>
              <p className="text-gray-500 text-sm mt-2">
                Tell us about your NGO and the challenges you're facing. We'll connect you with the right tech talent.
              </p>
            </div>

            {submitted ? (
              <motion.div
                initial={{ opacity: 0, scale: 0.95 }}
                animate={{ opacity: 1, scale: 1 }}
                className="text-center py-8 space-y-3"
              >
                <div className="w-14 h-14 rounded-full bg-amber-100 flex items-center justify-center mx-auto text-2xl">
                  🎉
                </div>
                <h2 className="font-semibold text-gray-800 text-lg">Registration Submitted!</h2>
                <p className="text-gray-500 text-sm">
                  Check the console for the payload. Backend integration coming soon.
                </p>
                <button
                  onClick={() => { setForm(INITIAL); setSubmitted(false) }}
                  className="mt-4 text-sm text-amber-600 hover:text-amber-700 font-medium underline underline-offset-2"
                >
                  Register another
                </button>
              </motion.div>
            ) : (
              <form onSubmit={handleSubmit} className="space-y-5">

                {/* NGO Name */}
                <div>
                  <label htmlFor="ngoName" className="block text-sm font-medium text-gray-700 mb-1.5">
                    NGO Name <span className="text-red-400">*</span>
                  </label>
                  <input
                    id="ngoName"
                    name="ngoName"
                    type="text"
                    required
                    value={form.ngoName}
                    onChange={handleChange}
                    placeholder="e.g. Green Earth Foundation"
                    className="w-full border border-gray-200 rounded-xl px-4 py-2.5 text-sm text-gray-800 placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-amber-400 focus:border-transparent transition"
                  />
                </div>

                {/* Mission Statement */}
                <div>
                  <label htmlFor="missionStatement" className="block text-sm font-medium text-gray-700 mb-1.5">
                    Mission Statement <span className="text-red-400">*</span>
                  </label>
                  <textarea
                    id="missionStatement"
                    name="missionStatement"
                    required
                    rows={4}
                    value={form.missionStatement}
                    onChange={handleChange}
                    placeholder="Describe your organization's mission and the impact you aim to create…"
                    className="w-full border border-gray-200 rounded-xl px-4 py-2.5 text-sm text-gray-800 placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-amber-400 focus:border-transparent transition resize-none"
                  />
                </div>

                {/* Operating Region */}
                <div>
                  <label htmlFor="operatingRegion" className="block text-sm font-medium text-gray-700 mb-1.5">
                    Operating Region <span className="text-red-400">*</span>
                  </label>
                  <input
                    id="operatingRegion"
                    name="operatingRegion"
                    type="text"
                    required
                    value={form.operatingRegion}
                    onChange={handleChange}
                    placeholder="e.g. South Asia, Sub-Saharan Africa, Global"
                    className="w-full border border-gray-200 rounded-xl px-4 py-2.5 text-sm text-gray-800 placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-amber-400 focus:border-transparent transition"
                  />
                </div>

                <button
                  type="submit"
                  className="w-full mt-2 bg-gradient-to-r from-amber-400 to-orange-400 hover:from-amber-500 hover:to-orange-500 text-white font-semibold py-3 rounded-xl transition-all shadow-md hover:shadow-lg text-sm tracking-wide"
                >
                  Register Organization
                </button>

              </form>
            )}
          </div>
        </div>

        <p className="text-center text-xs text-gray-400 mt-5">
          Already registered?{' '}
          <a href="/ngo" className="text-amber-600 hover:underline font-medium">
            Go to NGO Dashboard
          </a>
        </p>
      </motion.div>
    </div>
  )
}
