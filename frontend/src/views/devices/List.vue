<template>
  <div class="page-container">
    <!-- 检索栏 -->
    <div class="search-bar">
      <el-form :inline="true" :model="query">
        <el-form-item label="关键词">
          <el-input
            v-model="query.keyword"
            placeholder="设备名称/编号"
            clearable
            style="width: 180px"
            @keyup.enter="doSearch"
          />
        </el-form-item>
        <el-form-item label="实验室">
          <el-select v-model="query.labId" placeholder="全部" clearable style="width: 160px">
            <el-option v-for="l in labs" :key="l.id" :label="l.name" :value="l.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="query.categoryId" placeholder="全部" clearable style="width: 140px">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 130px">
            <el-option
              v-for="o in deviceStatusOptions"
              :key="o.value"
              :label="o.label"
              :value="o.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="doSearch">查询</el-button>
          <el-button :icon="Refresh" @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 表格 -->
    <div class="table-card">
      <div class="table-toolbar">
        <span style="font-weight: 600">设备列表（共 {{ total }} 台）</span>
        <div>
          <el-button :icon="Refresh" @click="loadList">刷新</el-button>
          <el-button :icon="Download" @click="doExport">导出</el-button>
          <template v-if="userStore.isAdmin">
            <el-button :icon="Upload" @click="importVisible = true">导入</el-button>
            <el-button type="primary" :icon="Plus" @click="openEdit(null)">新增设备</el-button>
          </template>
        </div>
      </div>

      <div class="table-scroll-wrapper">
        <el-table :data="list" v-loading="loading" border stripe>
          <el-table-column prop="code" label="设备编号" min-width="110" />
          <el-table-column prop="name" label="设备名称" min-width="140" show-overflow-tooltip />
          <el-table-column prop="model" label="型号" min-width="100" show-overflow-tooltip />
          <el-table-column prop="brand" label="品牌" min-width="90" />
          <el-table-column label="分类" min-width="100">
            <template #default="{ row }">{{ row.categoryName || '-' }}</template>
          </el-table-column>
          <el-table-column label="实验室" min-width="120">
            <template #default="{ row }">{{ row.labName || '-' }}</template>
          </el-table-column>
          <el-table-column label="购置日期" min-width="105">
            <template #default="{ row }">{{ formatDate(row.purchaseDate) }}</template>
          </el-table-column>
          <el-table-column label="原值" min-width="100" align="right">
            <template #default="{ row }">{{ formatMoney(row.originalValue) }}</template>
          </el-table-column>
          <el-table-column label="状态" width="90" align="center">
            <template #default="{ row }">
              <StatusTag :map="DEVICE_STATUS" :value="row.status" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="170" fixed="right">
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

    <!-- 新增/编辑抽屉 -->
    <el-drawer v-model="editVisible" :title="form.id ? '编辑设备' : '新增设备'" size="480px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="设备编号" prop="code">
          <el-input v-model="form.code" placeholder="唯一，重复时后端校验报错" />
        </el-form-item>
        <el-form-item label="设备名称" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="型号" prop="model">
          <el-input v-model="form.model" />
        </el-form-item>
        <el-form-item label="规格" prop="spec">
          <el-input v-model="form.spec" />
        </el-form-item>
        <el-form-item label="品牌" prop="brand">
          <el-input v-model="form.brand" />
        </el-form-item>
        <el-form-item label="分类" prop="categoryId">
          <el-select v-model="form.categoryId" placeholder="请选择" style="width: 100%">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="实验室" prop="labId">
          <el-select v-model="form.labId" placeholder="请选择" style="width: 100%">
            <el-option v-for="l in labs" :key="l.id" :label="l.name" :value="l.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="购置日期" prop="purchaseDate">
          <el-date-picker
            v-model="form.purchaseDate"
            type="date"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="原值(元)" prop="originalValue">
          <el-input-number v-model="form.originalValue" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="图片URL" prop="imageUrl">
          <el-input v-model="form.imageUrl" placeholder="可通过报修图片上传后粘贴地址，可选" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-drawer>

    <!-- 详情抽屉 -->
    <el-drawer v-model="detailVisible" title="设备详情" size="560px">
      <template v-if="detail">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="设备编号">{{ detail.code }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <StatusTag :map="DEVICE_STATUS" :value="detail.status" />
          </el-descriptions-item>
          <el-descriptions-item label="名称" :span="2">{{ detail.name }}</el-descriptions-item>
          <el-descriptions-item label="型号">{{ detail.model || '-' }}</el-descriptions-item>
          <el-descriptions-item label="规格">{{ detail.spec || '-' }}</el-descriptions-item>
          <el-descriptions-item label="品牌">{{ detail.brand || '-' }}</el-descriptions-item>
          <el-descriptions-item label="分类">{{ detail.categoryName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="实验室">{{ detail.labName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="购置日期">{{ formatDate(detail.purchaseDate) }}</el-descriptions-item>
          <el-descriptions-item label="原值">{{ formatMoney(detail.originalValue) }}</el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ detail.remark || '-' }}</el-descriptions-item>
        </el-descriptions>

        <el-tabs style="margin-top: 16px">
          <el-tab-pane label="变更日志">
            <el-table :data="detailLogs" size="small" border>
              <el-table-column label="时间" min-width="150">
                <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
              </el-table-column>
              <el-table-column label="操作人" min-width="90">
                <template #default="{ row }">{{ pick(row, ['operatorName', 'operator', 'username'], '-') }}</template>
              </el-table-column>
              <el-table-column label="动作" min-width="80">
                <template #default="{ row }">{{ row.action || '-' }}</template>
              </el-table-column>
              <el-table-column label="说明" min-width="160">
                <template #default="{ row }">{{ pick(row, ['detail', 'content', 'remark', 'description'], '-') }}</template>
              </el-table-column>
            </el-table>
          </el-tab-pane>
          <el-tab-pane label="设备二维码">
            <div style="text-align: center; padding: 12px">
              <!-- GET 接口无法携带请求头，token 作为 query 参数传递（后端拦截器未支持时降级不显示） -->
              <img
                v-if="detail.id"
                :src="qrcodeUrl"
                alt="设备二维码"
                style="max-width: 260px"
                @error="qrError = true"
              />
              <el-alert
                v-if="qrError"
                type="warning"
                :closable="false"
                title="二维码加载失败（后端可能暂未支持 token query 参数）"
                style="margin-top: 10px"
              />
            </div>
          </el-tab-pane>
        </el-tabs>
      </template>
    </el-drawer>

    <!-- Excel 导入 -->
    <el-dialog v-model="importVisible" title="Excel 批量导入" width="460px">
      <el-alert
        type="info"
        :closable="false"
        style="margin-bottom: 14px"
        title="模板列：设备编号、设备名称、型号、规格、品牌、分类名称、实验室编号、购置日期、原值"
      />
      <el-upload
        drag
        action="/api/devices/import"
        :headers="uploadHeaders()"
        :limit="1"
        :on-success="onImportSuccess"
        :on-error="onImportError"
        accept=".xls,.xlsx"
      >
        <el-icon size="40"><UploadFilled /></el-icon>
        <div>拖拽文件到此处，或点击选择 Excel 文件</div>
      </el-upload>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Download, Upload, Plus } from '@element-plus/icons-vue'
import StatusTag from '@/components/StatusTag.vue'
import ActionButtons from '@/components/ActionButtons.vue'
import { useUserStore } from '@/stores/user'
import {
  pageDevices,
  getDevice,
  createDevice,
  updateDevice,
  deleteDevice,
  getDeviceLogs,
  exportDevices,
  deviceQrcodeUrl
} from '@/api/device'
import { listCategories } from '@/api/category'
import { allLabs } from '@/api/lab'
import { uploadHeaders } from '@/api/file'
import { DEVICE_STATUS, toOptions } from '@/utils/constants'
import { formatDate, formatDateTime, formatMoney, pick } from '@/utils/format'

const userStore = useUserStore()
const deviceStatusOptions = toOptions(DEVICE_STATUS)

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  labId: null,
  categoryId: null,
  status: ''
})
const list = ref([])
const total = ref(0)
const loading = ref(false)

