import request from './request'

export function pageOperationLogs(params) {
  return request.get('/logs/operations', { params })
}

export function pageLoginLogs(params) {
  return request.get('/logs/logins', { params })
}
