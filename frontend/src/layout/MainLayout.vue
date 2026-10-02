<template>
  <el-container class="layout-root">
    <!-- 桌面端侧栏 -->
    <el-aside
      v-if="!appStore.isMobile"
      :width="appStore.sidebarCollapsed ? '64px' : '220px'"
      style="transition: width 0.25s"
    >
      <div class="sidebar-panel">
        <div class="sidebar-logo">
          <el-icon :size="22"><Cpu /></el-icon>
          <span v-if="!appStore.sidebarCollapsed">实验室设备管理系统</span>
        </div>
        <SidebarMenu :collapse="appStore.sidebarCollapsed" />
      </div>
    </el-aside>

    <!-- 移动端抽屉侧栏 -->
    <el-drawer
      v-model="appStore.mobileSidebarVisible"
      direction="ltr"
      :with-header="false"
      size="220px"
    >
      <div class="sidebar-panel" style="height: 100vh">
        <div class="sidebar-logo">
          <el-icon :size="22"><Cpu /></el-icon>
          <span>实验室设备管理</span>
        </div>
        <SidebarMenu :collapse="false" @select="appStore.mobileSidebarVisible = false" />
      </div>
    </el-drawer>

    <el-container>
      <el-header class="header-bar">
        <div class="header-left">
          <el-icon
            v-if="!appStore.isMobile"
            :size="20"
            style="cursor: pointer"
            @click="appStore.toggleSidebar()"
          >
            <Fold v-if="!appStore.sidebarCollapsed" />
            <Expand v-else />
          </el-icon>
          <el-icon
            v-else
            :size="20"
            style="cursor: pointer"
            @click="appStore.openMobileSidebar()"
          >
            <Expand />
          </el-icon>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item
              v-for="item in breadcrumbs"
              :key="item.path"
            >
              {{ item.title }}
            </el-breadcrumb-item>
          </el-breadcrumb>
        </div>

        <div class="header-right">
          <el-badge :value="unread" :hidden="!unread" :max="99" class="bell-badge">
            <el-icon :size="18" style="cursor: pointer" @click="goMessages">
              <Bell />
            </el-icon>
          </el-badge>
          <el-dropdown @command="onUserCommand">
            <span class="user-name" style="cursor: pointer; display: flex; align-items: center; gap: 6px">
              <el-icon><UserFilled /></el-icon>
              {{ userStore.userInfo ? userStore.userInfo.realName || userStore.userInfo.username : '' }}
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="password">修改密码</el-dropdown-item>
                <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="main-area">
        <router-view />
      </el-main>
    </el-container>

    <!-- 修改密码弹窗 -->
    <el-dialog v-model="pwdVisible" title="修改密码" width="420px" :close-on-click-modal="false">
      <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="90px">
        <el-form-item label="原密码" prop="oldPassword">
          <el-input v-model="pwdForm.oldPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="pwdForm.newPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="pwdForm.confirmPassword" type="password" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdVisible = false">取消</el-button>
        <el-button type="primary" :loading="pwdLoading" @click="submitPassword">确定</el-button>
      </template>
    </el-dialog>
  </el-container>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import SidebarMenu from './SidebarMenu.vue'
import { useUserStore } from '@/stores/user'
import { useAppStore } from '@/stores/app'
import { unreadCount } from '@/api/message'
import { updatePassword } from '@/api/auth'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const appStore = useAppStore()

const unread = ref(0)
let unreadTimer = null

async function loadUnread() {
  try {
    const data = await unreadCount()
    unread.value = Number(data) || 0
  } catch (e) {
    /* 静默失败，不打扰用户 */
  }
}

onMounted(() => {
  loadUnread()
  unreadTimer = setInterval(loadUnread, 30000)
})
onBeforeUnmount(() => {
  if (unreadTimer) clearInterval(unreadTimer)
})

function goMessages() {
  router.push('/messages')
}

const breadcrumbs = computed(() => {
  const list = [{ path: '/', title: '首页' }]
  const matched = route.matched.filter((r) => r.meta && r.meta.title)
  matched.forEach((r) => {
    if (r.meta.title !== '首页' || r.path === '/dashboard') {
      list.push({ path: r.path, title: r.meta.title })
    }
  })
  return list
})

function onUserCommand(cmd) {
  if (cmd === 'logout') {
    ElMessageBox.confirm('确定要退出登录吗？', '提示', { type: 'warning' })
      .then(() => {
        userStore.logout()
        router.push('/login')
      })
      .catch(() => {})
  } else if (cmd === 'profile') {
    router.push('/profile')
  } else if (cmd === 'password') {
    pwdVisible.value = true
  }
}

/* 修改密码 */
const pwdVisible = ref(false)
const pwdLoading = ref(false)
const pwdFormRef = ref(null)
const pwdForm = ref({ oldPassword: '', newPassword: '', confirmPassword: '' })

const pwdRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (rule, value, cb) => {
        if (value !== pwdForm.value.newPassword) cb(new Error('两次输入的密码不一致'))
        else cb()
      },
      trigger: 'blur'
    }
  ]
}

async function submitPassword() {
  try {
    await pwdFormRef.value.validate()
  } catch (e) {
    return
  }
  pwdLoading.value = true
  try {
    await updatePassword({
      oldPassword: pwdForm.value.oldPassword,
      newPassword: pwdForm.value.newPassword
    })
    ElMessage.success('密码修改成功，请重新登录')
    pwdVisible.value = false
    userStore.logout()
    router.push('/login')
  } catch (e) {
    /* 拦截器已提示 */
  } finally {
    pwdLoading.value = false
  }
}
</script>

<style scoped>
.bell-badge {
  display: flex;
  align-items: center;
  cursor: pointer;
}
</style>
