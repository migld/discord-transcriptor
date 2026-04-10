import { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { getReveal } from '../api'
import styles from './Reveal.module.css'

export default function Reveal() {
  const { id }    = useParams()
  const navigate  = useNavigate()
  const [reveal,  setReveal]  = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    getReveal(id)
      .then(({ data }) => setReveal(data))
      .finally(() => setLoading(false))
  }, [id])

  if (loading) return <div className={styles.loading}>Unsealing case file...</div>

  const won = reveal?.playerWon === 'true'

  return (
    <div className={styles.container}>
      <div className={styles.card}>
        <p className={styles.stamp}>{won ? 'CASE CLOSED' : 'CASE UNSOLVED'}</p>
        <h1 className={styles.verdict}>
          {won
            ? `You caught them.`
            : `You were wrong.`}
        </h1>

        <div className={styles.file}>
          <div className={styles.row}>
            <span className={styles.label}>KILLER</span>
            <span className={styles.value}>{reveal?.killerName}</span>
          </div>
          <div className={styles.row}>
            <span className={styles.label}>VICTIM</span>
            <span className={styles.value}>{reveal?.victimName}</span>
          </div>
          <div className={styles.row}>
            <span className={styles.label}>MOTIVE</span>
            <span className={styles.value}>{reveal?.motive}</span>
          </div>
        </div>

        <p className={styles.scene}>{reveal?.openingScene}</p>

        <button className={styles.newGame} onClick={() => navigate('/lobby')}>
          Open a New Case
        </button>
      </div>
    </div>
  )
}
