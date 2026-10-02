<template>
  <div class="page-container">
    <div class="search-bar">
      <el-form :inline="true" :model="query">
        <el-form-item label="关键词">
          <el-input
            v-model="query.keyword"
            placeholder="单号/申请人"
            clearable
            style="width: 160px"
            @keyup.enter="doSearch"
          />
        </el-form-item>
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
        <el-form-item label="实验室">
          <el-select v-model="query.labId" placeholder="全部" clearable style="width: 150px">
            <el-option v-for="l in labs" :key="l.id" :label="l.name" :value="l.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="doSearch">查询</el-button>
          <el-button :icon="Refresh" @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="table-card">
      <div class="table-toolbar">
        <span style="font-weight: 600">全部借用记录（共 {{ total }} 条）</span>
        <div>
          <el-button :icon="Refresh" @click="loadList">刷新</el-button>
          <el-button :icon="Download" @click="doExport">导出 Excel</el-button>
        </div>
      </div>

      <div class="table-scroll-wrapper">
        <el-table :data="list" v-loading="loading" border stripe>
          <el-table-column prop="id" label="单号" width="80" />
          <el-table-column label="申请人" min-width="100">
            <template #default="{ row }">{{ pick(row, ['applicantName', 'applicant', 'username'], '-') }}</template>
          </el-table-column>
          <el-table-column label="设备" min-width="150">
            <template #default="{ row }">
              {{ pick(row, ['deviceName', 'deviceCode'], '-') }}
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
          <el-table-column label="用途" min-width="120" show-overflow-tooltip>
            <template #default="{ row }">{{ row.purpose || '-' }}</template>
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

    <!-- 审批弹窗 -->
    <el-dialog v-model="approveVisible" title="借用审批" width="460px" :close-on-click-modal="false">
      <el-form label-width="80px">
        <el-form-item label="审批结果">
          <el-radio-group v-model="approveForm.approved">
            <el-radio :value="true">同意</el-radio>
            <el-radio :value="false">驳回</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="approveForm.approved ? '备注' : '驳回理由'">
          <el-input
            v-model="approveForm.remark"
            type="textarea"
            :rows="3"
            :placeholder="approveForm.approved ? '选填' : '请填写驳回理由'"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="approveVisible = false">取消</el-button>
        <el-button type="primary" :loading="approving" @click="submitApprove">确定</el-button>
      </template>
    </el-dialog>

    <!-- 归还登记弹窗 -->
    <el-dialog v-model="returnVisible" title="归还登记" width="460px" :close-on-click-modal="false">
      <el-form label-width="90px">
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
          <el-input v-model="returnForm.remark" type="textarea" :rows="3" placeholder="选填" />
        </el-form-item>
        <el-alert
          v-if="returnForm.condition === 'DAMAGED'"
          type="warning"
          :closable="false"
          title="损坏归还将自动创建报修单"
          style="margin-top: 4px"
        />
      </el-form>
      <template #footer>
        <el-button @click="returnVisible = false">取消</el-button>
        <el-button type="primary" :loading="returning" @click="submitReturn">确定归还</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Download } from '@element-plus/icons-vue'
import StatusTag from '@/components/StatusTag.vue'
import ActionButtons from '@/components/ActionButtons.vue'
import { pageBorrows, approveBorrow, pickupBorrow, returnBorrow, exportBorrows } from '@/api/borrow'
import { allLabs } from '@/api/lab'
import { BORROW_STATUS, toOptions } from '@/utils/constants'
import { formatDateTime, pick } from '@/utils/format'

const borrowStatusOptions = toOptions(BORROW_STATUS)

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  status: '',
  labId: null
})
const list = ref([])
const total = ref(0)
const loading = ref(false)
const labs = ref([])

async function loadLabs() {
  try {
    labs.value = (await allLabs()) || []
  } catch (e) { /* ignore */ }
}

async function loadList() {
  loading.value = true
  try {
    const data = await pageBorrows({ ...query })
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
  query.keyword = ''
  query.status = ''
  query.labId = null
  doSearch()
}

function doExport() {
  exportBorrows({
    keyword: query.keyword || undefined,
    status: query.status || undefined,
    labId: query.labId || undefined
  })
}

/* ===== 行操作 ===== */
function rowActions(row) {
  const acts = []
  if (row.status === 'PENDING') {
    acts.push({ label: '审批', type: 'warning', click: () => openApprove(row) })
  }
  if (row.status === 'APPROVED') {
    acts.push({
      label: '取件确认',
      type: 'success',
      confirm: '确认申请人已取到设备？确认后单据变为「已借出」。',
      click: () => doPickup(row)
    })
  }
  if (row.status === 'BORROWED' || row.status === 'OVERDUE') {
    acts.push({ label: '归还登记', type: 'primary', click: () => openReturn(row) })
  }
  if (!acts.length) {
    acts.push({ label: '-', click: () => {} })
  }
  return acts
}

/* ===== 审批 ===== */
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
  if (!approveForm.approved && !approveForm.remark) {
    ElMessage.warning('驳回时请填写理由')
    return
  }
  approving.value = true
  try {
    await approveBorrow(currentRow.value.id, {
      approved: approveForm.approved,
      remark: approveForm.remark
    })
    ElMessage.success(approveForm.approved ? '已同意' : '已驳回')
    approveVisible.value = false
    loadList()
  } catch (e) { /* 拦截器已提示（设备冲突等） */ } finally {
    approving.value = false
  }
}

/* ===== 取件 ===== */
async function doPickup(row) {
  try {
    await pickupBorrow(row.id)
    ElMessage.success('已确认取件')
    loadList()
  } catch (e) { /* 拦截器已提示 */ }
}

/* ===== 归还 ===== */
const returnVisible = ref(false)
const returning = ref(false)
const returnForm = reactive({ condition: 'INTACT', compensation: 0, remark: '' })

function openReturn(row) {
  currentRow.value = row
  returnForm.condition = 'INTACT'
  returnForm.compensation = 0
  returnForm.remark = ''
  returnVisible.value = true
}

async function submitReturn() {
  returning.value = true
  try {
    const payload = { condition: returnForm.condition, remark: returnForm.remark }
    if (returnForm.condition === 'DAMAGED') {
      payload.compensation = returnForm.compensation
    }
    await returnBorrow(currentRow.value.id, payload)
    ElMessage.success('归还登记成功')
    returnVisible.value = false
    loadList()
  } catch (e) { /* 拦截器已提示 */ } finally {
    returning.value = false
  }
}

onMounted(() => {
  loadLabs()
  loadList()
})
</script>
