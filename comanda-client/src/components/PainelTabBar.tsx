import { BookOpen, ClipboardList, Settings } from 'lucide-react'
import { useNavigate } from 'react-router'

/**
 * Bottom tab bar (`.design/Comanda Painel.dc.html` "bottom tab bar"). Ajustes (task 9) now has
 * real screens behind it (Meu link, Plano e uso) — the dead-route caveat from `owner-pwa` no
 * longer applies.
 */
export function PainelTabBar({ current }: { current: 'pedidos' | 'cardapio' | 'ajustes' }) {
  const navigate = useNavigate()
  const tabs = [
    { key: 'pedidos' as const, label: 'Pedidos', icon: ClipboardList, path: '/painel/pedidos' },
    { key: 'cardapio' as const, label: 'Cardápio', icon: BookOpen, path: '/painel/cardapio' },
    { key: 'ajustes' as const, label: 'Ajustes', icon: Settings, path: '/painel/ajustes' },
  ]

  return (
    <div className="flex flex-none border-t border-line bg-cream px-2 pt-1.5 pb-0.5">
      {tabs.map((tab) => {
        const active = tab.key === current
        const Icon = tab.icon
        return (
          <button
            key={tab.key}
            type="button"
            onClick={() => navigate(tab.path)}
            className="flex flex-1 flex-col items-center gap-1 py-1.5"
            style={{ color: active ? 'var(--acc-d)' : '#a89a82' }}
          >
            <Icon className="size-[22px]" strokeWidth={active ? 2.25 : 2} />
            <span className="text-[10.5px] font-bold">{tab.label}</span>
          </button>
        )
      })}
    </div>
  )
}
