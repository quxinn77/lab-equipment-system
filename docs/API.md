# 实验室设备借用管理系统 — API 契约（v1.0）

统一约定：
- Base URL：`/api`
- 认证：登录后除 `/api/auth/login`、`/api/auth/register`、`/api/files/**`(GET) 外均需请求头 `Authorization: Bearer <token>`（JWT）
- 响应统一格式：`{ "code": 200, "msg": "success", "data": ... }`；错误时 code 为非 200（401 未登录 / 403 无权限 / 400 业务校验失败 / 500 系统错误），业务校验失败返回 HTTP 200 + code=400 + msg 说明
- 分页请求参数：`pageNum`（默认1）、`pageSize`（默认10）；分页响应：`{ "total": n, "records": [...] }`
- 时间格式：`yyyy-MM-dd HH:mm:ss`（后端 Jackson 全局配置）

## 枚举

- 角色编码：`STUDENT`（学生）、`LAB_ADMIN`（实验室管理员）、`SUPER_ADMIN`（超级管理员）
- 设备状态：`IDLE` 空闲 / `BORROWED` 借出 / `REPAIRING` 维修中 / `SCRAPPED` 报废 / `RESERVED` 预留
- 借用单状态：`PENDING` 待审批 / `APPROVED` 审批通过(未取) / `REJECTED` 已驳回 / `BORROWED` 已借出 / `RETURNED` 已归还 / `OVERDUE` 逾期未还
- 归还状况：`INTACT` 完好 / `DAMAGED` 损坏
- 报修状态：`PENDING` 待处理 / `REPAIRING` 维修中 / `FINISHED` 维修完成 / `SCRAPPED` 报废
- 预约状态：`PENDING` / `APPROVED` / `REJECTED` / `CANCELLED` / `EXPIRED`

## 1. 认证 / 注册

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| POST | /api/auth/login | 公开 | 登录 `{username,password}` → `{token, user:{id,username,realName,roleId,roleCode,roleName}}`；成功/失败写登录日志 |
| POST | /api/auth/register | 公开 | 学生自助注册 `{username,password,realName,college,phone,email}`，默认 STUDENT 角色 |
| GET | /api/auth/info | 登录 | 当前用户详情 |
| PUT | /api/auth/password | 登录 | 修改密码 `{oldPassword,newPassword}` |

## 2. 用户管理（SUPER_ADMIN；LAB_ADMIN 可查看）

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | /api/users | 分页 `keyword(姓名/学号模糊)`、`roleId`、`status` |
| POST | /api/users | 新增用户 `{username,password,realName,college,phone,email,roleId,status}` |
| PUT | /api/users/{id} | 编辑（不含密码） |
| DELETE | /api/users/{id} | 删除（不可删除自己） |
| PUT | /api/users/{id}/reset-password | 管理员重置密码 `{newPassword}` |
| PUT | /api/users/{id}/status | 启用/禁用 `{status:0/1}` |

## 3. 实验室管理（LAB_ADMIN / SUPER_ADMIN）

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | /api/labs | 分页 `keyword`；GET /api/labs/all 返回全部启用实验室（下拉用） |
| POST | /api/labs | 新增 `{code,name,location,manager,description}` |
| PUT | /api/labs/{id} | 编辑 |
| DELETE | /api/labs/{id} | 删除（有关联设备时禁止删除，提示先转移设备） |

## 4. 设备分类

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | /api/categories | 全量列表 |
| POST | /api/categories | 新增 `{name,remark}` |
| PUT | /api/categories/{id} | 编辑 |
| DELETE | /api/categories/{id} | 删除（有关联设备禁止删除） |

## 5. 设备管理（核心）

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| GET | /api/devices | 登录 | 分页检索：`keyword(名称/编号模糊)`、`labId`、`categoryId`、`status`；学生仅能查看 |
| GET | /api/devices/{id} | 登录 | 详情（含实验室名称、分类名称冗余字段 labName/categoryName） |
| POST | /api/devices | 管理员 | 新增 `{code,name,model,spec,brand,categoryId,labId,purchaseDate,originalValue,imageUrl,remark}`；编号唯一校验 |
| PUT | /api/devices/{id} | 管理员 | 编辑（状态不允许在此接口直接改） |
| DELETE | /api/devices/{id} | 管理员 | 删除（BORROWED 状态禁止删除）；写设备变更日志 |
| POST | /api/devices/import | 管理员 | Excel 批量导入（multipart file），模板列：设备编号、设备名称、型号、规格、品牌、分类名称、实验室编号、购置日期、原值 |
| GET | /api/devices/export | 登录 | 导出设备台账 Excel（按当前筛选条件） |
| GET | /api/devices/{id}/logs | 登录 | 该设备的变更日志列表 |
| GET | /api/devices/qrcode/{id} | 登录 | 生成设备二维码图片（PNG） |

所有设备增删改写 `device_log`（操作人、时间、动作、新旧状态）。

