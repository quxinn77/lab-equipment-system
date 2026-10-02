import request from './request'

export function createReservation(data) {
  return request.post('/reservations', data)
}

export function myReservations(params) {
  return request.get('/reservations/my', { params })
}

export function pageReservations(params) {
  return request.get('/reservations', { params })
}

export function approveReservation(id, data) {
  return request.put(`/reservations/${id}/approve`, data)
}

export function cancelReservation(id) {
  return request.put(`/reservations/${id}/cancel`)
}
