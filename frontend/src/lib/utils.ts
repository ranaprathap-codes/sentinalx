import { clsx, type ClassValue } from 'clsx'
import { twMerge } from 'tailwind-merge'

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}

export function formatCurrency(cents: number, currency = 'USD'): string {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency,
    minimumFractionDigits: 2,
  }).format(cents / 100)
}

export function formatNumber(num: number): string {
  return new Intl.NumberFormat('en-US').format(num)
}

export function formatDate(date: string | Date): string {
  return new Intl.DateTimeFormat('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(date))
}

export function formatRelativeTime(date: string | Date): string {
  const now = new Date()
  const then = new Date(date)
  const diffMs = now.getTime() - then.getTime()
  const diffSecs = Math.floor(diffMs / 1000)
  const diffMins = Math.floor(diffSecs / 60)
  const diffHours = Math.floor(diffMins / 60)
  const diffDays = Math.floor(diffHours / 24)

  if (diffSecs < 60) return 'just now'
  if (diffMins < 60) return `${diffMins}m ago`
  if (diffHours < 24) return `${diffHours}h ago`
  if (diffDays < 7) return `${diffDays}d ago`
  return formatDate(date)
}

export function getStatusColor(status: string): string {
  const colors: Record<string, string> = {
    CREATED: 'bg-slate-500/20 text-slate-400',
    PROCESSING: 'bg-blue-500/20 text-blue-400',
    RISK_REVIEW: 'bg-amber-500/20 text-amber-400',
    AUTHORIZED: 'bg-green-500/20 text-green-400',
    COMPLETED: 'bg-emerald-500/20 text-emerald-400',
    BLOCKED: 'bg-red-500/20 text-red-400',
    FAILED: 'bg-red-500/20 text-red-400',
    CANCELLED: 'bg-slate-500/20 text-slate-400',
    REFUNDED: 'bg-purple-500/20 text-purple-400',
    ALLOW: 'bg-green-500/20 text-green-400',
    REVIEW: 'bg-amber-500/20 text-amber-400',
    BLOCK: 'bg-red-500/20 text-red-400',
    OPEN: 'bg-blue-500/20 text-blue-400',
    INVESTIGATING: 'bg-amber-500/20 text-amber-400',
    IN_DEVELOPMENT: 'bg-purple-500/20 text-purple-400',
    TESTING: 'bg-cyan-500/20 text-cyan-400',
    RESOLVED: 'bg-green-500/20 text-green-400',
    CLOSED: 'bg-slate-500/20 text-slate-400',
    WONT_FIX: 'bg-slate-500/20 text-slate-400',
    LOW: 'bg-green-500/20 text-green-400',
    MEDIUM: 'bg-amber-500/20 text-amber-400',
    HIGH: 'bg-orange-500/20 text-orange-400',
    CRITICAL: 'bg-red-500/20 text-red-400',
    SEV1: 'bg-red-500/20 text-red-400',
    SEV2: 'bg-orange-500/20 text-orange-400',
    SEV3: 'bg-amber-500/20 text-amber-400',
    SEV4: 'bg-blue-500/20 text-blue-400',
    DETECTED: 'bg-blue-500/20 text-blue-400',
    RUNNING: 'bg-blue-500/20 text-blue-400',
    COMPLETED: 'bg-green-500/20 text-green-400',
    FAILED: 'bg-red-500/20 text-red-400',
    CANCELLED: 'bg-slate-500/20 text-slate-400',
  }
  return colors[status] || 'bg-slate-500/20 text-slate-400'
}

export function getSeverityColor(severity: string): string {
  const colors: Record<string, string> = {
    LOW: 'bg-green-500/20 text-green-400 border-green-500/30',
    MEDIUM: 'bg-amber-500/20 text-amber-400 border-amber-500/30',
    HIGH: 'bg-orange-500/20 text-orange-400 border-orange-500/30',
    CRITICAL: 'bg-red-500/20 text-red-400 border-red-500/30',
  }
  return colors[severity] || 'bg-slate-500/20 text-slate-400 border-slate-500/30'
}

export function truncate(str: string, length: number): string {
  if (str.length <= length) return str
  return str.slice(0, length) + '...'
}

export function generateIdempotencyKey(): string {
  return `idem_${Date.now()}_${Math.random().toString(36).slice(2, 11)}`
}