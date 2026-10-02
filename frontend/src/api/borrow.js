import request, { downloadBlobResponse } from './request'

export function createBorrow(data) {
  return request.post('/borrows', data)
}

export function myBorrows(params) {
  return request.get('/borrows/my', { params })
}

export function pageBorrows(params) {
  return request.get('/borrows', { params })
}

export function approveBorrow(id, data) {
  return request.put(`/borrows/${id}/approve`, data)
}

export function pickupBorrow(id) {
  return request.put(`/borrows/${id}/pickup`)
}

export function returnBorrow(id, data) {
  return request.put(`/borrows/${id}/return`, data)
}

export function exportBorrows(params) {
  return request
    .get('/borrows/export', { params, responseType: 'blob' })
    .then((res) => downloadBlobResponse(res, '借用记录.xlsx'))
}
