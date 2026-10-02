<template>
  <div class="page-container">
    <div class="search-bar">
      <el-form :inline="true" :model="query">
        <el-form-item label="关键词">
          <el-input
            v-model="query.keyword"
            placeholder="编号/名称"
            clearable
            style="width: 180px"
            @keyup.enter="doSearch"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="doSearch">查询</el-button>
          <el-button :icon="Refresh" @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="table-card">
      <div class="table-toolbar">
        <span style="font-weight: 600">实验室列表（共 {{ total }} 个）</span>
        <el-button type="primary" :icon="Plus" @click="openEdit(null)">新增实验室</el-button>
      </div>

      <div class="table-scroll-wrapper">
        <el-table :data="list" v-loading="loading" border stripe>
          <el-table-column prop="code" label="实验室编号" min-width="110" />
          <el-table-column prop="name" label="名称" min-width="150" />
          <el-table-column prop="location" label="位置" min-width="130">
            <template #default="{ row }">{{ row.location || '-' }}</template>
          </el-table-column>
          <el-table-column prop="manager" label="负责人" min-width="100">
            <template #default="{ row }">{{ row.manager || '-' }}</template>
          </el-table-column>
          <el-table-column prop="description" label="描述" min-width="180" show-overflow-tooltip>
            <template #default="{ row }">{{ row.description || '-' }}</template>
          </el-table-column>
          <el-table-column label="操作" width="140" fixed="right">
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

    <el-dialog v-model="editVisible" :title="form.id ? '编辑实验室' : '新增实验室'" width="480px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="编号" prop="code">
          <el-input v-model="form.code" placeholder="如 LAB-101" />
        </el-form-item>
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="位置" prop="location">
          <el-input v-model="form.location" />
        </el-form-item>
        <el-form-item label="负责人" prop="manager">
          <el-input v-model="form.manager" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Refresh, Plus } from '@element-plus/icons-vue'
import ActionButtons from '@/components/ActionButtons.vue'
import { pageLabs, createLab, updateLab, deleteLab } from '@/api/lab'

const query = reactive({ pageNum: 1, pageSize: 10, keyword: '' })
const list = ref([])
const total = ref(0)
const loading = ref(false)

async function loadList() {
  loading.value = true
  try {
    const data = await pageLabs({ ...query })
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
  doSearch()
}

function rowActions(row) {
  return [
    { label: '编辑', type: 'primary', click: () => openEdit(row) },
    {
      label: '删除',
      type: 'danger',
      confirm: `确定删除实验室「${row.name}」吗？有关联设备时后端会禁止删除。`,
      click: () => doDelete(row)
    }
  ]
}

async function doDelete(row) {
  try {
    await deleteLab(row.id)
    ElMessage.success('删除成功')
    loadList()
  } catch (e) { /* 拦截器已提示（有关联设备禁止删除） */ }
}

const editVisible = ref(false)
const saving = ref(false)
const formRef = ref(null)
const defaultForm = { id: null, code: '', name: '', location: '', manager: '', description: '' }
const form = reactive({ ...defaultForm })

const rules = {
  code: [{ required: true, message: '请输入编号', trigger: 'blur' }],
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }]
}

function openEdit(row) {
  Object.assign(form, defaultForm)
  if (row) {
    Object.assign(form, {
      id: row.id,
      code: row.code,
      name: row.name,
      location: row.location || '',
      manager: row.manager || '',
      description: row.description || ''
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
    const { id, ...payload } = form
    if (id) {
      await updateLab(id, payload)
      ElMessage.success('修改成功')
    } else {
      await createLab(payload)
      ElMessage.success('新增成功')
    }
    editVisible.value = false
    loadList()
  } catch (e) { /* 拦截器已提示 */ } finally {
    saving.value = false
  }
}

onMounted(loadList)
</script>
