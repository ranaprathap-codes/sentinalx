'use client'

import { useState, useEffect } from 'react'
import { Shield, Zap, FlaskConical, Bug, Users, BarChart3, Terminal, ArrowUpRight, TrendingUp, TrendingDown, Activity } from 'lucide-react'
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
      // Fetch data from multiple endpoints in parallel
      const [paymentsRes, riskRes, threatlabRes, communityRes] = await Promise.allSettled([
        api.get('/payments?page=0&size=5'),
        api.get('/risk/rules'),
        api.get('/threatlab/scenarios'),
        api.get('/community/reports?page=0&size=5'),
      ])

      // Process payments
      let paymentsStats = { total: 0, completed: 0, blocked: 0, review: 0, totalAmount: 0 }
      if (paymentsRes.status === 'fulfilled') {
        const payments = paymentsRes.value.data?.payments || []
        paymentsStats = payments.reduce((acc, p: any) => {
          acc.total++
          acc.totalAmount += p.amountCents
          if (p.status === 'COMPLETED') acc.completed++
          else if (p.status === 'BLOCKED') acc.blocked++
          else if (p.status === 'RISK_REVIEW') acc.review++
          return acc
        }, paymentsStats)
      }

      // Process risk
      let riskStats = { evaluations: 0, avgScore: 0, allowRate: 0, blockRate: 0 }
      if (riskRes.status === 'fulfilled') {
        riskStats.evaluations = riskRes.value.data?.length || 0
      }

      // Process threatlab
      let threatlabStats = { simulations: 0, detected: 0, scenarios: 0 }
      if (threatlabRes.status === 'fulfilled') {
        threatlabStats.scenarios = threatlabRes.value.data?.length || 0
      }

      // Process community
      let communityStats = { reports: 0, openReports: 0, headlines: 0 }
      if (communityRes.status === 'fulfilled') {
        const reports = communityRes.value.data?.reports || []
        communityStats.reports = communityRes.value.data?.totalElements || 0
        communityStats.openReports = reports.filter((r: any) => r.status === 'OPEN').length
      }

      setStats({
        payments: paymentsStats,
        risk: riskStats,
        threatlab: threatlabStats,
        community: communityStats,
      })

      // Mock recent activity for now
      setActivity([
        { id: '1', type: 'payment', title: 'Payment Processed', description: 'Transaction txn_abc123 completed for $1,250.00', timestamp: new Date().toISOString(), status: 'COMPLETED' },
        { id: '2', type: 'risk', title: 'Risk Evaluation', description: 'High velocity transaction blocked (score: 87)', timestamp: new Date(Date.now() - 300000).toISOString(), status: 'BLOCK' },
        { id: '3', type: 'simulation', title: 'Simulation Completed', description: 'Rapid Fire Transactions scenario detected threat', timestamp: new Date(Date.now() - 600000).toISOString(), status: 'DETECTED' },
        { id: '4', type: 'report', title: 'New Flaw Report', description: 'Duplicate payment processing after retry', timestamp: new Date(Date.now() - 900000).toISOString(), status: 'OPEN' },
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
      value: stats ? formatNumber(stats.payments.total) : '—',
      change: '+12%',
      changeType: 'up',
      icon: Zap,
      color: 'bg-blue-500/20 text-blue-400',
      href: '/payments',
    },
    {
      title: 'Completed Volume',
      value: stats ? formatCurrency(stats.payments.totalAmount) : '—',
      change: '+8%',
      changeType: 'up',
      icon: Shield,
      color: 'bg-green-500/20 text-green-400',
      href: '/payments',
    },
    {
      title: 'Blocked Transactions',
      value: stats ? formatNumber(stats.payments.blocked) : '—',
      change: '-3%',
      changeType: 'down',
      icon: Shield,
      color: 'bg-red-500/20 text-red-400',
      href: '/payments',
    },
    {
      title: 'Risk Evaluations',
      value: stats ? formatNumber(stats.risk.evaluations) : '—',
      change: '+5%',
      changeType: 'up',
      icon: Activity,
      color: 'bg-purple-500/20 text-purple-400',
      href: '/risk',
    },
    {
      title: 'Active Scenarios',
      value: stats ? formatNumber(stats.threatlab.scenarios) : '—',
      change: '0%',
      changeType: 'neutral',
      icon: FlaskConical,
      color: 'bg-orange-500/20 text-orange-400',
      href: '/threatlab',
    },
    {
      title: 'Open Reports',
      value: stats ? formatNumber(stats.community.openReports) : '—',
      change: '+2',
      changeType: 'up',
      icon: Bug,
      color: 'bg-amber-500/20 text-amber-400',
      href: '/community',
    },
  ]

  if (loading) {
    return (
      <div className="space-y-6">
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {[...Array(6)].map((_, i) => (
            <Card key={i} className="animate-pulse">
              <CardContent className="pt-6">
                <div className="h-4 bg-sentinel-border rounded w-3/4 mb-4" />
                <div className="h-8 bg-sentinel-border rounded w-1/2" />
              </CardContent>
            </Card>
          ))}
        </div>
        <Card className="animate-pulse">
          <CardContent className="pt-6 space-y-4">
            {[...Array(5)].map((_, i) => (
              <div key={i} className="h-12 bg-sentinel-border rounded" />
            ))}
          </CardContent>
        </Card>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-white">Dashboard</h1>
          <p className="text-slate-400 mt-1">Overview of your SentinelX environment</p>
        </div>
        <div className="flex gap-2">
          <Button variant="outline" size="sm">
            <ArrowUpRight className="h-4 w-4 mr-2" />
            View All
          </Button>
        </div>
      </div>

      {/* Stats Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-6 gap-4">
        {statCards.map((stat) => (
          <Card key={stat.title} className="group hover:border-primary-500/30 transition-colors">
            <CardContent className="pt-6">
              <div className="flex items-start justify-between">
                <div>
                  <p className="text-sm text-slate-400">{stat.title}</p>
                  <p className="text-2xl font-bold text-white mt-1">{stat.value}</p>
                  <div className="flex items-center gap-1 mt-2">
                    {stat.changeType === 'up' && <TrendingUp className="h-4 w-4 text-green-400" />}
                    {stat.changeType === 'down' && <TrendingDown className="h-4 w-4 text-red-400" />}
                    {stat.changeType === 'neutral' && <Activity className="h-4 w-4 text-slate-400" />}
                    <span className={cn(
                      'text-sm font-medium',
                      stat.changeType === 'up' && 'text-green-400',
                      stat.changeType === 'down' && 'text-red-400',
                      stat.changeType === 'neutral' && 'text-slate-400'
                    )}>
                      {stat.change}
                    </span>
                  </div>
                </div>
                <div className={cn('p-3 rounded-lg', stat.color)}>
                  <stat.icon className="h-6 w-6" />
                </div>
              </div>
            </CardContent>
          </Card>
        ))}
      </div>

      {/* Quick Actions & Recent Activity */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Quick Actions */}
        <Card className="lg:col-span-1">
          <CardHeader>
            <CardTitle>Quick Actions</CardTitle>
            <CardDescription>Common tasks to get started</CardDescription>
          </CardHeader>
          <CardContent className="space-y-3">
            <Button variant="outline" className="w-full justify-start gap-3" asChild>
              <Link href="/payments/new">
                <Zap className="h-4 w-4" />
                Create Payment
              </Link>
            </Button>
            <Button variant="outline" className="w-full justify-start gap-3" asChild>
              <Link href="/threatlab/run">
                <FlaskConical className="h-4 w-4" />
                Run Simulation
              </Link>
            </Button>
            <Button variant="outline" className="w-full justify-start gap-3" asChild>
              <Link href="/community/new">
                <Bug className="h-4 w-4" />
                Report Issue
              </Link>
            </Button>
            <Button variant="outline" className="w-full justify-start gap-3" asChild>
              <Link href="/dryrun/new">
                <Bug className="h-4 w-4" />
                Create Dry Run
              </Link>
            </Button>
            <Button variant="outline" className="w-full justify-start gap-3" asChild>
              <Link href="/risk/rules">
                <Shield className="h-4 w-4" />
                Manage Risk Rules
              </Link>
            </Button>
            <Button variant="outline" className="w-full justify-start gap-3" asChild>
              <Link href="/headlines">
                <BarChart3 className="h-4 w-4" />
                View Headlines
              </Link>
            </Button>
          </CardContent>
        </Card>

        {/* Recent Activity */}
        <Card className="lg:col-span-2">
          <CardHeader>
            <CardTitle>Recent Activity</CardTitle>
            <CardDescription>Latest events across the platform</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              {activity.map((item) => (
                <div
                  key={item.id}
                  className={cn(
                    'flex items-start gap-4 p-3 rounded-lg hover:bg-sentinel-border/50 transition-colors',
                    item.type === 'payment' && 'border-l-2 border-blue-500',
                    item.type === 'risk' && 'border-l-2 border-purple-500',
                    item.type === 'simulation' && 'border-l-2 border-orange-500',
                    item.type === 'report' && 'border-l-2 border-amber-500'
                  )}
                >
                  <div className={cn(
                    'p-2 rounded-lg',
                    item.type === 'payment' && 'bg-blue-500/20',
                    item.type === 'risk' && 'bg-purple-500/20',
                    item.type === 'simulation' && 'bg-orange-500/20',
                    item.type === 'report' && 'bg-amber-500/20'
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
                    {item.status && <Badge variant="status" status={item.status} />}
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
          { title: 'SentinelPay', icon: Zap, status: 'operational', description: 'Payment processing active', metrics: ['99.9% uptime', '<50ms avg latency'] },
          { title: 'Risk Engine', icon: Shield, status: 'operational', description: '25 rules active', metrics: ['<10ms eval time', '94% accuracy'] },
          { title: 'Threat Lab', icon: FlaskConical, status: 'operational', description: '15 scenarios ready', metrics: ['5 default scenarios', 'Custom supported'] },
          { title: 'Community', icon: Users, status: 'operational', description: 'Engineering collaboration', metrics: ['Real-time discussions', 'Auto-clustering'] },
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

import Link from 'next/link'