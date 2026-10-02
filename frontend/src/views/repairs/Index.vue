<template>
  <div class="page-container">
    <div class="search-bar">
      <el-form :inline="true" :model="query">
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 150px">
            <el-option
              v-for="o in repairStatusOptions"
              :key="o.value"
              :label="o.label"
              :value="o.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item v-if="!userStore.isStudent" label="设备">
          <el-select v-model="query.deviceId" placeholder="全部" clearable filterable style="width: 200px">
            <el-option v-for="d in devices" :key="d.id" :label="d.code + ' ' + d.name" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="doSearch">查询</el-button>
          <el-button :icon="Refresh" @click="resetSearch">重置</el-button>
          <el-button type="success" :icon="Plus" @click="openApply">提交报修</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="table-card">
      <div class="table-toolbar">
        <span style="font-weight: 600">
          {{ userStore.isStudent ? '我的报修' : '全部报修记录（共 ' + total + ' 条）' }}
        </span>
        <el-button :icon="Refresh" @click="loadList">刷新</el-button>
      </div>

      <div class="table-scroll-wrapper">
        <el-table :data="list" v-loading="loading" border stripe>
          <el-table-column prop="id" label="单号" width="80" />
          <el-table-column label="设备" min-width="150">
            <template #default="{ row }">{{ pick(row, ['deviceName', 'deviceCode'], '-') }}</template>
          </el-table-column>
          <el-table-column v-if="!userStore.isStudent" label="报修人" min-width="100">
            <template #default="{ row }">{{ pick(row, ['reporterName', 'reporter', 'username'], '-') }}</template>
          </el-table-column>
          <el-table-column label="故障描述" min-width="180" show-overflow-tooltip>
            <template #default="{ row }">{{ row.faultDesc || '-' }}</template>
          </el-table-column>
          <el-table-column label="故障图片" width="90" align="center">
            <template #default="{ row }">
              <el-image
                v-if="row.imageUrl"
                :src="row.imageUrl"
                :preview-src-list="[row.imageUrl]"
                preview-teleported
                style="width: 44px; height: 44px"
                fit="cover"
              />
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column label="报修时间" min-width="150">
            <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
          </el-table-column>
          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }">
              <StatusTag :map="REPAIR_STATUS" :value="row.status" />
            </template>
          </el-table-column>
          <el-table-column label="处理说明" min-width="140" show-overflow-tooltip>
            <template #default="{ row }">{{ row.handleRemark || '-' }}</template>
          </el-table-column>
          <el-table-column v-if="!userStore.isStudent" label="操作" width="100" fixed="right">
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

    <!-- 提交报修 -->
    <el-dialog v-model="applyVisible" title="提交故障报修" width="520px" :close-on-click-modal="false">
      <el-form ref="applyFormRef" :model="applyForm" :rules="applyRules" label-width="90px">
        <el-form-item label="选择设备" prop="deviceId">
          <el-select v-model="applyForm.deviceId" placeholder="请选择故障设备" filterable style="width: 100%">
            <el-option
              v-for="d in devices"
              :key="d.id"
              :label="`${d.code} - ${d.name}`"
              :value="d.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="故障描述" prop="faultDesc">
          <el-input v-model="applyForm.faultDesc" type="textarea" :rows="4" placeholder="请描述故障现象" />
        </el-form-item>
        <el-form-item label="故障图片">
          <el-upload
            :show-file-list="false"
            :http-request="doUploadImage"
            accept="image/*"
          >
            <el-image
              v-if="applyForm.imageUrl"
              :src="applyForm.imageUrl"
              style="width: 100px; height: 100px"
              fit="cover"
            />
            <el-button v-else :loading="uploading">上传图片</el-button>
          </el-upload>
          <div v-if="applyForm.imageUrl" style="margin-top: 6px">
            <el-button link type="danger" size="small" @click="applyForm.imageUrl = ''">移除图片</el-button>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="applyVisible = false">取消</el-button>
        <el-button type="primary" :loading="applying" @click="submitApply">提交报修</el-button>
      </template>
    </el-dialog>

    <!-- 管理员处理 -->
    <el-dialog v-model="handleVisible" title="处理报修" width="460px" :close-on-click-modal="false">
      <el-form label-width="90px">
        <el-form-item label="更新状态">
          <el-select v-model="handleForm.status" style="width: 100%">
            <el-option label="维修中" value="REPAIRING" />
            <el-option label="维修完成（设备恢复空闲）" value="FINISHED" />
            <el-option label="报废（设备置报废）" value="SCRAPPED" />
          </el-select>
        </el-form-item>
        <el-form-item label="处理说明">
          <el-input v-model="handleForm.handleRemark" type="textarea" :rows="3" placeholder="选填" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="handleVisible = false">取消</el-button>
        <el-button type="primary" :loading="handling" @click="submitHandle">确定</el-button>
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
import { createRepair, myRepairs, pageRepairs, updateRepair } from '@/api/repair'
import { pageDevices } from '@/api/device'
import { uploadFile } from '@/api/file'
import { REPAIR_STATUS, toOptions } from '@/utils/constants'
import { formatDateTime, pick } from '@/utils/format'

