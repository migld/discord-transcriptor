import { useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'

export default function AuthCallback() {
  const navigate = useNavigate()
  const { login } = useAuth()

  useEffect(() => {
    // JWT arrives in the URL hash: /auth/callback#token=...
    const hash  = window.location.hash.substring(1)
    const params = new URLSearchParams(hash)
    const token  = params.get('token')

    if (token) {
      login(token)
      navigate('/lobby', { replace: true })
    } else {
      navigate('/', { replace: true })
    }
  }, [])

  return <p style={{ color: '#ccc', textAlign: 'center', marginTop: '4rem' }}>Signing in...</p>
}
