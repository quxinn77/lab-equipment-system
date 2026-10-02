# 实验室设备借用管理系统 — 完整数据接口文档

> 本文档基于前后端源码自动整理，覆盖全部 62 个数据接口。

---

## 〇、统一约定

| 项目 | 说明 |
|---|---|
| Base URL | `http://{host}:8080/api` |
| 认证方式 | 除登录、注册、文件静态访问外，均需请求头 `Authorization: Bearer <token>`（JWT，有效期 24 小时） |
| 响应统一格式 | `{ "code": 200, "msg": "success", "data": ... }` |
| 成功 code | `200` |
| 未登录 code | `401`（HTTP 200 + body code=401） |
| 无权限 code | `403` |
| 业务校验失败 code | `400` |
| 系统异常 code | `500` |
| 分页请求参数 | `pageNum`（默认 1）、`pageSize`（默认 10） |
| 分页响应结构 | `{ "total": n, "records": [...] }` |
| 时间格式 | `yyyy-MM-dd HH:mm:ss`（GMT+8） |
| Content-Type | `application/json;charset=UTF-8`（文件上传为 `multipart/form-data`） |
| 跨域 | 已开启 CORS，允许所有来源 |

### 数据字典总览

#### 1. 角色（role）
| 编码 | 名称 | roleId |
|---|---|---|
| STUDENT | 学生 | 1 |
| LAB_ADMIN | 实验室管理员 | 2 |
| SUPER_ADMIN | 超级管理员 | 3 |

#### 2. 设备状态（device.status）
| 值 | 说明 |
|---|---|
| IDLE | 空闲 |
| BORROWED | 借出 |
| REPAIRING | 维修中 |
| SCRAPPED | 报废 |
| RESERVED | 预留 |

#### 3. 借用单状态（borrow_record.status）
| 值 | 说明 |
|---|---|
| PENDING | 待审批 |
| APPROVED | 审批通过（未取） |
| REJECTED | 已驳回 |
| BORROWED | 已借出 |
| RETURNED | 已归还 |
| OVERDUE | 逾期未还 |

状态机：`PENDING →(同意)→ APPROVED →(取件)→ BORROWED →(归还)→ RETURNED`；`PENDING →(驳回)→ REJECTED`；`BORROWED →(超时)→ OVERDUE →(归还)→ RETURNED`。

#### 4. 归还状况（borrow_record.return_condition）
| 值 | 说明 |
|---|---|
| INTACT | 完好 |
| DAMAGED | 损坏 |

#### 5. 报修状态（repair_record.status）
| 值 | 说明 |
|---|---|
| PENDING | 待处理 |
| REPAIRING | 维修中 |
| FINISHED | 维修完成 |
| SCRAPPED | 报废 |

#### 6. 预约状态（reservation.status）
| 值 | 说明 |
|---|---|
| PENDING | 待审核 |
| APPROVED | 已通过 |
| REJECTED | 已驳回 |
| CANCELLED | 已取消 |
| EXPIRED | 已过期 |

#### 7. 用户状态（sys_user.status）
| 值 | 说明 |
|---|---|
| 0 | 禁用 |
| 1 | 启用 |

#### 8. 消息已读状态（message.is_read）
| 值 | 说明 |
|---|---|
| 0 | 未读 |
| 1 | 已读 |

#### 9. 登录日志状态（login_log.status）
| 值 | 说明 |
|---|---|
| 0 | 失败 |
| 1 | 成功 |

#### 10. 逾期记录处理状态（overdue_record.handled）
| 值 | 说明 |
|---|---|
| 0 | 未处理 |
| 1 | 已处理 |

---

## 一、认证模块（Auth）

### 接口 1：用户登录

**1. 接口状态**：已完成

**2. 接口基础信息**

- **2.1 接口路径**：`/api/auth/login`
- **2.2 路由信息**：`POST`，`Content-Type: application/json`，无需认证
- **2.3 接口说明**：用户登录，校验用户名密码，成功后签发 JWT token，并写入登录日志。

**4. 请求参数**（Body）

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| username | string | 是 | 学号/工号 |
| password | string | 是 | 密码 |

**5. 响应参数**（data）

| 字段 | 类型 | 说明 |
|---|---|---|
| token | string | JWT 令牌 |
| user | object | 用户信息对象 |
| user.id | long | 用户 ID |
| user.username | string | 学号/工号 |
| user.realName | string | 姓名 |
| user.college | string | 学院 |
| user.phone | string | 手机号 |
| user.email | string | 邮箱 |
| user.roleId | long | 角色 ID |
| user.roleCode | string | 角色编码（见数据字典 1） |
| user.roleName | string | 角色名称 |
| user.status | int | 用户状态（见数据字典 7） |
| user.overdueCount | int | 逾期次数 |
| user.createTime | string | 创建时间 |

**6. 请求示例**

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}'
```

响应示例：
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "user": {
      "id": 1,
      "username": "admin",
      "realName": "超级管理员",
      "college": null,
      "phone": null,
      "email": null,
      "roleId": 3,
      "roleCode": "SUPER_ADMIN",
      "roleName": "超级管理员",
      "status": 1,
      "overdueCount": 0,
      "createTime": "2026-01-01 00:00:00"
    }
  }
}
```

**7. 数据字典**：用户状态见【数据字典 7】，角色见【数据字典 1】。

---

### 接口 2：用户注册

**1. 接口状态**：已完成

**2. 接口基础信息**

- **2.1 接口路径**：`/api/auth/register`
- **2.2 路由信息**：`POST`，`Content-Type: application/json`，无需认证
- **2.3 接口说明**：学生自助注册，默认分配 STUDENT 角色，状态为启用。

**4. 请求参数**（Body）

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| username | string | 是 | 学号/工号 |
| password | string | 是 | 密码（6-32 位） |
| realName | string | 是 | 姓名 |
| college | string | 否 | 学院 |
| phone | string | 否 | 手机号 |
| email | string | 否 | 邮箱 |

**5. 响应参数**（data）：`null`（无数据返回）

**6. 请求示例**

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"2026001","password":"123456","realName":"张三","college":"计算机学院","phone":"13800138000","email":"zhangsan@edu.cn"}'
```

响应示例：
```json
{ "code": 200, "msg": "success", "data": null }
```

**7. 数据字典**：默认角色为 STUDENT（见【数据字典 1】）。

---

### 接口 3：获取当前用户信息

**1. 接口状态**：已完成

**2. 接口基础信息**

- **2.1 接口路径**：`/api/auth/info`
- **2.2 路由信息**：`GET`，需登录
- **2.3 接口说明**：获取当前登录用户的详细信息（含角色、逾期次数等）。

**4. 请求参数**：无

**5. 响应参数**（data）：同【接口 1】的 `user` 对象结构。

**6. 请求示例**

```bash
curl -X GET http://localhost:8080/api/auth/info \
  -H "Authorization: Bearer <token>"
```

**7. 数据字典**：用户状态见【数据字典 7】，角色见【数据字典 1】。

---

### 接口 4：修改密码

**1. 接口状态**：已完成

**2. 接口基础信息**

- **2.1 接口路径**：`/api/auth/password`
- **2.2 路由信息**：`PUT`，`Content-Type: application/json`，需登录
- **2.3 接口说明**：当前登录用户修改自己的密码，需校验原密码。

**4. 请求参数**（Body）

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| oldPassword | string | 是 | 原密码 |
| newPassword | string | 是 | 新密码（6-32 位） |

**5. 响应参数**（data）：`null`

**6. 请求示例**

```bash
curl -X PUT http://localhost:8080/api/auth/password \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"oldPassword":"123456","newPassword":"654321"}'
```

**7. 数据字典**：无

---

## 二、用户管理模块（User）

> 权限：查看需 `LAB_ADMIN` 或 `SUPER_ADMIN`；增删改仅 `SUPER_ADMIN`。

### 接口 5：分页查询用户

**1. 接口状态**：已完成

**2. 接口基础信息**

- **2.1 接口路径**：`/api/users`
- **2.2 路由信息**：`GET`，需 `LAB_ADMIN` / `SUPER_ADMIN`
- **2.3 接口说明**：分页查询用户列表，支持按姓名/学号模糊搜索、角色、状态筛选。

**4. 请求参数**（Query）

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| keyword | string | 否 | - | 姓名/学号模糊搜索 |
| roleId | long | 否 | - | 角色 ID（见数据字典 1） |
| status | int | 否 | - | 用户状态（见数据字典 7） |
| pageNum | int | 否 | 1 | 页码 |
| pageSize | int | 否 | 10 | 每页条数 |

**5. 响应参数**（data）

| 字段 | 类型 | 说明 |
|---|---|---|
| total | long | 总记录数 |
| records | array | 用户列表，元素结构同【接口 1】的 `user` 对象 |

**6. 请求示例**

```bash
curl -X GET "http://localhost:8080/api/users?keyword=张&roleId=1&status=1&pageNum=1&pageSize=10" \
  -H "Authorization: Bearer <token>"
