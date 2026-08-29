import { Analytics } from '@vercel/analytics/next'
import type { Metadata, Viewport } from 'next'
import './globals.css'
import Navbar from '@/components/navbar'
import BackendStatusBanner from '@/components/backend-status-banner'
export const metadata: Metadata = { title: 'Connecting Dots — Technology for possibility', description: 'A trusted community connecting NGOs with technical contributors.', generator: 'v0.app' }
export const viewport: Viewport = { colorScheme: 'light', themeColor: '#f5f6f2' }
export default function RootLayout({children}:{children:React.ReactNode}){return <html lang="en" className="bg-[#f5f6f2]"><body className="antialiased"><BackendStatusBanner/><Navbar/>{children}{process.env.NODE_ENV==='production'&&<Analytics/>}</body></html>}
