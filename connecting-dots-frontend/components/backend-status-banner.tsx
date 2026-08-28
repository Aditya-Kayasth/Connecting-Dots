'use client'

import { useEffect, useState } from 'react'
import { AlertCircle, CheckCircle2, Loader2, RefreshCw } from 'lucide-react'

export function BackendStatusBanner() {
  const [status, setStatus] = useState<'checking' | 'healthy' | 'waking' | 'error'>('checking')
  const [isVisible, setIsVisible] = useState(true)

  const checkHealth = async () => {
    setStatus('checking')
    const baseUrl = process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8080'
    const controller = new AbortController()
    const timeoutId = setTimeout(() => {
      setStatus('waking')
    }, 2500)

    try {
      const res = await fetch(`${baseUrl}/api/v1/core/admin/stats`, {
        method: 'GET',
        signal: controller.signal,
      }).catch(() => null)

      clearTimeout(timeoutId)
      if (res && (res.ok || res.status === 401 || res.status === 403)) {
        setStatus('healthy')
        setTimeout(() => setIsVisible(false), 3000)
      } else {
        setStatus('waking')
      }
    } catch {
      clearTimeout(timeoutId)
      setStatus('waking')
    }
  }

  useEffect(() => {
    checkHealth()
  }, [])

  if (!isVisible && status === 'healthy') return null

  return (
    <div className="w-full bg-slate-900 text-slate-100 text-xs py-2 px-4 border-b border-slate-800 transition-all">
      <div className="max-w-7xl mx-auto flex items-center justify-between gap-3">
        <div className="flex items-center gap-2">
          {status === 'checking' && (
            <>
              <Loader2 className="w-3.5 h-3.5 animate-spin text-teal-400" />
              <span>Connecting to backend services...</span>
            </>
          )}

          {status === 'waking' && (
            <>
              <AlertCircle className="w-3.5 h-3.5 text-amber-400 animate-pulse" />
              <span>
                <strong>Backend Cold Start Notice:</strong> If running on free-tier cloud hosting (Render), boot-up can take ~30–60 seconds. Thank you for your patience!
              </span>
            </>
          )}

          {status === 'healthy' && (
            <>
              <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400" />
              <span className="text-emerald-300 font-medium">Backend microservices online and connected!</span>
            </>
          )}
        </div>

        <button
          onClick={checkHealth}
          className="flex items-center gap-1 hover:text-teal-300 underline underline-offset-2 text-[11px] opacity-80 hover:opacity-100 transition-opacity"
        >
          <RefreshCw className="w-3 h-3" /> Retry Connection
        </button>
      </div>
    </div>
  )
}
