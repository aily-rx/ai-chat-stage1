export const API_BASE = (import.meta.env.VITE_API_BASE_URL || '/api').replace(/\/$/, '')
export const USE_MOCK = import.meta.env.VITE_USE_MOCK !== 'false'
