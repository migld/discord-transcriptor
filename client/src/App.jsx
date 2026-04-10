import { Routes, Route, Navigate } from 'react-router-dom'
import Login from './pages/Login'
import AuthCallback from './pages/AuthCallback'
import Lobby from './pages/Lobby'
import Game from './pages/Game'
import Accuse from './pages/Accuse'
import Reveal from './pages/Reveal'
import { useAuth } from './hooks/useAuth'

function PrivateRoute({ children }) {
  const { token } = useAuth()
  return token ? children : <Navigate to="/" replace />
}

export default function App() {
  return (
    <Routes>
      <Route path="/"                  element={<Login />} />
      <Route path="/auth/callback"     element={<AuthCallback />} />
      <Route path="/lobby"             element={<PrivateRoute><Lobby /></PrivateRoute>} />
      <Route path="/game/:id"          element={<PrivateRoute><Game /></PrivateRoute>} />
      <Route path="/game/:id/accuse"   element={<PrivateRoute><Accuse /></PrivateRoute>} />
      <Route path="/game/:id/reveal"   element={<PrivateRoute><Reveal /></PrivateRoute>} />
    </Routes>
  )
}
