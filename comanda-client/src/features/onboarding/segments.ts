import { Cake, IceCreamBowl, Sandwich, UtensilsCrossed } from 'lucide-react'

import type { Segment } from '@/features/onboarding/api'

export const SEGMENTS: { value: Segment; label: string; desc: string; icon: typeof Cake }[] = [
  { value: 'MARMITARIA', label: 'Marmitaria', desc: 'Marmitas e refeições prontas', icon: UtensilsCrossed },
  { value: 'CONFEITARIA', label: 'Confeitaria', desc: 'Bolos, doces e salgados', icon: Cake },
  { value: 'HAMBURGUERIA', label: 'Hamburgueria', desc: 'Lanches e burgers artesanais', icon: Sandwich },
  { value: 'ACAIZERIA', label: 'Açaiteria', desc: 'Açaí e complementos', icon: IceCreamBowl },
]