```

**7. 数据字典**：角色见【数据字典 1】，用户状态见【数据字典 7】。

---

### 接口 6：新增用户

**1. 接口状态**：已完成

**2. 接口基础信息**

- **2.1 接口路径**：`/api/users`
- **2.2 路由信息**：`POST`，`Content-Type: application/json`，需 `SUPER_ADMIN`
- **2.3 接口说明**：管理员新增用户，可指定角色。

**4. 请求参数**（Body）

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| username | string | 是 | 学号/工号 |
| password | string | 否 | 密码（不传则使用默认密码） |
| realName | string | 是 | 姓名 |
| college | string | 否 | 学院 |
| phone | string | 否 | 手机号 |
| email | string | 否 | 邮箱 |
| roleId | long | 是 | 角色 ID（见数据字典 1） |
| status | int | 否 | 用户状态，默认 1（见数据字典 7） |

**5. 响应参数**（data）：`null`

**6. 请求示例**

```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"username":"2026002","password":"123456","realName":"李四","roleId":1,"status":1}'
```

**7. 数据字典**：角色见【数据字典 1】，用户状态见【数据字典 7】。

---

### 接口 7：编辑用户

**1. 接口状态**：已完成

**2. 接口基础信息**

- **2.1 接口路径**：`/api/users/{id}`
- **2.2 路由信息**：`PUT`，`Content-Type: application/json`，需 `SUPER_ADMIN`
- **2.3 接口说明**：编辑用户信息（不含密码，密码通过重置接口修改）。

**4. 请求参数**

Path：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| id | long | 是 | 用户 ID |

Body（同【接口 6】，id 字段忽略）：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| username | string | 是 | 学号/工号 |
| realName | string | 是 | 姓名 |
| college | string | 否 | 学院 |
| phone | string | 否 | 手机号 |
| email | string | 否 | 邮箱 |
| roleId | long | 是 | 角色 ID |
| status | int | 否 | 用户状态 |

**5. 响应参数**（data）：`null`

**6. 请求示例**

```bash
curl -X PUT http://localhost:8080/api/users/5 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"username":"2026002","realName":"李四","roleId":1,"status":1}'
```

**7. 数据字典**：角色见【数据字典 1】，用户状态见【数据字典 7】。

---

### 接口 8：删除用户

**1. 接口状态**：已完成

**2. 接口基础信息**

- **2.1 接口路径**：`/api/users/{id}`
- **2.2 路由信息**：`DELETE`，需 `SUPER_ADMIN`
- **2.3 接口说明**：删除用户，不允许删除自己。

**4. 请求参数**（Path）

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| id | long | 是 | 用户 ID |

**5. 响应参数**（data）：`null`

**6. 请求示例**

```bash
curl -X DELETE http://localhost:8080/api/users/5 \
  -H "Authorization: Bearer <token>"
```

**7. 数据字典**：无

---

### 接口 9：重置用户密码

**1. 接口状态**：已完成

**2. 接口基础信息**

- **2.1 接口路径**：`/api/users/{id}/reset-password`
- **2.2 路由信息**：`PUT`，`Content-Type: application/json`，需 `SUPER_ADMIN`
- **2.3 接口说明**：管理员重置指定用户的密码。

**4. 请求参数**

Path：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| id | long | 是 | 用户 ID |

Body：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| newPassword | string | 是 | 新密码 |

**5. 响应参数**（data）：`null`

**6. 请求示例**

```bash
curl -X PUT http://localhost:8080/api/users/5/reset-password \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"newPassword":"123456"}'
```

**7. 数据字典**：无

---

### 接口 10：启用/禁用用户

**1. 接口状态**：已完成

**2. 接口基础信息**

- **2.1 接口路径**：`/api/users/{id}/status`
- **2.2 路由信息**：`PUT`，`Content-Type: application/json`，需 `SUPER_ADMIN`
- **2.3 接口说明**：切换用户启用/禁用状态。

**4. 请求参数**

Path：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| id | long | 是 | 用户 ID |

Body：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| status | int | 是 | 状态：0-禁用，1-启用（见数据字典 7） |

**5. 响应参数**（data）：`null`

**6. 请求示例**

```bash
curl -X PUT http://localhost:8080/api/users/5/status \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"status":0}'
```

**7. 数据字典**：用户状态见【数据字典 7】。

---

## 三、实验室管理模块（Lab）

> 权限：需 `LAB_ADMIN` 或 `SUPER_ADMIN`。

### 接口 11：分页查询实验室

**1. 接口状态**：已完成

**2. 接口基础信息**

- **2.1 接口路径**：`/api/labs`
- **2.2 路由信息**：`GET`，需 `LAB_ADMIN` / `SUPER_ADMIN`
- **2.3 接口说明**：分页查询实验室列表。

**4. 请求参数**（Query）

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| keyword | string | 否 | - | 名称/编号模糊搜索 |
| pageNum | int | 否 | 1 | 页码 |
| pageSize | int | 否 | 10 | 每页条数 |

**5. 响应参数**（data）

| 字段 | 类型 | 说明 |
|---|---|---|
| total | long | 总记录数 |
| records | array | 实验室列表 |
| records[].id | long | 实验室 ID |
| records[].code | string | 实验室编号 |
| records[].name | string | 实验室名称 |
| records[].location | string | 位置 |
| records[].manager | string | 负责人 |
| records[].description | string | 描述 |
| records[].status | int | 状态 |
| records[].createTime | string | 创建时间 |
| records[].updateTime | string | 更新时间 |

**6. 请求示例**

```bash
curl -X GET "http://localhost:8080/api/labs?keyword=物理&pageNum=1&pageSize=10" \
  -H "Authorization: Bearer <token>"
```

**7. 数据字典**：实验室 status 字段：1-启用，0-禁用。

---

### 接口 12：获取全部实验室

**1. 接口状态**：已完成

**2. 接口基础信息**

- **2.1 接口路径**：`/api/labs/all`
- **2.2 路由信息**：`GET`，需 `LAB_ADMIN` / `SUPER_ADMIN`
- **2.3 接口说明**：获取全部实验室列表（下拉选择用）。

**4. 请求参数**：无

**5. 响应参数**（data）：array，元素结构同【接口 11】的 `records[]`。

**6. 请求示例**

```bash
curl -X GET http://localhost:8080/api/labs/all \
  -H "Authorization: Bearer <token>"
```

**7. 数据字典**：实验室 status：1-启用，0-禁用。

---

### 接口 13：新增实验室

**1. 接口状态**：已完成

**2. 接口基础信息**

- **2.1 接口路径**：`/api/labs`
- **2.2 路由信息**：`POST`，`Content-Type: application/json`，需 `LAB_ADMIN` / `SUPER_ADMIN`
- **2.3 接口说明**：新增实验室。

**4. 请求参数**（Body）

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| code | string | 是 | 实验室编号 |
| name | string | 是 | 实验室名称 |
| location | string | 否 | 位置 |
| manager | string | 否 | 负责人 |
| description | string | 否 | 描述 |

**5. 响应参数**（data）：`null`

**6. 请求示例**

```bash
curl -X POST http://localhost:8080/api/labs \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"code":"LAB001","name":"物理实验室","location":"教学楼A101","manager":"王老师"}'
```

**7. 数据字典**：无

---

### 接口 14：编辑实验室

**1. 接口状态**：已完成

**2. 接口基础信息**

- **2.1 接口路径**：`/api/labs/{id}`
- **2.2 路由信息**：`PUT`，`Content-Type: application/json`，需 `LAB_ADMIN` / `SUPER_ADMIN`
- **2.3 接口说明**：编辑实验室信息。

**4. 请求参数**

Path：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| id | long | 是 | 实验室 ID |

Body（同【接口 13】）。

**5. 响应参数**（data）：`null`

**6. 请求示例**

```bash
curl -X PUT http://localhost:8080/api/labs/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"code":"LAB001","name":"物理实验室（更新）","location":"教学楼A101"}'
```

**7. 数据字典**：无

---

### 接口 15：删除实验室

**1. 接口状态**：已完成

**2. 接口基础信息**

- **2.1 接口路径**：`/api/labs/{id}`
- **2.2 路由信息**：`DELETE`，需 `LAB_ADMIN` / `SUPER_ADMIN`
- **2.3 接口说明**：删除实验室。若有关联设备则禁止删除，需先转移设备。

**4. 请求参数**（Path）

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| id | long | 是 | 实验室 ID |

**5. 响应参数**（data）：`null`

**6. 请求示例**

```bash
curl -X DELETE http://localhost:8080/api/labs/1 \
  -H "Authorization: Bearer <token>"
