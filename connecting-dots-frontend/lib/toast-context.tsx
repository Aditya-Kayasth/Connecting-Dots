'use client'

import React, { createContext, useContext, useState, useCallback } from 'react'

export interface Toast {
  id: string
  type: 'success' | 'error' | 'info'
  message: string
  duration?: number
}

interface ToastContextType {
  toasts: Toast[]
  addToast: (message: string, type?: 'success' | 'error' | 'info', duration?: number) => void
  removeToast: (id: string) => void
}

const ToastContext = createContext<ToastContextType | undefined>(undefined)

export function ToastProvider({ children }: { children: React.ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([])

  const removeToast = useCallback((id: string) => {
    setToasts((prev) => prev.filter((t) => t.id !== id))
  }, [])

  const addToast = useCallback(
    (message: string, type: 'success' | 'error' | 'info' = 'info', duration = 4000) => {
      const id = Math.random().toString(36).substr(2, 9)
      const toast: Toast = { id, type, message, duration }
      setToasts((prev) => [...prev, toast])

      if (duration > 0) {
        setTimeout(() => removeToast(id), duration)
      }
    },
    [removeToast],
  )

  return (
    <ToastContext.Provider value={{ toasts, addToast, removeToast }}>
      {children}
    </ToastContext.Provider>
  )
}

export function useToast() {
  const context = useContext(ToastContext)
  if (!context) {
    throw new Error('useToast must be used within ToastProvider')
  }
  return context
}
