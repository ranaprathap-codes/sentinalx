'use client'

import { AiChatPanel } from '@/components/ai/AiChatPanel'
import { Card, CardContent } from '@/components/ui/Card'
import { ShieldCheck, Cpu, Database, Activity } from 'lucide-react'

export default function AiAssistantPage() {
  return (
    <div className="space-y-6">
      {/* Page Title */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2">
        <div>
          <h1 className="text-2xl font-bold text-white">AI Copilot</h1>
          <p className="text-slate-400">
            Interactive security & fraud reasoning powered by Google Gemini (gemini-3.8-flash)
          </p>
        </div>
      </div>

      {/* Feature Pills */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <Card className="bg-sentinel-card/60">
          <CardContent className="pt-4 pb-4 flex items-center gap-3">
            <div className="p-2 rounded-lg bg-blue-500/20 text-blue-400">
              <ShieldCheck className="h-5 w-5" />
            </div>
            <div>
              <p className="text-sm font-semibold text-white">Explainable Decisions</p>
              <p className="text-xs text-slate-400">Plain-English reasoning for all blocked & reviewed transactions</p>
            </div>
          </CardContent>
        </Card>

        <Card className="bg-sentinel-card/60">
          <CardContent className="pt-4 pb-4 flex items-center gap-3">
            <div className="p-2 rounded-lg bg-purple-500/20 text-purple-400">
              <Cpu className="h-5 w-5" />
            </div>
            <div>
              <p className="text-sm font-semibold text-white">Live Streaming SSE</p>
              <p className="text-xs text-slate-400">Low-latency token streaming from Gemini models</p>
            </div>
          </CardContent>
        </Card>

        <Card className="bg-sentinel-card/60">
          <CardContent className="pt-4 pb-4 flex items-center gap-3">
            <div className="p-2 rounded-lg bg-green-500/20 text-green-400">
              <Activity className="h-5 w-5" />
            </div>
            <div>
              <p className="text-sm font-semibold text-white">Incident & Rule Advice</p>
              <p className="text-xs text-slate-400">Actionable recommendations for rules and telemetry</p>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Copilot Chat Window */}
      <div className="h-[650px]">
        <AiChatPanel />
      </div>
    </div>
  )
}