```

**7. 数据字典**：无

---

## 四、设备分类模块（Category）

> 权限：查看需登录；增删改需 `LAB_ADMIN` / `SUPER_ADMIN`。

### 接口 16：获取全部分类

**1. 接口状态**：已完成

**2. 接口基础信息**

- **2.1 接口路径**：`/api/categories`
- **2.2 路由信息**：`GET`，需登录
- **2.3 接口说明**：获取全部分类列表。

**4. 请求参数**：无

**5. 响应参数**（data）：array

| 字段 | 类型 | 说明 |
|---|---|---|
| id | long | 分类 ID |
| name | string | 分类名称 |
| remark | string | 备注 |
| createTime | string | 创建时间 |

**6. 请求示例**

```bash
curl -X GET http://localhost:8080/api/categories \
  -H "Authorization: Bearer <token>"
```

响应示例：
```json
{
  "code": 200,
  "msg": "success",
  "data": [
    { "id": 1, "name": "电子仪器", "remark": "示波器、万用表等", "createTime": "2026-01-01 00:00:00" }
  ]
}
```

**7. 数据字典**：无

---

### 接口 17：新增分类

**1. 接口状态**：已完成

**2. 接口基础信息**

- **2.1 接口路径**：`/api/categories`
- **2.2 路由信息**：`POST`，`Content-Type: application/json`，需 `LAB_ADMIN` / `SUPER_ADMIN`
- **2.3 接口说明**：新增设备分类。

**4. 请求参数**（Body）

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| name | string | 是 | 分类名称 |
| remark | string | 否 | 备注 |

**5. 响应参数**（data）：`null`

**6. 请求示例**

```bash
curl -X POST http://localhost:8080/api/categories \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"name":"光学仪器","remark":"显微镜、分光计等"}'
```

**7. 数据字典**：无

---

### 接口 18：编辑分类

**1. 接口状态**：已完成

**2. 接口基础信息**

- **2.1 接口路径**：`/api/categories/{id}`
- **2.2 路由信息**：`PUT`，`Content-Type: application/json`，需 `LAB_ADMIN` / `SUPER_ADMIN`
- **2.3 接口说明**：编辑分类信息。

**4. 请求参数**

Path：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| id | long | 是 | 分类 ID |

Body：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| name | string | 是 | 分类名称 |
| remark | string | 否 | 备注 |

**5. 响应参数**（data）：`null`

**6. 请求示例**

```bash
curl -X PUT http://localhost:8080/api/categories/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"name":"电子仪器设备","remark":""}'
```

**7. 数据字典**：无

---

### 接口 19：删除分类

**1. 接口状态**：已完成

**2. 接口基础信息**

- **2.1 接口路径**：`/api/categories/{id}`
- **2.2 路由信息**：`DELETE`，需 `LAB_ADMIN` / `SUPER_ADMIN`
- **2.3 接口说明**：删除分类。若有关联设备则禁止删除。

**4. 请求参数**（Path）

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| id | long | 是 | 分类 ID |

**5. 响应参数**（data）：`null`

**6. 请求示例**

```bash
curl -X DELETE http://localhost:8080/api/categories/1 \
  -H "Authorization: Bearer <token>"
```

**7. 数据字典**：无

---

## 五、设备管理模块（Device）

> 权限：查看需登录（学生仅能查看）；增删改及导入需 `LAB_ADMIN` / `SUPER_ADMIN`。
> 所有设备增删改操作均写入 `device_log` 变更日志。

### 接口 20：分页查询设备

**1. 接口状态**：已完成

**2. 接口基础信息**

- **2.1 接口路径**：`/api/devices`
- **2.2 路由信息**：`GET`，需登录
- **2.3 接口说明**：分页检索设备，支持按名称/编号模糊搜索、实验室、分类、状态筛选。

**4. 请求参数**（Query）

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| keyword | string | 否 | - | 设备名称/编号模糊搜索 |
| labId | long | 否 | - | 实验室 ID |
| categoryId | long | 否 | - | 分类 ID |
| status | string | 否 | - | 设备状态（见数据字典 2） |
| pageNum | int | 否 | 1 | 页码 |
| pageSize | int | 否 | 10 | 每页条数 |

**5. 响应参数**（data）

| 字段 | 类型 | 说明 |
|---|---|---|
| total | long | 总记录数 |
| records | array | 设备列表 |
| records[].id | long | 设备 ID |
| records[].code | string | 设备编号 |
| records[].name | string | 设备名称 |
| records[].model | string | 型号 |
| records[].spec | string | 规格 |
| records[].brand | string | 品牌 |
| records[].categoryId | long | 分类 ID |
| records[].categoryName | string | 分类名称 |
| records[].labId | long | 实验室 ID |
| records[].labName | string | 实验室名称 |
| records[].purchaseDate | string | 购置日期 |
| records[].originalValue | decimal | 原值 |
| records[].imageUrl | string | 图片地址 |
| records[].status | string | 设备状态（见数据字典 2） |
| records[].remark | string | 备注 |
| records[].createTime | string | 创建时间 |
| records[].updateTime | string | 更新时间 |

**6. 请求示例**

```bash
curl -X GET "http://localhost:8080/api/devices?keyword=示波器&labId=1&status=IDLE&pageNum=1&pageSize=10" \
  -H "Authorization: Bearer <token>"
```

**7. 数据字典**：设备状态见【数据字典 2】。

---

### 接口 21：设备详情

**1. 接口状态**：已完成

**2. 接口基础信息**

- **2.1 接口路径**：`/api/devices/{id}`
- **2.2 路由信息**：`GET`，需登录
- **2.3 接口说明**：获取设备详情，含实验室名称、分类名称冗余字段。

**4. 请求参数**（Path）

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| id | long | 是 | 设备 ID |

**5. 响应参数**（data）：同【接口 20】的 `records[]` 单个对象。

**6. 请求示例**

```bash
curl -X GET http://localhost:8080/api/devices/1 \
  -H "Authorization: Bearer <token>"
```

**7. 数据字典**：设备状态见【数据字典 2】。

---

### 接口 22：新增设备

**1. 接口状态**：已完成

**2. 接口基础信息**

- **2.1 接口路径**：`/api/devices`
- **2.2 路由信息**：`POST`，`Content-Type: application/json`，需 `LAB_ADMIN` / `SUPER_ADMIN`
- **2.3 接口说明**：新增设备，编号需唯一校验。

**4. 请求参数**（Body）

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| code | string | 是 | 设备编号（唯一） |
| name | string | 是 | 设备名称 |
| model | string | 否 | 型号 |
| spec | string | 否 | 规格 |
| brand | string | 否 | 品牌 |
| categoryId | long | 否 | 分类 ID |
| labId | long | 否 | 实验室 ID |
| purchaseDate | string | 否 | 购置日期（yyyy-MM-dd） |
| originalValue | decimal | 否 | 原值 |
| imageUrl | string | 否 | 图片地址 |
| remark | string | 否 | 备注 |

**5. 响应参数**（data）：`null`

**6. 请求示例**

```bash
curl -X POST http://localhost:8080/api/devices \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"code":"DEV001","name":"数字示波器","model":"DS1052E","brand":"RIGOL","categoryId":1,"labId":1,"purchaseDate":"2025-03-15","originalValue":3500.00}'
```

**7. 数据字典**：无

---

### 接口 23：编辑设备

**1. 接口状态**：已完成

**2. 接口基础信息**

- **2.1 接口路径**：`/api/devices/{id}`
- **2.2 路由信息**：`PUT`，`Content-Type: application/json`，需 `LAB_ADMIN` / `SUPER_ADMIN`
- **2.3 接口说明**：编辑设备信息（状态不允许在此接口直接修改）。

**4. 请求参数**

Path：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| id | long | 是 | 设备 ID |

Body（同【接口 22】）。

**5. 响应参数**（data）：`null`

**6. 请求示例**

```bash
curl -X PUT http://localhost:8080/api/devices/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"code":"DEV001","name":"数字示波器","model":"DS1052E","brand":"RIGOL"}'
```

**7. 数据字典**：无

---

### 接口 24：删除设备

**1. 接口状态**：已完成

**2. 接口基础信息**

- **2.1 接口路径**：`/api/devices/{id}`
- **2.2 路由信息**：`DELETE`，需 `LAB_ADMIN` / `SUPER_ADMIN`
- **2.3 接口说明**：删除设备（逻辑删除）。状态为 `BORROWED`（借出）时禁止删除，并写设备变更日志。

**4. 请求参数**（Path）

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| id | long | 是 | 设备 ID |

**5. 响应参数**（data）：`null`

**6. 请求示例**

```bash
curl -X DELETE http://localhost:8080/api/devices/1 \
  -H "Authorization: Bearer <token>"
