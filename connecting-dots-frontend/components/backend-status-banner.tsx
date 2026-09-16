'use client'

import { useEffect, useState, useRef } from 'react'

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8080'

export default function BackendStatusBanner() {
  const [gatewayOnline, setGatewayOnline] = useState<boolean | null>(null)
  const [coreOnline, setCoreOnline] = useState<boolean | null>(null)
  const [aiOnline, setAiOnline] = useState<boolean | null>(null)
  const [loading, setLoading] = useState(true)
  const [mounted, setMounted] = useState(false)
  const [hasInitializedSession, setHasInitializedSession] = useState(false)
  const [showSuccessToast, setShowSuccessToast] = useState(false)

  // Track previous connection state to detect mid-session recovery
  const wasOfflineRef = useRef(false)

  // Avoid hydration mismatch by initializing variables on client mount
  useEffect(() => {
    setMounted(true)
  }, [])

  useEffect(() => {
    if (!mounted) return

    let active = true

    // Fetch with AbortController to prevent TCP connection hangs
    const checkPing = async (url: string) => {
      const controller = new AbortController()
      const timeoutId = setTimeout(() => controller.abort(), 1500)

      try {
        const res = await fetch(url, { signal: controller.signal, cache: 'no-store' })
        clearTimeout(timeoutId)
        return res.ok
      } catch (err) {
        clearTimeout(timeoutId)

        // Loopback fallback for Windows dev environment localhost mapping
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
        const [gatewayRes, coreRes, aiRes] = await Promise.all([
          checkPing(`${API_BASE_URL}/actuator/health/liveness`),
          checkPing(`${API_BASE_URL}/api/v1/core/ping`),
          checkPing(`${API_BASE_URL}/api/v1/ai/ping`)
        ])

        if (!active) return

        // If core or ai ping succeeded through port 8080 gateway, gateway is definitely alive
        const isGatewayAlive = gatewayRes || coreRes || aiRes

        setGatewayOnline(isGatewayAlive)
        setCoreOnline(coreRes)
        setAiOnline(aiRes)
        setLoading(false)

        const allOnline = isGatewayAlive && coreRes && aiRes

        if (allOnline) {
          // If initializing session for the first time
          setHasInitializedSession(true)

          // If recovering from a mid-session outage, show brief success toast
          if (wasOfflineRef.current) {
            wasOfflineRef.current = false
            setShowSuccessToast(true)
            setTimeout(() => {
              if (active) setShowSuccessToast(false)
            }, 4000)
          }
        } else {
          // If an outage is occurring mid-session, track it
          if (hasInitializedSession) {
            wasOfflineRef.current = true
          }
        }
      } catch (err) {
        if (active) {
          setGatewayOnline(false)
          setCoreOnline(false)
          setAiOnline(false)
          setLoading(false)
          if (hasInitializedSession) {
            wasOfflineRef.current = true
          }
        }
      }
    }

    checkStatus()
    const interval = setInterval(checkStatus, 4000)

    return () => {
      active = false
      clearInterval(interval)
    }
  }, [mounted, hasInitializedSession])

  // Don't render anything during SSR
  if (!mounted) return null

  const isSystemOnline = gatewayOnline === true && coreOnline === true && aiOnline === true
  const isOffline = gatewayOnline === false || coreOnline === false || aiOnline === false

  // --------------------------------------------------------------------------
  // SCENARIO A: INITIAL PAGE LOAD / STARTUP (Full-Screen Blurred Overlay Modal)
  // --------------------------------------------------------------------------
  if (!hasInitializedSession && (!isSystemOnline || loading)) {
    return (
      <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/80 backdrop-blur-md transition-all duration-500">
        <div className="w-[460px] max-w-[92vw] overflow-hidden rounded-2xl border border-white/15 bg-slate-900/90 p-6 text-center text-white shadow-2xl backdrop-blur-xl animate-in fade-in zoom-in-95 duration-300">
          {/* Status Icon Header */}
          <div className="mx-auto mb-5 flex h-16 w-16 items-center justify-center rounded-full bg-white/5 border border-white/10 shadow-inner">
            <div className="relative flex h-7 w-7 items-center justify-center">
              <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-amber-400 opacity-75"></span>
              <span className="relative inline-flex rounded-full h-4 w-4 bg-amber-500 shadow-md"></span>
            </div>
          </div>

          {/* Title & Subtitle */}
          <h2 className="text-xl font-bold tracking-tight text-slate-100 mb-2">
            Warming Up Platform Services
          </h2>

          <p className="text-xs text-slate-300 px-3 mb-6 leading-relaxed">
            Connecting Dots backend microservices are currently starting up. Please wait a moment while connection is established.
          </p>

          {/* Microservices Status Checklist */}
          <div className="space-y-3 rounded-xl bg-slate-950/60 p-4 text-left border border-white/10 mb-6">
            <div className="flex items-center justify-between text-xs">
              <span className="text-slate-300 font-medium">Gateway Service (Port 8080)</span>
              {gatewayOnline === null || loading ? (
                <span className="text-slate-400 text-xs animate-pulse">Checking...</span>
              ) : gatewayOnline ? (
                <span className="text-emerald-400 font-semibold flex items-center gap-1.5">
                  <span className="h-2 w-2 rounded-full bg-emerald-400" /> Connected
                </span>
              ) : (
                <span className="text-amber-400 font-semibold flex items-center gap-1.5 animate-pulse">
                  <span className="h-2 w-2 rounded-full bg-amber-400" /> Waking Up...
                </span>
              )}
            </div>

            <div className="flex items-center justify-between text-xs">
              <span className="text-slate-300 font-medium">Core Database Service (Port 8081)</span>
              {coreOnline === null || loading ? (
                <span className="text-slate-400 text-xs animate-pulse">Checking...</span>
              ) : coreOnline ? (
                <span className="text-emerald-400 font-semibold flex items-center gap-1.5">
                  <span className="h-2 w-2 rounded-full bg-emerald-400" /> Connected
                </span>
              ) : (
                <span className="text-amber-400 font-semibold flex items-center gap-1.5 animate-pulse">
                  <span className="h-2 w-2 rounded-full bg-amber-400" /> Waking Up...
                </span>
              )}
            </div>

            <div className="flex items-center justify-between text-xs">
              <span className="text-slate-300 font-medium">Gemini AI Service Worker (Port 8082)</span>
              {aiOnline === null || loading ? (
                <span className="text-slate-400 text-xs animate-pulse">Checking...</span>
              ) : aiOnline ? (
                <span className="text-emerald-400 font-semibold flex items-center gap-1.5">
                  <span className="h-2 w-2 rounded-full bg-emerald-400" /> Connected
                </span>
              ) : (
                <span className="text-amber-400 font-semibold flex items-center gap-1.5 animate-pulse">
                  <span className="h-2 w-2 rounded-full bg-amber-400" /> Waking Up...
                </span>
              )}
            </div>
          </div>

          <div className="flex items-center justify-center gap-2 text-slate-400 text-xs py-1">
            <span className="animate-spin inline-block h-3.5 w-3.5 border-2 border-amber-400 border-t-transparent rounded-full" />
            <span>Connecting to local/dev environment...</span>
          </div>
        </div>
      </div>
    )
  }

  // --------------------------------------------------------------------------
  // SCENARIO B: MID-SESSION OUTAGE (Highly Readable Floating Bottom-Right Toast)
  // --------------------------------------------------------------------------
  if (hasInitializedSession && isOffline) {
    const offlineServices = []
    if (gatewayOnline === false) offlineServices.push('Gateway (Port 8080)')
    if (coreOnline === false) offlineServices.push('Core Database (Port 8081)')
    if (aiOnline === false) offlineServices.push('Gemini AI (Port 8082)')

    return (
      <div className="fixed bottom-5 right-5 z-50 flex flex-col gap-2.5 rounded-xl border border-amber-500/50 bg-slate-900 p-4 text-slate-100 shadow-2xl backdrop-blur-xl animate-in fade-in slide-in-from-bottom-5 duration-300 w-84 max-w-[90vw]">
        <div className="flex items-center gap-2.5">
          <span className="relative flex h-3 w-3">
            <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-amber-400 opacity-75"></span>
            <span className="relative inline-flex rounded-full h-3 w-3 bg-amber-500"></span>
          </span>
          <strong className="text-sm font-bold text-amber-400 tracking-wide">
            Service Disconnection Alert
          </strong>
        </div>

        <p className="text-xs text-slate-100 font-medium leading-relaxed mt-0.5">
          {offlineServices.join(' & ')} {offlineServices.length > 1 ? 'are currently offline.' : 'is currently offline.'}
        </p>

        <div className="flex items-center justify-between mt-1 pt-2 border-t border-slate-800">
          <span className="text-[11px] text-slate-400 font-mono flex items-center gap-1.5">
            <span className="animate-spin inline-block h-2.5 w-2.5 border border-amber-400 border-t-transparent rounded-full" />
            Reconnecting automatically...
          </span>
        </div>
      </div>
    )
  }

  // --------------------------------------------------------------------------
  // SCENARIO C: RECOVERY TOAST (Brief Success Toast When Service Comes Back)
  // --------------------------------------------------------------------------
  if (showSuccessToast) {
    return (
      <div className="fixed bottom-5 right-5 z-50 flex items-center gap-3 rounded-xl border border-emerald-500/40 bg-slate-900 p-4 text-slate-100 shadow-2xl backdrop-blur-xl animate-in fade-in slide-in-from-bottom-5 duration-300 w-80">
        <div className="flex h-7 w-7 items-center justify-center rounded-full bg-emerald-500/20 text-emerald-400 font-bold text-sm">
          ✓
        </div>
        <div>
          <strong className="text-xs font-bold text-emerald-400 block">Connection Restored</strong>
          <span className="text-[11px] text-slate-300">All backend services are back online.</span>
        </div>
      </div>
    )
  }

  return null
}
