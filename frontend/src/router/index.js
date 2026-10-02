import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录', public: true }
  },
  {
    path: '/',
    component: () => import('@/layout/MainLayout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: { title: '首页看板', icon: 'Odometer', menu: true , menuOrder: 1 }
      },
      {
        path: 'devices',
        name: 'Devices',
        component: () => import('@/views/devices/List.vue'),
        meta: { title: '设备管理', icon: 'Monitor', menu: true , menuOrder: 2 }
      },
      {
        path: 'borrows/my',
        name: 'MyBorrows',
        component: () => import('@/views/borrows/MyBorrows.vue'),
        meta: { title: '我的借用', icon: 'Tickets', menu: true, roles: ['STUDENT'] , menuOrder: 3 }
      },
      {
        path: 'borrows',
        name: 'Borrows',
        component: () => import('@/views/borrows/Manage.vue'),
        meta: {
          title: '借用管理',
          icon: 'Tickets',
          menu: true,
          roles: ['LAB_ADMIN', 'SUPER_ADMIN']
        , menuOrder: 4 }
      },
      {
        path: 'reservations',
        name: 'Reservations',
        component: () => import('@/views/reservations/Index.vue'),
        meta: { title: '设备预约', icon: 'Calendar', menu: true , menuOrder: 5 }
      },
      {
        path: 'repairs',
        name: 'Repairs',
        component: () => import('@/views/repairs/Index.vue'),
        meta: { title: '故障报修', icon: 'Tools', menu: true , menuOrder: 6 }
      },
      {
        path: 'labs',
        name: 'Labs',
        component: () => import('@/views/labs/Index.vue'),
        meta: {
          title: '实验室管理',
          icon: 'OfficeBuilding',
          menu: true,
          roles: ['LAB_ADMIN', 'SUPER_ADMIN']
        , menuOrder: 7 }
      },
      {
        path: 'categories',
        name: 'Categories',
        component: () => import('@/views/categories/Index.vue'),
        meta: {
          title: '设备分类',
          icon: 'Files',
          menu: true,
          roles: ['LAB_ADMIN', 'SUPER_ADMIN']
        , menuOrder: 8 }
      },
      {
        path: 'stats',
        name: 'Stats',
        component: () => import('@/views/stats/Index.vue'),
        meta: {
          title: '统计报表',
          icon: 'DataAnalysis',
          menu: true,
          roles: ['LAB_ADMIN', 'SUPER_ADMIN']
        , menuOrder: 9 }
      },
      {
        path: 'users',
        name: 'Users',
        component: () => import('@/views/users/Index.vue'),
        meta: {
          title: '用户管理',
          icon: 'UserFilled',
          menu: true,
          roles: ['SUPER_ADMIN']
        , menuOrder: 10 }
      },
      {
        path: 'messages',
        name: 'Messages',
        component: () => import('@/views/messages/Index.vue'),
        meta: { title: '消息中心', icon: 'Bell', menu: true , menuOrder: 11 }
      },
      {
        path: 'logs',
        name: 'Logs',
        component: () => import('@/views/logs/Index.vue'),
        meta: {
          title: '系统日志',
          icon: 'Document',
          menu: true,
          roles: ['SUPER_ADMIN']
        , menuOrder: 12 }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/profile/Index.vue'),
        meta: { title: '个人中心', icon: 'User' }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/dashboard'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (to.meta.public) {
    next()
    return
  }
  if (!token) {
    next('/login')
    return
  }
  const userInfo = JSON.parse(localStorage.getItem('userInfo') || 'null')
  const roles = to.meta.roles
  if (roles && roles.length) {
    if (!userInfo || !roles.includes(userInfo.roleCode)) {
      ElMessage.error('无权限访问该页面')
      next(userInfo && userInfo.roleCode === 'STUDENT' ? '/borrows/my' : '/dashboard')
      return
    }
  }
  next()
})

export default router
