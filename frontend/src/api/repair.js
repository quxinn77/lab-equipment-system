import request from './request'

export function createRepair(data) {
  return request.post('/repairs', data)
}

export function myRepairs(params) {
  return request.get('/repairs/my', { params })
}

export function pageRepairs(params) {
  return request.get('/repairs', { params })
}

export function updateRepair(id, data) {
  return request.put(`/repairs/${id}`, data)
}
