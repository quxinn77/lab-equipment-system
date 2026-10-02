<template>
  <div class="page-container">
    <div class="chart-grid">
      <div class="chart-card" style="grid-column: 1 / -1">
        <div class="chart-title">
          <span>月度借用统计</span>
          <el-button :icon="Refresh" circle size="small" @click="loadMonthly" />
        </div>
        <BaseChart :option="monthlyOption" height="300px" />
      </div>

      <div class="chart-card">
        <div class="chart-title">
          <span>热门设备 TOP10</span>
          <el-button :icon="Refresh" circle size="small" @click="loadTopDevices" />
        </div>
        <BaseChart :option="topDevicesOption" height="320px" />
      </div>

      <div class="chart-card">
        <div class="chart-title">
          <span>学生借用排行 TOP10</span>
          <el-button :icon="Refresh" circle size="small" @click="loadUserRank" />
        </div>
        <BaseChart :option="userRankOption" height="320px" />
      </div>

      <div class="chart-card">
        <div class="chart-title">
          <span>报修统计</span>
          <el-button :icon="Refresh" circle size="small" @click="loadRepairSummary" />
        </div>
        <BaseChart :option="repairOption" height="300px" />
      </div>

      <div class="chart-card">
        <div class="chart-title">
          <span>高频故障设备</span>
          <el-button :icon="Refresh" circle size="small" @click="loadRepairSummary" />
        </div>
        <BaseChart :option="faultDevicesOption" height="300px" />
      </div>

      <div class="chart-card">
        <div class="chart-title">
          <span>逾期统计</span>
          <el-button :icon="Refresh" circle size="small" @click="loadOverdue" />
        </div>
        <template v-if="overdueRows.length">
          <div class="table-scroll-wrapper">
            <el-table :data="overdueRows" size="small" border max-height="300">
              <el-table-column
                v-for="col in overdueColumns"
                :key="col"
                :prop="col"
                :label="col"
                min-width="120"
                show-overflow-tooltip
              >
                <template #default="{ row }">{{ formatCell(row[col]) }}</template>
              </el-table-column>
            </el-table>
          </div>
        </template>
        <el-empty v-else description="暂无逾期数据" :image-size="60" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import BaseChart from '@/components/BaseChart.vue'
import {
  borrowMonthly,
  topDevices,
  userRank,
  repairSummary,
  overdueStats
} from '@/api/stats'
import { formatDateTime } from '@/utils/format'

const monthlyData = ref([])
const topDevicesData = ref([])
const userRankData = ref([])
const repairStatusData = ref([])
const faultDevicesData = ref([])
const overdueRows = ref([])
const overdueColumns = ref([])

// 从任意结构中找出第一个数组字段
function firstArray(data) {
  if (Array.isArray(data)) return data
  if (data && typeof data === 'object') {
    for (const k of Object.keys(data)) {
      if (Array.isArray(data[k])) return data[k]
    }
  }
  return []
}

// 报修统计兼容：状态数组与高频故障数组
function parseRepair(data) {
  if (!data) return
  if (Array.isArray(data)) {
    repairStatusData.value = data
    faultDevicesData.value = []
    return
  }
  if (typeof data === 'object') {
    const keys = Object.keys(data)
    const arrKeys = keys.filter((k) => Array.isArray(data[k]))
    if (arrKeys.length > 0) repairStatusData.value = data[arrKeys[0]]
    if (arrKeys.length > 1) faultDevicesData.value = data[arrKeys[1]]
    else {
      faultDevicesData.value =
        data.topDevices || data.frequentDevices || data.hotDevices || []
    }
  }
}

async function loadMonthly() {
  try {
    const data = await borrowMonthly(6)
    monthlyData.value = Array.isArray(data) ? data : []
  } catch (e) { /* ignore */ }
}

async function loadTopDevices() {
  try {
    const data = await topDevices(10)
    topDevicesData.value = Array.isArray(data) ? data : []
  } catch (e) { /* ignore */ }
}

