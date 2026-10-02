<template>
  <div class="page-container">
    <div class="table-card">
      <div class="table-toolbar">
        <span style="font-weight: 600">设备分类（共 {{ list.length }} 个）</span>
        <div>
          <el-button :icon="Refresh" @click="loadList">刷新</el-button>
          <el-button type="primary" :icon="Plus" @click="openEdit(null)">新增分类</el-button>
        </div>
      </div>

      <div class="table-scroll-wrapper">
        <el-table :data="list" v-loading="loading" border stripe>
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="name" label="分类名称" min-width="180" />
          <el-table-column prop="remark" label="备注" min-width="240">
            <template #default="{ row }">{{ row.remark || '-' }}</template>
          </el-table-column>
          <el-table-column label="操作" width="140" fixed="right">
            <template #default="{ row }">
              <ActionButtons :actions="rowActions(row)" />
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <el-dialog v-model="editVisible" :title="form.id ? '编辑分类' : '新增分类'" width="440px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="3" />
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
import { Refresh, Plus } from '@element-plus/icons-vue'
import ActionButtons from '@/components/ActionButtons.vue'
import { listCategories, createCategory, updateCategory, deleteCategory } from '@/api/category'

const list = ref([])
const loading = ref(false)

async function loadList() {
  loading.value = true
  try {
    const data = await listCategories()
    list.value = Array.isArray(data) ? data : []
  } catch (e) { /* 拦截器已提示 */ } finally {
    loading.value = false
  }
}

function rowActions(row) {
  return [
    { label: '编辑', type: 'primary', click: () => openEdit(row) },
    {
      label: '删除',
      type: 'danger',
      confirm: `确定删除分类「${row.name}」吗？有关联设备时后端会禁止删除。`,
      click: () => doDelete(row)
    }
  ]
}

async function doDelete(row) {
  try {
    await deleteCategory(row.id)
    ElMessage.success('删除成功')
    loadList()
  } catch (e) { /* 拦截器已提示 */ }
}

const editVisible = ref(false)
const saving = ref(false)
const formRef = ref(null)
const defaultForm = { id: null, name: '', remark: '' }
const form = reactive({ ...defaultForm })

const rules = {
  name: [{ required: true, message: '请输入分类名称', trigger: 'blur' }]
}

function openEdit(row) {
  Object.assign(form, defaultForm)
  if (row) {
    Object.assign(form, { id: row.id, name: row.name, remark: row.remark || '' })
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
      await updateCategory(id, payload)
      ElMessage.success('修改成功')
    } else {
      await createCategory(payload)
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
