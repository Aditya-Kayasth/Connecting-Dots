import { apiGet } from './api-client'

export interface CloudinarySignatureResponse {
  timestamp: number
  signature: string
  apiKey: string
  cloudName: string
}

/**
 * Directly uploads a file (PDF, Audio, Image) to Cloudinary after requesting a signature from core-service
 */
export async function uploadFileToCloudinary(file: File): Promise<string> {
  // 1. Get upload signature from core-service
  const sigData = await apiGet<CloudinarySignatureResponse>('/api/v1/core/files/signature')

  if (!sigData || !sigData.signature || !sigData.cloudName) {
    throw new Error('Failed to obtain file upload signature from backend service.')
  }

  // 2. Build multipart form data for Cloudinary
  const formData = new FormData()
  formData.append('file', file)
  formData.append('api_key', sigData.apiKey)
  formData.append('timestamp', sigData.timestamp.toString())
  formData.append('signature', sigData.signature)

  // 3. Post to Cloudinary REST endpoint
  const uploadUrl = `https://api.cloudinary.com/v1_1/${sigData.cloudName}/auto/upload`
  const res = await fetch(uploadUrl, {
    method: 'POST',
    body: formData,
  })

  if (!res.ok) {
    const errText = await res.text()
    throw new Error(`Cloudinary Upload Failed [${res.status}]: ${errText}`)
  }

  const result = await res.json()
  return result.secure_url || result.url
}
