import { formatCurrency } from '@/features/menu/format'
import { unitPrice, type CartLine } from '@/features/storefront/cart'
import type { DeliveryType } from '@/features/storefront/api'

/**
 * Builds the order message in the exact format of PRD Seção 3.1 — the only surface in the whole
 * UI allowed to use emojis (Regra 13). Per-line price shown is the product's own base price (not
 * multiplied by quantity, not including additionals) — additionals get their own line below; only
 * the final total at the bottom is the authoritative sum, matching the PRD's own example.
 */
export function buildOrderMessage(params: {
  lines: CartLine[]
  deliveryType: DeliveryType
  address: string
  deliveryFee: number
}): string {
  const { lines, deliveryType, address, deliveryFee } = params

  let message = 'Olá! Gostaria de fazer um pedido:\n\n🛒 Meu Pedido\n'
  for (const line of lines) {
    const spec = line.specs.length > 0 ? ` (${line.specs.join(', ')})` : ''
    message += `• ${line.quantity}x ${line.name}${spec} — ${formatCurrency(line.basePrice)}\n`
    for (const add of line.adds) {
      message += `  - Adicional: ${add.name} (+${formatCurrency(add.price)})\n`
    }
  }
  message += '\n'
  message += deliveryType === 'ENTREGA' ? `📍 Entrega em: ${address.trim() || '—'}\n\n` : '🛍️ Retirada no balcão\n\n'

  const fee = deliveryType === 'ENTREGA' ? deliveryFee : 0
  const subtotal = lines.reduce((sum, line) => sum + unitPrice(line) * line.quantity, 0)
  const total = subtotal + fee
  message += `💰 Total: ${formatCurrency(total)}${fee > 0 ? ` (inclui ${formatCurrency(fee)} de entrega)` : ''}\n`

  const notes = lines.filter((line) => line.note.trim().length > 0).map((line) => `${line.name}: ${line.note.trim()}`)
  if (notes.length > 0) {
    message += `\n📝 Obs: ${notes.join(' · ')}\n`
  }
  message += '\nPode confirmar disponibilidade e forma de pagamento?'
  return message
}

export function buildWhatsAppUrl(whatsappNumber: string, message: string): string {
  const digits = whatsappNumber.replace(/\D/g, '')
  return `https://wa.me/${digits}?text=${encodeURIComponent(message)}`
}
