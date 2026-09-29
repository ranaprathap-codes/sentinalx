import { HTMLAttributes, forwardRef } from 'react'
import { cn } from '@/lib/utils'

interface BadgeProps extends HTMLAttributes<HTMLSpanElement> {
  variant?: 'default' | 'success' | 'warning' | 'danger' | 'info' | 'status'
  status?: string
}

export const Badge = forwardRef<HTMLSpanElement, BadgeProps>(
  ({ className, variant = 'default', status, children, ...props }, ref) => {
    const variants = {
      default: 'bg-slate-500/20 text-slate-300 border border-slate-500/30',
      success: 'bg-green-500/20 text-green-400 border border-green-500/30',
      warning: 'bg-amber-500/20 text-amber-400 border border-amber-500/30',
      danger: 'bg-red-500/20 text-red-400 border border-red-500/30',
      info: 'bg-blue-500/20 text-blue-400 border border-blue-500/30',
      status: status ? cn(
        'inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium',
        getStatusColor(status)
      ) : 'bg-slate-500/20 text-slate-300 border border-slate-500/30',
    }

    return (
      <span
        ref={ref}
        className={cn(
          'inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium border',
          variants[variant],
          className
        )}
        {...props}
      >
        {children}
      </span>
    )
  }
)

Badge.displayName = 'Badge'

function getStatusColor(status: string): string {
  const colors: Record<string, string> = {
    CREATED: 'bg-slate-500/20 text-slate-400 border-slate-500/30',
    PROCESSING: 'bg-blue-500/20 text-blue-400 border-blue-500/30',
    RISK_REVIEW: 'bg-amber-500/20 text-amber-400 border-amber-500/30',
    AUTHORIZED: 'bg-green-500/20 text-green-400 border-green-500/30',
    COMPLETED: 'bg-emerald-500/20 text-emerald-400 border-emerald-500/30',
    BLOCKED: 'bg-red-500/20 text-red-400 border-red-500/30',
    FAILED: 'bg-red-500/20 text-red-400 border-red-500/30',
    CANCELLED: 'bg-slate-500/20 text-slate-400 border-slate-500/30',
    REFUNDED: 'bg-purple-500/20 text-purple-400 border-purple-500/30',
    ALLOW: 'bg-green-500/20 text-green-400 border-green-500/30',
    REVIEW: 'bg-amber-500/20 text-amber-400 border-amber-500/30',
    BLOCK: 'bg-red-500/20 text-red-400 border-red-500/30',
    OPEN: 'bg-blue-500/20 text-blue-400 border-blue-500/30',
    INVESTIGATING: 'bg-amber-500/20 text-amber-400 border-amber-500/30',
    IN_DEVELOPMENT: 'bg-purple-500/20 text-purple-400 border-purple-500/30',
    TESTING: 'bg-cyan-500/20 text-cyan-400 border-cyan-500/30',
    RESOLVED: 'bg-green-500/20 text-green-400 border-green-500/30',
    CLOSED: 'bg-slate-500/20 text-slate-400 border-slate-500/30',
    WONT_FIX: 'bg-slate-500/20 text-slate-400 border-slate-500/30',
    LOW: 'bg-green-500/20 text-green-400 border-green-500/30',
    MEDIUM: 'bg-amber-500/20 text-amber-400 border-amber-500/30',
    HIGH: 'bg-orange-500/20 text-orange-400 border-orange-500/30',
    CRITICAL: 'bg-red-500/20 text-red-400 border-red-500/30',
    SEV1: 'bg-red-500/20 text-red-400 border-red-500/30',
    SEV2: 'bg-orange-500/20 text-orange-400 border-orange-500/30',
    SEV3: 'bg-amber-500/20 text-amber-400 border-amber-500/30',
    SEV4: 'bg-blue-500/20 text-blue-400 border-blue-500/30',
    DETECTED: 'bg-blue-500/20 text-blue-400 border-blue-500/30',
    RUNNING: 'bg-blue-500/20 text-blue-400 border-blue-500/30',
    FAILED: 'bg-red-500/20 text-red-400 border-red-500/30',
    CANCELLED: 'bg-slate-500/20 text-slate-400 border-slate-500/30',
  }
  return colors[status] || 'bg-slate-500/20 text-slate-400 border-slate-500/30'
}