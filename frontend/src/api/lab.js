import request from './request'

export function pageLabs(params) {
  return request.get('/labs', { params })
}

export function allLabs() {
  return request.get('/labs/all')
}

export function createLab(data) {
  return request.post('/labs', data)
}

export function updateLab(id, data) {
  return request.put(`/labs/${id}`, data)
}

export function deleteLab(id) {
  return request.delete(`/labs/${id}`)
}
