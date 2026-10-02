<template>
  <div ref="chartEl" :style="{ width: '100%', height: height }" />
</template>

<script setup>
import { onMounted, onBeforeUnmount, ref, watch, nextTick } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  option: { type: Object, default: () => ({}) },
  height: { type: String, default: '320px' }
})

const chartEl = ref(null)
let chart = null

function resize() {
  chart && chart.resize()
}

onMounted(async () => {
  await nextTick()
  if (chartEl.value) {
    chart = echarts.init(chartEl.value)
    chart.setOption(props.option || {})
    window.addEventListener('resize', resize)
  }
})

watch(
  () => props.option,
  (val) => {
    if (chart) {
      chart.clear()
      chart.setOption(val || {})
    }
  },
  { deep: true }
)

onBeforeUnmount(() => {
  window.removeEventListener('resize', resize)
  if (chart) {
    chart.dispose()
    chart = null
  }
})
</script>
