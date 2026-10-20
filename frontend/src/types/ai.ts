export interface AiMessage {
  id: string
  role: 'user' | 'assistant'
  content: string
  timestamp: Date
  streaming?: boolean
}

export interface RiskExplanation {
  text: string | null
}

