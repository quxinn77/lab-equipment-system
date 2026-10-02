<template>
  <div class="page-container">
    <div class="search-bar">
      <el-form :inline="true" :model="query">
        <el-form-item label="关键词">
          <el-input
            v-model="query.keyword"
            placeholder="姓名/学号"
            clearable
            style="width: 160px"
            @keyup.enter="doSearch"
          />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="query.roleId" placeholder="全部" clearable style="width: 160px">
            <el-option
              v-for="r in roleOptions"
              :key="r.roleId"
              :label="r.roleName"
              :value="r.roleId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
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
        <span style="font-weight: 600">用户列表（共 {{ total }} 人）</span>
        <el-button type="primary" :icon="Plus" @click="openEdit(null)">新增用户</el-button>
      </div>

      <div class="table-scroll-wrapper">
        <el-table :data="list" v-loading="loading" border stripe>
          <el-table-column prop="username" label="账号/学号" min-width="110" />
          <el-table-column prop="realName" label="姓名" min-width="100" />
          <el-table-column label="角色" min-width="120">
            <template #default="{ row }">{{ roleText(row) }}</template>
          </el-table-column>
          <el-table-column prop="college" label="学院" min-width="140">
            <template #default="{ row }">{{ row.college || '-' }}</template>
          </el-table-column>
          <el-table-column prop="phone" label="手机号" min-width="120">
            <template #default="{ row }">{{ row.phone || '-' }}</template>
          </el-table-column>
          <el-table-column prop="email" label="邮箱" min-width="160" show-overflow-tooltip>
            <template #default="{ row }">{{ row.email || '-' }}</template>
          </el-table-column>
          <el-table-column label="启用状态" width="100" align="center">
            <template #default="{ row }">
              <el-switch
                :model-value="row.status === 1"
                @change="(v) => toggleStatus(row, v)"
              />
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

    <!-- 新增/编辑 -->
    <el-dialog v-model="editVisible" :title="form.id ? '编辑用户' : '新增用户'" width="500px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="账号" prop="username">
          <el-input v-model="form.username" :disabled="!!form.id" placeholder="学号或工号" />
        </el-form-item>
        <el-form-item v-if="!form.id" label="初始密码" prop="password">
          <el-input v-model="form.password" type="password" show-password />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="form.realName" />
        </el-form-item>
        <el-form-item label="角色" prop="roleId">
          <el-select v-model="form.roleId" style="width: 100%">
            <el-option
              v-for="r in roleOptions"
              :key="r.roleId"
              :label="r.roleName"
              :value="r.roleId"
            />
          </el-select>
        </el-form-item>
        <el-form-item v-if="!form.id" label="启用状态" prop="status">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="学院" prop="college">
          <el-input v-model="form.college" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <!-- 重置密码 -->
    <el-dialog v-model="resetVisible" title="重置密码" width="420px" :close-on-click-modal="false">
      <el-form :model="resetForm" label-width="90px">
        <el-form-item label="新密码">
          <el-input v-model="resetForm.newPassword" type="password" show-password placeholder="至少 6 位" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetVisible = false">取消</el-button>
        <el-button type="primary" :loading="resetting" @click="submitReset">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Refresh, Plus } from '@element-plus/icons-vue'
import ActionButtons from '@/components/ActionButtons.vue'
import {
  pageUsers,
  createUser,
  updateUser,
  deleteUser,
  resetPassword,
  updateUserStatus
} from '@/api/user'
import { useUserStore } from '@/stores/user'
import { ROLE_OPTIONS, ROLES } from '@/utils/constants'
import { pick } from '@/utils/format'

const userStore = useUserStore()
const roleOptions = ROLE_OPTIONS

const query = reactive({ pageNum: 1, pageSize: 10, keyword: '', roleId: null, status: null })
const list = ref([])
const total = ref(0)
const loading = ref(false)

function roleText(row) {
  if (row.roleName) return row.roleName
  if (row.roleCode && ROLES[row.roleCode]) return ROLES[row.roleCode]
  return pick(row, ['roleName', 'roleCode'], '-')
}

async function loadList() {
  loading.value = true
  try {
    const data = await pageUsers({ ...query })
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
  query.roleId = null
  query.status = null
  doSearch()
}

function rowActions(row) {
  return [
    { label: '编辑', type: 'primary', click: () => openEdit(row) },
    { label: '重置密码', click: () => openReset(row) },
    {
      label: '删除',
      type: 'danger',
      show: row.id !== (userStore.userInfo && userStore.userInfo.id),
      confirm: `确定删除用户「${row.realName || row.username}」吗？`,
      click: () => doDelete(row)
    }
  ]
}

async function toggleStatus(row, value) {
  const status = value ? 1 : 0
  try {
    await updateUserStatus(row.id, status)
    row.status = status
    ElMessage.success(status === 1 ? '已启用' : '已禁用')
  } catch (e) { /* 拦截器已提示 */ }
}

async function doDelete(row) {
  try {
    await deleteUser(row.id)
    ElMessage.success('删除成功')
    loadList()
  } catch (e) { /* 拦截器已提示（不可删除自己） */ }
}

/* ===== 新增/编辑 ===== */
const editVisible = ref(false)
const saving = ref(false)
const formRef = ref(null)
const defaultForm = {
  id: null,
  username: '',
  password: '',
  realName: '',
  roleId: 3,
  status: 1,
  college: '',
  phone: '',
  email: ''
}
const form = reactive({ ...defaultForm })

const rules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入初始密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' }
  ],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  roleId: [{ required: true, message: '请选择角色', trigger: 'change' }]
}

function openEdit(row) {
  Object.assign(form, defaultForm)
  if (row) {
    Object.assign(form, {
      id: row.id,
      username: row.username,
      password: '',
      realName: row.realName || '',
      roleId: row.roleId ?? 3,
      status: row.status ?? 1,
      college: row.college || '',
      phone: row.phone || '',
      email: row.email || ''
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
    if (form.id) {
      const { id, password, ...payload } = form
      await updateUser(id, payload)
      ElMessage.success('修改成功')
    } else {
      await createUser({ ...form })
      ElMessage.success('新增成功')
    }
    editVisible.value = false
    loadList()
  } catch (e) { /* 拦截器已提示 */ } finally {
    saving.value = false
  }
}

/* ===== 重置密码 ===== */
const resetVisible = ref(false)
const resetting = ref(false)
const resetRow = ref(null)
const resetForm = reactive({ newPassword: '' })

function openReset(row) {
  resetRow.value = row
  resetForm.newPassword = ''
  resetVisible.value = true
}

async function submitReset() {
  if (!resetForm.newPassword || resetForm.newPassword.length < 6) {
    ElMessage.warning('请输入至少 6 位的新密码')
    return
  }
  resetting.value = true
  try {
    await resetPassword(resetRow.value.id, resetForm.newPassword)
    ElMessage.success('密码已重置')
    resetVisible.value = false
  } catch (e) { /* 拦截器已提示 */ } finally {
    resetting.value = false
  }
}

onMounted(loadList)
</script>
