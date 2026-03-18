import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { motion, AnimatePresence } from 'framer-motion'

type Side = 'ngo' | 'contributor' | null

export default function Landing() {
  const [selected, setSelected] = useState<Side>(null)
  const navigate = useNavigate()

  const handleSelect = (side: Side) => {
    setSelected(side)
    setTimeout(() => {
      navigate(side === 'ngo' ? '/ngo' : '/contributor')
    }, 700)
  }

  return (
    <div className="relative w-full h-full flex overflow-hidden select-none">

      {/* ── NGO side (warm amber) ── */}
      <motion.div
        className="relative flex-1 flex items-center justify-center cursor-pointer"
        style={{ backgroundColor: '#F59E0B' }}
        animate={selected === 'ngo' ? { flex: 2 } : selected === 'contributor' ? { flex: 0 } : { flex: 1 }}
        transition={{ duration: 0.6, ease: 'easeInOut' }}
        onClick={() => !selected && handleSelect('ngo')}
      >
        <AnimatePresence>
          {selected !== 'contributor' && (
            <motion.div
              className="text-center px-8"
              initial={{ opacity: 1 }}
              exit={{ opacity: 0, x: -40 }}
              transition={{ duration: 0.3 }}
            >
              <p className="text-white/60 text-sm font-medium tracking-widest uppercase mb-3">
                I represent
              </p>
              <h2 className="text-white text-4xl font-bold leading-tight">
                an NGO
              </h2>
              <p className="text-white/70 text-sm mt-4 max-w-xs">
                Submit your problem and let AI structure it for the right tech contributors.
              </p>
            </motion.div>
          )}
        </AnimatePresence>
      </motion.div>

      {/* ── Center smiley overlay ── */}
      <div className="absolute inset-0 flex items-center justify-center pointer-events-none z-10">
        <motion.div
          animate={selected ? { scale: 1.15, opacity: 0 } : { scale: 1, opacity: 1 }}
          transition={{ duration: 0.4 }}
        >
          <SmileyFace />
        </motion.div>
      </div>

      {/* ── Contributor side (cool indigo) ── */}
      <motion.div
        className="relative flex-1 flex items-center justify-center cursor-pointer"
        style={{ backgroundColor: '#4F46E5' }}
        animate={selected === 'contributor' ? { flex: 2 } : selected === 'ngo' ? { flex: 0 } : { flex: 1 }}
        transition={{ duration: 0.6, ease: 'easeInOut' }}
        onClick={() => !selected && handleSelect('contributor')}
      >
        <AnimatePresence>
          {selected !== 'ngo' && (
            <motion.div
              className="text-center px-8"
              initial={{ opacity: 1 }}
              exit={{ opacity: 0, x: 40 }}
              transition={{ duration: 0.3 }}
            >
              <p className="text-white/60 text-sm font-medium tracking-widest uppercase mb-3">
                I am a
              </p>
              <h2 className="text-white text-4xl font-bold leading-tight">
                Tech Contributor
              </h2>
              <p className="text-white/70 text-sm mt-4 max-w-xs">
                Browse open NGO problems and find where your skills can make an impact.
              </p>
            </motion.div>
          )}
        </AnimatePresence>
      </motion.div>

    </div>
  )
}

function SmileyFace() {
  return (
    <svg
      width="120"
      height="120"
      viewBox="0 0 120 120"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
    >
      {/* Face circle */}
      <circle cx="60" cy="60" r="56" fill="white" fillOpacity="0.15" stroke="white" strokeWidth="3" />

      {/* Left eye — NGO dot (amber) */}
      <circle cx="40" cy="48" r="8" fill="#F59E0B" />

      {/* Right eye — Contributor dot (indigo) */}
      <circle cx="80" cy="48" r="8" fill="#4F46E5" />

      {/* Smile arc */}
      <path
        d="M 36 72 Q 60 96 84 72"
        stroke="white"
        strokeWidth="4"
        strokeLinecap="round"
        fill="none"
      />
    </svg>
  )
}
