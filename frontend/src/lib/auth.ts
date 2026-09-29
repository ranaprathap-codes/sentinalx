import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import { api } from './api'

export interface User {
  id: string
  email: string
  fullName: string | null
  role: string
  isActive: boolean
  emailVerified: boolean
}

interface AuthState {
  user: User | null
  isAuthenticated: boolean
  isLoading: boolean
  login: (email: string, password: string) => Promise<void>
  register: (email: string, password: string, fullName?: string) => Promise<void>
  logout: () => void
  refreshUser: () => Promise<void>
  setUser: (user: User | null) => void
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      user: null,
      isAuthenticated: false,
      isLoading: false,

      login: async (email: string, password: string) => {
        set({ isLoading: true })
        try {
          const response = await api.post<{ data: { accessToken: string; refreshToken: string; user: User } }>('/auth/login', {
            email,
            password,
          })
          
          api.setTokens(response.data.accessToken, response.data.refreshToken)
          set({ user: response.data.user, isAuthenticated: true, isLoading: false })
        } catch (error) {
          set({ isLoading: false })
          throw error
        }
      },

      register: async (email: string, password: string, fullName?: string) => {
        set({ isLoading: true })
        try {
          const response = await api.post<{ data: { accessToken: string; refreshToken: string; user: User } }>('/auth/register', {
            email,
            password,
            fullName,
          })
          
          api.setTokens(response.data.accessToken, response.data.refreshToken)
          set({ user: response.data.user, isAuthenticated: true, isLoading: false })
        } catch (error) {
          set({ isLoading: false })
          throw error
        }
      },

      logout: () => {
        api.clearTokens()
        set({ user: null, isAuthenticated: false })
      },

      refreshUser: async () => {
        const token = api.getAccessToken()
        if (!token) {
          set({ user: null, isAuthenticated: false })
          return
        }

        try {
          const response = await api.get<{ data: User }>('/auth/me')
          set({ user: response.data, isAuthenticated: true })
        } catch {
          api.clearTokens()
          set({ user: null, isAuthenticated: false })
        }
      },

      setUser: (user: User | null) => {
        set({ user, isAuthenticated: !!user })
      },
    }),
    {
      name: 'auth-storage',
      partialize: (state) => ({ user: state.user, isAuthenticated: state.isAuthenticated }),
    }
  )
)