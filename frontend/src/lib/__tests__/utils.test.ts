import { formatCurrency, formatNumber, formatRelativeTime, getStatusColor, truncate, generateIdempotencyKey } from '@/lib/utils'

describe('formatCurrency', () => {
  it('formats cents to dollars correctly', () => {
    expect(formatCurrency(10000)).toBe('$100.00')
    expect(formatCurrency(12345)).toBe('$123.45')
    expect(formatCurrency(0)).toBe('$0.00')
  })

  it('handles different currencies', () => {
    expect(formatCurrency(10000, 'EUR')).toContain('€')
    expect(formatCurrency(10000, 'GBP')).toContain('£')
  })
})

describe('formatNumber', () => {
  it('formats numbers with commas', () => {
    expect(formatNumber(1000)).toBe('1,000')
    expect(formatNumber(1000000)).toBe('1,000,000')
    expect(formatNumber(42)).toBe('42')
  })
})

describe('formatRelativeTime', () => {
  const now = new Date('2024-01-15T12:00:00Z')

  beforeEach(() => {
    jest.useFakeTimers()
    jest.setSystemTime(now)
  })

  afterEach(() => {
    jest.useRealTimers()
  })

  it('shows "just now" for recent times', () => {
    expect(formatRelativeTime(new Date(now.getTime() - 30000))).toBe('just now')
  })

  it('shows minutes ago', () => {
    expect(formatRelativeTime(new Date(now.getTime() - 300000))).toBe('5m ago')
  })

  it('shows hours ago', () => {
    expect(formatRelativeTime(new Date(now.getTime() - 7200000))).toBe('2h ago')
  })

  it('shows days ago', () => {
    expect(formatRelativeTime(new Date(now.getTime() - 86400000))).toBe('1d ago')
  })

  it('shows formatted date for older times', () => {
    expect(formatRelativeTime(new Date(now.getTime() - 86400000 * 10))).toContain('Jan')
  })
})

describe('getStatusColor', () => {
  it('returns correct colors for payment statuses', () => {
    expect(getStatusColor('COMPLETED')).toContain('green')
    expect(getStatusColor('BLOCKED')).toContain('red')
    expect(getStatusColor('PROCESSING')).toContain('blue')
    expect(getStatusColor('RISK_REVIEW')).toContain('amber')
  })

  it('returns correct colors for risk decisions', () => {
    expect(getStatusColor('ALLOW')).toContain('green')
    expect(getStatusColor('REVIEW')).toContain('amber')
    expect(getStatusColor('BLOCK')).toContain('red')
  })

  it('returns default for unknown status', () => {
    expect(getStatusColor('UNKNOWN')).toContain('slate')
  })
})

describe('truncate', () => {
  it('truncates long strings', () => {
    expect(truncate('hello world', 8)).toBe('hello...')
    expect(truncate('short', 10)).toBe('short')
  })

  it('handles edge cases', () => {
    expect(truncate('', 5)).toBe('')
    expect(truncate('exact', 5)).toBe('exact')
  })
})

describe('generateIdempotencyKey', () => {
  it('generates unique keys', () => {
    const keys = new Set()
    for (let i = 0; i < 100; i++) {
      keys.add(generateIdempotencyKey())
    }
    expect(keys.size).toBe(100)
  })

  it('follows expected format', () => {
    const key = generateIdempotencyKey()
    expect(key).toMatch(/^idem_\d+_[a-z0-9]+$/)
  })
})