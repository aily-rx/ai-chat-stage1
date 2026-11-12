export const API_BASE = (import.meta.env.VITE_API_BASE_URL || '/api').replace(/\/$/, '')
// 默认连接真实后端；仅当显式设置 VITE_USE_MOCK=true 时启用演示模式。
export const USE_MOCK = import.meta.env.VITE_USE_MOCK === 'true'
