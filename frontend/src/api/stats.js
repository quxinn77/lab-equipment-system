import request from './request'

export function statsOverview() {
  return request.get('/stats/overview')
}

export function deviceByStatus() {
  return request.get('/stats/device-by-status')
}

export function deviceByLab() {
  return request.get('/stats/device-by-lab')
}

export function borrowMonthly(months = 6) {
  return request.get('/stats/borrow-monthly', { params: { months } })
}

export function topDevices(limit = 10) {
  return request.get('/stats/top-devices', { params: { limit } })
}

export function overdueStats() {
  return request.get('/stats/overdue')
}

export function repairSummary() {
  return request.get('/stats/repair-summary')
}

export function userRank(limit = 10) {
  return request.get('/stats/user-rank', { params: { limit } })
}
