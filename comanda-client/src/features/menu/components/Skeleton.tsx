import { cn } from '@/lib/utils'

/** Shimmer placeholder (`.sk` in the approved design) shown while menu data is loading. */
export function Skeleton({ className }: { className?: string }) {
  return <div className={cn('sk', className)} />
}
