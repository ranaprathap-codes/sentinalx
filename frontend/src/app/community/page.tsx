'use client'

import { useState, useEffect } from 'react'
import { Bug, MessageSquare, ThumbsUp, Filter, Plus, Search, Send, ShieldAlert, Lightbulb, Code } from 'lucide-react'
import { Card, CardHeader, CardTitle, CardContent, CardDescription } from '@/components/ui/Card'
import { Button } from '@/components/ui/Button'
import { Input } from '@/components/ui/Input'
import { Badge } from '@/components/ui/Badge'
import { cn } from '@/lib/utils'
import { api } from '@/lib/api'

interface Report {
  id: string
  type: 'FLAW_REPORT' | 'CODER_BLOCK' | 'FEATURE_PROPOSAL'
  title: string
  description: string
  severity: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'
  component: string
  tags: string[]
  status: 'OPEN' | 'INVESTIGATING' | 'RESOLVED'
  createdAt: string
  author: string
}

export default function CommunityPage() {
  const [reports, setReports] = useState<Report[]>([])
  const [loading, setLoading] = useState(true)
  const [filter, setFilter] = useState('ALL')
  const [searchQuery, setSearchQuery] = useState('')
  const [showCreateModal, setShowCreateModal] = useState(false)

  useEffect(() => {
    fetchReports()
  }, [])

  const fetchReports = async () => {
    setLoading(true)
    try {
      const res = await api.get('/community/reports?page=0&size=50')
      setReports(res.data?.reports || [])
    } catch (error) {
      console.error('Failed to fetch reports:', error)
    } finally {
      setLoading(false)
    }
  }

  const filteredReports = reports.filter(r => {
    const matchesFilter = filter === 'ALL' ? true : r.type === filter
    const matchesSearch = r.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
                          r.description.toLowerCase().includes(searchQuery.toLowerCase())
    return matchesFilter && matchesSearch
  })

  const typeColors = {
    FLAW_REPORT: 'text-red-500 bg-red-500/10 border-red-500/20',
    CODER_BLOCK: 'text-blue-500 bg-blue-500/10 border-blue-500/20',
    FEATURE_PROPOSAL: 'text-green-500 bg-green-500/10 border-green-500/20',
  }

  const typeIcons = {
    FLAW_REPORT: <ShieldAlert className="h-4 w-4" />,
    CODER_BLOCK: <Code className="h-4 w-4" />,
    FEATURE_PROPOSAL: <Lightbulb className="h-4 w-4" />,
  }

  return (
    <div className="space-y-8 p-1">
      {/* Header Section */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div className="space-y-1">
          <div className="flex items-center gap-3">
            <div className="p-2 bg-primary/20 rounded-xl text-primary">
              <Users className="h-6 w-6" />
            </div>
            <h1 className="text-3xl font-bold tracking-tight text-foreground">Engineering Hub</h1>
          </div>
          <p className="text-muted-foreground">Collaborative intelligence for flaw reports and system improvements</p>
        </div>

        <div className="flex gap-3">
          <Button variant="neumorphic" onClick={() => setShowCreateModal(true)} className="gap-2">
            <Plus className="h-4 w-4" />
            Submit Report
          </Button>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-4 gap-8">
        {/* Filter Sidebar */}
        <div className="space-y-6">
          <Card variant="glass" className="border-primary/20">
            <CardHeader>
              <CardTitle className="text-sm font-medium flex items-center gap-2">
                <Filter className="h-4 w-4" />
                Content Type
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-2">
              {[
                { id: 'ALL', label: 'All Contributions', icon: Users },
                { id: 'FLAW_REPORT', label: 'Flaw Reports', icon: ShieldAlert },
                { id: 'CODER_BLOCK', label: 'Coder Blocks', icon: Code },
                { id: 'FEATURE_PROPOSAL', label: 'Feature Proposals', icon: Lightbulb },
              ].map((type) => (
                <button
                  key={type.id}
                  onClick={() => setFilter(type.id)}
                  className={cn(
                    'w-full flex items-center gap-3 px-3 py-2 rounded-xl text-sm transition-all duration-200',
                    filter === type.id
                      ? 'bg-primary text-primary-foreground shadow-lg shadow-primary/30 scale-[1.02]'
                      : 'hover:bg-white/10 text-muted-foreground'
                  )}
                >
                  <type.icon className={cn('h-4 w-4', filter === type.id ? 'text-white' : 'text-muted-foreground')} />
                  {type.label}
                </button>
              ))}
            </CardContent>
          </Card>

          <Card variant="neumorphic">
            <CardHeader>
              <CardTitle className="text-sm">Community Stats</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="flex justify-between items-center">
                <span className="text-xs text-muted-foreground">Total Reports</span>
                <span className="text-sm font-bold text-foreground">{reports.length}</span>
              </div>
              <div className="flex justify-between items-center">
                <span className="text-xs text-muted-foreground">Resolved</span>
                <span className="text-sm font-bold text-green-500">
                  {reports.filter(r => r.status === 'RESOLVED').length}
                </span>
              </div>
              <div className="h-px bg-border/50 my-2" />
              <p className="text-[10px] text-muted-foreground italic leading-relaxed">
                Contributing to the hub helps refine the Risk Engine's deterministic rules.
              </p>
            </CardContent>
          </Card>
        </div>

        {/* Reports Feed */}
        <div className="lg:col-span-3 space-y-6">
          <div className="relative group">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground group-focus-within:text-primary transition-colors" />
            <Input
              className="pl-10 bg-white/5 border-white/10 focus:bg-white/10 transition-all rounded-2xl h-12"
              placeholder="Search reports, authors, or components..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
            />
          </div>

          <div className="grid grid-cols-1 gap-4">
            {loading ? (
              Array.from({ length: 3 }).map((_, i) => (
                <div key={i} className="p-6 rounded-2xl bg-white/5 animate-pulse h-40" />
              ))
            ) : filteredReports.length > 0 ? (
              filteredReports.map((report) => (
                <Card key={report.id} variant="glass" className="group hover:border-primary/40 transition-all duration-300">
                  <CardContent className="p-6">
                    <div className="flex items-start justify-between mb-4">
                      <div className="flex items-center gap-3">
                        <div className={cn('p-2 rounded-lg', typeColors[report.type])}>
                          {typeIcons[report.type]}
                        </div>
                        <div>
                          <h3 className="font-bold text-foreground group-hover:text-primary transition-colors">{report.title}</h3>
                          <div className="flex items-center gap-2 mt-1">
                            <span className="text-[10px] text-muted-foreground">by {report.author}</span>
                            <span className="text-muted-foreground">•</span>
                            <span className="text-[10px] text-muted-foreground">{report.createdAt}</span>
                          </div>
                        </div>
                      </div>
                      <Badge variant="outline" className={cn('text-[10px] font-bold border',
                        report.severity === 'CRITICAL' ? 'text-red-500 border-red-500/20 bg-red-500/5' : 'text-muted-foreground'
                      )}>
                        {report.severity}
                      </Badge>
                    </div>
                    <p className="text-sm text-muted-foreground leading-relaxed mb-6">
                      {report.description}
                    </p>
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-2">
                        <Badge variant="outline" className="text-[10px] bg-white/5">
                          {report.component}
                        </Badge>
                        {report.tags.map(tag => (
                          <Badge key={tag} variant="outline" className="text-[10px] bg-white/5 italic">
                            #{tag}
                          </Badge>
                        ))}
                      </div>
                      <div className="flex items-center gap-2">
                        <Button variant="ghost" size="sm" className="h-8 px-2 rounded-full hover:bg-primary/10 text-muted-foreground hover:text-primary transition-all">
                          <ThumbsUp className="h-3 w-3 mr-1" />
                          <span className="text-xs">Upvote</span>
                        </Button>
                        <Button variant="neumorphic" size="sm" className="h-8 px-3 rounded-full text-xs">
                          <MessageSquare className="h-3 w-3 mr-1" />
                          Discuss
                        </Button>
                      </div>
                    </div>
                  </CardContent>
                </Card>
              ))
            ) : (
              <div className="flex flex-col items-center justify-center py-20 text-muted-foreground opacity-40">
                <Users className="h-12 w-12 mb-4" />
                <p>No contributions found matching your criteria</p>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Create Report Modal */}
      {showCreateModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-background/80 backdrop-blur-sm">
          <Card variant="glass" className="w-full max-w-xl border-primary/30 shadow-2xl">
            <CardHeader>
              <CardTitle>Submit Contribution</CardTitle>
              <CardDescription>Share a flaw, a block, or a proposed feature with the community</CardDescription>
            </CardHeader>
            <CardContent className="space-y-6">
              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-2">
                  <label className="text-xs font-medium text-muted-foreground">Report Type</label>
                  <select className="w-full p-2 rounded-xl bg-transparent border border-border neumorphic-inset text-sm focus:ring-2 focus:ring-primary outline-none">
                    <option value="FLAW_REPORT">Flaw Report</option>
                    <option value="CODER_BLOCK">Coder Block</option>
                    <option value="FEATURE_PROPOSAL">Feature Proposal</option>
                  </select>
                </div>
                <div className="space-y-2">
                  <label className="text-xs font-medium text-muted-foreground">Severity</label>
                  <select className="w-full p-2 rounded-xl bg-transparent border border-border neumorphic-inset text-sm focus:ring-2 focus:ring-primary outline-none">
                    <option value="LOW">Low</option>
                    <option value="MEDIUM">Medium</option>
                    <option value="HIGH">High</option>
                    <option value="CRITICAL">Critical</option>
                  </select>
                </div>
              </div>
              <div className="space-y-2">
                <label className="text-xs font-medium text-muted-foreground">Title</label>
                <Input className="neumorphic-inset bg-transparent" placeholder="Concise summary of the issue/idea" />
              </div>
              <div className="space-y-2">
                <label className="text-xs font-medium text-muted-foreground">Component</label>
                <Input className="neumorphic-inset bg-transparent" placeholder="e.g. Payment Engine, Risk API" />
              </div>
              <div className="space-y-2">
                <label className="text-xs font-medium text-muted-foreground">Detailed Description</label>
                <textarea
                  className="w-full p-3 rounded-xl bg-transparent border border-border neumorphic-inset text-sm focus:ring-2 focus:ring-primary outline-none min-h-[120px]"
                  placeholder="Provide technical details, reproduction steps, or design proposals..."
                />
              </div>
              <div className="flex gap-3 pt-4">
                <Button variant="outline" className="flex-1" onClick={() => setShowCreateModal(false)}>
                  Discard
                </Button>
                <Button variant="primary" className="flex-1 gap-2" onClick={() => setShowCreateModal(false)}>
                  <Send className="h-4 w-4" />
                  Post to Hub
                </Button>
              </div>
            </CardContent>
          </Card>
        </div>
      )}
    </div>
  )
}