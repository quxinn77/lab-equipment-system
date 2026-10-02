import { defineStore } from 'pinia'

export const useAppStore = defineStore('app', {
  state: () => ({
    windowWidth: typeof window !== 'undefined' ? window.innerWidth : 1200,
    sidebarCollapsed: false,
    mobileSidebarVisible: false
  }),
  getters: {
    isMobile: (s) => s.windowWidth < 992
  },
  actions: {
    updateWidth(w) {
      this.windowWidth = w
      if (w >= 992) this.mobileSidebarVisible = false
    },
    toggleSidebar() {
      this.sidebarCollapsed = !this.sidebarCollapsed
    },
    openMobileSidebar() {
      this.mobileSidebarVisible = true
    }
  }
})
