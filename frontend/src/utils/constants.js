/**
 * 系统枚举统一映射：value -> { label, tagType, color }
 * tagType: Element Plus el-tag 类型；color 为自定义色（无内置类型时使用）
 */

// 角色
export const ROLES = {
  STUDENT: '学生',
  LAB_ADMIN: '实验室管理员',
  SUPER_ADMIN: '超级管理员'
}

export const ROLE_OPTIONS = [
  { roleId: 3, roleCode: 'STUDENT', roleName: '学生' },
  { roleId: 2, roleCode: 'LAB_ADMIN', roleName: '实验室管理员' },
  { roleId: 1, roleCode: 'SUPER_ADMIN', roleName: '超级管理员' }
]

// 设备状态
export const DEVICE_STATUS = {
  IDLE: { label: '空闲', tagType: 'success' },
  BORROWED: { label: '借出', tagType: 'primary' },
  REPAIRING: { label: '维修中', tagType: 'warning' },
  SCRAPPED: { label: '报废', tagType: 'danger' },
  RESERVED: { label: '预留', color: '#722ed1' }
}

// 借用单状态
export const BORROW_STATUS = {
  PENDING: { label: '待审批', tagType: 'warning' },
  APPROVED: { label: '审批通过', tagType: 'primary' },
  REJECTED: { label: '已驳回', tagType: 'info' },
  BORROWED: { label: '已借出', tagType: 'success' },
  RETURNED: { label: '已归还', tagType: 'info' },
  OVERDUE: { label: '逾期未还', tagType: 'danger' }
}

// 借用状态进度（用于 el-steps）
export const BORROW_STEPS = ['PENDING', 'APPROVED', 'BORROWED', 'RETURNED']

// 归还状况
export const RETURN_CONDITION = {
  INTACT: '完好',
  DAMAGED: '损坏'
}

// 报修状态
export const REPAIR_STATUS = {
  PENDING: { label: '待处理', tagType: 'warning' },
  REPAIRING: { label: '维修中', tagType: 'primary' },
  FINISHED: { label: '维修完成', tagType: 'success' },
  SCRAPPED: { label: '报废', tagType: 'danger' }
}

// 预约状态
export const RESERVATION_STATUS = {
  PENDING: { label: '待审核', tagType: 'warning' },
  APPROVED: { label: '已通过', tagType: 'success' },
  REJECTED: { label: '已驳回', tagType: 'danger' },
  CANCELLED: { label: '已取消', tagType: 'info' },
  EXPIRED: { label: '已过期', tagType: 'info' }
}

// 用户启用状态
export const USER_STATUS = {
  0: { label: '禁用', tagType: 'danger' },
  1: { label: '启用', tagType: 'success' }
}

// 生成下拉选项：[{ value, label }]
export function toOptions(map) {
  return Object.keys(map).map((k) => ({
    value: k,
    label: typeof map[k] === 'string' ? map[k] : map[k].label
  }))
}

// 按 label 取 value（用于反查）
export function findByLabel(map, label) {
  const key = Object.keys(map).find((k) =>
    typeof map[k] === 'string' ? map[k] === label : map[k].label === label
  )
  return key || ''
}
