import request from './request'

export function pageMessages(params) {
  return request.get('/messages', { params })
}

export function unreadCount() {
  return request.get('/messages/unread-count')
}

export function markRead(id) {
  return request.put(`/messages/${id}/read`)
}

export function markAllRead() {
  return request.put('/messages/read-all')
}
