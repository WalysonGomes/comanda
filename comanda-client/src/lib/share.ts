/**
 * Owner sharing their own cardápio link (onboarding step 4 / "Meu link", tasks 6.6 and 8.1) —
 * unlike `storefront/whatsapp.ts`'s `buildWhatsAppUrl`, there's no fixed destination number here:
 * the dono picks the contact in their own WhatsApp. Prefers the Web Share API (native share sheet
 * when installed as a PWA); falls back to `wa.me` with no number, which opens WhatsApp's own
 * contact picker.
 */
export async function shareMenuLink(menuUrl: string, businessName: string): Promise<void> {
  const text = `Confira o cardápio de ${businessName}: https://${menuUrl}`
  if (navigator.share) {
    try {
      await navigator.share({ text })
      return
    } catch {
      // User cancelled or share failed — fall through to the wa.me fallback.
    }
  }
  window.open(`https://wa.me/?text=${encodeURIComponent(text)}`, '_blank', 'noopener,noreferrer')
}

export async function copyText(text: string): Promise<boolean> {
  try {
    await navigator.clipboard.writeText(text)
    return true
  } catch {
    return false
  }
}
