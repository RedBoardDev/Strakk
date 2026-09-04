import { supabase } from './supabase.ts'

// Compress an image file to a JPEG blob under ~300 KB (KMP convention for AI
// uploads), downscaling to fit maxDim.
export async function compressImage(file: File, maxDim = 1280, quality = 0.8): Promise<Blob> {
  const bitmap = await createImageBitmap(file)
  const scale = Math.min(1, maxDim / Math.max(bitmap.width, bitmap.height))
  const width = Math.round(bitmap.width * scale)
  const height = Math.round(bitmap.height * scale)
  const canvas = document.createElement('canvas')
  canvas.width = width
  canvas.height = height
  const ctx = canvas.getContext('2d')
  if (!ctx) throw new Error('Canvas unavailable')
  ctx.drawImage(bitmap, 0, 0, width, height)
  bitmap.close()

  let q = quality
  for (;;) {
    const blob = await new Promise<Blob | null>((resolve) => canvas.toBlob(resolve, 'image/jpeg', q))
    if (!blob) throw new Error('Image encoding failed')
    if (blob.size <= 300_000 || q <= 0.4) return blob
    q -= 0.15
  }
}

export async function blobToBase64(blob: Blob): Promise<string> {
  const dataUrl = await new Promise<string>((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(reader.result as string)
    reader.onerror = () => reject(new Error('Failed to read image'))
    reader.readAsDataURL(blob)
  })
  return dataUrl.slice(dataUrl.indexOf(',') + 1) // strip the data-URI prefix
}

// Upload a meal photo for AI scanning. Path convention (bucket RLS):
// {userId}/{draftId}/{itemId}.jpg — first segment must be the caller's uid.
export async function uploadMealPhoto(blob: Blob): Promise<string> {
  const { data } = await supabase.auth.getUser()
  const userId = data.user?.id
  if (!userId) throw new Error('Not signed in')
  const draftId = `draft-${crypto.randomUUID().replaceAll('-', '')}`
  const path = `${userId}/${draftId}/${crypto.randomUUID()}.jpg`
  const { error } = await supabase.storage
    .from('meal-photos')
    .upload(path, blob, { contentType: 'image/jpeg', upsert: true })
  if (error) throw new Error(error.message)
  return path
}
