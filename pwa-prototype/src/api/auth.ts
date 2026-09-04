import { supabase } from './supabase.ts'
import type { Session } from '@supabase/supabase-js'

export async function signIn(email: string, password: string): Promise<Session> {
  const { data, error } = await supabase.auth.signInWithPassword({ email, password })
  if (error) throw new Error(friendlyAuthError(error.message))
  return data.session
}

export async function signUp(email: string, password: string): Promise<Session | null> {
  const { data, error } = await supabase.auth.signUp({ email, password })
  if (error) throw new Error(friendlyAuthError(error.message))
  // Session is null when email confirmation is required by project settings.
  return data.session
}

export async function resetPassword(email: string): Promise<void> {
  const { error } = await supabase.auth.resetPasswordForEmail(email)
  if (error) throw new Error(friendlyAuthError(error.message))
}

export async function signOut(): Promise<void> {
  await supabase.auth.signOut()
}

export async function getSession(): Promise<Session | null> {
  const { data } = await supabase.auth.getSession()
  return data.session
}

// Display name lives in auth user metadata (profiles has no name column).
export async function updateFirstName(firstName: string): Promise<void> {
  const { error } = await supabase.auth.updateUser({ data: { first_name: firstName } })
  if (error) throw new Error(error.message)
}

function friendlyAuthError(message: string): string {
  const msg = message.toLowerCase()
  if (msg.includes('invalid login credentials')) return 'Wrong email or password.'
  if (msg.includes('already registered')) return 'An account already exists for this email.'
  if (msg.includes('at least 6')) return 'Password must be at least 6 characters.'
  if (msg.includes('rate limit')) return 'Too many attempts — wait a minute and retry.'
  return message
}