async function loadUserRank() {
  try {
    const data = await userRank(10)
    userRankData.value = Array.isArray(data) ? data : []
  } catch (e) { /* ignore */ }
}

async function loadRepairSummary() {
  try {
    const data = await repairSummary()
    parseRepair(data)
  } catch (e) { /* ignore */ }
}

async function loadOverdue() {
  try {
    const data = await overdueStats()
    let rows = []
    if (Array.isArray(data)) {
      rows = data
    } else if (data && typeof data === 'object') {
      // 对象结构：可能是 { count, list/records } 或纯键值
      rows = data.list || data.records || firstArray(data)
      if (!rows.length && !(data.list || data.records)) {
        // 标量键值 -> 转成一行
        rows = [Object.fromEntries(Object.entries(data).filter(([, v]) => typeof v !== 'object'))]
      }
    }
    overdueRows.value = rows.filter((r) => r && typeof r === 'object')
    overdueColumns.value = overdueRows.value.length
      ? Object.keys(overdueRows.value[0]).slice(0, 7)
      : []
  } catch (e) { /* ignore */ }
}

function formatCell(v) {
  if (v === null || v === undefined || v === '') return '-'
  if (typeof v === 'string' && /^\d{4}-\d{2}-\d{2}[T ]/.test(v)) return formatDateTime(v)
  return String(v)
}

const monthlyOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: 50, right: 20, bottom: 40, top: 20 },
  xAxis: { type: 'category', data: monthlyData.value.map((d) => d.month) },
  yAxis: { type: 'value' },
  series: [
    {
      name: '借用次数',
      type: 'bar',
      barMaxWidth: 44,
      itemStyle: { color: '#409eff' },
      data: monthlyData.value.map((d) => d.count)
    }
  ]
}))

const topDevicesOption = computed(() => {
  const names = topDevicesData.value.map((d) => d.name)
  const values = topDevicesData.value.map((d) => d.count ?? d.value)
  return {
    tooltip: { trigger: 'axis' },
    grid: { left: 130, right: 30, bottom: 30, top: 10 },
    xAxis: { type: 'value' },
    yAxis: { type: 'category', data: names.reverse(), axisLabel: { width: 120 } },
    series: [
      {
        type: 'bar',
        barMaxWidth: 18,
        itemStyle: { color: '#67c23a' },
        data: values.reverse()
      }
    ]
  }
})

const userRankOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: 50, right: 20, bottom: 40, top: 10 },
  xAxis: {
    type: 'category',
    data: userRankData.value.map((d) => d.name),
    axisLabel: { rotate: 30 }
  },
  yAxis: { type: 'value' },
  series: [
    {
      type: 'bar',
      barMaxWidth: 36,
      itemStyle: { color: '#e6a23c' },
      data: userRankData.value.map((d) => d.count)
    }
  ]
}))

const repairOption = computed(() => ({
  tooltip: { trigger: 'item' },
  legend: { bottom: 0 },
  series: [
    {
      type: 'pie',
      radius: ['35%', '60%'],
      data: repairStatusData.value.map((d) => ({ name: d.name, value: d.value ?? d.count })),
      label: { formatter: '{b}: {c}' }
    }
  ]
}))

const faultDevicesOption = computed(() => {
  const names = faultDevicesData.value.map((d) => d.name)
  const values = faultDevicesData.value.map((d) => d.count ?? d.value)
  return {
    tooltip: { trigger: 'axis' },
    grid: { left: 130, right: 30, bottom: 30, top: 10 },
    xAxis: { type: 'value' },
    yAxis: { type: 'category', data: names.reverse(), axisLabel: { width: 120 } },
    series: [
      {
        type: 'bar',
        barMaxWidth: 18,
        itemStyle: { color: '#f56c6c' },
        data: values.reverse()
      }
    ]
  }
})

onMounted(() => {
  loadMonthly()
  loadTopDevices()
  loadUserRank()
  loadRepairSummary()
  loadOverdue()
})
</script>
