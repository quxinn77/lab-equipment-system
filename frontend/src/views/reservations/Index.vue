<template>
  <div class="page-container">
    <div class="search-bar">
      <el-form :inline="true" :model="query">
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 150px">
            <el-option
              v-for="o in reservationStatusOptions"
              :key="o.value"
              :label="o.label"
              :value="o.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="doSearch">查询</el-button>
          <el-button :icon="Refresh" @click="resetSearch">重置</el-button>
          <el-button v-if="userStore.isStudent" type="success" :icon="Plus" @click="openApply">
            申请预约
          </el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="table-card">
      <div class="table-toolbar">
        <span style="font-weight: 600">
          {{ userStore.isStudent ? '我的预约' : '全部预约（共 ' + total + ' 条）' }}
        </span>
        <el-button :icon="Refresh" @click="loadList">刷新</el-button>
      </div>

      <div class="table-scroll-wrapper">
        <el-table :data="list" v-loading="loading" border stripe>
          <el-table-column prop="id" label="单号" width="80" />
          <el-table-column v-if="!userStore.isStudent" label="预约人" min-width="100">
            <template #default="{ row }">{{ pick(row, ['applicantName', 'applicant', 'username'], '-') }}</template>
          </el-table-column>
          <el-table-column label="设备" min-width="150">
            <template #default="{ row }">{{ pick(row, ['deviceName', 'deviceCode'], '-') }}</template>
          </el-table-column>
          <el-table-column label="开始时间" min-width="150">
            <template #default="{ row }">{{ formatDateTime(row.startTime) }}</template>
          </el-table-column>
          <el-table-column label="结束时间" min-width="150">
            <template #default="{ row }">{{ formatDateTime(row.endTime) }}</template>
          </el-table-column>
          <el-table-column label="备注" min-width="130" show-overflow-tooltip>
            <template #default="{ row }">{{ row.remark || '-' }}</template>
          </el-table-column>
          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }">
              <StatusTag :map="RESERVATION_STATUS" :value="row.status" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="130" fixed="right">
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

    <!-- 学生：申请预约 -->
    <el-dialog v-model="applyVisible" title="预约设备" width="520px" :close-on-click-modal="false">
      <el-form ref="applyFormRef" :model="applyForm" :rules="applyRules" label-width="90px">
        <el-form-item label="选择设备" prop="deviceId">
          <el-select v-model="applyForm.deviceId" placeholder="空闲/预留设备" filterable style="width: 100%">
            <el-option
              v-for="d in devices"
              :key="d.id"
              :label="`${d.code} - ${d.name}`"
              :value="d.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="预约时间" prop="timeRange">
          <el-date-picker
            v-model="applyForm.timeRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="applyForm.remark" type="textarea" :rows="3" placeholder="选填" />
        </el-form-item>
        <el-alert type="info" :closable="false" title="同设备时段冲突由系统校验，如有冲突将提示错误" />
      </el-form>
      <template #footer>
        <el-button @click="applyVisible = false">取消</el-button>
        <el-button type="primary" :loading="applying" @click="submitApply">提交预约</el-button>
      </template>
    </el-dialog>

    <!-- 管理员：审核 -->
    <el-dialog v-model="approveVisible" title="预约审核" width="440px" :close-on-click-modal="false">
      <el-form label-width="80px">
        <el-form-item label="审核结果">
          <el-radio-group v-model="approveForm.approved">
            <el-radio :value="true">通过</el-radio>
            <el-radio :value="false">驳回</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="approveForm.remark" type="textarea" :rows="3" placeholder="选填" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="approveVisible = false">取消</el-button>
        <el-button type="primary" :loading="approving" @click="submitApprove">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Refresh, Plus } from '@element-plus/icons-vue'
import StatusTag from '@/components/StatusTag.vue'
import ActionButtons from '@/components/ActionButtons.vue'
import { useUserStore } from '@/stores/user'
import {
  createReservation,
  myReservations,
  pageReservations,
  approveReservation,
  cancelReservation
} from '@/api/reservation'
import { pageDevices } from '@/api/device'
import { RESERVATION_STATUS, toOptions } from '@/utils/constants'
import { formatDateTime, pick } from '@/utils/format'

const userStore = useUserStore()
const reservationStatusOptions = toOptions(RESERVATION_STATUS)

const query = reactive({ pageNum: 1, pageSize: 10, status: '' })
const list = ref([])
const total = ref(0)
const loading = ref(false)

async function loadList() {
  loading.value = true
  try {
    const fetcher = userStore.isStudent ? myReservations : pageReservations
    const data = await fetcher({ ...query })
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
  query.status = ''
  doSearch()
}

function rowActions(row) {
  const acts = []
  if (userStore.isStudent) {
    if (row.status === 'PENDING') {
      acts.push({
        label: '取消预约',
        type: 'danger',
        confirm: '确定取消该预约吗？',
        click: () => doCancel(row)
      })
    }
  } else if (row.status === 'PENDING') {
    acts.push({ label: '审核', type: 'warning', click: () => openApprove(row) })
  }
  if (!acts.length) acts.push({ label: '-', click: () => {} })
  return acts
}

async function doCancel(row) {
  try {
    await cancelReservation(row.id)
    ElMessage.success('已取消预约')
    loadList()
  } catch (e) { /* 拦截器已提示 */ }
}

/* ===== 学生申请 ===== */
const applyVisible = ref(false)
const applying = ref(false)
const applyFormRef = ref(null)
const devices = ref([])
const applyForm = reactive({ deviceId: null, timeRange: null, remark: '' })

const applyRules = {
  deviceId: [{ required: true, message: '请选择设备', trigger: 'change' }],
  timeRange: [{ required: true, message: '请选择预约时间', trigger: 'change' }]
}

async function openApply() {
  applyForm.deviceId = null
  applyForm.timeRange = null
  applyForm.remark = ''
  applyVisible.value = true
  try {
    const data = await pageDevices({ pageNum: 1, pageSize: 500 })
    const all = (data && data.records) || []
    devices.value = all.filter((d) => d.status === 'IDLE' || d.status === 'RESERVED')
  } catch (e) { /* 拦截器已提示 */ }
}

async function submitApply() {
  try {
    await applyFormRef.value.validate()
  } catch (e) {
    return
  }
  const [startTime, endTime] = applyForm.timeRange
  applying.value = true
  try {
    await createReservation({
      deviceId: applyForm.deviceId,
      startTime,
      endTime,
      remark: applyForm.remark
    })
    ElMessage.success('预约已提交，等待审核')
    applyVisible.value = false
    loadList()
  } catch (e) { /* 拦截器已提示（时段冲突等） */ } finally {
    applying.value = false
  }
}

/* ===== 管理员审核 ===== */
const approveVisible = ref(false)
const approving = ref(false)
const currentRow = ref(null)
const approveForm = reactive({ approved: true, remark: '' })

function openApprove(row) {
  currentRow.value = row
  approveForm.approved = true
  approveForm.remark = ''
  approveVisible.value = true
}

async function submitApprove() {
  approving.value = true
  try {
    await approveReservation(currentRow.value.id, {
      approved: approveForm.approved,
      remark: approveForm.remark
    })
    ElMessage.success('审核完成')
    approveVisible.value = false
    loadList()
  } catch (e) { /* 拦截器已提示 */ } finally {
    approving.value = false
  }
}

onMounted(loadList)
</script>
