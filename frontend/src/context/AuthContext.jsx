import { createContext, useContext, useState, useEffect } from 'react'
import { tokenStore } from '../services/token'
import { authService } from '../services/authService'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [loading, setLoading] = useState(true)

  // Sayfa yenilendiğinde refresh cookie varsa oturumu geri yükle
  useEffect(() => {
    authService.refresh()
      .then((data) => {
        tokenStore.set(data.accessToken)
        return authService.getMe()
      })
      .then(setUser)
      .catch(() => {}) // cookie yoksa sessizce geç
      .finally(() => setLoading(false))
  }, [])

  const login = async (email, password) => {
    const data = await authService.login(email, password)
    tokenStore.set(data.accessToken)
    const me = await authService.getMe()
    setUser(me)
    return me
  }

  const logout = async () => {
    await authService.logout()
    tokenStore.clear()
    setUser(null)
  }

  return (
    <AuthContext.Provider value={{ user, loading, login, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  return useContext(AuthContext)
}