```

**7. 数据字典**：设备状态见【数据字典 2】。

---

### 接口 25：Excel 批量导入设备

**1. 接口状态**：已完成

**2. 接口基础信息**

- **2.1 接口路径**：`/api/devices/import`
- **2.2 路由信息**：`POST`，`Content-Type: multipart/form-data`，需 `LAB_ADMIN` / `SUPER_ADMIN`
- **2.3 接口说明**：通过 Excel 批量导入设备。模板列：设备编号、设备名称、型号、规格、品牌、分类名称、实验室编号、购置日期、原值。

**4. 请求参数**（FormData）

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| file | file | 是 | Excel 文件（.xlsx/.xls，最大 10MB） |

**5. 响应参数**（data）：string，导入结果提示信息。

**6. 请求示例**

```bash
curl -X POST http://localhost:8080/api/devices/import \
  -H "Authorization: Bearer <token>" \
  -F "file=@设备清单.xlsx"
```

响应示例：
```json
{ "code": 200, "msg": "success", "data": "成功导入 15 条，失败 0 条" }
```

**7. 数据字典**：无

---

### 接口 26：导出设备台账 Excel

**1. 接口状态**：已完成

**2. 接口基础信息**

- **2.1 接口路径**：`/api/devices/export`
- **2.2 路由信息**：`GET`，需登录
- **2.3 接口说明**：按当前筛选条件导出设备台账为 Excel 文件。

**4. 请求参数**（Query）

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| keyword | string | 否 | 设备名称/编号模糊搜索 |
| labId | long | 否 | 实验室 ID |
| categoryId | long | 否 | 分类 ID |
| status | string | 否 | 设备状态（见数据字典 2） |

**5. 响应参数**：二进制流（`application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`），文件名 `设备台账.xlsx`。

**6. 请求示例**

```bash
curl -X GET "http://localhost:8080/api/devices/export?labId=1" \
  -H "Authorization: Bearer <token>" \
  -o 设备台账.xlsx
```

**7. 数据字典**：设备状态见【数据字典 2】。

---

### 接口 27：设备变更日志

**1. 接口状态**：已完成

**2. 接口基础信息**

- **2.1 接口路径**：`/api/devices/{id}/logs`
- **2.2 路由信息**：`GET`，需登录
- **2.3 接口说明**：获取指定设备的变更日志列表。

**4. 请求参数**（Path）

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| id | long | 是 | 设备 ID |

**5. 响应参数**（data）：array

| 字段 | 类型 | 说明 |
|---|---|---|
| id | long | 日志 ID |
| deviceId | long | 设备 ID |
| deviceCode | string | 设备编号 |
| operatorId | long | 操作人 ID |
| operator | string | 操作人姓名 |
| action | string | 操作动作（新增/编辑/删除/状态变更等） |
| oldStatus | string | 旧状态 |
| newStatus | string | 新状态 |
| detail | string | 详情 |
| createTime | string | 操作时间 |

**6. 请求示例**

```bash
curl -X GET http://localhost:8080/api/devices/1/logs \
  -H "Authorization: Bearer <token>"
```

**7. 数据字典**：设备状态见【数据字典 2】。

---

### 接口 28：生成设备二维码

**1. 接口状态**：已完成

**2. 接口基础信息**

- **2.1 接口路径**：`/api/devices/qrcode/{id}`
- **2.2 路由信息**：`GET`，需登录
- **2.3 接口说明**：生成设备二维码图片（PNG 格式）。

**4. 请求参数**（Path）

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| id | long | 是 | 设备 ID |

**5. 响应参数**：二进制流（`image/png`）。

**6. 请求示例**

```bash
curl -X GET http://localhost:8080/api/devices/qrcode/1 \
  -H "Authorization: Bearer <token>" \
  -o device-qrcode.png
```

前端通过 `<img src="/api/devices/qrcode/{id}?token={token}">` 方式访问。

**7. 数据字典**：无

---

## 六、借用/归还模块（Borrow）

> 借用申请限学生；审批、取件、归还限 `LAB_ADMIN` / `SUPER_ADMIN`。

### 接口 29：提交借用申请

**1. 接口状态**：已完成

**2. 接口基础信息**

- **2.1 接口路径**：`/api/borrows`
- **2.2 路由信息**：`POST`，`Content-Type: application/json`，需登录（学生）
- **2.3 接口说明**：学生提交设备借用申请。校验：设备状态必须为 IDLE；起止时间合法（开始 < 截止 且 开始 >= 当前）；当前用户无 OVERDUE 记录；逾期次数未超上限。

**4. 请求参数**（Body）

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| deviceId | long | 是 | 设备 ID |
| startTime | string | 是 | 开始时间（yyyy-MM-dd HH:mm:ss） |
| dueTime | string | 是 | 截止时间（yyyy-MM-dd HH:mm:ss） |
| purpose | string | 否 | 借用用途 |

**5. 响应参数**（data）：long，借用记录 ID。

**6. 请求示例**

```bash
curl -X POST http://localhost:8080/api/borrows \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"deviceId":1,"startTime":"2026-10-05 09:00:00","dueTime":"2026-10-12 18:00:00","purpose":"课程实验"}'
```

响应示例：
```json
{ "code": 200, "msg": "success", "data": 1001 }
```

**7. 数据字典**：设备状态见【数据字典 2】，借用状态见【数据字典 3】。

---

### 接口 30：我的借用记录

**1. 接口状态**：已完成

**2. 接口基础信息**

- **2.1 接口路径**：`/api/borrows/my`
- **2.2 路由信息**：`GET`，需登录
- **2.3 接口说明**：查询当前用户的借用记录，支持按状态筛选。

**4. 请求参数**（Query）

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| status | string | 否 | - | 借用状态（见数据字典 3） |
| pageNum | int | 否 | 1 | 页码 |
| pageSize | int | 否 | 10 | 每页条数 |

**5. 响应参数**（data）

| 字段 | 类型 | 说明 |
|---|---|---|
| total | long | 总记录数 |
| records | array | 借用记录列表 |
| records[].id | long | 记录 ID |
| records[].recordNo | string | 借用单号 |
| records[].userId | long | 申请人 ID |
| records[].userName | string | 申请人姓名 |
| records[].deviceId | long | 设备 ID |
| records[].deviceCode | string | 设备编号 |
| records[].deviceName | string | 设备名称 |
| records[].startTime | string | 开始时间 |
| records[].dueTime | string | 截止时间 |
| records[].actualReturnTime | string | 实际归还时间 |
| records[].purpose | string | 借用用途 |
| records[].status | string | 借用状态（见数据字典 3） |
| records[].approverId | long | 审批人 ID |
| records[].approver | string | 审批人姓名 |
| records[].approveTime | string | 审批时间 |
| records[].approveRemark | string | 审批备注 |
| records[].returnCondition | string | 归还状况（见数据字典 4） |
| records[].returnRemark | string | 归还备注 |
| records[].compensation | decimal | 赔偿金额 |
| records[].createTime | string | 申请时间 |

**6. 请求示例**

```bash
curl -X GET "http://localhost:8080/api/borrows/my?status=PENDING&pageNum=1&pageSize=10" \
  -H "Authorization: Bearer <token>"
```

**7. 数据字典**：借用状态见【数据字典 3】，归还状况见【数据字典 4】。

---

### 接口 31：全部借用记录

**1. 接口状态**：已完成

**2. 接口基础信息**

- **2.1 接口路径**：`/api/borrows`
- **2.2 路由信息**：`GET`，需 `LAB_ADMIN` / `SUPER_ADMIN`
- **2.3 接口说明**：管理员查询全部借用记录，支持按编号/申请人、状态、设备、实验室筛选。

**4. 请求参数**（Query）

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| keyword | string | 否 | - | 借用单号/申请人模糊搜索 |
| status | string | 否 | - | 借用状态（见数据字典 3） |
| deviceId | long | 否 | - | 设备 ID |
| labId | long | 否 | - | 实验室 ID |
| pageNum | int | 否 | 1 | 页码 |
| pageSize | int | 否 | 10 | 每页条数 |

**5. 响应参数**（data）：同【接口 30】。

**6. 请求示例**

```bash
curl -X GET "http://localhost:8080/api/borrows?status=PENDING&pageNum=1&pageSize=10" \
  -H "Authorization: Bearer <token>"