const labs = ref([])
const categories = ref([])

async function loadOptions() {
  try {
    labs.value = (await allLabs()) || []
  } catch (e) { /* ignore */ }
  try {
    const cs = await listCategories()
    categories.value = Array.isArray(cs) ? cs : []
  } catch (e) { /* ignore */ }
}

async function loadList() {
  loading.value = true
  try {
    const data = await pageDevices({ ...query })
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
  query.labId = null
  query.categoryId = null
  query.status = ''
  doSearch()
}

function doExport() {
  exportDevices({
    keyword: query.keyword || undefined,
    labId: query.labId || undefined,
    categoryId: query.categoryId || undefined,
    status: query.status || undefined
  })
}

/* ===== 行操作 ===== */
function rowActions(row) {
  const acts = [
    { label: '详情', type: 'primary', click: () => openDetail(row) }
  ]
  if (userStore.isAdmin) {
    acts.push({ label: '编辑', click: () => openEdit(row) })
    acts.push({
      label: '删除',
      type: 'danger',
      confirm: `确定删除设备「${row.name}」吗？借出状态设备后端将禁止删除。`,
      click: () => doDelete(row)
    })
  }
  return acts
}

async function doDelete(row) {
  try {
    await deleteDevice(row.id)
    ElMessage.success('删除成功')
    loadList()
  } catch (e) { /* 拦截器已提示 */ }
}

/* ===== 新增/编辑 ===== */
const editVisible = ref(false)
const saving = ref(false)
const formRef = ref(null)
const defaultForm = {
  id: null,
  code: '',
  name: '',
  model: '',
  spec: '',
  brand: '',
  categoryId: null,
  labId: null,
  purchaseDate: '',
  originalValue: 0,
  imageUrl: '',
  remark: ''
}
const form = reactive({ ...defaultForm })

const rules = {
  code: [{ required: true, message: '请输入设备编号', trigger: 'blur' }],
  name: [{ required: true, message: '请输入设备名称', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  labId: [{ required: true, message: '请选择实验室', trigger: 'change' }]
}

function openEdit(row) {
  Object.assign(form, defaultForm)
  if (row) {
    Object.assign(form, {
      id: row.id,
      code: row.code,
      name: row.name,
      model: row.model || '',
      spec: row.spec || '',
      brand: row.brand || '',
      categoryId: row.categoryId ?? null,
      labId: row.labId ?? null,
      purchaseDate: row.purchaseDate ? String(row.purchaseDate).slice(0, 10) : '',
      originalValue: row.originalValue ?? 0,
      imageUrl: row.imageUrl || '',
      remark: row.remark || ''
    })
  }
  editVisible.value = true
}

async function save() {
  try {
    await formRef.value.validate()
  } catch (e) {
    return
  }
  saving.value = true
  try {
    const payload = { ...form }
    delete payload.id
    if (form.id) {
      await updateDevice(form.id, payload)
      ElMessage.success('修改成功')
    } else {
      await createDevice(payload)
      ElMessage.success('新增成功')
    }
    editVisible.value = false
    loadList()
  } catch (e) { /* 拦截器已提示（编号唯一等后端校验） */ } finally {
    saving.value = false
  }
}

/* ===== 详情 ===== */
const detailVisible = ref(false)
const detail = ref(null)
const detailLogs = ref([])
const qrError = ref(false)

const qrcodeUrl = ref('')

async function openDetail(row) {
  qrError.value = false
  detailVisible.value = true
  detail.value = null
  detailLogs.value = []
  try {
    detail.value = await getDevice(row.id)
    qrcodeUrl.value = deviceQrcodeUrl(row.id)
  } catch (e) { /* 拦截器已提示 */ }
  try {
    const logs = await getDeviceLogs(row.id)
    detailLogs.value = Array.isArray(logs) ? logs : (logs && logs.records) || []
  } catch (e) { /* ignore */ }
}

/* ===== 导入 ===== */
const importVisible = ref(false)

function onImportSuccess(res) {
  if (res && res.code === 200) {
    ElMessage.success('导入成功：' + (res.msg || '已处理'))
    importVisible.value = false
    loadList()
  } else {
    ElMessage.error((res && res.msg) || '导入失败')
  }
}

function onImportError() {
  ElMessage.error('上传失败，请检查文件格式')
}

onMounted(() => {
  loadOptions()
  loadList()
})
</script>
