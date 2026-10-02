<template>
  <div class="page-container">
    <div class="table-card" style="max-width: 720px">
      <div class="table-toolbar">
        <span style="font-weight: 600">个人信息</span>
        <el-button type="primary" :loading="saving" @click="saveInfo">保存修改</el-button>
      </div>

      <el-form
        ref="infoFormRef"
        :model="infoForm"
        :rules="infoRules"
        label-width="90px"
        style="max-width: 480px; margin: 10px auto"
      >
        <el-form-item label="账号">
          <el-input :model-value="infoForm.username" disabled />
        </el-form-item>
        <el-form-item label="角色">
          <el-input :model-value="roleText" disabled />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="infoForm.realName" />
        </el-form-item>
        <el-form-item label="学院" prop="college">
          <el-input v-model="infoForm.college" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="infoForm.phone" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="infoForm.email" />
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getInfo, updatePassword } from '@/api/auth'
import { updateUser } from '@/api/user'
import { useUserStore } from '@/stores/user'
import { ROLES } from '@/utils/constants'

const userStore = useUserStore()

const infoFormRef = ref(null)
const infoForm = reactive({
  id: null,
  username: '',
  roleCode: '',
  realName: '',
  college: '',
  phone: '',
  email: ''
})

const infoRules = {
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  phone: [{ pattern: /^1[0-9]{10}$/, message: '手机号格式不正确', trigger: 'blur' }],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }]
}

const roleText = computed(() => ROLES[infoForm.roleCode] || userStore.roleName || '-')

const saving = ref(false)

async function loadInfo() {
  try {
    const data = await getInfo()
    if (data) {
      Object.assign(infoForm, {
        id: data.id,
        username: data.username || '',
        roleCode: data.roleCode || '',
        realName: data.realName || '',
        college: data.college || '',
        phone: data.phone || '',
        email: data.email || ''
      })
      userStore.setUserInfo({
        id: data.id,
        username: data.username,
        realName: data.realName,
        roleId: data.roleId,
        roleCode: data.roleCode,
        roleName: data.roleName
      })
    }
  } catch (e) { /* 拦截器已提示 */ }
}

async function saveInfo() {
  try {
    await infoFormRef.value.validate()
  } catch (e) {
    return
  }
  saving.value = true
  try {
    await updateUser(infoForm.id, {
      username: infoForm.username,
      realName: infoForm.realName,
      college: infoForm.college,
      phone: infoForm.phone,
      email: infoForm.email
    })
    ElMessage.success('个人信息已更新')
    userStore.setUserInfo({
      ...userStore.userInfo,
      realName: infoForm.realName
    })
  } catch (e) { /* 拦截器已提示 */ } finally {
    saving.value = false
  }
}

onMounted(loadInfo)
</script>