```

**7. 数据字典**：借用状态见【数据字典 3】。

---

### 接口 32：借用详情

**1. 接口状态**：已完成

**2. 接口基础信息**

- **2.1 接口路径**：`/api/borrows/{id}`
- **2.2 路由信息**：`GET`，需 `LAB_ADMIN` / `SUPER_ADMIN`
- **2.3 接口说明**：获取借用记录详情。

**4. 请求参数**（Path）

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| id | long | 是 | 借用记录 ID |

**5. 响应参数**（data）：同【接口 30】的 `records[]` 单个对象。

**6. 请求示例**

```bash
curl -X GET http://localhost:8080/api/borrows/1001 \
  -H "Authorization: Bearer <token>"
```

**7. 数据字典**：借用状态见【数据字典 3】。

---

### 接口 33：借用审批

**1. 接口状态**：已完成

**2. 接口基础信息**

- **2.1 接口路径**：`/api/borrows/{id}/approve`
- **2.2 路由信息**：`PUT`，`Content-Type: application/json`，需 `LAB_ADMIN` / `SUPER_ADMIN`
- **2.3 接口说明**：审批借用申请。同意→若设备仍为 IDLE 则状态变 RESERVED、单据变 APPROVED，向申请人发站内信；若设备已被处理则提示冲突。驳回→单据变 REJECTED 并记录理由，发站内信。

**4. 请求参数**

Path：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| id | long | 是 | 借用记录 ID |

Body：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| approved | boolean | 是 | 审批结果：true-同意，false-驳回 |
| remark | string | 否 | 审批备注/驳回理由 |

**5. 响应参数**（data）：`null`

**6. 请求示例**

```bash
curl -X PUT http://localhost:8080/api/borrows/1001/approve \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"approved":true,"remark":"同意借用"}'
```

**7. 数据字典**：借用状态见【数据字典 3】，设备状态见【数据字典 2】。

---

### 接口 34：确认取件

**1. 接口状态**：已完成

**2. 接口基础信息**

- **2.1 接口路径**：`/api/borrows/{id}/pickup`
- **2.2 路由信息**：`PUT`，需 `LAB_ADMIN` / `SUPER_ADMIN`
- **2.3 接口说明**：确认学生取件，单据状态 APPROVED → BORROWED，设备状态 RESERVED → BORROWED。

**4. 请求参数**（Path）

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| id | long | 是 | 借用记录 ID |

**5. 响应参数**（data）：`null`

**6. 请求示例**

```bash
curl -X PUT http://localhost:8080/api/borrows/1001/pickup \
  -H "Authorization: Bearer <token>"
```

**7. 数据字典**：借用状态见【数据字典 3】，设备状态见【数据字典 2】。

---

### 接口 35：归还登记

**1. 接口状态**：已完成

**2. 接口基础信息**

- **2.1 接口路径**：`/api/borrows/{id}/return`
- **2.2 路由信息**：`PUT`，`Content-Type: application/json`，需 `LAB_ADMIN` / `SUPER_ADMIN`
- **2.3 接口说明**：归还登记。单据状态变 RETURNED，记录 actualReturnTime；设备状态变 IDLE。若归还状况为 DAMAGED（损坏），自动创建报修单并关联，记录赔偿金额。

**4. 请求参数**

Path：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| id | long | 是 | 借用记录 ID |

Body：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| condition | string | 是 | 归还状况：INTACT-完好，DAMAGED-损坏（见数据字典 4） |
| remark | string | 否 | 归还备注 |
| compensation | decimal | 否 | 赔偿金额（损坏时填写） |

**5. 响应参数**（data）：`null`

**6. 请求示例**

```bash
curl -X PUT http://localhost:8080/api/borrows/1001/return \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"condition":"DAMAGED","remark":"外壳有划痕","compensation":200.00}'
```

**7. 数据字典**：归还状况见【数据字典 4】，设备状态见【数据字典 2】。

---

### 接口 36：导出借用记录 Excel

**1. 接口状态**：已完成

**2. 接口基础信息**

- **2.1 接口路径**：`/api/borrows/export`
- **2.2 路由信息**：`GET`，需 `LAB_ADMIN` / `SUPER_ADMIN`
- **2.3 接口说明**：按筛选条件导出借用记录为 Excel 文件。

**4. 请求参数**（Query）

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| keyword | string | 否 | 借用单号/申请人模糊搜索 |
| status | string | 否 | 借用状态（见数据字典 3） |
| deviceId | long | 否 | 设备 ID |
| labId | long | 否 | 实验室 ID |

**5. 响应参数**：二进制流（Excel 文件）。

**6. 请求示例**

```bash
curl -X GET "http://localhost:8080/api/borrows/export?status=OVERDUE" \
  -H "Authorization: Bearer <token>" \
  -o 借用记录.xlsx
```

**7. 数据字典**：借用状态见【数据字典 3】。

---

## 七、预约模块（Reservation）

> 预约申请限学生；审核限 `LAB_ADMIN` / `SUPER_ADMIN`。

### 接口 37：提交预约申请

**1. 接口状态**：已完成

**2. 接口基础信息**

- **2.1 接口路径**：`/api/reservations`
- **2.2 路由信息**：`POST`，`Content-Type: application/json`，需登录（学生）
- **2.3 接口说明**：提交设备预约申请。校验：同设备时段冲突（与 APPROVED/PENDING 的预约相交则拒绝）；设备状态必须为 IDLE 或 RESERVED。

**4. 请求参数**（Body）

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| deviceId | long | 是 | 设备 ID |
| startTime | string | 是 | 开始时间（yyyy-MM-dd HH:mm:ss） |
| endTime | string | 是 | 结束时间（yyyy-MM-dd HH:mm:ss） |
| remark | string | 否 | 备注 |

**5. 响应参数**（data）：long，预约记录 ID。

**6. 请求示例**

```bash
curl -X POST http://localhost:8080/api/reservations \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"deviceId":1,"startTime":"2026-10-10 09:00:00","endTime":"2026-10-10 12:00:00","remark":"实验课使用"}'
```

**7. 数据字典**：设备状态见【数据字典 2】，预约状态见【数据字典 6】。

---

### 接口 38：我的预约

**1. 接口状态**：已完成

**2. 接口基础信息**

- **2.1 接口路径**：`/api/reservations/my`
- **2.2 路由信息**：`GET`，需登录
- **2.3 接口说明**：查询当前用户的预约记录。

**4. 请求参数**（Query）

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| status | string | 否 | - | 预约状态（见数据字典 6） |
| pageNum | int | 否 | 1 | 页码 |
| pageSize | int | 否 | 10 | 每页条数 |

**5. 响应参数**（data）

| 字段 | 类型 | 说明 |
|---|---|---|
| total | long | 总记录数 |
| records | array | 预约记录列表 |
| records[].id | long | 预约 ID |
| records[].userId | long | 预约人 ID |
| records[].userName | string | 预约人姓名 |
| records[].deviceId | long | 设备 ID |
| records[].deviceCode | string | 设备编号 |
| records[].deviceName | string | 设备名称 |
| records[].startTime | string | 开始时间 |
| records[].endTime | string | 结束时间 |
| records[].status | string | 预约状态（见数据字典 6） |
| records[].remark | string | 备注 |
| records[].approverId | long | 审批人 ID |
| records[].approver | string | 审批人姓名 |
| records[].approveTime | string | 审批时间 |
| records[].approveRemark | string | 审批备注 |
| records[].createTime | string | 创建时间 |

**6. 请求示例**

```bash
curl -X GET "http://localhost:8080/api/reservations/my?pageNum=1&pageSize=10" \
  -H "Authorization: Bearer <token>"
```

**7. 数据字典**：预约状态见【数据字典 6】。

---

### 接口 39：全部预约

**1. 接口状态**：已完成

**2. 接口基础信息**

- **2.1 接口路径**：`/api/reservations`
- **2.2 路由信息**：`GET`，需 `LAB_ADMIN` / `SUPER_ADMIN`
- **2.3 接口说明**：管理员查询全部预约记录。

**4. 请求参数**（Query）

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| status | string | 否 | - | 预约状态（见数据字典 6） |
| pageNum | int | 否 | 1 | 页码 |
| pageSize | int | 否 | 10 | 每页条数 |

**5. 响应参数**（data）：同【接口 38】。

**6. 请求示例**

```bash
curl -X GET "http://localhost:8080/api/reservations?status=PENDING&pageNum=1&pageSize=10" \
  -H "Authorization: Bearer <token>"