## 6. 借用 / 归还（业务核心）

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| POST | /api/borrows | 学生 | 提交借用申请 `{deviceId,startTime,dueTime,purpose}`；校验：设备状态必须 IDLE、起止时间合法（开始<截止 且 开始>=当前）、当前用户无 OVERDUE 记录（读配置 overdue.limit.apply）、逾期次数未超上限（overdue.max.times）|
| GET | /api/borrows/my | 学生 | 我的借用记录分页 `status` |
| GET | /api/borrows | 管理员 | 全部借用记录分页：`keyword(编号/申请人)`、`status`、`deviceId`、`labId` |
| PUT | /api/borrows/{id}/approve | 管理员 | 审批 `{approved:boolean, remark}`；同意→若设备仍为 IDLE 则状态变 RESERVED、单据 APPROVED，向申请人发站内信；设备已被处理则提示冲突；驳回→单据 REJECTED 记录理由，发站内信 |
| PUT | /api/borrows/{id}/pickup | 管理员 | 确认取件：APPROVED→BORROWED，设备状态 RESERVED→BORROWED |
| PUT | /api/borrows/{id}/return | 管理员 | 归还登记 `{condition:INTACT/DAMAGED, remark, compensation?}`；单据 RETURNED + actualReturnTime；设备 IDLE；若 DAMAGED 自动创建报修单并关联，记录赔偿金额 |
| GET | /api/borrows/export | 管理员 | 按筛选条件导出借用记录 Excel |

状态机：PENDING →(approve 同意)→ APPROVED →(pickup)→ BORROWED →(return)→ RETURNED；PENDING →(驳回)→ REJECTED；BORROWED →(定时任务发现超期)→ OVERDUE →(return)→ RETURNED。

## 7. 预约模块

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| POST | /api/reservations | 学生 | `{deviceId,startTime,endTime,remark}`；同设备时段冲突校验（与 APPROVED/PENDING 的预约相交则拒绝）；设备必须 IDLE/RESERVED |
| GET | /api/reservations/my | 学生 | 我的预约 |
| GET | /api/reservations | 管理员 | 全部预约 `status` |
| PUT | /api/reservations/{id}/approve | 管理员 | `{approved, remark}`；同意后可由管理员把设备置 RESERVED 并把关联借用审批流程走 pickup |
| PUT | /api/reservations/{id}/cancel | 学生 | 取消自己的预约 |
| 定时 | - | - | 预约结束时间已过仍未取 → EXPIRED，释放设备为 IDLE |

## 8. 故障报修

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| POST | /api/repairs | 登录 | `{deviceId,faultDesc,imageUrl?}`；设备状态置 REPAIRING，写设备日志 |
| GET | /api/repairs/my | 登录 | 我提交的报修 |
| GET | /api/repairs | 管理员 | 全部报修分页 `status`、`deviceId` |
| PUT | /api/repairs/{id} | 管理员 | 更新 `{status, handleRemark}`；REPAIRING/SCRAPPED/FINISHED；FINISHED→设备回 IDLE + finishTime；SCRAPPED→设备置 SCRAPPED；站内信通知报修人 |

## 9. 站内消息

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | /api/messages | 我的消息分页 `isRead` |
| GET | /api/messages/unread-count | 未读数量 |
| PUT | /api/messages/{id}/read | 标记已读 |
| PUT | /api/messages/read-all | 全部已读 |

## 10. 统计报表

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| GET | /api/stats/overview | 登录 | 首页看板：设备各状态数量、设备总数、本月借用数、待审批数、逾期数、待处理报修数 |
| GET | /api/stats/device-by-status | 登录 | `[{name:'空闲',value:12},...]` |
| GET | /api/stats/device-by-lab | 登录 | 各实验室设备数量 `[{name,value}]` |
| GET | /api/stats/borrow-monthly?months=6 | 管理员 | 近 N 月借用次数 `[{month,count}]` |
| GET | /api/stats/top-devices?limit=10 | 管理员 | 热门设备排行 `[{name,count}]` |
| GET | /api/stats/overdue | 管理员 | 逾期统计 |
| GET | /api/stats/repair-summary | 管理员 | 报修统计（各状态数量 + 高频故障设备） |
| GET | /api/stats/user-rank?limit=10 | 管理员 | 学生借用排行 |

## 11. 系统日志（SUPER_ADMIN）

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | /api/logs/operations | 操作日志分页 `keyword`、`module` |
| GET | /api/logs/logins | 登录日志分页 |

## 12. 文件上传

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | /api/files/upload | multipart `file`；返回 `{url:"/api/files/xxx.png"}`；存储层抽象 StorageService，默认本地磁盘（可配置目录），预留云端存储实现（阿里云 OSS / 腾讯云 COS，读配置切换） |
| GET | /api/files/{filename} | 静态访问（免认证） |

## 13. 定时任务（Spring @Scheduled）

1. 每 30 分钟扫描：BORROWED 且 now > due_time → 状态 OVERDUE + 写 overdue_record + 用户 overdue_count+1 + 站内信提醒
2. 每 30 分钟扫描：APPROVED 预约到期未取 → EXPIRED，设备 RESERVED→IDLE

## 角色权限矩阵（后端注解拦截）

- 学生：设备查看/导出、借用申请、预约、报修、我的消息、个人统计
- LAB_ADMIN：学生全部 + 设备管理(CRUD/导入)、借用审批/归还、预约审核、报修处理、本系统统计、操作日志
- SUPER_ADMIN：LAB_ADMIN 全部 + 用户管理、实验室管理、分类管理、登录日志、全局统计
