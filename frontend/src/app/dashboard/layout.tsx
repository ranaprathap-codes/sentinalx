'use client'

import { useEffect } from 'react'
import Link from 'next/link'
import { usePathname } from 'next/navigation'
import { Shield, Zap, FlaskConical, Bug, Users, BarChart3, Terminal, Settings, LogOut, Menu, X, ChevronRight } from 'lucide-react'
import { useAuthStore } from '@/lib/auth'
import { Button } from '@/components/ui/Button'
import { cn } from '@/lib/utils'

const navigation = [
  { name: 'Dashboard', href: '/dashboard', icon: Shield },
  { name: 'Payments', href: '/payments', icon: Zap },
  { name: 'Risk Engine', href: '/risk', icon: Shield },
  { name: 'Threat Lab', href: '/threatlab', icon: FlaskConical },
  { name: 'Dry Run', href: '/dryrun', icon: Bug },
  { name: 'Community', href: '/community', icon: Users },
  { name: 'Headlines', href: '/headlines', icon: BarChart3 },
  { name: 'OpsSentinel', href: '/ops', icon: Terminal },
  { name: 'Admin', href: '/admin', icon: Settings, adminOnly: true },
]

export default function DashboardLayout({
  children,
}: {
  children: React.ReactNode
}) {
  const { user, logout, isAuthenticated } = useAuthStore()
  const pathname = usePathname()
  const [sidebarOpen, setSidebarOpen] = useState(false)
  const [userMenuOpen, setUserMenuOpen] = useState(false)

  useEffect(() => {
    if (isAuthenticated) {
      // User is already loaded from persist
    }
  }, [isAuthenticated])

  return (
    <div className="min-h-screen bg-sentinel-darker">
      {/* Mobile sidebar backdrop */}
      {sidebarOpen && (
        <div
          className="fixed inset-0 bg-black/50 z-40 lg:hidden"
          onClick={() => setSidebarOpen(false)}
          aria-hidden="true"
        />
      )}

      {/* Sidebar */}
      <aside
        className={cn(
          'fixed inset-y-0 left-0 z-50 w-64 bg-sentinel-card border-r border-sentinel-border transform transition-transform duration-300 lg:translate-x-0',
          sidebarOpen ? 'translate-x-0' : '-translate-x-full'
        )}
        aria-label="Main navigation"
      >
        <div className="flex flex-col h-full">
          {/* Logo */}
          <div className="flex items-center gap-2 h-16 px-4 border-b border-sentinel-border">
            <Shield className="h-8 w-8 text-primary-500" />
            <span className="text-xl font-bold text-white">SentinelX</span>
          </div>

          {/* Navigation */}
          <nav className="flex-1 px-3 py-4 space-y-1 overflow-y-auto" role="navigation" aria-label="Main">
            {navigation
              .filter((item) => !item.adminOnly || user?.role === 'ADMIN')
              .map((item) => {
                const isActive = pathname === item.href || pathname.startsWith(item.href + '/')
                return (
                  <Link
                    key={item.name}
                    href={item.href}
                    className={cn(
                      'flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors',
                      isActive
                        ? 'bg-primary-500/20 text-primary-400'
                        : 'text-slate-400 hover:bg-sentinel-border hover:text-white'
                    )}
                    aria-current={isActive ? 'page' : undefined}
                  >
                    <item.icon className="h-5 w-5 flex-shrink-0" />
                    {item.name}
                  </Link>
                )
              })}
          </nav>

          {/* User section */}
          <div className="p-3 border-t border-sentinel-border">
            <div className="flex items-center gap-3 px-3 py-2">
              <div className="w-8 h-8 rounded-full bg-primary-500/20 flex items-center justify-center">
                <span className="text-sm font-medium text-primary-400">
                  {user?.email?.charAt(0).toUpperCase() || 'U'}
                </span>
              </div>
              <div className="flex-1 min-w-0">
                <p className="text-sm font-medium text-white truncate">{user?.fullName || user?.email}</p>
                <p className="text-xs text-slate-500 capitalize">{user?.role?.toLowerCase()}</p>
              </div>
            </div>
          </div>
        </div>
      </aside>

      {/* Main content */}
      <div className="lg:pl-64">
        {/* Top bar */}
        <header className="sticky top-0 z-30 h-16 bg-sentinel-card/80 backdrop-blur-sm border-b border-sentinel-border">
          <div className="flex items-center justify-between h-full px-4 sm:px-6">
            <div className="flex items-center gap-4">
              <Button
                variant="ghost"
                size="icon"
                className="lg:hidden"
                onClick={() => setSidebarOpen(true)}
                aria-label="Open navigation menu"
              >
                <Menu className="h-5 w-5" />
              </Button>
              <h1 className="text-lg font-semibold text-white hidden sm:block">
                {navigation.find((n) => pathname === n.href || pathname.startsWith(n.href + '/'))?.name || 'Dashboard'}
              </h1>
            </div>

            <div className="flex items-center gap-3">
              {/* User menu */}
              <div className="relative">
                <Button
                  variant="ghost"
                  size="icon"
                  onClick={() => setUserMenuOpen(!userMenuOpen)}
                  aria-expanded={userMenuOpen}
                  aria-label="User menu"
                >
                  <div className="w-8 h-8 rounded-full bg-primary-500/20 flex items-center justify-center">
                    <span className="text-sm font-medium text-primary-400">
                      {user?.email?.charAt(0).toUpperCase() || 'U'}
                    </span>
                  </div>
                </Button>

                {userMenuOpen && (
                  <>
                    <div className="fixed inset-0 z-40" onClick={() => setUserMenuOpen(false)} />
                    <div className="absolute right-0 top-full mt-2 w-48 bg-sentinel-card border border-sentinel-border rounded-lg shadow-lg py-1 z-50 animate-fade-in">
                      <div className="px-3 py-2 border-b border-sentinel-border">
                        <p className="text-sm font-medium text-white truncate">{user?.fullName || user?.email}</p>
                        <p className="text-xs text-slate-500 capitalize">{user?.role?.toLowerCase()}</p>
                      </div>
                      <button
                        onClick={() => { logout(); setUserMenuOpen(false); }}
                        className="w-full flex items-center gap-2 px-3 py-2 text-sm text-red-400 hover:bg-sentinel-border hover:text-red-300 transition-colors"
                      >
                        <LogOut className="h-4 w-4" />
                        Sign Out
                      </button>
                    </div>
                  </>
                )}
              </div>
            </div>
          </div>
        </header>

        {/* Page content */}
        <main className="p-4 sm:p-6 lg:p-8">
          {children}
        </main>
      </div>
    </div>
  )
}

import { useState } from 'react'