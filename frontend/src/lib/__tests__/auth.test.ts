import { act } from '@testing-library/react'
import { useAuthStore } from '@/lib/auth'

describe('useAuthStore', () => {
  beforeEach(() => {
    useAuthStore.setState({ user: null, isAuthenticated: false })
    localStorage.clear()
  })

  it('initializes with unauthenticated state', () => {
    const { user, isAuthenticated } = useAuthStore.getState()
    expect(user).toBeNull()
    expect(isAuthenticated).toBe(false)
  })

  it('sets user and authentication state', () => {
    const mockUser = {
      id: '1',
      email: 'test@example.com',
      fullName: 'Test User',
      role: 'DEVELOPER',
      isActive: true,
      emailVerified: false,
    }

    act(() => {
      useAuthStore.setState({ user: mockUser, isAuthenticated: true })
    })

    const { user, isAuthenticated } = useAuthStore.getState()
    expect(user).toEqual(mockUser)
    expect(isAuthenticated).toBe(true)
  })

  it('persists to localStorage', () => {
    const mockUser = {
      id: '1',
      email: 'test@example.com',
      fullName: 'Test User',
      role: 'DEVELOPER',
      isActive: true,
      emailVerified: false,
    }

    act(() => {
      useAuthStore.setState({ user: mockUser, isAuthenticated: true })
    })

    const stored = localStorage.getItem('auth-storage')
    expect(stored).toBeTruthy()
    const parsed = JSON.parse(stored!)
    expect(parsed.state.user).toEqual(mockUser)
    expect(parsed.state.isAuthenticated).toBe(true)
  })

  it('clears auth on logout', () => {
    const mockUser = {
      id: '1',
      email: 'test@example.com',
      fullName: 'Test User',
      role: 'DEVELOPER',
      isActive: true,
      emailVerified: false,
    }

    act(() => {
      useAuthStore.setState({ user: mockUser, isAuthenticated: true })
    })

    act(() => {
      useAuthStore.getState().logout()
    })

    const { user, isAuthenticated } = useAuthStore.getState()
    expect(user).toBeNull()
    expect(isAuthenticated).toBe(false)
  })
})