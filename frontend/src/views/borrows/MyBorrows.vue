<template>
  <div class="page-container">
    <div class="search-bar">
      <el-form :inline="true" :model="query">
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 150px">
            <el-option
              v-for="o in borrowStatusOptions"
              :key="o.value"
              :label="o.label"
              :value="o.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="doSearch">查询</el-button>
          <el-button :icon="Refresh" @click="resetSearch">重置</el-button>
          <el-button type="success" :icon="Plus" @click="openApply">申请借用</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="table-card">
      <div class="table-toolbar">
        <span style="font-weight: 600">我的借用记录（共 {{ total }} 条）</span>
        <el-button :icon="Refresh" @click="loadList">刷新</el-button>
      </div>

      <div class="table-scroll-wrapper">
        <el-table :data="list" v-loading="loading" border stripe>
          <el-table-column type="expand">
            <template #default="{ row }">
              <div style="padding: 12px 24px">
                <el-steps :active="stepIndex(row.status)" align-center finish-status="success">
                  <el-step title="待审批" />
                  <el-step title="审批通过" />
                  <el-step title="已借出" />
                  <el-step title="已归还" />
                </el-steps>
                <el-descriptions :column="2" border size="small" style="margin-top: 14px">
                  <el-descriptions-item label="用途">{{ row.purpose || '-' }}</el-descriptions-item>
                  <el-descriptions-item label="审批备注">
                    {{ pick(row, ['remark', 'approveRemark'], '-') }}
                  </el-descriptions-item>
                  <el-descriptions-item label="实际归还时间">
                    {{ formatDateTime(row.actualReturnTime) }}
                  </el-descriptions-item>
                  <el-descriptions-item label="赔偿金额">
                    {{ row.compensation != null ? formatMoney(row.compensation) : '-' }}
                  </el-descriptions-item>
                </el-descriptions>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="id" label="单号" width="80" />
          <el-table-column label="设备" min-width="160">
            <template #default="{ row }">
              {{ pick(row, ['deviceName', 'deviceCode'], '-') }}
              <span v-if="row.deviceCode" style="color: #909399; font-size: 12px">
                （{{ row.deviceCode }}）
              </span>
            </template>
          </el-table-column>
          <el-table-column label="开始时间" min-width="150">
            <template #default="{ row }">{{ formatDateTime(row.startTime) }}</template>
          </el-table-column>
          <el-table-column label="应还时间" min-width="150">
            <template #default="{ row }">{{ formatDateTime(row.dueTime) }}</template>
          </el-table-column>
          <el-table-column label="实际归还" min-width="150">
            <template #default="{ row }">{{ formatDateTime(row.actualReturnTime) }}</template>
          </el-table-column>
          <el-table-column label="状态" width="110" align="center">
            <template #default="{ row }">
              <StatusTag :map="BORROW_STATUS" :value="row.status" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="150" fixed="right">
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

    <!-- 借用申请 -->
    <el-dialog v-model="applyVisible" title="申请借用设备" width="520px" :close-on-click-modal="false">
      <el-form ref="applyFormRef" :model="applyForm" :rules="applyRules" label-width="90px">
        <el-form-item label="选择设备" prop="deviceId">
          <el-select
            v-model="applyForm.deviceId"
            placeholder="仅显示空闲设备"
            filterable
            style="width: 100%"
          >
            <el-option
              v-for="d in idleDevices"
              :key="d.id"
              :label="`${d.code} - ${d.name}`"
              :value="d.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="借用时间" prop="timeRange">
          <el-date-picker
            v-model="applyForm.timeRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="应还时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="借用用途" prop="purpose">
          <el-input v-model="applyForm.purpose" type="textarea" :rows="3" placeholder="请说明用途" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="applyVisible = false">取消</el-button>
        <el-button type="primary" :loading="applying" @click="submitApply">提交申请</el-button>
      </template>
    </el-dialog>

    <!-- 归还登记（B5） -->
    <el-dialog v-model="returnVisible" title="归还设备" width="480px" :close-on-click-modal="false">
      <el-form :model="returnForm" label-width="90px">
        <el-form-item label="借用单号">
          <span style="color: #606266">{{ returnRow && returnRow.recordNo }}</span>
        </el-form-item>
        <el-form-item label="归还状况">
          <el-radio-group v-model="returnForm.condition">
            <el-radio value="INTACT">完好</el-radio>
            <el-radio value="DAMAGED">损坏</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="returnForm.condition === 'DAMAGED'" label="赔偿金额">
          <el-input-number v-model="returnForm.compensation" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input
            v-model="returnForm.remark"
            type="textarea"
            :rows="2"
            placeholder="归还备注（损坏时请说明情况）"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="returnVisible = false">取消</el-button>
        <el-button type="warning" :loading="returning" @click="submitReturn">确认归还</el-button>
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
import { myBorrows, createBorrow, returnBorrow } from '@/api/borrow'
import { pageDevices } from '@/api/device'
import { BORROW_STATUS, BORROW_STEPS, toOptions } from '@/utils/constants'
import { formatDateTime, formatMoney, pick } from '@/utils/format'

