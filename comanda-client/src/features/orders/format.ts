import type { DeliveryType, OrderStatus } from '@/features/orders/api'

export function formatCurrency(value: string | number): string {
  const amount = typeof value === 'string' ? Number(value) : value
  return amount.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })
}

export function formatRelativeTime(iso: string): string {
  const diffMs = Date.now() - new Date(iso).getTime()
  const minutes = Math.floor(diffMs / 60000)
  if (minutes < 1) return 'agora'
  if (minutes < 60) return `${minutes} min`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `há ${hours}h`
  return new Date(iso).toLocaleDateString('pt-BR')
}

/** Status META — colors/labels copied verbatim from `.design/Comanda Painel.dc.html` (`META`). */
export const STATUS_META: Record<OrderStatus, { label: string; fg: string; bg: string; accent: string; step: number }> = {
  RECEBIDO: { label: 'Novo', fg: '#b53c25', bg: '#fbe6df', accent: '#d6492f', step: 0 },
  ACEITO: { label: 'Aceito', fg: '#2f49b8', bg: '#e7ebfb', accent: '#3b5bd0', step: 1 },
  EM_PREPARO: { label: 'Em preparo', fg: '#7a5310', bg: '#f7ecd2', accent: '#c9881a', step: 2 },
  PRONTO: { label: 'Pronto', fg: '#176b41', bg: '#dcefe1', accent: '#1f8a52', step: 3 },
  ENTREGUE: { label: 'Entregue', fg: '#6a6052', bg: '#eee8dd', accent: '#8a7f6e', step: 4 },
  CANCELADO: { label: 'Cancelado', fg: '#8a4a3d', bg: '#f0e1dc', accent: '#a05a4c', step: -1 },
}

export const STEPS: { label: string; color: string }[] = [
  { label: 'Novo', color: '#d6492f' },
  { label: 'Aceito', color: '#3b5bd0' },
  { label: 'Preparo', color: '#c9881a' },
  { label: 'Pronto', color: '#1f8a52' },
  { label: 'Entregue', color: '#8a7f6e' },
]

export const NEXT_STATUS: Partial<Record<OrderStatus, OrderStatus>> = {
  RECEBIDO: 'ACEITO',
  ACEITO: 'EM_PREPARO',
  EM_PREPARO: 'PRONTO',
  PRONTO: 'ENTREGUE',
}

export const NEXT_LABEL: Partial<Record<OrderStatus, string>> = {
  RECEBIDO: 'Aceitar pedido',
  ACEITO: 'Iniciar preparo',
  EM_PREPARO: 'Marcar como pronto',
  PRONTO: 'Marcar como entregue',
}

export const FILTER_DEFS: { key: OrderStatus | 'TODOS'; label: string }[] = [
  { key: 'TODOS', label: 'Todos' },
  { key: 'RECEBIDO', label: 'Novos' },
  { key: 'ACEITO', label: 'Aceitos' },
  { key: 'EM_PREPARO', label: 'Em preparo' },
  { key: 'PRONTO', label: 'Prontos' },
  { key: 'ENTREGUE', label: 'Entregues' },
  { key: 'CANCELADO', label: 'Cancelados' },
]

export const EMPTY_STATE: Record<OrderStatus | 'TODOS', [string, string]> = {
  TODOS: ['Nenhum pedido ainda hoje', 'Compartilhe o link do seu cardápio para começar a receber pedidos.'],
  RECEBIDO: ['Nenhum pedido novo', 'Os pedidos que chegarem aparecem aqui automaticamente.'],
  ACEITO: ['Nenhum pedido aceito', 'Aceite um pedido novo para vê-lo nesta lista.'],
  EM_PREPARO: ['Nada em preparo', 'Inicie o preparo de um pedido aceito.'],
  PRONTO: ['Nenhum pedido pronto', 'Os pedidos prontos para entrega ou retirada aparecem aqui.'],
  ENTREGUE: ['Nenhum pedido entregue ainda', 'O histórico de entregas de hoje aparece aqui.'],
  CANCELADO: ['Nenhum cancelamento', 'Que bom — nenhum pedido foi cancelado hoje.'],
}

/** WhatsApp preview per advance, format verbatim from the design (emojis only in this message). */
export function waMessageForAdvance(customerName: string, shortCode: string, next: OrderStatus, deliveryType: DeliveryType): string {
  const fn = customerName.split(' ')[0]
  switch (next) {
    case 'ACEITO':
      return `Olá ${fn}! ✅ Recebi seu pedido ${shortCode} e ele já foi *aceito*. Começo o preparo já já!`
    case 'EM_PREPARO':
      return `${fn}, boa notícia: seu pedido ${shortCode} já está na chapa 🔥 Preparando tudo com carinho!`
    case 'PRONTO':
      return deliveryType === 'ENTREGA'
        ? `${fn}, seu pedido ${shortCode} está *pronto* e saindo para entrega 🛵 Já te aviso quando chegar!`
        : `${fn}, seu pedido ${shortCode} está *pronto* para retirada 🍔 Pode vir buscar!`
    case 'ENTREGUE':
      return `Pedido ${shortCode} entregue ✅ Muito obrigado pela preferência, ${fn}! Volte sempre 🙏`
    default:
      return ''
  }
}
