import { defineStore } from 'pinia'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    userInfo: JSON.parse(localStorage.getItem('userInfo') || 'null')
  }),
  getters: {
    roleCode: (s) => (s.userInfo ? s.userInfo.roleCode : ''),
    roleName: (s) => (s.userInfo ? s.userInfo.roleName || '' : ''),
    isStudent: (s) => !!s.userInfo && s.userInfo.roleCode === 'STUDENT',
    isAdmin: (s) =>
      !!s.userInfo && ['LAB_ADMIN', 'SUPER_ADMIN'].includes(s.userInfo.roleCode),
    isSuperAdmin: (s) => !!s.userInfo && s.userInfo.roleCode === 'SUPER_ADMIN'
  },
  actions: {
    setLogin(token, user) {
      this.token = token
      this.userInfo = user
      localStorage.setItem('token', token)
      localStorage.setItem('userInfo', JSON.stringify(user || null))
    },
    setUserInfo(user) {
      this.userInfo = user
      localStorage.setItem('userInfo', JSON.stringify(user || null))
    },
    logout() {
      this.token = ''
      this.userInfo = null
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
    },
    hasRole(roles) {
      if (!roles || !roles.length) return true
      return roles.includes(this.roleCode)
    }
  }
})
