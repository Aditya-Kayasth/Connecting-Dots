'use client'

import { useEffect, useState } from 'react'

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8080'

export default function BackendStatusBanner() {
  const [coreOnline, setCoreOnline] = useState<boolean | null>(null)
  const [aiOnline, setAiOnline] = useState<boolean | null>(null)
  const [loading, setLoading] = useState(true)
  const [mounted, setMounted] = useState(false)
  const [dismissed, setDismissed] = useState(false)
  const [showSuccess, setShowSuccess] = useState(false)

  // Avoid hydration mismatch by initializing variables on client mount
  useEffect(() => {
    setMounted(true)
    if (typeof window !== 'undefined') {
      setDismissed(localStorage.getItem('services_warmed_up') === 'true')
    }
  }, [])

  useEffect(() => {
    if (!mounted) return

    let active = true
    let consecutiveSuccessCount = 0

    // Fetch with AbortController to prevent indefinite TCP connection hangs
    const checkPing = async (url: string) => {
      const controller = new AbortController()
      const timeoutId = setTimeout(() => controller.abort(), 1500)
      
      try {
        const res = await fetch(url, { signal: controller.signal, cache: 'no-store' })
        clearTimeout(timeoutId)
        return res.ok
      } catch (err) {
        clearTimeout(timeoutId)
        
        // Loopback fallback for Windows dev environment localhost mapping blocks
        if (url.includes('localhost')) {
          const fallbackUrl = url.replace('localhost', '127.0.0.1')
          const fallbackController = new AbortController()
          const fallbackTimeoutId = setTimeout(() => fallbackController.abort(), 1500)
          try {
            const res = await fetch(fallbackUrl, { signal: fallbackController.signal, cache: 'no-store' })
            clearTimeout(fallbackTimeoutId)
            return res.ok
          } catch (fallbackErr) {
            clearTimeout(fallbackTimeoutId)
            return false
          }
        }
        return false
      }
    }

    const checkStatus = async () => {
      try {
        const [coreRes, aiRes] = await Promise.all([
          checkPing(`${API_BASE_URL}/api/v1/core/ping`),
          checkPing(`${API_BASE_URL}/api/v1/ai/ping`)
        ])

        if (!active) return

        setCoreOnline(coreRes)
        setAiOnline(aiRes)
        setLoading(false)

        if (coreRes && aiRes) {
          consecutiveSuccessCount++
          if (consecutiveSuccessCount >= 1) {
            setShowSuccess(true)
            if (typeof window !== 'undefined') {
              localStorage.setItem('services_warmed_up', 'true')
            }
            // Auto-hide the banner after 4 seconds of success
            const timer = setTimeout(() => {
              setDismissed(true)
            }, 4000)
            return () => clearTimeout(timer)
          }
        } else {
          consecutiveSuccessCount = 0
          setShowSuccess(false)
        }
      } catch (err) {
        if (active) {
          setCoreOnline(false)
          setAiOnline(false)
          setLoading(false)
          setShowSuccess(false)
          consecutiveSuccessCount = 0
        }
      }
    }

    checkStatus()
    const interval = setInterval(checkStatus, 5000)

    return () => {
      active = false
      clearInterval(interval)
    }
  }, [mounted])

  // Don't render anything during server-side pre-render
  if (!mounted) return null

  if (dismissed) {
    // If dismissed but a service goes offline, show a non-intrusive alert toast in the corner
    if (coreOnline === false || aiOnline === false) {
      return (
        <div className="fixed bottom-4 right-4 z-50 flex flex-col gap-2 rounded-xl border border-amber-500/30 bg-amber-500/10 p-4 text-white shadow-2xl backdrop-blur-md animate-in fade-in slide-in-from-bottom-5 duration-300 w-80">
          <div className="flex items-center gap-2">
            <span className="relative flex h-3 w-3">
              <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-amber-400 opacity-75"></span>
              <span className="relative inline-flex rounded-full h-3 w-3 bg-amber-500"></span>
            </span>
            <strong className="text-sm font-bold text-amber-200">Sleeping Service Alert</strong>
          </div>
          <p className="text-xs text-white/80 leading-normal mt-1">
            {coreOnline === false && aiOnline === false
              ? "Core Database & AI Worker have gone offline."
              : coreOnline === false
                ? "Core Database Service is currently sleeping..."
                : "Gemini AI Service Worker is currently sleeping..."}
          </p>
          <small className="text-[10px] text-white/50">Waking up in the background...</small>
        </div>
      )
    }
    return null
  }

  const isSystemOnline = coreOnline && aiOnline

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/65 backdrop-blur-md transition-all duration-500">
      <div className="w-[450px] max-w-[90vw] overflow-hidden rounded-xl border border-white/20 bg-white/10 p-6 text-center text-white shadow-2xl backdrop-blur-xl animate-in fade-in zoom-in-95 duration-300">
        {/* Pulsing loading state / Success check icon */}
        <div className="mx-auto mb-6 flex h-16 w-16 items-center justify-center rounded-full bg-white/5 shadow-inner">
          {isSystemOnline ? (
            <span className="text-3xl text-emerald-400 animate-bounce">✓</span>
          ) : (
            <div className="relative flex h-6 w-6">
              <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-amber-400 opacity-75"></span>
              <span className="relative inline-flex rounded-full h-6 w-6 bg-amber-500"></span>
            </div>
          )}
        </div>

        {/* Title */}
        <h2 className="text-2xl font-bold tracking-tight text-white mb-2">
          {isSystemOnline ? "All Systems Connected!" : "Warming Up Service Registry"}
        </h2>

        {/* Subtitle */}
        <p className="text-sm text-white/70 px-4 mb-6 leading-relaxed">
          {isSystemOnline
            ? "Connecting Dots is fully online and active. You can now browse NGOs, submit projects, and collaborate freely."
            : "We deploy on a serverless free tier which takes around 30-50 seconds to spin up on cold start. Thank you for your patience!"}
        </p>

        {/* Checklist */}
        <div className="space-y-3 rounded-lg bg-black/20 p-4 text-left border border-white/5 mb-6">
          <div className="flex items-center justify-between text-sm">
            <span className="text-white/80 font-medium">Gateway Proxy Node</span>
            <span className="text-emerald-400 font-semibold text-xs flex items-center gap-1.5">
              <span className="h-2 w-2 rounded-full bg-emerald-400" /> Active
            </span>
          </div>

          <div className="flex items-center justify-between text-sm">
            <span className="text-white/80 font-medium">Core Database Service</span>
            {coreOnline === null || loading ? (
              <span className="text-white/40 text-xs animate-pulse">Checking...</span>
            ) : coreOnline ? (
              <span className="text-emerald-400 font-semibold text-xs flex items-center gap-1.5">
                <span className="h-2 w-2 rounded-full bg-emerald-400" /> Connected
              </span>
            ) : (
              <span className="text-amber-400 font-semibold text-xs flex items-center gap-1.5 animate-pulse">
                <span className="h-2 w-2 rounded-full bg-amber-400" /> Waking Up...
              </span>
            )}
          </div>

          <div className="flex items-center justify-between text-sm">
            <span className="text-white/80 font-medium">Gemini AI Service Worker</span>
            {aiOnline === null || loading ? (
              <span className="text-white/40 text-xs animate-pulse">Checking...</span>
            ) : aiOnline ? (
              <span className="text-emerald-400 font-semibold text-xs flex items-center gap-1.5">
                <span className="h-2 w-2 rounded-full bg-emerald-400" /> Connected
              </span>
            ) : (
              <span className="text-amber-400 font-semibold text-xs flex items-center gap-1.5 animate-pulse">
                <span className="h-2 w-2 rounded-full bg-amber-400" /> Waking Up...
              </span>
            )}
          </div>
        </div>

        {/* Interactive Button */}
        {isSystemOnline ? (
          <button
            onClick={() => {
              if (typeof window !== 'undefined') {
                localStorage.setItem('services_warmed_up', 'true')
              }
              setDismissed(true)
            }}
            className="w-full rounded-lg bg-emerald-500 py-3 text-sm font-bold text-white shadow-lg transition-all duration-300 hover:bg-emerald-600 hover:scale-[1.02] active:scale-[0.98]"
          >
            Go Ahead & Interact
          </button>
        ) : (
          <div className="flex gap-3">
            <button
              disabled
              className="flex-1 rounded-lg bg-white/5 py-3 text-sm font-bold text-white/50 border border-white/10"
            >
              Waiting for wake-up...
            </button>
            <button
              onClick={() => {
                if (typeof window !== 'undefined') {
                  localStorage.setItem('services_warmed_up', 'true')
                }
                setDismissed(true)
              }}
              className="rounded-lg border border-white/20 px-4 text-xs text-white/60 hover:text-white hover:bg-white/5"
            >
              Dismiss
            </button>
          </div>
        )}
      </div>
    </div>
  )
}
