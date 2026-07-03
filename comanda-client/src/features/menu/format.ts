export function formatCurrency(value: string | number): string {
  const amount = typeof value === 'string' ? Number(value) : value
  return amount.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })
}
