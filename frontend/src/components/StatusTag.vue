<template>
  <el-tag
    v-if="meta"
    :type="meta.tagType || 'info'"
    :color="meta.color"
    :style="meta.color ? 'color:#fff;border:none' : ''"
    :effect="meta.color ? 'dark' : 'light'"
    size="small"
  >
    {{ meta.label }}
  </el-tag>
  <span v-else>{{ value || '-' }}</span>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  // 枚举映射表，如 DEVICE_STATUS
  map: { type: Object, required: true },
  value: { type: [String, Number], default: '' }
})

const meta = computed(() => props.map[props.value])
</script>