```

**7. 数据字典**：预约状态见【数据字典 6】。

---

### 接口 40：预约审核

**1. 接口状态**：已完成

**2. 接口基础信息**

- **2.1 接口路径**：`/api/reservations/{id}/approve`
- **2.2 路由信息**：`PUT`，`Content-Type: application/json`，需 `LAB_ADMIN` / `SUPER_ADMIN`
- **2.3 接口说明**：审核预约申请。同意后可由管理员把设备置为 RESERVED 并走借用审批流程取件；驳回则记录理由。

**4. 请求参数**

Path：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| id | long | 是 | 预约 ID |

Body：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| approved | boolean | 是 | 审批结果：true-同意，false-驳回 |
| remark | string | 否 | 审批备注/驳回理由 |

**5. 响应参数**（data）：`null`

**6. 请求示例**

```bash
curl -X PUT http://localhost:8080/api/reservations/1/approve \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"approved":true,"remark":"同意预约"}'
```

**7. 数据字典**：预约状态见【数据字典 6】。

---

### 接口 41：取消预约

**1. 接口状态**：已完成

**2. 接口基础信息**

- **2.1 接口路径**：`/api/reservations/{id}/cancel`
- **2.2 路由信息**：`PUT`，需登录
- **2.3 接口说明**：学生取消自己的预约。

**4. 请求参数**（Path）

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| id | long | 是 | 预约 ID |

**5. 响应参数**（data）：`null`

**6. 请求示例**

```bash
curl -X PUT http://localhost:8080/api/reservations/1/cancel \
  -H "Authorization: Bearer <token>"
```

**7. 数据字典**：预约状态见【数据字典 6】。

---

## 八、故障报修模块（Repair）

> 报修提交需登录；处理报修限 `LAB_ADMIN` / `SUPER_ADMIN`。

### 接口 42：提交报修

**1. 接口状态**：已完成

**2. 接口基础信息**

- **2.1 接口路径**：`/api/repairs`
- **2.2 路由信息**：`POST`，`Content-Type: application/json`，需登录
- **2.3 接口说明**：提交设备故障报修。设备状态置为 REPAIRING，并写设备变更日志。

**4. 请求参数**（Body）

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| deviceId | long | 是 | 设备 ID |
| faultDesc | string | 是 | 故障描述 |
| imageUrl | string | 否 | 故障图片地址 |

**5. 响应参数**（data）：long，报修记录 ID。

**6. 请求示例**

```bash
curl -X POST http://localhost:8080/api/repairs \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"deviceId":1,"faultDesc":"屏幕无显示，无法开机","imageUrl":"/api/files/fault.png"}'
```

**7. 数据字典**：设备状态见【数据字典 2】，报修状态见【数据字典 5】。

---

### 接口 43：我的报修

**1. 接口状态**：已完成

**2. 接口基础信息**

- **2.1 接口路径**：`/api/repairs/my`
- **2.2 路由信息**：`GET`，需登录
- **2.3 接口说明**：查询当前用户提交的报修记录。

**4. 请求参数**（Query）

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| status | string | 否 | - | 报修状态（见数据字典 5） |
| pageNum | int | 否 | 1 | 页码 |
| pageSize | int | 否 | 10 | 每页条数 |

**5. 响应参数**（data）

| 字段 | 类型 | 说明 |
|---|---|---|
| total | long | 总记录数 |
| records | array | 报修记录列表 |
| records[].id | long | 报修 ID |
| records[].deviceId | long | 设备 ID |
| records[].deviceCode | string | 设备编号 |
| records[].deviceName | string | 设备名称 |
| records[].reporterId | long | 报修人 ID |
| records[].reporter | string | 报修人姓名 |
| records[].faultDesc | string | 故障描述 |
| records[].imageUrl | string | 故障图片地址 |
| records[].status | string | 报修状态（见数据字典 5） |
| records[].handlerId | long | 处理人 ID |
| records[].handler | string | 处理人姓名 |
| records[].handleRemark | string | 处理备注 |
| records[].finishTime | string | 完成时间 |
| records[].createTime | string | 报修时间 |

**6. 请求示例**

```bash
curl -X GET "http://localhost:8080/api/repairs/my?pageNum=1&pageSize=10" \
  -H "Authorization: Bearer <token>"
```

**7. 数据字典**：报修状态见【数据字典 5】。

---

### 接口 44：全部报修

**1. 接口状态**：已完成

**2. 接口基础信息**

- **2.1 接口路径**：`/api/repairs`
- **2.2 路由信息**：`GET`，需 `LAB_ADMIN` / `SUPER_ADMIN`
- **2.3 接口说明**：管理员查询全部报修记录，支持按状态、设备筛选。

**4. 请求参数**（Query）

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| status | string | 否 | - | 报修状态（见数据字典 5） |
| deviceId | long | 否 | - | 设备 ID |
| pageNum | int | 否 | 1 | 页码 |
| pageSize | int | 否 | 10 | 每页条数 |

**5. 响应参数**（data）：同【接口 43】。

**6. 请求示例**

```bash
curl -X GET "http://localhost:8080/api/repairs?status=PENDING&pageNum=1&pageSize=10" \
  -H "Authorization: Bearer <token>"
```

**7. 数据字典**：报修状态见【数据字典 5】。

---

### 接口 45：处理报修

**1. 接口状态**：已完成

**2. 接口基础信息**

- **2.1 接口路径**：`/api/repairs/{id}`
- **2.2 路由信息**：`PUT`，`Content-Type: application/json`，需 `LAB_ADMIN` / `SUPER_ADMIN`
- **2.3 接口说明**：更新报修状态及处理备注。状态为 FINISHED 时设备回 IDLE 并记录 finishTime；状态为 SCRAPPED 时设备置 SCRAPPED。同时通过站内信通知报修人。

**4. 请求参数**

Path：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| id | long | 是 | 报修 ID |

Body：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| status | string | 是 | 报修状态（见数据字典 5） |
| handleRemark | string | 否 | 处理备注 |

**5. 响应参数**（data）：`null`

**6. 请求示例**

```bash
curl -X PUT http://localhost:8080/api/repairs/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"status":"REPAIRING","handleRemark":"已安排维修人员处理"}'
```

**7. 数据字典**：报修状态见【数据字典 5】，设备状态见【数据字典 2】。

---

## 九、站内消息模块（Message）

> 权限：需登录，仅操作自己的消息。

### 接口 46：我的消息列表

**1. 接口状态**：已完成

**2. 接口基础信息**

- **2.1 接口路径**：`/api/messages`
- **2.2 路由信息**：`GET`，需登录
- **2.3 接口说明**：分页查询当前用户的站内消息，支持按已读状态筛选。

**4. 请求参数**（Query）

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| isRead | int | 否 | - | 已读状态：0-未读，1-已读（见数据字典 8） |
| pageNum | int | 否 | 1 | 页码 |
| pageSize | int | 否 | 10 | 每页条数 |

**5. 响应参数**（data）

| 字段 | 类型 | 说明 |
|---|---|---|
| total | long | 总记录数 |
| records | array | 消息列表 |
| records[].id | long | 消息 ID |
| records[].userId | long | 接收用户 ID |
| records[].title | string | 消息标题 |
| records[].content | string | 消息内容 |
| records[].type | string | 消息类型 |
| records[].isRead | int | 已读状态（见数据字典 8） |
| records[].createTime | string | 消息时间 |

**6. 请求示例**

```bash
curl -X GET "http://localhost:8080/api/messages?isRead=0&pageNum=1&pageSize=10" \
  -H "Authorization: Bearer <token>"
```

**7. 数据字典**：消息已读状态见【数据字典 8】。

---

### 接口 47：未读消息数量

**1. 接口状态**：已完成

**2. 接口基础信息**

- **2.1 接口路径**：`/api/messages/unread-count`
- **2.2 路由信息**：`GET`，需登录
- **2.3 接口说明**：获取当前用户未读消息数量。

**4. 请求参数**：无

**5. 响应参数**（data）：long，未读消息数量。

**6. 请求示例**

```bash
curl -X GET http://localhost:8080/api/messages/unread-count \
  -H "Authorization: Bearer <token>"
```

响应示例：
```json
{ "code": 200, "msg": "success", "data": 3 }
```

**7. 数据字典**：无

---

### 接口 48：标记消息已读

**1. 接口状态**：已完成

**2. 接口基础信息**

- **2.1 接口路径**：`/api/messages/{id}/read`
- **2.2 路由信息**：`PUT`，需登录
- **2.3 接口说明**：将指定消息标记为已读。

**4. 请求参数**（Path）

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| id | long | 是 | 消息 ID |

**5. 响应参数**（data）：`null`

**6. 请求示例**

```bash
curl -X PUT http://localhost:8080/api/messages/1/read \
  -H "Authorization: Bearer <token>"
