import type { Metadata } from 'next'
import { Inter } from 'next/font/google'
import './globals.css'
import { Providers } from './providers'

const inter = Inter({ subsets: ['latin'], variable: '--font-inter' })

export const metadata: Metadata = {
  title: 'SentinelX - Simulated Fintech Engineering Platform',
  description: 'A simulated fintech platform for payment processing, fraud detection, and engineering collaboration',
}

export default function RootLayout({
  children,
}: {
  children: React.ReactNode
}) {
  return (
    <html lang="en" className={`${inter.variable} font-sans antialiased`}>
      <body className="min-h-screen bg-sentinel-darker text-white">
        <Providers>{children}</Providers>
      </body>
    </html>
  )
}