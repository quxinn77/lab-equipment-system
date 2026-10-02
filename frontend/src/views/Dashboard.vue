<template>
  <div class="page-container">
    <!-- 统计卡片 -->
    <div class="stat-cards">
      <div class="stat-card" v-for="card in cards" :key="card.label">
        <div class="stat-icon" :style="{ background: card.bg }">
          <el-icon><component :is="card.icon" /></el-icon>
        </div>
        <div>
          <div class="stat-value">{{ card.value }}</div>
          <div class="stat-label">{{ card.label }}</div>
        </div>
      </div>
    </div>

    <!-- 图表区 -->
    <div class="chart-grid">
      <div class="chart-card">
        <div class="chart-title"><span>设备状态分布</span>
          <el-button :icon="Refresh" circle size="small" @click="loadPie" />
        </div>
        <BaseChart :option="pieOption" height="300px" />
      </div>
      <div class="chart-card">
        <div class="chart-title"><span>各实验室设备数量</span>
          <el-button :icon="Refresh" circle size="small" @click="loadLabBar" />
        </div>
        <BaseChart :option="labBarOption" height="300px" />
      </div>
      <div v-if="userStore.isAdmin" class="chart-card">
        <div class="chart-title"><span>近 6 个月借用趋势</span>
          <el-button :icon="Refresh" circle size="small" @click="loadTrend" />
        </div>
        <BaseChart :option="trendOption" height="300px" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import BaseChart from '@/components/BaseChart.vue'
import { useUserStore } from '@/stores/user'
import { statsOverview, deviceByStatus, deviceByLab, borrowMonthly } from '@/api/stats'
import { pick } from '@/utils/format'

const userStore = useUserStore()

const overview = ref({})
const pieData = ref([])
const labData = ref([])
const trendData = ref([])

// 卡片（字段名做多 key 兜底）
const cards = computed(() => {
  const o = overview.value || {}
  const get = (...keys) => {
    for (const k of keys) {
      if (o[k] !== undefined && o[k] !== null) return o[k]
    }
    return 0
  }
  return [
    { label: '设备总数', icon: 'Monitor', bg: '#409eff', value: get('deviceTotal', 'totalDevices', 'total') },
    { label: '本月借用', icon: 'Tickets', bg: '#67c23a', value: get('monthBorrows', 'monthBorrowCount', 'monthBorrowed') },
    { label: '待审批', icon: 'Stamp', bg: '#e6a23c', value: get('pendingApprovals', 'pendingApproval', 'pendingBorrows') },
    { label: '逾期未还', icon: 'AlarmClock', bg: '#f56c6c', value: get('overdueCount', 'overdue') },
    { label: '待处理报修', icon: 'Tools', bg: '#722ed1', value: get('pendingRepairs', 'pendingRepair') }
  ]
})

async function loadOverview() {
  try {
    overview.value = (await statsOverview()) || {}
  } catch (e) { /* 拦截器已提示 */ }
}

async function loadPie() {
  try {
    const data = await deviceByStatus()
    // 兼容 [{name,value}] 或 {IDLE:3} 两种返回
    if (Array.isArray(data)) {
      pieData.value = data
    } else if (data && typeof data === 'object') {
      pieData.value = Object.keys(data).map((k) => ({ name: k, value: data[k] }))
    } else {
      pieData.value = []
    }
  } catch (e) { /* ignore */ }
}

async function loadLabBar() {
  try {
    const data = await deviceByLab()
    labData.value = Array.isArray(data) ? data : []
  } catch (e) { /* ignore */ }
}

async function loadTrend() {
  if (!userStore.isAdmin) return
  try {
    const data = await borrowMonthly(6)
    trendData.value = Array.isArray(data) ? data : []
  } catch (e) { /* ignore */ }
}

const pieOption = computed(() => ({
  tooltip: { trigger: 'item' },
  legend: { bottom: 0 },
  series: [
    {
      type: 'pie',
      radius: ['40%', '65%'],
      data: pieData.value,
      label: { formatter: '{b}: {c}' }
    }
  ]
}))

const labBarOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: 50, right: 20, bottom: 60, top: 20 },
  xAxis: { type: 'category', data: labData.value.map((d) => d.name), axisLabel: { rotate: 30 } },
  yAxis: { type: 'value' },
  series: [{ type: 'bar', barMaxWidth: 40, data: labData.value.map((d) => d.value ?? d.count) }]
}))

const trendOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: 50, right: 20, bottom: 40, top: 20 },
  xAxis: { type: 'category', data: trendData.value.map((d) => d.month) },
  yAxis: { type: 'value' },
  series: [
    {
      type: 'line',
      smooth: true,
      data: trendData.value.map((d) => d.count),
      areaStyle: { opacity: 0.15 }
    }
  ]
}))

onMounted(() => {
  loadOverview()
  loadPie()
  loadLabBar()
  loadTrend()
})
</script>
