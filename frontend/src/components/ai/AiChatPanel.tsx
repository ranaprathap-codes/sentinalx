'use client'

import { useState, useRef, useEffect } from 'react'
import { Send, Bot, User, Loader2, Sparkles, ShieldAlert, Cpu } from 'lucide-react'
import { Button } from '@/components/ui/Button'
import { Input } from '@/components/ui/Input'
import { api } from '@/lib/api'

interface Message {
  id: string
  role: 'user' | 'assistant'
  content: string
  timestamp: Date
  streaming?: boolean
}

/**
 * AI Chat Panel powered by Google Gemini (gemini-3.8-flash).
 * Streams responses token-by-token from /api/ai/ask via SSE.
 */
export function AiChatPanel() {
  const [messages, setMessages] = useState<Message[]>([
    {
      id: '0',
      role: 'assistant',
      content: 'Hello! I am SentinelX AI Copilot, powered by Google Gemini. Ask me about your transaction security, fraud rules, system incidents, or architecture suggestions.',
      timestamp: new Date(),
    },
  ])
  const [input, setInput] = useState('')
  const [loading, setLoading] = useState(false)
  const bottomRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages])

  const sendMessage = async () => {
    if (!input.trim() || loading) return
    const question = input.trim()
    setInput('')
    const userMsg: Message = {
      id: Date.now().toString(),
      role: 'user',
      content: question,
      timestamp: new Date(),
    }
    const assistantMsgId = (Date.now() + 1).toString()
    const assistantMsg: Message = {
      id: assistantMsgId,
      role: 'assistant',
      content: '',
      timestamp: new Date(),
      streaming: true,
    }
    setMessages((prev) => [...prev, userMsg, assistantMsg])
    setLoading(true)

    try {
      const token = api.getAccessToken()
      const apiUrl = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080'
      const response = await fetch(`${apiUrl}/api/ai/ask`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          ...(token ? { Authorization: `Bearer ${token}` } : {}),
        },
        body: JSON.stringify({ question }),
        credentials: 'include',
      })

      if (!response.ok || !response.body) {
        throw new Error('Stream failed')
      }

      const reader = response.body.getReader()
      const decoder = new TextDecoder()
      let accumulated = ''

      while (true) {
        const { done, value } = await reader.read()
        if (done) break
        const chunk = decoder.decode(value, { stream: true })
        const lines = chunk.split('\n')
        for (const line of lines) {
          if (line.startsWith('data: ')) {
            const tokenText = line.slice(6)
            accumulated += tokenText
            setMessages((prev) =>
              prev.map((m) =>
                m.id === assistantMsgId ? { ...m, content: accumulated, streaming: true } : m
              )
            )
          }
        }
      }

      setMessages((prev) =>
        prev.map((m) =>
          m.id === assistantMsgId ? { ...m, streaming: false } : m
        )
      )
    } catch {
      setMessages((prev) =>
        prev.map((m) =>
          m.id === assistantMsgId
            ? {
                ...m,
                content:
                  'SentinelX AI Copilot is operating in offline fallback mode. Ensure the backend is running with GEMINI_API_KEY configured for full real-time intelligence.',
                streaming: false,
              }
            : m
        )
      )
    } finally {
      setLoading(false)
    }
  }

  const promptSuggestions = [
    'How does SentinelX evaluate high velocity payment risk?',
    'Explain the difference between RISK_REVIEW and BLOCKED.',
    'Recommend 3 ways to enhance our fraud rules engine.',
  ]

  return (
    <div className="flex flex-col h-full bg-sentinel-card border border-sentinel-border rounded-xl overflow-hidden shadow-xl">
      {/* Header */}
      <div className="flex items-center justify-between px-5 py-4 border-b border-sentinel-border bg-sentinel-card/90 backdrop-blur-sm">
        <div className="flex items-center gap-3">
          <div className="p-2 bg-primary-500/20 rounded-lg">
            <Sparkles className="h-5 w-5 text-primary-400" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <p className="text-base font-semibold text-white">SentinelX AI Copilot</p>
              <span className="text-[10px] font-mono uppercase bg-primary-500/20 text-primary-300 px-1.5 py-0.5 rounded">
                Gemini 3.8 Flash
              </span>
            </div>
            <p className="text-xs text-slate-400">Real-time security reasoning & risk explanations</p>
          </div>
        </div>
        <div className="flex items-center gap-2 text-xs text-slate-400">
          <Cpu className="h-3.5 w-3.5 text-green-400" />
          <span>Active</span>
        </div>
      </div>

      {/* Messages Scroll Area */}
      <div className="flex-1 overflow-y-auto p-5 space-y-4">
        {messages.map((msg) => (
          <div
            key={msg.id}
            className={`flex gap-3 ${
              msg.role === 'user' ? 'flex-row-reverse' : 'flex-row'
            }`}
          >
            <div
              className={`shrink-0 p-2.5 rounded-full ${
                msg.role === 'user'
                  ? 'bg-primary-500/20 text-primary-400'
                  : 'bg-slate-700/60 text-slate-200'
              }`}
            >
              {msg.role === 'user' ? <User className="h-4 w-4" /> : <Bot className="h-4 w-4" />}
            </div>
            <div
              className={`max-w-[80%] px-4 py-3 rounded-2xl text-sm leading-relaxed ${
                msg.role === 'user'
                  ? 'bg-primary-600/25 border border-primary-500/30 text-white rounded-tr-sm'
                  : 'bg-slate-800/80 border border-slate-700/50 text-slate-200 rounded-tl-sm'
              }`}
            >
              <div className="whitespace-pre-wrap">{msg.content}</div>
              {msg.streaming && (
                <span className="inline-block w-1.5 h-4 ml-1 bg-primary-400 animate-pulse rounded-sm align-middle" />
              )}
            </div>
          </div>
        ))}
        <div ref={bottomRef} />
      </div>

      {/* Suggestion Chips */}
      {messages.length === 1 && (
        <div className="px-5 pb-2">
          <p className="text-xs text-slate-400 mb-2 flex items-center gap-1">
            <ShieldAlert className="h-3 w-3" /> Try asking:
          </p>
          <div className="flex flex-wrap gap-2">
            {promptSuggestions.map((text, idx) => (
              <button
                key={idx}
                onClick={() => {
                  setInput(text)
                }}
                className="text-xs px-3 py-1.5 rounded-lg bg-slate-800/80 hover:bg-slate-700/80 text-slate-300 border border-slate-700/60 transition-colors text-left"
              >
                {text}
              </button>
            ))}
          </div>
        </div>
      )}

      {/* Input Bar */}
      <div className="p-4 border-t border-sentinel-border bg-sentinel-card">
        <div className="flex gap-2">
          <Input
            placeholder="Ask about risk decisions, threats, payments, or architecture..."
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && !e.shiftKey && sendMessage()}
            className="flex-1"
            disabled={loading}
          />
          <Button
            onClick={sendMessage}
            disabled={loading || !input.trim()}
            className="gap-2 shrink-0"
          >
            {loading ? (
              <Loader2 className="h-4 w-4 animate-spin" />
            ) : (
              <>
                <span>Send</span>
                <Send className="h-4 w-4" />
              </>
            )}
          </Button>
        </div>
      </div>
    </div>
  )
}

