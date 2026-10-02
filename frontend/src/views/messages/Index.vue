<template>
  <div class="page-container">
    <div class="search-bar">
      <el-form :inline="true" :model="query">
        <el-form-item label="状态">
          <el-select v-model="query.isRead" placeholder="全部" clearable style="width: 130px">
            <el-option label="未读" :value="0" />
            <el-option label="已读" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="doSearch">查询</el-button>
          <el-button :icon="Refresh" @click="resetSearch">重置</el-button>
          <el-button type="success" :icon="Check" @click="doReadAll">全部已读</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="table-card">
      <div class="table-toolbar">
        <span style="font-weight: 600">我的消息（共 {{ total }} 条）</span>
        <el-button :icon="Refresh" @click="loadList">刷新</el-button>
      </div>

      <div class="table-scroll-wrapper">
        <el-table
          :data="list"
          v-loading="loading"
          border
          stripe
          :row-class-name="rowClass"
          @row-click="onRowClick"
        >
          <el-table-column label="标题" min-width="200">
            <template #default="{ row }">
              <span :style="row.isRead ? '' : 'font-weight: 700'">
                {{ row.title || row.content || '站内消息' }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="内容" min-width="260" show-overflow-tooltip>
            <template #default="{ row }">{{ row.content || '-' }}</template>
          </el-table-column>
          <el-table-column label="时间" min-width="160">
            <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
          </el-table-column>
          <el-table-column label="状态" width="90" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.isRead" type="info" size="small">已读</el-tag>
              <el-tag v-else type="danger" size="small" effect="dark">未读</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="110" fixed="right">
            <template #default="{ row }">
              <ActionButtons :actions="rowActions(row)" />
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="pagination-bar">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadList"
          @current-change="loadList"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Check } from '@element-plus/icons-vue'
import ActionButtons from '@/components/ActionButtons.vue'
import { pageMessages, markRead, markAllRead } from '@/api/message'
import { formatDateTime } from '@/utils/format'

const query = reactive({ pageNum: 1, pageSize: 10, isRead: null })
const list = ref([])
const total = ref(0)
const loading = ref(false)

async function loadList() {
  loading.value = true
  try {
    const data = await pageMessages({ ...query })
    list.value = (data && data.records) || []
    total.value = (data && data.total) || 0
  } catch (e) { /* 拦截器已提示 */ } finally {
    loading.value = false
  }
}

function doSearch() {
  query.pageNum = 1
  loadList()
}

function resetSearch() {
  query.isRead = null
  doSearch()
}

function rowClass({ row }) {
  return row.isRead ? '' : 'unread-row'
}

function rowActions(row) {
  return [
    {
      label: '标记已读',
      show: !row.isRead,
      click: () => doMarkRead(row)
    }
  ]
}

async function doMarkRead(row) {
  try {
    await markRead(row.id)
    row.isRead = 1
  } catch (e) { /* 拦截器已提示 */ }
}

async function onRowClick(row) {
  if (!row.isRead) doMarkRead(row)
}

async function doReadAll() {
  try {
    await ElMessageBox.confirm('确定将全部消息标记为已读吗？', '提示', { type: 'warning' })
  } catch (e) {
    return
  }
  try {
    await markAllRead()
    ElMessage.success('已全部标记为已读')
    loadList()
  } catch (e) { /* 拦截器已提示 */ }
}

onMounted(loadList)
</script>

<style scoped>
:deep(.unread-row) {
  cursor: pointer;
}
</style>