```

**7. 数据字典**：无

---

### 接口 49：全部消息标记已读

**1. 接口状态**：已完成

**2. 接口基础信息**

- **2.1 接口路径**：`/api/messages/read-all`
- **2.2 路由信息**：`PUT`，需登录
- **2.3 接口说明**：将当前用户所有未读消息标记为已读。

**4. 请求参数**：无

**5. 响应参数**（data）：`null`

**6. 请求示例**

```bash
curl -X PUT http://localhost:8080/api/messages/read-all \
  -H "Authorization: Bearer <token>"
```

**7. 数据字典**：无

---

## 十、统计报表模块（Stats）

> 权限：基础统计需登录；详细统计需 `LAB_ADMIN` / `SUPER_ADMIN`。

### 接口 50：首页看板概览

**1. 接口状态**：已完成

**2. 接口基础信息**

- **2.1 接口路径**：`/api/stats/overview`
- **2.2 路由信息**：`GET`，需登录
- **2.3 接口说明**：首页看板数据，含设备各状态数量、设备总数、本月借用数、待审批数、逾期数、待处理报修数。

**4. 请求参数**：无

**5. 响应参数**（data）

| 字段 | 类型 | 说明 |
|---|---|---|
| deviceTotal | long | 设备总数 |
| idleCount | long | 空闲设备数 |
| borrowedCount | long | 借出设备数 |
| repairingCount | long | 维修中设备数 |
| scrappedCount | long | 报废设备数 |
| reservedCount | long | 预留设备数 |
| monthBorrowCount | long | 本月借用数 |
| pendingBorrowCount | long | 待审批借用数 |
| overdueCount | long | 逾期未还数 |
| pendingRepairCount | long | 待处理报修数 |

**6. 请求示例**

```bash
curl -X GET http://localhost:8080/api/stats/overview \
  -H "Authorization: Bearer <token>"
```

响应示例：
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "deviceTotal": 120,
    "idleCount": 80,
    "borrowedCount": 20,
    "repairingCount": 5,
    "scrappedCount": 10,
    "reservedCount": 5,
    "monthBorrowCount": 45,
    "pendingBorrowCount": 3,
    "overdueCount": 2,
    "pendingRepairCount": 4
  }
}
```

**7. 数据字典**：设备状态见【数据字典 2】。

---

### 接口 51：设备状态分布

**1. 接口状态**：已完成

**2. 接口基础信息**

- **2.1 接口路径**：`/api/stats/device-by-status`
- **2.2 路由信息**：`GET`，需登录
- **2.3 接口说明**：按设备状态统计数量，用于饼图展示。

**4. 请求参数**：无

**5. 响应参数**（data）：array

| 字段 | 类型 | 说明 |
|---|---|---|
| name | string | 状态名称（中文：空闲/借出/维修中/报废/预留） |
| value | long | 数量 |

**6. 请求示例**

```bash
curl -X GET http://localhost:8080/api/stats/device-by-status \
  -H "Authorization: Bearer <token>"
```

响应示例：
```json
{
  "code": 200,
  "msg": "success",
  "data": [
    { "name": "空闲", "value": 80 },
    { "name": "借出", "value": 20 },
    { "name": "维修中", "value": 5 },
    { "name": "报废", "value": 10 },
    { "name": "预留", "value": 5 }
  ]
}
```

**7. 数据字典**：设备状态见【数据字典 2】。

---

### 接口 52：各实验室设备数量

**1. 接口状态**：已完成

**2. 接口基础信息**

- **2.1 接口路径**：`/api/stats/device-by-lab`
- **2.2 路由信息**：`GET`，需登录
- **2.3 接口说明**：按实验室统计设备数量。

**4. 请求参数**：无

**5. 响应参数**（data）：array

| 字段 | 类型 | 说明 |
|---|---|---|
| name | string | 实验室名称 |
| value | long | 设备数量 |

**6. 请求示例**

```bash
curl -X GET http://localhost:8080/api/stats/device-by-lab \
  -H "Authorization: Bearer <token>"
```

**7. 数据字典**：无

---

### 接口 53：近 N 月借用次数

**1. 接口状态**：已完成

**2. 接口基础信息**

- **2.1 接口路径**：`/api/stats/borrow-monthly`
- **2.2 路由信息**：`GET`，需 `LAB_ADMIN` / `SUPER_ADMIN`
- **2.3 接口说明**：统计近 N 个月每月的借用次数，用于折线图。

**4. 请求参数**（Query）

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| months | int | 否 | 6 | 统计最近 N 个月 |

**5. 响应参数**（data）：array

| 字段 | 类型 | 说明 |
|---|---|---|
| month | string | 月份（yyyy-MM） |
| count | long | 借用次数 |

**6. 请求示例**

```bash
curl -X GET "http://localhost:8080/api/stats/borrow-monthly?months=6" \
  -H "Authorization: Bearer <token>"
```

响应示例：
```json
{
  "code": 200,
  "msg": "success",
  "data": [
    { "month": "2026-05", "count": 30 },
    { "month": "2026-06", "count": 42 },
    { "month": "2026-07", "count": 15 },
    { "month": "2026-08", "count": 28 },
    { "month": "2026-09", "count": 50 },
    { "month": "2026-10", "count": 45 }
  ]
}
```

**7. 数据字典**：无

---

### 接口 54：热门设备排行

**1. 接口状态**：已完成

**2. 接口基础信息**

- **2.1 接口路径**：`/api/stats/top-devices`
- **2.2 路由信息**：`GET`，需 `LAB_ADMIN` / `SUPER_ADMIN`
- **2.3 接口说明**：按借用次数排行的热门设备列表。

**4. 请求参数**（Query）

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| limit | int | 否 | 10 | 返回前 N 名 |

**5. 响应参数**（data）：array

| 字段 | 类型 | 说明 |
|---|---|---|
| name | string | 设备名称 |
| count | long | 借用次数 |

**6. 请求示例**

```bash
curl -X GET "http://localhost:8080/api/stats/top-devices?limit=10" \
  -H "Authorization: Bearer <token>"
```

**7. 数据字典**：无

---

### 接口 55：逾期统计

**1. 接口状态**：已完成

**2. 接口基础信息**

- **2.1 接口路径**：`/api/stats/overdue`
- **2.2 路由信息**：`GET`，需 `LAB_ADMIN` / `SUPER_ADMIN`
- **2.3 接口说明**：逾期记录统计。

**4. 请求参数**：无

**5. 响应参数**（data）

| 字段 | 类型 | 说明 |
|---|---|---|
| total | long | 逾期记录总数 |
| unhandled | long | 未处理逾期数 |
| handled | long | 已处理逾期数 |
| currentOverdueBorrows | long | 当前逾期未还借用单数 |

**6. 请求示例**

```bash
curl -X GET http://localhost:8080/api/stats/overdue \
  -H "Authorization: Bearer <token>"
```

响应示例：
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "total": 25,
    "unhandled": 2,
    "handled": 23,
    "currentOverdueBorrows": 2
  }
}
```

**7. 数据字典**：逾期处理状态见【数据字典 10】。

---

### 接口 56：报修统计

**1. 接口状态**：已完成

**2. 接口基础信息**

- **2.1 接口路径**：`/api/stats/repair-summary`
- **2.2 路由信息**：`GET`，需 `LAB_ADMIN` / `SUPER_ADMIN`
- **2.3 接口说明**：报修统计，含各状态报修数量及高频故障设备。

**4. 请求参数**：无

**5. 响应参数**（data）

| 字段 | 类型 | 说明 |
|---|---|---|
| byStatus | array | 各状态报修数量 |
| byStatus[].name | string | 状态名称（中文） |
| byStatus[].value | long | 数量 |
| topFaultDevices | array | 高频故障设备排行 |
| topFaultDevices[].name | string | 设备名称 |
| topFaultDevices[].count | long | 报修次数 |

**6. 请求示例**

```bash
curl -X GET http://localhost:8080/api/stats/repair-summary \
  -H "Authorization: Bearer <token>"
```

响应示例：
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "byStatus": [
      { "name": "待处理", "value": 4 },
      { "name": "维修中", "value": 3 },
      { "name": "维修完成", "value": 20 },
      { "name": "报废", "value": 2 }
    ],
    "topFaultDevices": [
      { "name": "数字示波器", "count": 8 },
      { "name": "万用表", "count": 5 }
    ]
  }
}
```

**7. 数据字典**：报修状态见【数据字典 5】。

---

### 接口 57：学生借用排行

**1. 接口状态**：已完成

**2. 接口基础信息**

- **2.1 接口路径**：`/api/stats/user-rank`
- **2.2 路由信息**：`GET`，需 `LAB_ADMIN` / `SUPER_ADMIN`
- **2.3 接口说明**：按借用次数排行的学生列表。

