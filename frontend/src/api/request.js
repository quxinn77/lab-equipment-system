import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: window.API_BASE_URL || '/api',
  timeout: 30000
})

request.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = 'Bearer ' + token
  }
  return config
})

request.interceptors.response.use(
  (response) => {
    // 二进制流（Excel 导出等）直接透传
    if (response.config.responseType === 'blob') {
      return response
    }
    const res = response.data
    if (!res || typeof res.code === 'undefined') {
      return res
    }
    if (res.code === 200) {
      return res.data
    }
    if (res.code === 401) {
      clearAuthAndRedirect()
      return Promise.reject(new Error(res.msg || '未登录'))
    }
    ElMessage.error(res.msg || '操作失败')
    return Promise.reject(new Error(res.msg || '操作失败'))
  },
  (error) => {
    const status = error.response ? error.response.status : 0
    if (status === 401) {
      clearAuthAndRedirect()
    } else {
      const msg =
        (error.response && error.response.data && error.response.data.msg) ||
        error.message ||
        '网络异常，请稍后重试'
      ElMessage.error(msg)
    }
    return Promise.reject(error)
  }
)

function clearAuthAndRedirect() {
  localStorage.removeItem('token')
  localStorage.removeItem('userInfo')
  ElMessage.error('登录已过期，请重新登录')
  if (!location.pathname.startsWith('/login')) {
    location.href = '/login'
  }
}

/**
 * 下载 blob 响应为文件（Excel 导出等）
 */
export function downloadBlobResponse(response, filename) {
  const contentType = String(response.headers['content-type'] || '')
  // 后端返回 JSON 说明导出失败（业务错误）
  if (contentType.includes('application/json')) {
    new Blob([response.data]).text().then((text) => {
      try {
        const json = JSON.parse(text)
        ElMessage.error(json.msg || '导出失败')
      } catch (e) {
        ElMessage.error('导出失败')
      }
    })
    return
  }
  const blob = new Blob([response.data], {
    type: contentType || 'application/octet-stream'
  })
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(url)
}

export default request
