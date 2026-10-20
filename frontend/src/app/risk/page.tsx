'use client'

import { useState, useEffect } from 'react'
import { Shield, AlertTriangle, CheckCircle2, Search, Plus, Save, Trash2, Info, Activity, Scale } from 'lucide-react'
import { Card, CardHeader, CardTitle, CardContent, CardDescription } from '@/components/ui/Card'
import { Button } from '@/components/ui/Button'
import { Input } from '@/components/ui/Input'
import { Badge } from '@/components/ui/Badge'
import { cn } from '@/lib/utils'
import { api } from '@/lib/api'

interface RiskRule {
  id: string
  name: string
  description: string
  priority: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'
  status: 'ACTIVE' | 'INACTIVE'
  scoreImpact: number
  createdAt: string
}

export default function RiskPage() {
  const [rules, setRules] = useState<RiskRule[]>([])
  const [loading, setLoading] = useState(true)
  const [searchQuery, setSearchQuery] = useState('')
  const [showAddModal, setShowAddModal] = useState(false)

  useEffect(() => {
    fetchRules()
  }, [])

  const fetchRules = async () => {
    setLoading(true)
    try {
      const res = await api.get('/risk/rules')
      setRules(res.data || [])
    } catch (error) {
      console.error('Failed to fetch risk rules:', error)
    } finally {
      setLoading(false)
    }
  }

  const filteredRules = rules.filter(r =>
    r.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
    r.description.toLowerCase().includes(searchQuery.toLowerCase())
  )

  const priorityColors = {
    CRITICAL: 'text-red-500 bg-red-500/10 border-red-500/20',
    HIGH: 'text-orange-500 bg-orange-500/10 border-orange-500/20',
    MEDIUM: 'text-amber-500 bg-amber-500/10 border-amber-500/20',
    LOW: 'text-blue-500 bg-blue-500/10 border-blue-500/20',
  }

  return (
    <div className="space-y-8 p-1">
      {/* Header Section */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div className="space-y-1">
          <div className="flex items-center gap-3">
            <div className="p-2 bg-primary/20 rounded-xl text-primary">
              <Shield className="h-6 w-6" />
            </div>
            <h1 className="text-3xl font-bold tracking-tight text-foreground">Risk Control Center</h1>
          </div>
          <p className="text-muted-foreground">Manage deterministic risk rules and scoring logic</p>
        </div>

        <div className="flex gap-3">
          <Button variant="neumorphic" onClick={() => setShowAddModal(true)} className="gap-2">
            <Plus className="h-4 w-4" />
            Create New Rule
          </Button>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-4 gap-8">
        {/* Risk Summary Sidebar */}
        <div className="space-y-6">
          <Card variant="glass" className="border-primary/20">
            <CardHeader>
              <CardTitle className="text-sm font-medium flex items-center gap-2">
                <Scale className="h-4 w-4" />
                Engine Health
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="p-4 rounded-2xl bg-white/5 border border-white/10 space-y-2">
                <p className="text-xs text-muted-foreground uppercase tracking-wider">Active Rules</p>
                <p className="text-3xl font-bold text-foreground">
                  {rules.filter(r => r.status === 'ACTIVE').length}
                </p>
              </div>
              <div className="p-4 rounded-2xl bg-white/5 border border-white/10 space-y-2">
                <p className="text-xs text-muted-foreground uppercase tracking-wider">Avg Score Impact</p>
                <p className="text-3xl font-bold text-primary">
                  {rules.length > 0
                    ? Math.round(rules.reduce((sum, r) => sum + r.scoreImpact, 0) / rules.length)
                    : 0
                  }
                </p>
              </div>
              <div className="p-4 rounded-2xl bg-white/5 border border-white/10 space-y-2">
                <p className="text-xs text-muted-foreground uppercase tracking-wider">Engine Status</p>
                <div className="flex items-center gap-2 text-green-500">
                  <div className="h-2 w-2 rounded-full bg-green-500 animate-pulse" />
                  <span className="text-sm font-semibold">Operational</span>
                </div>
              </div>
            </CardContent>
          </Card>

          <Card variant="neumorphic">
            <CardHeader>
              <CardTitle className="text-sm">Rule Guidelines</CardTitle>
            </CardHeader>
            <CardContent className="text-xs text-muted-foreground space-y-3">
              <div className="flex gap-2">
                <Info className="h-4 w-4 shrink-0 text-primary" />
                <p>Rules are evaluated deterministically using SQL aggregates.</p>
              </div>
              <div className="flex gap-2">
                <AlertTriangle className="h-4 w-4 shrink-0 text-amber-500" />
                <p>Critical rules trigger immediate BLOCK status regardless of total score.</p>
              </div>
            </CardContent>
          </Card>
        </div>

        {/* Rule Management Table */}
        <div className="lg:col-span-3 space-y-4">
          <Card variant="glass" className="border-primary/20 overflow-hidden">
            <CardHeader className="flex flex-row items-center justify-between">
              <div>
                <CardTitle className="text-foreground">Deterministic Rules</CardTitle>
                <CardDescription className="text-muted-foreground">Configure the scoring logic for the Risk Engine</CardDescription>
              </div>
              <div className="relative w-72">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                <Input
                  className="pl-9 bg-white/5 border-white/10 focus:bg-white/10 transition-all"
                  placeholder="Search rules by name or description..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                />
              </div>
            </CardHeader>
            <CardContent className="p-0">
              <div className="overflow-x-auto">
                <table className="w-full text-left border-collapse">
                  <thead className="bg-white/5 text-muted-foreground text-xs uppercase tracking-wider">
                    <tr>
                      <th className="px-6 py-4 font-medium">Rule Details</th>
                      <th className="px-6 py-4 font-medium">Priority</th>
                      <th className="px-6 py-4 font-medium">Score Impact</th>
                      <th className="px-6 py-4 font-medium">Status</th>
                      <th className="px-6 py-4 font-medium text-right">Actions</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-border/50">
                    {loading ? (
                      Array.from({ length: 5 }).map((_, i) => (
                        <tr key={i} className="animate-pulse">
                          <td colSpan={5} className="px-6 py-4 bg-white/5 h-16" />
                        </tr>
                      ))
                    ) : filteredRules.length > 0 ? (
                      filteredRules.map((rule) => (
                        <tr
                          key={rule.id}
                          className="group hover:bg-white/5 transition-all duration-200 cursor-pointer"
                        >
                          <td className="px-6 py-4">
                            <div className="flex flex-col">
                              <span className="text-sm font-semibold text-foreground">{rule.name}</span>
                              <span className="text-xs text-muted-foreground line-clamp-1">{rule.description}</span>
                            </div>
                          </td>
                          <td className="px-6 py-4">
                            <Badge variant="outline" className={cn('text-[10px] font-bold border', priorityColors[rule.priority])}>
                              {rule.priority}
                            </Badge>
                          </td>
                          <td className="px-6 py-4">
                            <div className="flex items-center gap-2">
                              <div className="h-1.5 w-12 bg-border rounded-full overflow-hidden">
                                <div
                                  className="h-full bg-primary"
                                  style={{ width: `${Math.min(rule.scoreImpact, 100)}%` }}
                                />
                              </div>
                              <span className="text-sm font-mono font-medium text-foreground">+{rule.scoreImpact}</span>
                            </div>
                          </td>
                          <td className="px-6 py-4">
                            <div className={cn(
                              'inline-flex items-center gap-1.5 px-2 py-1 rounded-full text-[10px] font-bold border',
                              rule.status === 'ACTIVE' ? 'text-green-500 bg-green-500/10 border-green-500/20' : 'text-slate-500 bg-slate-500/10 border-slate-500/20'
                            )}>
                              {rule.status === 'ACTIVE' ? <CheckCircle2 className="h-3 w-3" /> : <XCircle className="h-3 w-3" />}
                              {rule.status}
                            </div>
                          </td>
                          <td className="px-6 py-4 text-right">
                            <div className="flex justify-end gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
                              <Button variant="ghost" size="icon" className="h-8 w-8 rounded-lg hover:bg-primary/20">
                                <Save className="h-4 w-4" />
                              </Button>
                              <Button variant="ghost" size="icon" className="h-8 w-8 rounded-lg hover:bg-red-500/20 text-red-500">
                                <Trash2 className="h-4 w-4" />
                              </Button>
                            </div>
                          </td>
                        </tr>
                      ))
                    ) : (
                      <tr>
                        <td colSpan={5} className="px-6 py-12 text-center text-muted-foreground">
                          <div className="flex flex-col items-center gap-2">
                            <Shield className="h-8 w-8 opacity-20" />
                            <p>No risk rules found matching the search criteria</p>
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

      {/* Add Rule Modal */}
      {showAddModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-background/80 backdrop-blur-sm">
          <Card variant="glass" className="w-full max-w-lg border-primary/30 shadow-2xl">
            <CardHeader>
              <CardTitle>Define New Risk Rule</CardTitle>
              <CardDescription>Add a deterministic scoring rule to the engine</CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-2">
                  <label className="text-xs font-medium text-muted-foreground">Rule Name</label>
                  <Input className="neumorphic-inset bg-transparent" placeholder="e.g. High Velocity Check" />
                </div>
                <div className="space-y-2">
                  <label className="text-xs font-medium text-muted-foreground">Priority</label>
                  <select className="w-full p-2 rounded-xl bg-transparent border border-border neumorphic-inset text-sm focus:ring-2 focus:ring-primary outline-none">
                    <option value="LOW">Low</option>
                    <option value="MEDIUM">Medium</option>
                    <option value="HIGH">High</option>
                    <option value="CRITICAL">Critical</option>
                  </select>
                </div>
              </div>
              <div className="space-y-2">
                <label className="text-xs font-medium text-muted-foreground">Description</label>
                <Input className="neumorphic-inset bg-transparent" placeholder="Explain the logic (e.g. > 5 tx in 1 min)" />
              </div>
              <div className="space-y-2">
                <label className="text-xs font-medium text-muted-foreground">Score Impact (0-100)</label>
                <Input type="number" className="neumorphic-inset bg-transparent" placeholder="e.g. 45" />
              </div>
              <div className="flex gap-3 pt-4">
                <Button variant="outline" className="flex-1" onClick={() => setShowAddModal(false)}>
                  Cancel
                </Button>
                <Button variant="primary" className="flex-1" onClick={() => setShowAddModal(false)}>
                  Deploy Rule
                </Button>
              </div>
            </CardContent>
          </Card>
        </div>
      )}
    </div>
  )
}