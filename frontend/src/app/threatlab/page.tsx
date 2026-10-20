'use client'

import { useState, useEffect } from 'react'
import { FlaskConical, Play, Pause, RotateCcw, AlertTriangle, Activity, Terminal, Target, CheckCircle2, XCircle, Search } from 'lucide-react'
import { Card, CardHeader, CardTitle, CardContent, CardDescription } from '@/components/ui/Card'
import { Button } from '@/components/ui/Button'
import { Input } from '@/components/ui/Input'
import { Badge } from '@/components/ui/Badge'
import { cn } from '@/lib/utils'
import { api } from '@/lib/api'

interface Scenario {
  id: string
  name: string
  description: string
  severity: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'
  category: string
}

interface SimulationLog {
  timestamp: string
  event: string
  level: 'INFO' | 'WARN' | 'ERROR' | 'SUCCESS'
}

export default function ThreatLabPage() {
  const [scenarios, setScenarios] = useState<Scenario[]>([])
  const [loading, setLoading] = useState(true)
  const [activeSimulation, setActiveSimulation] = useState<string | null>(null)
  const [logs, setLogs] = useState<SimulationLog[]>([])
  const [searchQuery, setSearchQuery] = useState('')

  useEffect(() => {
    fetchScenarios()
  }, [])

  const fetchScenarios = async () => {
    setLoading(true)
    try {
      const res = await api.get('/threatlab/scenarios')
      setScenarios(res.data || [])
    } catch (error) {
      console.error('Failed to fetch scenarios:', error)
    } finally {
      setLoading(false)
    }
  }

  const runSimulation = async (scenarioId: string) => {
    setActiveSimulation(scenarioId)
    setLogs([])

    // Simulation sequence mock (to be replaced by SSE/WebSocket if available)
    const mockEvents = [
      { timestamp: new Date().toISOString(), event: 'Initializing attack vector...', level: 'INFO' as const },
      { timestamp: new Date().toISOString(), event: 'Bypassing initial idempotency check...', level: 'INFO' as const },
      { timestamp: new Date().toISOString(), event: 'Triggering velocity threshold breach...', level: 'WARN' as const },
      { timestamp: new Date().toISOString(), event: 'Risk Engine evaluating score impact...', level: 'INFO' as const },
      { timestamp: new Date().toISOString(), event: 'Simulation result: BLOCKED by Rule-X42', level: 'SUCCESS' as const },
    ]

    for (const event of mockEvents) {
      await new Promise(resolve => setTimeout(resolve, 1200))
      setLogs(prev => [...prev, event])
    }
    setActiveSimulation(null)
  }

  const filteredScenarios = scenarios.filter(s =>
    s.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
    s.description.toLowerCase().includes(searchQuery.toLowerCase())
  )

  const severityColors = {
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
              <FlaskConical className="h-6 w-6" />
            </div>
            <h1 className="text-3xl font-bold tracking-tight text-foreground">Threat Lab</h1>
          </div>
          <p className="text-muted-foreground">Simulate fraud patterns and stress-test the risk engine</p>
        </div>

        <div className="flex gap-3">
          <Button variant="outline" onClick={fetchScenarios} className="gap-2">
            <RotateCcw className="h-4 w-4" />
            Sync Scenarios
          </Button>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
        {/* Scenario Selection - Neumorphic List */}
        <div className="lg:col-span-4 space-y-6">
          <Card variant="glass" className="border-primary/20">
            <CardHeader>
              <CardTitle className="text-foreground flex items-center gap-2">
                <Target className="h-4 w-4" />
                Attack Scenarios
              </CardTitle>
              <div className="relative mt-2">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                <Input
                  className="pl-9 bg-white/5 border-white/10 focus:bg-white/10 transition-all"
                  placeholder="Search simulations..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                />
              </div>
            </CardHeader>
            <CardContent className="p-0 max-h-[600px] overflow-y-auto scrollbar-hide">
              <div className="divide-y divide-border/50">
                {loading ? (
                  Array.from({ length: 5 }).map((_, i) => (
                    <div key={i} className="p-4 animate-pulse bg-white/5 h-24" />
                  ))
                ) : filteredScenarios.length > 0 ? (
                  filteredScenarios.map((s) => (
                    <div
                      key={s.id}
                      className={cn(
                        'p-4 transition-all duration-200 cursor-pointer flex items-center justify-between group',
                        activeSimulation === s.id ? 'bg-primary/20' : 'hover:bg-white/5'
                      )}
                      onClick={() => runSimulation(s.id)}
                    >
                      <div className="space-y-1">
                        <div className="flex items-center gap-2">
                          <span className="text-sm font-semibold text-foreground">{s.name}</span>
                          <Badge variant="outline" className={cn('text-[10px] border', severityColors[s.severity])}>
                            {s.severity}
                          </Badge>
                        </div>
                        <p className="text-xs text-muted-foreground line-clamp-1">{s.description}</p>
                      </div>
                      <Button
                        variant="neumorphic"
                        size="icon"
                        className={cn(
                          'h-8 w-8 transition-all',
                          activeSimulation === s.id ? 'bg-primary text-white scale-90' : 'opacity-0 group-hover:opacity-100'
                        )}
                      >
                        {activeSimulation === s.id ? <Pause className="h-4 w-4" /> : <Play className="h-4 w-4" />}
                      </Button>
                    </div>
                  ))
                ) : (
                  <div className="p-8 text-center text-muted-foreground">
                    <AlertTriangle className="h-8 w-8 mx-auto opacity-20 mb-2" />
                    <p className="text-sm">No scenarios found</p>
                  </div>
                )}
              </div>
            </CardContent>
          </Card>

          <Card variant="neumorphic">
            <CardHeader>
              <CardTitle className="text-sm flex items-center gap-2">
                <Info className="h-4 w-4" />
                Simulation Guide
              </CardTitle>
            </CardHeader>
            <CardContent className="text-xs text-muted-foreground space-y-3">
              <p>Simulations bypass the frontend UI and hit the Risk Engine directly to test atomic rule failures.</p>
              <p>Results are logged in real-time via the telemetry stream.</p>
            </CardContent>
          </Card>
        </div>

        {/* Simulation Console - The "Main Event" */}
        <div className="lg:col-span-8 space-y-6">
          <Card variant="glass" className="border-primary/20 h-full flex flex-col min-h-[600px]">
            <CardHeader className="flex flex-row items-center justify-between border-b border-white/10">
              <div className="flex items-center gap-3">
                <div className="p-2 bg-primary/20 rounded-xl text-primary">
                  <Terminal className="h-5 w-5" />
                </div>
                <div>
                  <CardTitle className="text-foreground">Execution Console</CardTitle>
                  <CardDescription className="text-muted-foreground">Real-time simulation telemetry</CardDescription>
                </div>
              </div>
              {activeSimulation && (
                <div className="flex items-center gap-3">
                  <div className="flex items-center gap-2 px-3 py-1 rounded-full bg-primary/20 border border-primary/30">
                    <div className="h-2 w-2 rounded-full bg-primary animate-ping" />
                    <span className="text-xs font-bold text-primary uppercase">Executing...</span>
                  </div>
                  <Button variant="ghost" size="sm" onClick={() => setActiveSimulation(null)} className="text-red-400 hover:text-red-500">
                    Abort
                  </Button>
                </div>
              )}
            </CardHeader>
            <CardContent className="flex-1 p-0 bg-slate-950/50 font-mono text-sm relative overflow-hidden">
              {/* Background Grid Effect */}
              <div className="absolute inset-0 opacity-10 pointer-events-none"
                   style={{ backgroundImage: 'radial-gradient(circle, #3b82f6 1px, transparent 1px)', backgroundSize: '24px 24px' }} />

              <div className="relative z-10 p-6 space-y-2 h-full overflow-y-auto max-h-[500px] scrollbar-hide">
                {!activeSimulation && logs.length === 0 && (
                  <div className="h-full flex flex-col items-center justify-center text-muted-foreground opacity-40 space-y-4 py-20">
                    <Activity className="h-12 w-12 animate-pulse" />
                    <p className="text-center max-w-xs">Select a scenario from the left to begin the threat simulation pipeline</p>
                  </div>
                )}

                {logs.map((log, i) => (
                  <div key={i} className="flex items-start gap-3 animate-in fade-in slide-in-from-left-2 duration-300">
                    <span className="text-slate-600 shrink-0">[{log.timestamp.slice(11, 19)}]</span>
                    <div className={cn(
                      'flex items-center gap-2 px-2 py-0.5 rounded text-xs font-medium',
                      log.level === 'INFO' && 'text-blue-400 bg-blue-400/10',
                      log.level === 'WARN' && 'text-amber-400 bg-amber-400/10',
                      log.level === 'ERROR' && 'text-red-400 bg-red-400/10',
                      log.level === 'SUCCESS' && 'text-green-400 bg-green-400/10'
                    )}>
                      {log.level === 'SUCCESS' && <CheckCircle2 className="h-3 w-3" />}
                      {log.level === 'WARN' && <AlertTriangle className="h-3 w-3" />}
                      {log.level === 'ERROR' && <XCircle className="h-3 w-3" />}
                      {log.level}
                    </div>
                    <span className="text-slate-300">{log.event}</span>
                  </div>
                ))}
                {activeSimulation && (
                  <div className="flex items-center gap-2 text-primary animate-pulse">
                    <span className="text-slate-600">[{new Date().toISOString().slice(11, 19)}]</span>
                    <span className="text-xs font-bold">SENS_CORE:</span>
                    <span className="text-xs"> Processing payload...</span>
                    <span className="w-2 h-4 bg-primary animate-bounce" />
                  </div>
                )}
              </div>
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  )
}