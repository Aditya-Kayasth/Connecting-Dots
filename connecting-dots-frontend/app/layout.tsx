import { Analytics } from '@vercel/analytics/next'
import type { Metadata, Viewport } from 'next'
import { BackendStatusBanner } from '@/components/backend-status-banner'
import './globals.css'

export const metadata: Metadata = {
  title: 'Connecting Dots — Technology for possibility',
  description: 'A trusted community connecting NGOs with technical contributors.',
  generator: 'v0.app',
}

export const viewport: Viewport = {
  colorScheme: 'light dark',
  themeColor: '#0F766E',
}

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en" className="bg-[#f5f6f2] dark:bg-slate-950">
      <body className="antialiased font-sans">
        <BackendStatusBanner />
        {children}
        {process.env.NODE_ENV === 'production' && <Analytics />}
      </body>
    </html>
  )
}
