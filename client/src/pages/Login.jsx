import styles from './Login.module.css'

export default function Login() {
  const handleLogin = () => {
    window.location.href = '/api/auth/login'
  }

  return (
    <div className={styles.container}>
      <div className={styles.card}>
        <p className={styles.eyebrow}>A MURDER MYSTERY</p>
        <h1 className={styles.title}>The Case Files</h1>
        <p className={styles.subtitle}>
          Someone is dead. Everyone is a suspect.<br />
          One of them is your friend.
        </p>
        <button className={styles.button} onClick={handleLogin}>
          Sign in with Discord
        </button>
        <p className={styles.note}>Only server members may enter.</p>
      </div>
    </div>
  )
}
