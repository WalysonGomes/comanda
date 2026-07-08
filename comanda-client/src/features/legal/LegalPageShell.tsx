import type { ReactNode } from 'react'
import { ArrowLeft } from 'lucide-react'
import { useNavigate } from 'react-router'

/** Shared chrome for the static legal pages (privacy policy, terms) — tasks 8.1-8.3. */
export function LegalPageShell({ title, children }: { title: string; children: ReactNode }) {
  const navigate = useNavigate()

  return (
    <div className="mx-auto flex min-h-svh max-w-2xl flex-col bg-cream px-4 pb-16">
      <header className="flex items-center gap-3 pt-5 pb-3">
        <button
          type="button"
          onClick={() => navigate(-1)}
          aria-label="Voltar"
          className="flex size-9 flex-none items-center justify-center rounded-[11px] bg-[#efe6d6] text-ink-2"
        >
          <ArrowLeft className="size-5" strokeWidth={2} />
        </button>
        <h1 className="font-display text-lg font-extrabold text-ink">{title}</h1>
      </header>
      <div className="prose flex flex-col gap-4 pt-2 text-[14px] leading-relaxed text-ink-2 [&_h2]:mt-2 [&_h2]:font-display [&_h2]:text-base [&_h2]:font-extrabold [&_h2]:text-ink [&_strong]:text-ink [&_ul]:list-disc [&_ul]:pl-5">
        {children}
      </div>
    </div>
  )
}
