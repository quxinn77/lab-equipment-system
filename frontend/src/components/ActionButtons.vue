<template>
  <!-- 桌面端：平铺按钮；移动端：收纳进下拉菜单 -->
  <template v-if="!isMobile">
    <el-button
      v-for="(act, i) in visibleActions"
      :key="i"
      :type="act.type || 'primary'"
      link
      size="small"
      @click="handle(act)"
    >
      {{ act.label }}
    </el-button>
  </template>
  <el-dropdown v-else trigger="click">
    <el-button size="small" type="primary" link>
      操作<el-icon class="el-icon--right"><arrow-down /></el-icon>
    </el-button>
    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item
          v-for="(act, i) in visibleActions"
          :key="i"
          @click="handle(act)"
        >
          {{ act.label }}
        </el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<script setup>
import { computed } from 'vue'
import { ElMessageBox } from 'element-plus'
import { useAppStore } from '@/stores/app'

const props = defineProps({
  // [{ label, type, confirm, click, show }]
  actions: { type: Array, default: () => [] }
})

const appStore = useAppStore()
const isMobile = computed(() => appStore.isMobile)

const visibleActions = computed(() =>
  props.actions.filter((a) => a.show !== false)
)

function handle(act) {
  if (act.confirm) {
    ElMessageBox.confirm(act.confirm, '提示', { type: 'warning' })
      .then(() => act.click && act.click())
      .catch(() => {})
  } else {
    act.click && act.click()
  }
}
</script>
