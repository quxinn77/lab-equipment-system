import request, { downloadBlobResponse } from './request'

export function pageDevices(params) {
  return request.get('/devices', { params })
}

export function getDevice(id) {
  return request.get(`/devices/${id}`)
}

export function createDevice(data) {
  return request.post('/devices', data)
}

export function updateDevice(id, data) {
  return request.put(`/devices/${id}`, data)
}

export function deleteDevice(id) {
  return request.delete(`/devices/${id}`)
}

export function getDeviceLogs(id) {
  return request.get(`/devices/${id}/logs`)
}

// 设备二维码地址（GET 需带 token，作为 query 参数传给后端）
export function deviceQrcodeUrl(id) {
  const token = localStorage.getItem('token') || ''
  return `/api/devices/qrcode/${id}?token=${encodeURIComponent(token)}`
}

export function exportDevices(params) {
  return request
    .get('/devices/export', { params, responseType: 'blob' })
    .then((res) => downloadBlobResponse(res, '设备台账.xlsx'))
}
