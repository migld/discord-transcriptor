import { useState, useEffect, useRef } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { getGame, talk } from '../api'
import styles from './Game.module.css'

export default function Game() {
  const { id }      = useParams()
  const navigate    = useNavigate()

  const [gameData,       setGameData]       = useState(null)
  const [activeNpc,      setActiveNpc]      = useState(null)
  const [messages,       setMessages]       = useState([])
  const [input,          setInput]          = useState('')
  const [loading,        setLoading]        = useState(false)
  const [initialLoading, setInitialLoading] = useState(true)
  const messagesEndRef = useRef(null)

  useEffect(() => {
    loadGame()
  }, [id])

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages])

  const loadGame = async () => {
    try {
      const { data } = await getGame(id)
      setGameData(data)
      // Show only messages for the active NPC
      if (activeNpc) {
        setMessages(data.messages.filter(m => m.npcId === activeNpc.id || m.sender === 'player'))
      }
    } finally {
      setInitialLoading(false)
    }
  }

  const selectNpc = (npc) => {
    setActiveNpc(npc)
    const filtered = gameData.messages.filter(
      m => m.npcId === npc.id || (m.sender === 'player' && gameData.messages.find(
        prev => prev.npcId === npc.id && prev.createdAt < m.createdAt
      ))
    )
    setMessages(filtered)
    setInput('')
  }

  const sendMessage = async (e) => {
    e.preventDefault()
    if (!input.trim() || !activeNpc || loading) return

    const userMsg = { sender: 'player', content: input, createdAt: new Date().toISOString() }
    setMessages(prev => [...prev, userMsg])
    setInput('')
    setLoading(true)

    try {
      const { data } = await talk(id, activeNpc.id, input)
      const npcMsg = { sender: 'npc', npcId: activeNpc.id, content: data.reply, createdAt: new Date().toISOString() }
      setMessages(prev => [...prev, npcMsg])

      if (data.newClues?.length > 0) {
        setGameData(prev => ({ ...prev, clues: [...(prev.clues || []), ...data.newClues] }))
      }
    } finally {
      setLoading(false)
    }
  }

  if (initialLoading) return <div className={styles.loading}>Loading case file...</div>
  if (!gameData)      return <div className={styles.loading}>Case not found.</div>

  const { scenario, suspects, clues } = gameData

  return (
    <div className={styles.container}>

      {/* Header */}
      <header className={styles.header}>
        <div>
          <span className={styles.caseLabel}>CASE FILE</span>
          <span className={styles.caseName}>{scenario.title}</span>
        </div>
        <div className={styles.headerRight}>
          <span className={styles.victim}>VICTIM: {scenario.victimName}</span>
          <button className={styles.accuseBtn} onClick={() => navigate(`/game/${id}/accuse`)}>
            Make Accusation
          </button>
        </div>
      </header>

      <div className={styles.layout}>

        {/* Sidebar */}
        <aside className={styles.sidebar}>
          <section className={styles.sideSection}>
            <h3 className={styles.sideTitle}>Suspects</h3>
            {suspects.map(npc => (
              <button
                key={npc.id}
                className={`${styles.suspectBtn} ${activeNpc?.id === npc.id ? styles.active : ''}`}
                onClick={() => selectNpc(npc)}
              >
                {npc.name}
              </button>
            ))}
          </section>

          <section className={styles.sideSection}>
            <h3 className={styles.sideTitle}>Clues Found ({clues?.length || 0})</h3>
            {clues?.length === 0 && <p className={styles.noClues}>None yet. Keep asking questions.</p>}
            {clues?.map(clue => (
              <div key={clue.id} className={styles.clue}>
                <strong>{clue.title}</strong>
                <p>{clue.description}</p>
              </div>
            ))}
          </section>
        </aside>

        {/* Main dialogue area */}
        <main className={styles.main}>
          {!activeNpc ? (
            <div className={styles.openingScene}>
              <h2 className={styles.sceneTitle}>{scenario.title}</h2>
              <p className={styles.sceneText}>{scenario.openingScene}</p>
              <p className={styles.hint}>← Select a suspect to begin questioning</p>
            </div>
          ) : (
            <>
              <div className={styles.npcHeader}>
                <span className={styles.interrogating}>Interrogating:</span>
                <span className={styles.npcName}>{activeNpc.name}</span>
              </div>

              <div className={styles.dialogue}>
                {messages.length === 0 && (
                  <p className={styles.empty}>Ask {activeNpc.name} something.</p>
                )}
                {messages.map((msg, i) => (
                  <div key={i} className={`${styles.message} ${msg.sender === 'player' ? styles.player : styles.npc}`}>
                    <span className={styles.msgSender}>
                      {msg.sender === 'player' ? 'You' : activeNpc.name}
                    </span>
                    <p className={styles.msgContent}>{msg.content}</p>
                  </div>
                ))}
                {loading && (
                  <div className={`${styles.message} ${styles.npc}`}>
                    <span className={styles.msgSender}>{activeNpc.name}</span>
                    <p className={styles.msgContent}>...</p>
                  </div>
                )}
                <div ref={messagesEndRef} />
              </div>

              <form className={styles.inputArea} onSubmit={sendMessage}>
                <input
                  className={styles.input}
                  value={input}
                  onChange={e => setInput(e.target.value)}
                  placeholder={`Question ${activeNpc.name}...`}
                  disabled={loading}
                  autoFocus
                />
                <button className={styles.sendBtn} type="submit" disabled={loading || !input.trim()}>
                  Ask
                </button>
              </form>
            </>
          )}
        </main>
      </div>
    </div>
  )
}
