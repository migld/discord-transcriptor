import { useState, useEffect } from 'react'

export function useAuth() {
  const [token, setToken] = useState(() => localStorage.getItem('jwt'))

  const login = (jwt) => {
    localStorage.setItem('jwt', jwt)
    setToken(jwt)
  }

  const logout = () => {
    localStorage.removeItem('jwt')
    setToken(null)
  }

  return { token, login, logout }
}
