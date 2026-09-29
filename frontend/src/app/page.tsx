import Link from 'next/link'
import { Shield, Zap, Users, Bug, FlaskConical, BarChart3, Terminal, Settings } from 'lucide-react'

export default function HomePage() {
  const features = [
    {
      icon: Shield,
      title: 'SentinelPay',
      description: 'Simulated payment processing with full transaction lifecycle, idempotency, and state management',
      href: '/payments',
      color: 'bg-blue-500/20 text-blue-400',
    },
    {
      icon: Zap,
      title: 'Risk Engine',
      description: 'Deterministic fraud detection with explainable scoring, rule-based decisions, and ML signals',
      href: '/risk',
      color: 'bg-orange-500/20 text-orange-400',
    },
    {
      icon: FlaskConical,
      title: 'Threat Lab',
      description: 'Controlled security simulations, scenario runner, and reproducible threat modeling',
      href: '/threatlab',
      color: 'bg-purple-500/20 text-purple-400',
    },
    {
      icon: Bug,
      title: 'Dry Run & Reproduction',
      description: 'Reproduce reported issues, propose fixes, and compare before/after behavior safely',
      href: '/dryrun',
      color: 'bg-red-500/20 text-red-400',
    },
    {
      icon: Users,
      title: 'Community Intelligence',
      description: 'Coder Blocks, Flaw Reports, Feature Proposals with collaborative discussion threads',
      href: '/community',
      color: 'bg-green-500/20 text-green-400',
    },
    {
      icon: BarChart3,
      title: 'Community Headlines',
      description: 'Automatic detection of recurring issues with evidence-based community alerts',
      href: '/headlines',
      color: 'bg-cyan-500/20 text-cyan-400',
    },
    {
      icon: Terminal,
      title: 'OpsSentinel',
      description: 'Internal monitoring, incident detection, and AI-assisted root cause analysis',
      href: '/ops',
      color: 'bg-amber-500/20 text-amber-400',
    },
    {
      icon: Settings,
      title: 'Administration',
      description: 'User management, risk rules configuration, and system health monitoring',
      href: '/admin',
      color: 'bg-slate-500/20 text-slate-400',
    },
  ]

  return (
    <main className="min-h-screen">
      {/* Header */}
      <header className="border-b border-sentinel-border">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center h-16">
            <div className="flex items-center gap-2">
              <Shield className="h-8 w-8 text-primary-500" />
              <span className="text-xl font-bold text-white">SentinelX</span>
            </div>
            <nav className="flex items-center gap-6">
              <Link href="/login" className="text-slate-400 hover:text-white transition-colors">
                Sign In
              </Link>
              <Link href="/register" className="px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 transition-colors">
                Get Started
              </Link>
            </nav>
          </div>
        </div>
      </header>

      {/* Hero Section */}
      <section className="relative py-20 lg:py-32 overflow-hidden">
        <div className="absolute inset-0 bg-[radial-gradient(ellipse_at_center,_var(--tw-gradient-stops))] from-primary-500/10 via-transparent to-transparent" />
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 relative">
          <div className="text-center max-w-3xl mx-auto">
            <h1 className="text-4xl sm:text-5xl lg:text-6xl font-bold tracking-tight mb-6">
              Simulated Fintech{' '}
              <span className="text-primary-500">Engineering Platform</span>
            </h1>
            <p className="text-lg sm:text-xl text-slate-300 mb-10 max-w-2xl mx-auto">
              Build, test, and collaborate on payment systems, fraud detection, and security engineering
              in a completely synthetic environment. No real money. No external risks. Pure engineering.
            </p>
            <div className="flex flex-col sm:flex-row items-center justify-center gap-4">
              <Link href="/register" className="w-full sm:w-auto px-8 py-3 bg-primary-600 text-white rounded-lg font-medium hover:bg-primary-700 transition-colors">
                Start Building
              </Link>
              <Link href="/docs" className="w-full sm:w-auto px-8 py-3 border border-sentinel-border text-slate-300 rounded-lg font-medium hover:bg-sentinel-card hover:text-white transition-colors">
                View Documentation
              </Link>
            </div>
          </div>

          {/* Stats */}
          <div className="mt-20 grid grid-cols-2 md:grid-cols-4 gap-6">
            {[
              { label: 'Transactions Processed', value: '10K+' },
              { label: 'Risk Rules Active', value: '25+' },
              { label: 'Threat Scenarios', value: '15+' },
              { label: 'Community Reports', value: '100+' },
            ].map((stat, i) => (
              <div key={i} className="text-center p-6 bg-sentinel-card/50 border border-sentinel-border rounded-xl">
                <div className="text-3xl sm:text-4xl font-bold text-primary-500">{stat.value}</div>
                <div className="text-sm text-slate-400 mt-1">{stat.label}</div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Core Loop */}
      <section className="py-20 bg-sentinel-dark/50 border-y border-sentinel-border">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <h2 className="text-3xl font-bold text-center mb-12">The Core Engineering Loop</h2>
          <div className="relative">
            <div className="absolute top-10 left-0 right-0 h-0.5 bg-gradient-to-r from-transparent via-primary-500 to-transparent" />
            <div className="flex flex-col md:flex-row items-center justify-center gap-4 relative z-10">
              {[
                { label: 'Simulate', icon: FlaskConical, desc: 'Create synthetic payments & threats' },
                { label: 'Detect', icon: Shield, desc: 'Risk engine evaluates every transaction' },
                { label: 'Report', icon: Bug, desc: 'Engineers file flaw reports & proposals' },
                { label: 'Discuss', icon: Users, desc: 'Community collaborates on solutions' },
                { label: 'Fix', icon: Zap, desc: 'Propose & validate fixes via dry-run' },
                { label: 'Learn', icon: BarChart3, desc: 'Patterns become community headlines' },
              ].map((step, i) => (
                <div key={i} className="flex flex-col items-center">
                  <div className={`w-20 h-20 rounded-full flex items-center justify-center ${step.color} mb-3`}>
                    <step.icon className="h-10 w-10" />
                  </div>
                  <span className="font-semibold text-white">{step.label}</span>
                  <span className="text-xs text-slate-400 text-center max-w-xs mt-1">{step.desc}</span>
                  {i < 5 && (
                    <div className="hidden md:block w-16 h-0.5 bg-sentinel-border mt-10" />
                  )}
                </div>
              ))}
            </div>
          </div>
        </div>
      </section>

      {/* Features Grid */}
      <section className="py-20">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <h2 className="text-3xl font-bold text-center mb-12">Platform Capabilities</h2>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
            {features.map((feature) => (
              <Link
                key={feature.title}
                href={feature.href}
                className="group p-6 bg-sentinel-card border border-sentinel-border rounded-xl hover:border-primary-500/50 transition-all duration-300"
              >
                <div className={`w-12 h-12 rounded-lg flex items-center justify-center ${feature.color} mb-4 group-hover:scale-110 transition-transform`}>
                  <feature.icon className="h-6 w-6" />
                </div>
                <h3 className="text-lg font-semibold text-white mb-2 group-hover:text-primary-400 transition-colors">
                  {feature.title}
                </h3>
                <p className="text-sm text-slate-400">{feature.description}</p>
              </Link>
            ))}
          </div>
        </div>
      </section>

      {/* Tech Stack */}
      <section className="py-20 bg-sentinel-dark/50 border-y border-sentinel-border">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <h2 className="text-3xl font-bold text-center mb-12">Technology Stack</h2>
          <div className="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-8 gap-6">
            {[
              { name: 'Next.js 14', category: 'Frontend' },
              { name: 'TypeScript', category: 'Frontend' },
              { name: 'Tailwind CSS', category: 'Frontend' },
              { name: 'React 18', category: 'Frontend' },
              { name: 'Spring Boot 3', category: 'Backend' },
              { name: 'Java 21', category: 'Backend' },
              { name: 'PostgreSQL 16', category: 'Database' },
              { name: 'Redis 7', category: 'Cache' },
            ].map((tech, i) => (
              <div key={i} className="p-4 bg-sentinel-card border border-sentinel-border rounded-lg text-center">
                <div className="font-medium text-white">{tech.name}</div>
                <div className="text-xs text-slate-500">{tech.category}</div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className="border-t border-sentinel-border py-12">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
          <div className="flex items-center justify-center gap-2 mb-4">
            <Shield className="h-6 w-6 text-primary-500" />
            <span className="text-lg font-bold text-white">SentinelX</span>
          </div>
          <p className="text-slate-500 text-sm">
            A portfolio project demonstrating full-stack fintech engineering capabilities.
            Built with Next.js, Spring Boot, PostgreSQL, and modern cloud technologies.
          </p>
          <div className="mt-6 flex justify-center gap-6 text-sm text-slate-500">
            <a href="https://github.com" target="_blank" rel="noopener noreferrer" className="hover:text-primary-400 transition-colors">
              GitHub
            </a>
            <a href="/docs" className="hover:text-primary-400 transition-colors">
              Documentation
            </a>
            <a href="/api-docs" className="hover:text-primary-400 transition-colors">
              API Reference
            </a>
          </div>
        </div>
      </footer>
    </main>
  )
}