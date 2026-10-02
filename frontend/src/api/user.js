import request from './request'

export function pageUsers(params) {
  return request.get('/users', { params })
}

export function createUser(data) {
  return request.post('/users', data)
}

export function updateUser(id, data) {
  return request.put(`/users/${id}`, data)
}

export function deleteUser(id) {
  return request.delete(`/users/${id}`)
}

export function resetPassword(id, newPassword) {
  return request.put(`/users/${id}/reset-password`, { newPassword })
}

export function updateUserStatus(id, status) {
  return request.put(`/users/${id}/status`, { status })
}
