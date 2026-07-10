import { Link } from 'react-router'

const LIMIT_CODES = new Set(['PRODUCT_LIMIT_REACHED', 'CATEGORY_LIMIT_REACHED'])

/**
 * Task 9.1: a 402 from `menu-management` isn't just an error string — it's a dead end the dono
 * can act on. When the code is a plan limit, adds a link to "Plano e uso" instead of leaving the
 * message as a plain, unactionable dead end (PRD Seção 4/11: nenhum estado silencioso de falha).
 */
export function PlanLimitError({ code, message }: { code: string | null; message: string }) {
  return (
    <p className="text-[13px] font-semibold text-[#a05a4c]">
      {message}
      {code && LIMIT_CODES.has(code) && (
        <>
          {' '}
          <Link to="/painel/ajustes/plano" className="underline">
            Assine o Essencial
          </Link>
        </>
      )}
    </p>
  )
}
