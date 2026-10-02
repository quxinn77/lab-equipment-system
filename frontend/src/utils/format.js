/**
 * 格式化工具
 */

function pad(n) {
  return String(n).padStart(2, '0')
}

export function formatDateTime(value, defaultText = '-') {
  if (!value) return defaultText
  const d = new Date(value)
  if (isNaN(d.getTime())) return String(value)
  return (
    `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ` +
    `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
  )
}

export function formatDate(value, defaultText = '-') {
  if (!value) return defaultText
  const d = new Date(value)
  if (isNaN(d.getTime())) return String(value)
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

// Date -> 'yyyy-MM-dd HH:mm:ss'（用于提交给后端）
export function toApiTime(date) {
  if (!date) return ''
  return formatDateTime(date, '')
}

// 金额：千分位 + 2 位小数
export function formatMoney(value, prefix = '¥') {
  if (value === null || value === undefined || value === '') return '-'
  const num = Number(value)
  if (isNaN(num)) return String(value)
  return prefix + num.toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  })
}

// 对象取第一个非空字段（后端字段命名兜底）
export function pick(obj, keys, defaultVal = '-') {
  if (!obj) return defaultVal
  for (const k of keys) {
    if (obj[k] !== undefined && obj[k] !== null && obj[k] !== '') return obj[k]
  }
  return defaultVal
}
