/** Footer with the privacy policy link (task 8.3, PRD 3.1) — the only footer content the MVP
 * requires. Terms of service is reachable from the privacy page, keeping this line short on a
 * mobile-first layout. */
export function Footer() {
  return (
    <footer className="flex flex-wrap items-center justify-center gap-x-3 gap-y-1 px-4 pt-2 pb-6 text-center text-[11.5px] text-ink-3">
      <a href="/privacidade" className="underline">
        Política de Privacidade
      </a>
      <span aria-hidden className="opacity-50">
        ·
      </span>
      <a href="/termos" className="underline">
        Termos de Serviço
      </a>
    </footer>
  )
}
