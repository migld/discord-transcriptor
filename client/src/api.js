import axios from 'axios'

const api = axios.create({ baseURL: '/api' })

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('jwt')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

export const startGame   = ()                        => api.post('/game/new')
export const getGame     = (id)                      => api.get(`/game/${id}`)
export const talk        = (id, npcId, message)      => api.post(`/game/${id}/talk`, { npcId, message })
export const accuse      = (id, npcId)               => api.post(`/game/${id}/accuse`, { npcId })
export const getReveal   = (id)                      => api.get(`/game/${id}/reveal`)
export const getNpcs     = ()                        => api.get('/npcs')

export default api
