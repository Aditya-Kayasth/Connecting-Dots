/** @type {import('next').NextConfig} */
const nextConfig = {
  // output: 'standalone' is for Docker/self-hosted only — Vercel manages its own output
  typescript: {
    ignoreBuildErrors: true,
  },
  images: {
    unoptimized: true,
  },
}

export default nextConfig