**4. 请求参数**（Query）

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| limit | int | 否 | 10 | 返回前 N 名 |

**5. 响应参数**（data）：array

| 字段 | 类型 | 说明 |
|---|---|---|
| name | string | 学生姓名 |
| count | long | 借用次数 |

**6. 请求示例**

```bash
curl -X GET "http://localhost:8080/api/stats/user-rank?limit=10" \
  -H "Authorization: Bearer <token>"
```

**7. 数据字典**：无

---

## 十一、系统日志模块（Log）

> 权限：仅 `SUPER_ADMIN`。

### 接口 58：操作日志

**1. 接口状态**：已完成

**2. 接口基础信息**

- **2.1 接口路径**：`/api/logs/operations`
- **2.2 路由信息**：`GET`，需 `SUPER_ADMIN`
- **2.3 接口说明**：分页查询操作日志，支持按操作人、模块筛选。

**4. 请求参数**（Query）

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| keyword | string | 否 | - | 操作人模糊搜索 |
| module | string | 否 | - | 模块（auth/user/lab/category/device/borrow/reservation/repair 等） |
| pageNum | int | 否 | 1 | 页码 |
| pageSize | int | 否 | 10 | 每页条数 |

**5. 响应参数**（data）

| 字段 | 类型 | 说明 |
|---|---|---|
| total | long | 总记录数 |
| records | array | 操作日志列表 |
| records[].id | long | 日志 ID |
| records[].userId | long | 操作人 ID |
| records[].username | string | 操作人用户名 |
| records[].module | string | 模块 |
| records[].operation | string | 操作动作 |
| records[].detail | string | 操作详情 |
| records[].ip | string | 操作 IP |
| records[].createTime | string | 操作时间 |

**6. 请求示例**

```bash
curl -X GET "http://localhost:8080/api/logs/operations?module=device&pageNum=1&pageSize=10" \
  -H "Authorization: Bearer <token>"
```

**7. 数据字典**：无

---

### 接口 59：登录日志

**1. 接口状态**：已完成

**2. 接口基础信息**

- **2.1 接口路径**：`/api/logs/logins`
- **2.2 路由信息**：`GET`，需 `SUPER_ADMIN`
- **2.3 接口说明**：分页查询登录日志。

**4. 请求参数**（Query）

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| pageNum | int | 否 | 1 | 页码 |
| pageSize | int | 否 | 10 | 每页条数 |

**5. 响应参数**（data）

| 字段 | 类型 | 说明 |
|---|---|---|
| total | long | 总记录数 |
| records | array | 登录日志列表 |
| records[].id | long | 日志 ID |
| records[].userId | long | 用户 ID |
| records[].username | string | 用户名 |
| records[].ip | string | 登录 IP |
| records[].status | int | 登录状态（见数据字典 9） |
| records[].createTime | string | 登录时间 |

**6. 请求示例**

```bash
curl -X GET "http://localhost:8080/api/logs/logins?pageNum=1&pageSize=10" \
  -H "Authorization: Bearer <token>"
```

**7. 数据字典**：登录状态见【数据字典 9】。

---

## 十二、文件上传模块（File）

### 接口 60：文件上传

**1. 接口状态**：已完成

**2. 接口基础信息**

- **2.1 接口路径**：`/api/files/upload`
- **2.2 路由信息**：`POST`，`Content-Type: multipart/form-data`，需登录
- **2.3 接口说明**：上传文件，返回可访问的 URL。存储层抽象 StorageService，默认本地磁盘（可配置目录），预留云端存储实现。单个文件最大 10MB。

**4. 请求参数**（FormData）

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| file | file | 是 | 上传的文件（最大 10MB） |

**5. 响应参数**（data）

| 字段 | 类型 | 说明 |
|---|---|---|
| url | string | 文件访问 URL（如 `/api/files/xxx.png`） |

**6. 请求示例**

```bash
curl -X POST http://localhost:8080/api/files/upload \
  -H "Authorization: Bearer <token>" \
  -F "file=@photo.png"
```

响应示例：
```json
{ "code": 200, "msg": "success", "data": { "url": "/api/files/1696234567890_photo.png" } }
```

**7. 数据字典**：无

---

### 接口 61：静态文件访问

**1. 接口状态**：已完成

**2. 接口基础信息**

- **2.1 接口路径**：`/api/files/{filename}`
- **2.2 路由信息**：`GET`，无需认证
- **2.3 接口说明**：访问已上传的静态文件。防目录穿越（禁止 `..`、`/`、`\`），支持图片、PDF、Excel、Word 等常见类型的 Content-Type 自动识别。

**4. 请求参数**（Path）

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| filename | string | 是 | 文件名 |

**5. 响应参数**：二进制流（根据文件类型返回对应 Content-Type）。

**6. 请求示例**

```bash
curl -X GET http://localhost:8080/api/files/1696234567890_photo.png \
  -o photo.png
```

**7. 数据字典**：无

---

## 十三、角色模块（Role）

### 接口 62：角色列表

**1. 接口状态**：已完成

**2. 接口基础信息**

- **2.1 接口路径**：`/api/roles`
- **2.2 路由信息**：`GET`，需登录
- **2.3 接口说明**：获取全部角色列表（下拉选择用）。

**4. 请求参数**：无

**5. 响应参数**（data）：array

| 字段 | 类型 | 说明 |
|---|---|---|
| id | long | 角色 ID |
| code | string | 角色编码（见数据字典 1） |
| name | string | 角色名称 |
| description | string | 角色描述 |
| createTime | string | 创建时间 |

**6. 请求示例**

```bash
curl -X GET http://localhost:8080/api/roles \
  -H "Authorization: Bearer <token>"
```

响应示例：
```json
{
  "code": 200,
  "msg": "success",
  "data": [
    { "id": 1, "code": "STUDENT", "name": "学生", "description": "学生用户", "createTime": "2026-01-01 00:00:00" },
    { "id": 2, "code": "LAB_ADMIN", "name": "实验室管理员", "description": "实验室管理员", "createTime": "2026-01-01 00:00:00" },
    { "id": 3, "code": "SUPER_ADMIN", "name": "超级管理员", "description": "超级管理员", "createTime": "2026-01-01 00:00:00" }
  ]
}
```

**7. 数据字典**：角色见【数据字典 1】。

---

## 附录 A：定时任务（Spring @Scheduled）

| 任务 | 频率 | 说明 |
|---|---|---|
| 逾期扫描 | 每 30 分钟 | 扫描 `BORROWED` 且 now > due_time 的借用单，状态置 `OVERDUE`，写 `overdue_record`，用户 `overdue_count+1`，发站内信提醒 |
| 预约过期扫描 | 每 30 分钟 | 扫描 `APPROVED` 预约到期未取的记录，状态置 `EXPIRED`，设备状态 `RESERVED → IDLE` |

## 附录 B：角色权限矩阵

| 功能模块 | 学生 (STUDENT) | 实验室管理员 (LAB_ADMIN) | 超级管理员 (SUPER_ADMIN) |
|---|:---:|:---:|:---:|
| 认证（登录/注册/改密） | ✅ | ✅ | ✅ |
| 设备查看/导出 | ✅ | ✅ | ✅ |
| 设备管理（CRUD/导入） | ❌ | ✅ | ✅ |
| 借用申请/我的借用 | ✅ | ✅ | ✅ |
| 借用审批/取件/归还 | ❌ | ✅ | ✅ |
| 预约申请/取消 | ✅ | ✅ | ✅ |
| 预约审核 | ❌ | ✅ | ✅ |
| 报修提交/我的报修 | ✅ | ✅ | ✅ |
| 报修处理 | ❌ | ✅ | ✅ |
| 站内消息 | ✅ | ✅ | ✅ |
| 基础统计（概览/设备分布） | ✅ | ✅ | ✅ |
| 详细统计（借用/排行/逾期/报修） | ❌ | ✅ | ✅ |
| 操作日志 | ❌ | ❌ | ✅ |
| 登录日志 | ❌ | ❌ | ✅ |
| 用户管理 | ❌ | 仅查看 | ✅ |
| 实验室管理 | ❌ | ✅ | ✅ |
| 分类管理 | 仅查看 | ✅ | ✅ |
| 文件上传 | ✅ | ✅ | ✅ |

---

> **文档说明**：本文档根据 [backend/src/main/java/com/lab/controller](file:///d:/ruankai/lab-equipment-system/backend/src/main/java/com/lab/controller) 目录下全部 13 个 Controller 及对应 DTO、VO、Entity 源码整理生成，共 62 个接口。所有接口状态均为"已完成"（代码已实现）。