const userStore = useUserStore()
const repairStatusOptions = toOptions(REPAIR_STATUS)

const query = reactive({ pageNum: 1, pageSize: 10, status: '', deviceId: null })
const list = ref([])
const total = ref(0)
const loading = ref(false)
const devices = ref([])

async function loadDevices() {
  try {
    const data = await pageDevices({ pageNum: 1, pageSize: 500 })
    devices.value = (data && data.records) || []
  } catch (e) { /* ignore */ }
}

async function loadList() {
  loading.value = true
  try {
    const fetcher = userStore.isStudent ? myRepairs : pageRepairs
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
  query.deviceId = null
  doSearch()
}

function rowActions(row) {
  if (row.status === 'FINISHED' || row.status === 'SCRAPPED') {
    return [{ label: '-', click: () => {} }]
  }
  return [{ label: '处理', type: 'warning', click: () => openHandle(row) }]
}

/* ===== 提交报修 ===== */
const applyVisible = ref(false)
const applying = ref(false)
const uploading = ref(false)
const applyFormRef = ref(null)
const applyForm = reactive({ deviceId: null, faultDesc: '', imageUrl: '' })

const applyRules = {
  deviceId: [{ required: true, message: '请选择设备', trigger: 'change' }],
  faultDesc: [{ required: true, message: '请描述故障', trigger: 'blur' }]
}

async function doUploadImage(option) {
  uploading.value = true
  try {
    const data = await uploadFile(option.file)
    applyForm.imageUrl = data && data.url
    ElMessage.success('图片上传成功')
  } catch (e) { /* 拦截器已提示 */ } finally {
    uploading.value = false
  }
}

function openApply() {
  applyForm.deviceId = null
  applyForm.faultDesc = ''
  applyForm.imageUrl = ''
  applyVisible.value = true
}

async function submitApply() {
  try {
    await applyFormRef.value.validate()
  } catch (e) {
    return
  }
  applying.value = true
  try {
    const payload = { deviceId: applyForm.deviceId, faultDesc: applyForm.faultDesc }
    if (applyForm.imageUrl) payload.imageUrl = applyForm.imageUrl
    await createRepair(payload)
    ElMessage.success('报修已提交')
    applyVisible.value = false
    loadList()
  } catch (e) { /* 拦截器已提示 */ } finally {
    applying.value = false
  }
}

/* ===== 管理员处理 ===== */
const handleVisible = ref(false)
const handling = ref(false)
const currentRow = ref(null)
const handleForm = reactive({ status: 'REPAIRING', handleRemark: '' })

function openHandle(row) {
  currentRow.value = row
  handleForm.status = 'REPAIRING'
  handleForm.handleRemark = ''
  handleVisible.value = true
}

async function submitHandle() {
  handling.value = true
  try {
    await updateRepair(currentRow.value.id, {
      status: handleForm.status,
      handleRemark: handleForm.handleRemark
    })
    ElMessage.success('处理成功')
    handleVisible.value = false
    loadList()
  } catch (e) { /* 拦截器已提示 */ } finally {
    handling.value = false
  }
}

onMounted(() => {
  loadDevices()
  loadList()
})
</script>
