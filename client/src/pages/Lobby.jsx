import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { startGame } from '../api'
import styles from './Lobby.module.css'

export default function Lobby() {
  const navigate  = useNavigate()
  const [loading, setLoading] = useState(false)
  const [error,   setError]   = useState(null)

  const handleNewGame = async () => {
    setLoading(true)
    setError(null)
    try {
      const { data } = await startGame()
      navigate(`/game/${data.id}`)
    } catch (err) {
      setError('Failed to start game. Make sure NPCs have been seeded.')
      setLoading(false)
    }
  }

  return (
    <div className={styles.container}>
      <header className={styles.header}>
        <h1 className={styles.title}>The Case Files</h1>
        <p className={styles.tagline}>Detective Bureau — Active Cases</p>
      </header>

      <main className={styles.main}>
        <div className={styles.caseBoard}>
          <h2 className={styles.sectionTitle}>Open a New Case</h2>
          <p className={styles.description}>
            A fresh crime scene awaits. The suspects have been assembled.
            Someone in your circle is a killer — find out who.
          </p>
          {error && <p className={styles.error}>{error}</p>}
          <button
            className={styles.startButton}
            onClick={handleNewGame}
            disabled={loading}
          >
            {loading ? 'Generating case...' : 'Begin Investigation'}
          </button>
        </div>
      </main>
    </div>
  )
}
