import { useEffect, useRef, useState } from 'react'
import { Sheet } from '../../components/Sheet.tsx'
import { Button } from '../../components/Button.tsx'
import { Icon } from '../../components/Icon.tsx'
import { useToast } from '../../components/Toast.tsx'
import { haptic } from '../../lib/ios.ts'
import { invokeEdge } from '../../api/edge.ts'

// Real Hevy export chain: pick a workout-program PDF → parse-workout-pdf →
// choose a session → export-to-hevy (creates the routine in the user's Hevy).
type Exercise = { name: string }
type Section = { name: string; exercises: Exercise[] }
type Session = { name: string; sections: Section[] }
type ParsedProgram = { program_name: string; sessions: Session[] }
type ExportResult = { routine_title: string; exercises_matched: number; exercises_created: number }

type Stage = 'pick' | 'parsing' | 'sessions' | 'exporting' | 'done'

async function fileToBase64(file: File): Promise<string> {
  const dataUrl = await new Promise<string>((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(reader.result as string)
    reader.onerror = () => reject(new Error('Failed to read PDF'))
    reader.readAsDataURL(file)
  })
  return dataUrl.slice(dataUrl.indexOf(',') + 1)
}

export function HevyExportSheet({ open, onClose }: { open: boolean; onClose: () => void }) {
  const toast = useToast()
  const fileRef = useRef<HTMLInputElement>(null)
  const [stage, setStage] = useState<Stage>('pick')
  const [program, setProgram] = useState<ParsedProgram | null>(null)
  const [result, setResult] = useState<ExportResult | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (open) {
      setStage('pick')
      setProgram(null)
      setResult(null)
      setError(null)
    }
  }, [open])

  const parse = async (file: File) => {
    setStage('parsing')
    setError(null)
    try {
      const pdf_base64 = await fileToBase64(file)
      const parsed = await invokeEdge<ParsedProgram>('parse-workout-pdf', { pdf_base64 })
      setProgram(parsed)
      setStage('sessions')
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Could not parse this PDF')
      setStage('pick')
    }
  }

  const exportSession = async (session: Session) => {
    setStage('exporting')
    setError(null)
    try {
      const res = await invokeEdge<ExportResult>('export-to-hevy', { session })
      setResult(res)
      setStage('done')
      haptic('success')
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Export failed')
      setStage('sessions')
    }
  }

  const exerciseCount = (s: Session) => s.sections.reduce((n, sec) => n + sec.exercises.length, 0)

  return (
    <Sheet open={open} onClose={onClose} title="Export to Hevy" detents={['medium']}>
      <input
        ref={fileRef}
        type="file"
        accept="application/pdf"
        className="hidden"
        onChange={(e) => {
          const file = e.target.files?.[0]
          if (file) void parse(file)
          e.target.value = ''
        }}
      />

      <div className="flex flex-col gap-3 pt-1 pb-4">
        {error && (
          <div className="bg-error/12 border border-error/25 rounded-card px-4 py-3 text-[13px] text-error">{error}</div>
        )}

        {stage === 'pick' && (
          <>
            <p className="text-[13px] text-ink-2 px-1">
              Pick your workout-program PDF — the AI parses it and creates a routine in your Hevy account.
            </p>
            <Button variant="primary" full glow onClick={() => fileRef.current?.click()}>
              <Icon name="note" size={16} /> Choose a PDF
            </Button>
          </>
        )}

        {(stage === 'parsing' || stage === 'exporting') && (
          <div className="py-8 flex flex-col items-center gap-3">
            <div className="size-6 rounded-full border-2 border-primary/30 border-t-primary animate-spin" />
            <span className="text-[13px] text-ink-2">
              {stage === 'parsing' ? 'Reading your program…' : 'Creating the routine in Hevy…'}
            </span>
          </div>
        )}

        {stage === 'sessions' && program && (
          <>
            <div className="text-[13px] text-ink-2 px-1">
              <span className="font-semibold text-ink">{program.program_name}</span> — choose the session to export:
            </div>
            {program.sessions.map((session) => (
              <button
                key={session.name}
                type="button"
                onClick={() => {
                  haptic('light')
                  void exportSession(session)
                }}
                className="w-full bg-surface-1 rounded-card px-4 py-3.5 flex items-center gap-3 text-left"
              >
                <div className="size-9 rounded-[10px] bg-surface-3 flex items-center justify-center shrink-0">
                  <Icon name="dumbbell.fill" size={16} className="text-primary" />
                </div>
                <div className="flex-1 min-w-0">
                  <div className="text-[15px] font-semibold text-ink truncate">{session.name}</div>
                  <div className="text-[12px] text-ink-3">{exerciseCount(session)} exercises</div>
                </div>
                <Icon name="chevron.right" size={14} className="text-ink-4 shrink-0" />
              </button>
            ))}
          </>
        )}

        {stage === 'done' && result && (
          <div className="flex flex-col items-center gap-3 py-4 text-center">
            <div className="size-12 rounded-full bg-success/15 flex items-center justify-center">
              <Icon name="check" size={24} className="text-success" />
            </div>
            <div>
              <div className="text-[16px] font-semibold text-ink">{result.routine_title}</div>
              <div className="text-[13px] text-ink-2 mt-1">
                {result.exercises_matched} matched · {result.exercises_created} created
              </div>
            </div>
            <Button
              variant="primary"
              full
              onClick={() => {
                onClose()
                toast.show('Routine exported to Hevy')
              }}
            >
              Done
            </Button>
          </div>
        )}
      </div>
    </Sheet>
  )
}
