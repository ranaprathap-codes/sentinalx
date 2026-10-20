'use client'

import { useState, useEffect } from 'react'
import { Zap, Shield, Search, Plus, Filter, ArrowUpRight, CheckCircle2, XCircle, Clock } from 'lucide-react'
import { Card, CardHeader, CardTitle, CardContent, CardDescription } from '@/components/ui/Card'
import { Button } from '@/components/ui/Button'
import { Input } from '@/components/ui/Input'
import { Badge } from '@/components/ui/Badge'
import { formatCurrency, formatNumber, cn } from '@/lib/utils'
import { api } from '@/lib/api'

interface Payment {
  id: string
  amountCents: number
  status: 'CREATED' | 'PROCESSING' | 'RISK_REVIEW' | 'AUTHORIZED' | 'COMPLETED' | 'BLOCKED'
  merchantId: string
  deviceId: string
  locationCountry: string
  createdAt: string
}

export default function PaymentsPage() {
  const [payments, setPayments] = useState<Payment[]>([])
  const [loading, setLoading] = useState(true)
  const [showCreateModal, setShowCreateModal] = useState(false)
  const [filter, setFilter] = useState('ALL')

  useEffect(() => {
    fetchPayments()
  }, [])

  const fetchPayments = async () => {
    setLoading(true)
    try {
      const res = await api.get('/payments?page=0&size=50')
      setPayments(res.data?.payments || [])
    } catch (error) {
      console.error('Failed to fetch payments:', error)
    } finally {
      setLoading(false)
    }
  }

  const filteredPayments = payments.filter(p =>
    filter === 'ALL' ? true : p.status === filter
  )

  const statusColors = {
    COMPLETED: 'text-green-500 bg-green-500/10 border-green-500/20',
    BLOCKED: 'text-red-500 bg-red-500/10 border-red-500/20',
    RISK_REVIEW: 'text-amber-500 bg-amber-500/10 border-amber-500/20',
    PROCESSING: 'text-blue-500 bg-blue-500/10 border-blue-500/20',
    CREATED: 'text-slate-500 bg-slate-500/10 border-slate-500/20',
    AUTHORIZED: 'text-cyan-500 bg-cyan-500/10 border-cyan-500/20',
  }

  const statusIcons = {
    COMPLETED: <CheckCircle2 className="h-3 w-3" />,
    BLOCKED: <XCircle className="h-3 w-3" />,
    RISK_REVIEW: <Clock className="h-3 w-3" />,
    PROCESSING: <Zap className="h-3 w-3 animate-pulse" />,
    CREATED: <Clock className="h-3 w-3" />,
    AUTHORIZED: <Shield className="h-3 w-3" />,
  }

  return (
    <div className="space-y-8 p-1">
      {/* Header Section */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div className="space-y-1">
          <div className="flex items-center gap-3">
            <div className="p-2 bg-primary/20 rounded-xl text-primary">
              <Zap className="h-6 w-6" />
            </div>
            <h1 className="text-3xl font-bold tracking-tight text-foreground">Payment Ledger</h1>
          </div>
          <p className="text-muted-foreground">Manage transaction lifecycles and real-time payment flows</p>
        </div>

        <div className="flex gap-3">
          <Button variant="neumorphic" onClick={() => setShowCreateModal(true)} className="gap-2">
            <Plus className="h-4 w-4" />
            New Payment
          </Button>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-4 gap-8">
        {/* Filters & Metrics Sidebar */}
        <div className="space-y-6">
          <Card variant="glass" className="border-primary/20">
            <CardHeader>
              <CardTitle className="text-sm font-medium flex items-center gap-2">
                <Filter className="h-4 w-4" />
                Filter Status
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-2">
              {['ALL', 'COMPLETED', 'BLOCKED', 'RISK_REVIEW', 'PROCESSING'].map((s) => (
                <button
                  key={s}
                  onClick={() => setFilter(s)}
                  className={cn(
                    'w-full text-left px-3 py-2 rounded-xl text-sm transition-all duration-200',
                    filter === s
                      ? 'bg-primary text-primary-foreground shadow-lg shadow-primary/30 scale-[1.02]'
                      : 'hover:bg-white/10 text-muted-foreground'
                  )}
                >
                  {s === 'ALL' ? 'All Transactions' : s.replace('_', ' ')}
                </button>
              ))}
            </CardContent>
          </Card>

          <Card variant="neumorphic">
            <CardContent className="pt-6 space-y-4">
              <div className="flex justify-between items-center">
                <span className="text-sm text-muted-foreground">Total Volume</span>
                <span className="text-lg font-bold text-foreground">
                  {formatCurrency(payments.reduce((sum, p) => sum + p.amountCents, 0))}
                </span>
              </div>
              <div className="flex justify-between items-center">
                <span className="text-sm text-muted-foreground">Avg Ticket</span>
                <span className="text-lg font-bold text-foreground">
                  {payments.length > 0
                    ? formatCurrency(payments.reduce((sum, p) => sum + p.amountCents, 0) / payments.length)
                    : '$0.00'
                  }
                </span>
              </div>
              <div className="h-px bg-border/50 my-2" />
              <div className="flex justify-between items-center">
                <span className="text-sm text-muted-foreground">Success Rate</span>
                <span className="text-lg font-bold text-green-500">
                  {payments.length > 0
                    ? Math.round((payments.filter(p => p.status === 'COMPLETED').length / payments.length) * 100) + '%'
                    : '0%'
                  }
                </span>
              </div>
            </CardContent>
          </Card>
        </div>

        {/* Main Ledger Table */}
        <div className="lg:col-span-3 space-y-4">
          <Card variant="glass" className="border-primary/20 overflow-hidden">
            <CardHeader className="flex flex-row items-center justify-between">
              <div>
                <CardTitle className="text-foreground">Transaction Ledger</CardTitle>
                <CardDescription className="text-muted-foreground">Live stream of all processed payments</CardDescription>
              </div>
              <div className="relative w-64">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                <Input
                  className="pl-9 bg-white/5 border-white/10 focus:bg-white/10 transition-all"
                  placeholder="Search TxID or Merchant..."
                />
              </div>
            </CardHeader>
            <CardContent className="p-0">
              <div className="overflow-x-auto">
                <table className="w-full text-left border-collapse">
                  <thead>
                    <tr className="bg-white/5 text-muted-foreground text-xs uppercase tracking-wider">
                      <th className="px-6 py-4 font-medium">Transaction ID</th>
                      <th className="px-6 py-4 font-medium">Amount</th>
                      <th className="px-6 py-4 font-medium">Status</th>
                      <th className="px-6 py-4 font-medium">Merchant</th>
                      <th className="px-6 py-4 font-medium text-right">Action</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-border/50">
                    {loading ? (
                      Array.from({ length: 5 }).map((_, i) => (
                        <tr key={i} className="animate-pulse">
                          <td colSpan={5} className="px-6 py-4 bg-white/5 h-16" />
                        </tr>
                      ))
                    ) : filteredPayments.length > 0 ? (
                      filteredPayments.map((p) => (
                        <tr
                          key={p.id}
                          className="group hover:bg-white/5 transition-all duration-200 cursor-pointer"
                        >
                          <td className="px-6 py-4">
                            <div className="flex flex-col">
                              <span className="text-sm font-mono font-medium text-foreground">
                                {p.id.slice(0, 8)}...
                              </span>
                              <span className="text-[10px] text-muted-foreground">{p.createdAt}</span>
                            </div>
                          </td>
                          <td className="px-6 py-4">
                            <span className="text-sm font-bold text-foreground">
                              {formatCurrency(p.amountCents)}
                            </span>
                          </td>
                          <td className="px-6 py-4">
                            <div className={cn(
                              'inline-flex items-center gap-1.5 px-2 py-1 rounded-full text-[10px] font-bold border uppercase',
                              statusColors[p.status] || 'text-slate-500 bg-slate-500/10 border-slate-500/20'
                            )}>
                              {statusIcons[p.status] || <Clock className="h-3 w-3" />}
                              {p.status}
                            </div>
                          </td>
                          <td className="px-6 py-4">
                            <div className="flex flex-col">
                              <span className="text-sm text-foreground">{p.merchantId}</span>
                              <span className="text-[10px] text-muted-foreground">{p.locationCountry}</span>
                            </div>
                          </td>
                          <td className="px-6 py-4 text-right">
                            <Button variant="ghost" size="icon" className="rounded-full opacity-0 group-hover:opacity-100 transition-opacity">
                              <ArrowUpRight className="h-4 w-4" />
                            </Button>
                          </td>
                        </tr>
                      ))
                    ) : (
                      <tr>
                        <td colSpan={5} className="px-6 py-12 text-center text-muted-foreground">
                          <div className="flex flex-col items-center gap-2">
                            <Zap className="h-8 w-8 opacity-20" />
                            <p>No transactions found matching the current filter</p>
                          </div>
                        </td>
                      </tr>
                    )}
                  </tbody>
                </table>
              </div>
            </CardContent>
          </Card>
        </div>
      </div>

      {/* Create Payment Modal (Conceptual implementation) */}
      {showCreateModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-background/80 backdrop-blur-sm">
          <Card variant="glass" className="w-full max-w-md border-primary/30 shadow-2xl">
            <CardHeader>
              <CardTitle>Initiate Payment</CardTitle>
              <CardDescription>Enter transaction details to trigger the payment engine</CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="space-y-2">
                <label className="text-xs font-medium text-muted-foreground">Amount (Cents)</label>
                <Input className="neumorphic-inset bg-transparent" placeholder="e.g. 10000" />
              </div>
              <div className="space-y-2">
                <label className="text-xs font-medium text-muted-foreground">Merchant ID</label>
                <Input className="neumorphic-inset bg-transparent" placeholder="MERCHANT_001" />
              </div>
              <div className="space-y-2">
                <label className="text-xs font-medium text-muted-foreground">Idempotency Key</label>
                <Input className="neumorphic-inset bg-transparent" placeholder="idem_unique_key" />
              </div>
              <div className="flex gap-3 pt-4">
                <Button variant="outline" className="flex-1" onClick={() => setShowCreateModal(false)}>
                  Cancel
                </Button>
                <Button variant="primary" className="flex-1" onClick={() => setShowCreateModal(false)}>
                  Execute Transfer
                </Button>
              </div>
            </CardContent>
          </Card>
        </div>
      )}
    </div>
  )
}