const borrowStatusOptions = toOptions(BORROW_STATUS)

const query = reactive({ pageNum: 1, pageSize: 10, status: '' })
const list = ref([])
const total = ref(0)
const loading = ref(false)

async function loadList() {
  loading.value = true
  try {
    const data = await myBorrows({ ...query })
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

function stepIndex(status) {
  const i = BORROW_STEPS.indexOf(status)
  return i < 0 ? 0 : i
}

function rowActions(row) {
  const actions = [{ label: '详情', type: 'primary', click: () => showDetail(row) }]
  // B5：APPROVED（未取直接归还）与 BORROWED 状态可自助归还
  if (row.status === 'APPROVED' || row.status === 'BORROWED') {
    actions.push({ label: '归还', type: 'warning', click: () => openReturn(row) })
  }
  return actions
}

function showDetail(row) {
  ElMessage.info('展开行可查看进度与详情')
}

/* ===== 归还登记（B5） ===== */
const returnVisible = ref(false)
const returning = ref(false)
const returnRow = ref(null)
const returnForm = reactive({ condition: 'INTACT', remark: '', compensation: 0 })

function openReturn(row) {
  returnRow.value = row
  returnForm.condition = 'INTACT'
  returnForm.remark = ''
  returnForm.compensation = 0
  returnVisible.value = true
}

async function submitReturn() {
  returning.value = true
  try {
    const payload = { condition: returnForm.condition, remark: returnForm.remark }
    if (returnForm.condition === 'DAMAGED' && returnForm.compensation > 0) {
      payload.compensation = returnForm.compensation
    }
    await returnBorrow(returnRow.value.id, payload)
    ElMessage.success('归还成功')
    returnVisible.value = false
    loadList()
  } catch (e) { /* 拦截器已提示 */ } finally {
    returning.value = false
  }
}

/* ===== 申请借用 ===== */
const applyVisible = ref(false)
const applying = ref(false)
const applyFormRef = ref(null)
const idleDevices = ref([])
const applyForm = reactive({ deviceId: null, timeRange: null, purpose: '' })

const applyRules = {
  deviceId: [{ required: true, message: '请选择设备', trigger: 'change' }],
  timeRange: [{ required: true, message: '请选择借用时间', trigger: 'change' }],
  purpose: [{ required: true, message: '请填写用途', trigger: 'blur' }]
}

async function openApply() {
  applyForm.deviceId = null
  applyForm.timeRange = null
  applyForm.purpose = ''
  applyVisible.value = true
  try {
    const data = await pageDevices({ pageNum: 1, pageSize: 200, status: 'IDLE' })
    idleDevices.value = (data && data.records) || []
  } catch (e) { /* 拦截器已提示 */ }
}

async function submitApply() {
  try {
    await applyFormRef.value.validate()
  } catch (e) {
    return
  }
  const [startTime, dueTime] = applyForm.timeRange
  applying.value = true
  try {
    await createBorrow({
      deviceId: applyForm.deviceId,
      startTime,
      dueTime,
      purpose: applyForm.purpose
    })
    ElMessage.success('申请已提交，等待管理员审批')
    applyVisible.value = false
    loadList()
  } catch (e) { /* 拦截器已提示（时间/逾期限制等后端校验） */ } finally {
    applying.value = false
  }
}

onMounted(loadList)
</script>
