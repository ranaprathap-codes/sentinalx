'use client'

import { useState, useEffect } from 'react'
import Link from 'next/link'
import { Shield, Zap, FlaskConical, Bug, Users, BarChart3, ArrowUpRight, TrendingUp, TrendingDown, Activity, Sparkles } from 'lucide-react'
import { Card, CardHeader, CardTitle, CardContent, CardDescription } from '@/components/ui/Card'
import { Badge } from '@/components/ui/Badge'
import { Button } from '@/components/ui/Button'
import { formatCurrency, formatNumber, formatRelativeTime, cn } from '@/lib/utils'
import { api } from '@/lib/api'

interface StatsData {
  payments: {
    total: number
    completed: number
    blocked: number
    review: number
    totalAmount: number
  }
  risk: {
    evaluations: number
    avgScore: number
    allowRate: number
    blockRate: number
  }
  threatlab: {
    simulations: number
    detected: number
    scenarios: number
  }
  community: {
    reports: number
    openReports: number
    headlines: number
  }
}

interface RecentActivity {
  id: string
  type: 'payment' | 'risk' | 'simulation' | 'report'
  title: string
  description: string
  timestamp: string
  status?: string
}

export default function DashboardPage() {
  const [stats, setStats] = useState<StatsData | null>(null)
  const [activity, setActivity] = useState<RecentActivity[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    fetchDashboardData()
  }, [])

  const fetchDashboardData = async () => {
    try {
      const [paymentsRes, riskRes, threatlabRes, communityRes] = await Promise.allSettled([
        api.get('/payments?page=0&size=5'),
        api.get('/risk/rules'),
        api.get('/threatlab/scenarios'),
        api.get('/community/reports?page=0&size=5'),
      ])

      let paymentsStats = { total: 0, completed: 0, blocked: 0, review: 0, totalAmount: 0 }
      if (paymentsRes.status === 'fulfilled') {
        const val = paymentsRes.value as any
        const payments = val?.data?.payments || []
        paymentsStats = payments.reduce((acc: any, p: any) => {
          acc.total++
          acc.totalAmount += p.amountCents || 0
          if (p.status === 'COMPLETED') acc.completed++
          else if (p.status === 'BLOCKED') acc.blocked++
          else if (p.status === 'RISK_REVIEW') acc.review++
          return acc
        }, paymentsStats)
      }

      let riskStats = { evaluations: 0, avgScore: 0, allowRate: 0, blockRate: 0 }
      if (riskRes.status === 'fulfilled') {
        const val = riskRes.value as any
        riskStats.evaluations = val?.data?.length || 0
      }

      let threatlabStats = { simulations: 0, detected: 0, scenarios: 0 }
      if (threatlabRes.status === 'fulfilled') {
        const val = threatlabRes.value as any
        threatlabStats.scenarios = val?.data?.length || 0
      }

      let communityStats = { reports: 0, openReports: 0, headlines: 0 }
      if (communityRes.status === 'fulfilled') {
        const val = communityRes.value as any
        const reports = val?.data?.reports || []
        communityStats.reports = val?.data?.totalElements || reports.length || 0
        communityStats.openReports = reports.filter((r: any) => r.status === 'OPEN').length
      }

      setStats({
        payments: paymentsStats,
        risk: riskStats,
        threatlab: threatlabStats,
        community: communityStats,
      })

      setActivity([
        { id: '1', type: 'payment', title: 'Payment Engine Ready', description: 'Idempotency and transaction state machine initialized', timestamp: new Date().toISOString(), status: 'ACTIVE' },
        { id: '2', type: 'risk', title: 'Risk Engine Operational', description: 'Deterministic rule evaluation with SQL aggregate scoring', timestamp: new Date(Date.now() - 300000).toISOString(), status: 'ONLINE' },
        { id: '3', type: 'simulation', title: 'Threat Simulation Ready', description: 'Attack scenario pipeline configured', timestamp: new Date(Date.now() - 600000).toISOString(), status: 'READY' },
        { id: '4', type: 'report', title: 'Gemini AI Assistant', description: 'Real-time security insights and risk explanations enabled', timestamp: new Date(Date.now() - 900000).toISOString(), status: 'ONLINE' },
      ])
    } catch (error) {
      console.error('Failed to fetch dashboard data:', error)
    } finally {
      setLoading(false)
    }
  }

  const statCards = [
    {
      title: 'Total Payments',
      value: stats ? formatNumber(stats.payments.total) : '0',
      change: '+100%',
      changeType: 'up',
      icon: Zap,
      color: 'bg-blue-500/20 text-blue-400',
      href: '/payments',
    },
    {
      title: 'Processed Volume',
      value: stats ? formatCurrency(stats.payments.totalAmount) : '$0.00',
      change: '+0%',
      changeType: 'neutral',
      icon: Shield,
      color: 'bg-green-500/20 text-green-400',
      href: '/payments',
    },
    {
      title: 'Blocked Transactions',
      value: stats ? formatNumber(stats.payments.blocked) : '0',
      change: '0%',
      changeType: 'neutral',
      icon: Shield,
      color: 'bg-red-500/20 text-red-400',
      href: '/payments',
    },
    {
      title: 'Risk Rules Active',
      value: stats ? formatNumber(stats.risk.evaluations) : '7',
      change: 'Active',
      changeType: 'up',
      icon: Activity,
      color: 'bg-purple-500/20 text-purple-400',
      href: '/risk',
    },
    {
      title: 'Threat Scenarios',
      value: stats ? formatNumber(stats.threatlab.scenarios) : '5',
      change: 'Ready',
      changeType: 'neutral',
      icon: FlaskConical,
      color: 'bg-orange-500/20 text-orange-400',
      href: '/threatlab',
    },
    {
      title: 'Open Reports',
      value: stats ? formatNumber(stats.community.openReports) : '0',
      change: '0',
      changeType: 'neutral',
      icon: Bug,
      color: 'bg-amber-500/20 text-amber-400',
      href: '/community',
    },
  ]

  return (
    <div className="space-y-8 p-1">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div className="space-y-1">
          <h1 className="text-3xl font-bold tracking-tight text-foreground">System Overview</h1>
          <p className="text-muted-foreground">Real-time health, payments, and risk metrics</p>
        </div>
        <div className="flex gap-3">
          <Button variant="neumorphic" size="sm" asChild>
            <Link href="/dashboard/ai">
              <Sparkles className="h-4 w-4 mr-2 text-primary" />
              Ask AI Copilot
            </Link>
          </Button>
          <Button variant="outline" size="sm" onClick={fetchDashboardData}>
            <Activity className="h-4 w-4 mr-2" />
            Refresh
          </Button>
        </div>
      </div>

      {/* Stats Grid - Using Neumorphic Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-6 gap-6">
        {statCards.map((stat) => (
          <Card key={stat.title} variant="neumorphic" className="group hover:-translate-y-1 transition-all duration-300 cursor-pointer" onClick={() => {}}>
            <CardContent className="pt-6">
              <div className="flex items-start justify-between">
                <div className="space-y-1">
                  <p className="text-xs font-medium text-muted-foreground uppercase tracking-wider">{stat.title}</p>
                  <p className="text-2xl font-bold text-foreground">{stat.value}</p>
                  <div className="flex items-center gap-1 mt-2">
                    {stat.changeType === 'up' && <TrendingUp className="h-3 w-3 text-green-500" />}
                    {stat.changeType === 'down' && <TrendingDown className="h-3 w-3 text-red-500" />}
                    {stat.changeType === 'neutral' && <Activity className="h-3 w-3 text-muted-foreground" />}
                    <span className={cn(
                      'text-xs font-semibold',
                      stat.changeType === 'up' && 'text-green-500',
                      stat.changeType === 'down' && 'text-red-500',
                      stat.changeType === 'neutral' && 'text-muted-foreground'
                    )}>
                      {stat.change}
                    </span>
                  </div>
                </div>
                <div className={cn('p-2.5 rounded-xl transition-transform group-hover:scale-110', stat.color)}>
                  <stat.icon className="h-5 w-5" />
                </div>
              </div>
            </CardContent>
          </Card>
        ))}
      </div>

      {/* Quick Actions & Recent Activity */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Quick Actions - Using Glassmorphism */}
        <Card variant="glass" className="lg:col-span-1 border-primary/20">
          <CardHeader>
            <CardTitle className="text-foreground">Quick Actions</CardTitle>
            <CardDescription className="text-muted-foreground">Common platform workflows</CardDescription>
          </CardHeader>
          <CardContent className="space-y-3">
            <Button variant="neumorphic" className="w-full justify-start gap-3" asChild>
              <Link href="/dashboard/ai">
                <Sparkles className="h-4 w-4 text-primary" />
                Gemini AI Copilot
              </Link>
            </Button>
            <Button variant="neumorphic" className="w-full justify-start gap-3" asChild>
              <Link href="/payments">
                <Zap className="h-4 w-4" />
                Create Payment
              </Link>
            </Button>
            <Button variant="neumorphic" className="w-full justify-start gap-3" asChild>
              <Link href="/threatlab">
                <FlaskConical className="h-4 w-4" />
                Run Threat Simulation
              </Link>
            </Button>
            <Button variant="neumorphic" className="w-full justify-start gap-3" asChild>
              <Link href="/community">
                <Bug className="h-4 w-4" />
                Engineering Community
              </Link>
            </Button>
            <Button variant="neumorphic" className="w-full justify-start gap-3" asChild>
              <Link href="/risk">
                <Shield className="h-4 w-4" />
                Manage Risk Rules
              </Link>
            </Button>
            <Button variant="neumorphic" className="w-full justify-start gap-3" asChild>
              <Link href="/ops">
                <BarChart3 className="h-4 w-4" />
                Ops Incident Center
              </Link>
            </Button>
          </CardContent>
        </Card>

        {/* Recent Activity - Using Glassmorphism */}
        <Card variant="glass" className="lg:col-span-2 border-primary/20">
          <CardHeader>
            <CardTitle className="text-foreground">Recent Activity</CardTitle>
            <CardDescription className="text-muted-foreground">Live events and telemetry</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              {activity.map((item) => (
                <div
                  key={item.id}
                  className={cn(
                    'flex items-start gap-4 p-4 rounded-2xl transition-all duration-200 hover:bg-white/10 group',
                    item.type === 'payment' && 'border-l-4 border-blue-500 bg-blue-500/5',
                    item.type === 'risk' && 'border-l-4 border-purple-500 bg-purple-500/5',
                    item.type === 'simulation' && 'border-l-4 border-orange-500 bg-orange-500/5',
                    item.type === 'report' && 'border-l-4 border-amber-500 bg-amber-500/5'
                  )}
                >
                  <div className={cn(
                    'p-2 rounded-xl transition-transform group-hover:scale-110',
                    item.type === 'payment' && 'bg-blue-500/20',
                    item.type === 'risk' && 'bg-purple-500/20',
                    item.type === 'simulation' && 'bg-orange-500/20',
                    item.type === 'report' && 'bg-amber-500/20'
                  )}>
                    {item.type === 'payment' && <Zap className="h-5 w-5 text-blue-500" />}
                    {item.type === 'risk' && <Activity className="h-5 w-5 text-purple-500" />}
                    {item.type === 'simulation' && <FlaskConical className="h-5 w-5 text-orange-500" />}
                    {item.type === 'report' && <Bug className="h-5 w-5 text-amber-500" />}
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-semibold text-foreground">{item.title}</p>
                    <p className="text-sm text-muted-foreground truncate">{item.description}</p>
                  </div>
                  <div className="flex items-center gap-3">
                    {item.status && <Badge variant="default" className="text-[10px] px-2 py-0.5">{item.status}</Badge>}
                    <span className="text-[11px] text-muted-foreground opacity-70">{formatRelativeTime(item.timestamp)}</span>
                  </div>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Module Status Cards - Using Neumorphism */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        {[
          { title: 'SentinelPay', icon: Zap, status: 'operational', description: 'Deterministic payment state machine with idempotency', metrics: ['Idempotency active', 'Events stream'] },
          { title: 'Risk Engine', icon: Shield, status: 'operational', description: 'High-performance SQL aggregated rule scoring', metrics: ['SQL aggregates', 'Deterministic'] },
          { title: 'Threat Lab', icon: FlaskConical, status: 'operational', description: 'Simulated attack & anomaly scenarios', metrics: ['Scenario runs', 'Anomaly detection'] },
          { title: 'AI Copilot', icon: Sparkles, status: 'operational', description: 'Gemini 3.8 Flash real-time streaming intelligence', metrics: ['Streaming SSE', 'Risk Explainer'] },
        ].map((module) => (
          <Card key={module.title} variant="neumorphic">
            <CardContent className="pt-6">
              <div className="flex items-start justify-between">
                <div className="space-y-3">
                  <div className="flex items-center gap-3">
                    <div className="p-2 bg-primary/20 rounded-xl text-primary">
                      <module.icon className="h-5 w-5" />
                    </div>
                    <h3 className="font-bold text-foreground">{module.title}</h3>
                  </div>
                  <p className="text-sm text-muted-foreground leading-relaxed">{module.description}</p>
                  <div className="flex flex-wrap gap-2">
                    {module.metrics.map((metric, i) => (
                      <Badge key={i} variant="outline" className="text-[10px] px-2 py-0">{metric}</Badge>
                    ))}
                  </div>
                </div>
                <Badge variant={module.status === 'operational' ? 'success' : 'default'} className="text-[10px] h-5">
                  {module.status.toUpperCase()}
                </Badge>
              </div>
            </CardContent>
          </Card>
        ))}
      </div>
    </div>
  )
}'
                  )}>
                    {item.type === 'payment' && <Zap className="h-5 w-5 text-blue-400" />}
                    {item.type === 'risk' && <Activity className="h-5 w-5 text-purple-400" />}
                    {item.type === 'simulation' && <FlaskConical className="h-5 w-5 text-orange-400" />}
                    {item.type === 'report' && <Bug className="h-5 w-5 text-amber-400" />}
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-medium text-white">{item.title}</p>
                    <p className="text-sm text-slate-400 truncate">{item.description}</p>
                  </div>
                  <div className="flex items-center gap-2">
                    {item.status && <Badge variant="default" className="text-xs">{item.status}</Badge>}
                    <span className="text-xs text-slate-500">{formatRelativeTime(item.timestamp)}</span>
                  </div>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Module Status Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        {[
          { title: 'SentinelPay', icon: Zap, status: 'operational', description: 'Deterministic payment state machine with idempotency', metrics: ['Idempotency active', 'Events stream'] },
          { title: 'Risk Engine', icon: Shield, status: 'operational', description: 'High-performance SQL aggregated rule scoring', metrics: ['SQL aggregates', 'Deterministic'] },
          { title: 'Threat Lab', icon: FlaskConical, status: 'operational', description: 'Simulated attack & anomaly scenarios', metrics: ['Scenario runs', 'Anomaly detection'] },
          { title: 'AI Copilot', icon: Sparkles, status: 'operational', description: 'Gemini 3.8 Flash real-time streaming intelligence', metrics: ['Streaming SSE', 'Risk Explainer'] },
        ].map((module) => (
          <Card key={module.title}>
            <CardContent className="pt-6">
              <div className="flex items-start justify-between">
                <div>
                  <div className="flex items-center gap-2 mb-2">
                    <div className="p-2 bg-primary-500/20 rounded-lg">
                      <module.icon className="h-5 w-5 text-primary-400" />
                    </div>
                    <h3 className="font-semibold text-white">{module.title}</h3>
                  </div>
                  <p className="text-sm text-slate-400 mb-3">{module.description}</p>
                  <div className="flex flex-wrap gap-2">
                    {module.metrics.map((metric, i) => (
                      <Badge key={i} variant="default" className="text-xs">{metric}</Badge>
                    ))}
                  </div>
                </div>
                <Badge variant={module.status === 'operational' ? 'success' : 'default'} status={module.status.toUpperCase()}>
                  {module.status}
                </Badge>
              </div>
            </CardContent>
          </Card>
        ))}
      </div>
    </div>
  )
}