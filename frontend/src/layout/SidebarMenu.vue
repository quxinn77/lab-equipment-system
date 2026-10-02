<template>
  <el-menu
    class="sidebar-menu"
    background-color="#1d2b45"
    text-color="#c8d2e2"
    active-text-color="#ffffff"
    :default-active="activeMenu"
    :collapse="collapse"
    :collapse-transition="false"
    router
    @select="onSelect"
  >
    <el-menu-item
      v-for="item in menuItems"
      :key="item.path"
      :index="item.path"
    >
      <el-icon><component :is="item.icon" /></el-icon>
      <template #title>{{ item.title }}</template>
    </el-menu-item>
  </el-menu>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import router from '@/router'
import { useUserStore } from '@/stores/user'

defineProps({
  collapse: { type: Boolean, default: false }
})
const emit = defineEmits(['select'])

const route = useRoute()
const userStore = useUserStore()

const activeMenu = computed(() => route.path)

const menuItems = computed(() => {
  const main = router
    .getRoutes()
    .filter(
      (r) =>
        r.meta &&
        r.meta.menu &&
        (!r.meta.roles || r.meta.roles.includes(userStore.roleCode))
    )
    .sort((a, b) => {
      const order = (r) => (r.meta && r.meta.menuOrder) || 999
      return order(a) - order(b)
    })
    .map((r) => ({
      path: r.path,
      title: r.meta.title,
      icon: r.meta.icon || 'Menu'
    }))
  return main
})

function onSelect() {
  emit('select')
}
</script>
