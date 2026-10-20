'use client'

import { useEffect, useState } from 'react'
import { Sparkles, Loader2, Info } from 'lucide-react'
import { api } from '@/lib/api'

interface RiskExplainerProps {
  score: number
  decision: string
  rules: string[]
}

/**
 * Fetches an AI-generated explanation for a transaction risk decision.
 */
export function RiskExplainer({ score, decision, rules }: RiskExplainerProps) {
  const [explanation, setExplanation] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let isMounted = true
    const params = new URLSearchParams({
      score: score.toString(),
      decision,
    })
    rules.forEach((r) => params.append('rules', r))

    api
      .get<{ data?: { explanation: string }; explanation?: string }>(`/ai/explain/risk?${params}`)
      .then((res: any) => {
        if (!isMounted) return
        const text = res?.data?.explanation || res?.explanation || null
        setExplanation(text)
      })
      .catch(() => {
        if (!isMounted) return
        setExplanation(
          `Transaction scored ${score}/100 and marked ${decision} based on rules: ${rules.join(', ') || 'threshold evaluation'}.`
        )
      })
      .finally(() => {
        if (isMounted) setLoading(false)
      })

    return () => {
      isMounted = false
    }
  }, [score, decision, rules])

  if (loading) {
    return (
      <div className="flex items-center gap-2 p-3 text-sm text-slate-400 bg-sentinel-card rounded-lg border border-sentinel-border">
        <Loader2 className="h-4 w-4 animate-spin text-primary-400" />
        <span>Gemini is analyzing risk signals...</span>
      </div>
    )
  }

  if (!explanation) return null

  return (
    <div className="flex items-start gap-3 p-4 bg-primary-500/10 border border-primary-500/25 rounded-xl text-sm">
      <div className="p-1.5 bg-primary-500/20 rounded-lg text-primary-400 shrink-0 mt-0.5">
        <Sparkles className="h-4 w-4" />
      </div>
      <div className="space-y-1">
        <p className="font-semibold text-primary-300 text-xs uppercase tracking-wide">
          AI Risk Intelligence
        </p>
        <p className="text-slate-300 leading-relaxed">{explanation}</p>
      </div>
    </div>
  )
}

