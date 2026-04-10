import { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { getGame, accuse } from '../api'
import styles from './Accuse.module.css'

export default function Accuse() {
  const { id }    = useParams()
  const navigate  = useNavigate()

  const [suspects, setSuspects] = useState([])
  const [selected, setSelected] = useState(null)
  const [loading,  setLoading]  = useState(false)

  useEffect(() => {
    getGame(id).then(({ data }) => setSuspects(data.suspects))
  }, [id])

  const handleAccuse = async () => {
    if (!selected) return
    setLoading(true)
    try {
      await accuse(id, selected)
      navigate(`/game/${id}/reveal`)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className={styles.container}>
      <div className={styles.card}>
        <h1 className={styles.title}>Make Your Accusation</h1>
        <p className={styles.subtitle}>
          Choose carefully. You only get one shot.
        </p>

        <div className={styles.suspects}>
          {suspects.map(npc => (
            <button
              key={npc.id}
              className={`${styles.suspectCard} ${selected === npc.id ? styles.selected : ''}`}
              onClick={() => setSelected(npc.id)}
            >
              <span className={styles.suspectName}>{npc.name}</span>
              {selected === npc.id && <span className={styles.check}>✓</span>}
            </button>
          ))}
        </div>

        <div className={styles.actions}>
          <button className={styles.backBtn} onClick={() => navigate(`/game/${id}`)}>
            Back to Investigation
          </button>
          <button
            className={styles.accuseBtn}
            onClick={handleAccuse}
            disabled={!selected || loading}
          >
            {loading ? 'Filing accusation...' : 'Accuse'}
          </button>
        </div>
      </div>
    </div>
  )
}
