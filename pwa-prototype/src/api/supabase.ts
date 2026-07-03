import { createClient } from '@supabase/supabase-js'

// Single Supabase client for the whole app. The anon key is public by design;
// authorization is enforced server-side by RLS.
const url = import.meta.env.VITE_SUPABASE_URL
const anonKey = import.meta.env.VITE_SUPABASE_ANON_KEY

if (!url || !anonKey) {
  throw new Error('Missing Supabase config — copy pwa-prototype/.env.example to .env.local and fill it in.')
}

export const supabase = createClient(url, anonKey, {
  auth: {
    persistSession: true,
    autoRefreshToken: true,
    detectSessionInUrl: true,
  },
})
