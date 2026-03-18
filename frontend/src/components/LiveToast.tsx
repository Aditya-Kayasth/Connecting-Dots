import { useEffect, useState } from 'react'
import { AnimatePresence, motion } from 'framer-motion'

interface ToastMessage {
  id: number
  text: string
  icon: string
}

const FACILITATION_MESSAGES: { text: string; icon: string }[] = [
  { text: 'New contributor joined from Pune!', icon: '👋' },
  { text: 'Vidarbha Water Initiative matched with a contributor!', icon: '🤝' },
  { text: 'Delhi Air Watch project received 3 offers!', icon: '📬' },
  { text: 'Green Earth marked a project as Solved!', icon: '✅' },
  { text: 'New NGO registered from Kerala!', icon: '🌱' },
  { text: 'Bangalore Tech Literacy got a new volunteer!', icon: '💻' },
  { text: 'Surat Textile Workers project is trending!', icon: '🔥' },
  { text: 'Contributor from Hyderabad offered help on IoT project!', icon: '⚡' },
]

let toastCounter = 0

export default function LiveToast() {
  const [toasts, setToasts] = useState<ToastMessage[]>([])

  useEffect(() => {
    // Fire first toast after 4s, then every 7–12s randomly
    const schedule = () => {
      const delay = 7000 + Math.random() * 5000
      return setTimeout(() => {
        const msg = FACILITATION_MESSAGES[Math.floor(Math.random() * FACILITATION_MESSAGES.length)]
        const newToast: ToastMessage = { id: ++toastCounter, ...msg }

        setToasts(prev => [...prev, newToast])

        // Auto-dismiss after 4s
        setTimeout(() => {
          setToasts(prev => prev.filter(t => t.id !== newToast.id))
        }, 4000)

        timerId = schedule()
      }, delay)
    }

    // First one fires sooner
    let timerId = setTimeout(() => {
      const msg = FACILITATION_MESSAGES[0]
      const newToast: ToastMessage = { id: ++toastCounter, ...msg }
      setToasts(prev => [...prev, newToast])
      setTimeout(() => setToasts(prev => prev.filter(t => t.id !== newToast.id)), 4000)
      timerId = schedule()
    }, 4000)

    return () => clearTimeout(timerId)
  }, [])

  const dismiss = (id: number) => setToasts(prev => prev.filter(t => t.id !== id))

  return (
    <div className="fixed bottom-5 right-5 z-50 flex flex-col gap-2 items-end pointer-events-none">
      <AnimatePresence>
        {toasts.map(toast => (
          <motion.div
            key={toast.id}
            initial={{ opacity: 0, x: 60, scale: 0.95 }}
            animate={{ opacity: 1, x: 0, scale: 1 }}
            exit={{ opacity: 0, x: 60, scale: 0.95 }}
            transition={{ type: 'spring', stiffness: 300, damping: 28 }}
            className="pointer-events-auto flex items-start gap-3 bg-white border border-gray-100 shadow-xl rounded-2xl px-4 py-3 max-w-xs w-full"
          >
            {/* Icon */}
            <span className="text-xl flex-shrink-0 mt-0.5">{toast.icon}</span>

            {/* Body */}
            <div className="flex-1 min-w-0">
              <p className="text-xs font-semibold text-indigo-600 uppercase tracking-widest mb-0.5">
                Live Update
              </p>
              <p className="text-sm text-gray-700 leading-snug">{toast.text}</p>
            </div>

            {/* Dismiss */}
            <button
              onClick={() => dismiss(toast.id)}
              className="text-gray-300 hover:text-gray-500 text-lg leading-none flex-shrink-0 mt-0.5 transition-colors"
              aria-label="Dismiss"
            >
              ×
            </button>
          </motion.div>
        ))}
      </AnimatePresence>
    </div>
  )
}
