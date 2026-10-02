<template>
  <div class="page-container">
    <div class="table-card">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="操作日志" name="operations">
          <div class="table-toolbar">
            <div>
              <el-input
                v-model="opQuery.keyword"
                placeholder="关键词"
                clearable
                style="width: 180px; margin-right: 8px"
                @keyup.enter="doOpSearch"
              />
              <el-input
                v-model="opQuery.module"
                placeholder="模块"
                clearable
                style="width: 140px; margin-right: 8px"
                @keyup.enter="doOpSearch"
              />
              <el-button type="primary" :icon="Search" @click="doOpSearch">查询</el-button>
            </div>
            <el-button :icon="Refresh" @click="loadOpLogs">刷新</el-button>
          </div>
          <div class="table-scroll-wrapper">
            <el-table :data="opLogs" v-loading="opLoading" border stripe>
              <el-table-column prop="id" label="ID" width="80" />
              <el-table-column label="操作人" min-width="100">
                <template #default="{ row }">{{ pick(row, ['username', 'operatorName', 'operator'], '-') }}</template>
              </el-table-column>
              <el-table-column prop="module" label="模块" min-width="100">
                <template #default="{ row }">{{ row.module || '-' }}</template>
              </el-table-column>
              <el-table-column label="操作内容" min-width="240" show-overflow-tooltip>
                <template #default="{ row }">{{ pick(row, ['action', 'operation', 'content', 'description'], '-') }}</template>
              </el-table-column>
              <el-table-column label="IP" min-width="120">
                <template #default="{ row }">{{ row.ip || '-' }}</template>
              </el-table-column>
              <el-table-column label="时间" min-width="160">
                <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
              </el-table-column>
            </el-table>
          </div>
          <div class="pagination-bar">
            <el-pagination
              v-model:current-page="opQuery.pageNum"
              v-model:page-size="opQuery.pageSize"
              :total="opTotal"
              :page-sizes="[10, 20, 50]"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="loadOpLogs"
              @current-change="loadOpLogs"
            />
          </div>
        </el-tab-pane>

        <el-tab-pane label="登录日志" name="logins">
          <div class="table-toolbar">
            <span style="color: #909399; font-size: 13px">登录成功/失败均会记录</span>
            <el-button :icon="Refresh" @click="loadLoginLogs">刷新</el-button>
          </div>
          <div class="table-scroll-wrapper">
            <el-table :data="loginLogs" v-loading="loginLoading" border stripe>
              <el-table-column prop="id" label="ID" width="80" />
              <el-table-column label="账号" min-width="110">
                <template #default="{ row }">{{ pick(row, ['username', 'account'], '-') }}</template>
              </el-table-column>
              <el-table-column label="IP" min-width="130">
                <template #default="{ row }">{{ row.ip || '-' }}</template>
              </el-table-column>
              <el-table-column label="浏览器/UA" min-width="240" show-overflow-tooltip>
                <template #default="{ row }">{{ pick(row, ['browser', 'userAgent', 'ua'], '-') }}</template>
              </el-table-column>
              <el-table-column label="结果" width="90" align="center">
                <template #default="{ row }">
                  <el-tag
                    :type="loginSuccess(row) ? 'success' : 'danger'"
                    size="small"
                  >
                    {{ loginSuccess(row) ? '成功' : '失败' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="时间" min-width="160">
                <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
              </el-table-column>
            </el-table>
          </div>
          <div class="pagination-bar">
            <el-pagination
              v-model:current-page="loginQuery.pageNum"
              v-model:page-size="loginQuery.pageSize"
              :total="loginTotal"
              :page-sizes="[10, 20, 50]"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="loadLoginLogs"
              @current-change="loadLoginLogs"
            />
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { Search, Refresh } from '@element-plus/icons-vue'
import { pageOperationLogs, pageLoginLogs } from '@/api/log'
import { formatDateTime, pick } from '@/utils/format'

const activeTab = ref('operations')

/* ===== 操作日志 ===== */
const opQuery = reactive({ pageNum: 1, pageSize: 10, keyword: '', module: '' })
const opLogs = ref([])
const opTotal = ref(0)
const opLoading = ref(false)

async function loadOpLogs() {
  opLoading.value = true
  try {
    const data = await pageOperationLogs({ ...opQuery })
    opLogs.value = (data && data.records) || []
    opTotal.value = (data && data.total) || 0
  } catch (e) { /* 拦截器已提示 */ } finally {
    opLoading.value = false
  }
}

function doOpSearch() {
  opQuery.pageNum = 1
  loadOpLogs()
}

/* ===== 登录日志 ===== */
const loginQuery = reactive({ pageNum: 1, pageSize: 10 })
const loginLogs = ref([])
const loginTotal = ref(0)
const loginLoading = ref(false)

async function loadLoginLogs() {
  loginLoading.value = true
  try {
    const data = await pageLoginLogs({ ...loginQuery })
    loginLogs.value = (data && data.records) || []
    loginTotal.value = (data && data.total) || 0
  } catch (e) { /* 拦截器已提示 */ } finally {
    loginLoading.value = false
  }
}

function loginSuccess(row) {
  if (row.success !== undefined) return !!row.success
  if (row.status !== undefined) return row.status === 1 || row.status === '1' || row.status === true
  if (row.result !== undefined) return row.result === 'SUCCESS' || row.result === 1
  return true
}

watch(activeTab, (tab) => {
  if (tab === 'logins' && !loginLogs.value.length) loadLoginLogs()
})

onMounted(loadOpLogs)
</script>
