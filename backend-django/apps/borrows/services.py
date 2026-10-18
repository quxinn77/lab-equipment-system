"""
借用域服务（B 部分核心业务，翻译自原 BorrowServiceImpl.java）

- B1：归还权限 = 申请人本人 或 管理员（LAB_ADMIN/SUPER_ADMIN）
- B2：归还状态放宽为 APPROVED / BORROWED / OVERDUE（配合去取件环节的状态机）
- B3：借用申请拦截改为库存公式 可借数 = 总库存 - (PENDING + APPROVED 单数)
"""
import random
import string
from datetime import datetime

from django.db import transaction
from django.db.models import Count

from apps.accounts.models import SysConfig, SysUser
from apps.borrows.models import BorrowRecord
from apps.common.exceptions import BusinessError, ForbiddenError
from apps.devices.models import Device, DeviceLog
from apps.repairs.models import RepairRecord

DT_FMT = "%Y-%m-%d %H:%M:%S"


def _gen_record_no() -> str:
    """生成唯一申请编号：BR + 年月日时分秒 + 4 位随机串"""
    while True:
        no = "BR" + datetime.now().strftime("%Y%m%d%H%M%S") + "".join(
            random.choices(string.ascii_uppercase + string.digits, k=4)
        )
        if not BorrowRecord.objects.filter(record_no=no).exists():
            return no


def borrow_vo(record: BorrowRecord) -> dict:
    """借用单 VO（camelCase + 时间字符串，对齐 Java 版 Jackson 格式）"""
    device = record.device
    lab = device.lab if device else None
    return {
        "id": record.id,
        "recordNo": record.record_no,
        "userId": record.user_id,
        "userName": record.user.real_name if record.user else None,
        "deviceId": record.device_id,
        "deviceName": device.name if device else None,
        "deviceCode": device.code if device else None,
        "labName": lab.name if lab else None,
        "startTime": record.start_time.strftime(DT_FMT) if record.start_time else None,
        "dueTime": record.due_time.strftime(DT_FMT) if record.due_time else None,
        "actualReturnTime": record.actual_return_time.strftime(DT_FMT) if record.actual_return_time else None,
        "purpose": record.purpose,
        "status": record.status,
        "approver": record.approver,
        "approveTime": record.approve_time.strftime(DT_FMT) if record.approve_time else None,
        "approveRemark": record.approve_remark,
        "returnCondition": record.return_condition,
        "returnRemark": record.return_remark,
        "compensation": float(record.compensation) if record.compensation is not None else None,
        "createTime": record.create_time.strftime(DT_FMT) if record.create_time else None,
    }


@transaction.atomic
def apply(uid: int, payload) -> int:
    """提交借用申请（B3 库存公式拦截）"""
    device = Device.objects.select_for_update().filter(id=payload.deviceId).first()
    if device is None:
        raise BusinessError("设备不存在")
    if device.status in ("REPAIRING", "SCRAPPED"):
        raise BusinessError(f"设备当前状态为 {device.status}，不可提交借用申请")

    # B3：可借数 = 总库存 - (PENDING + APPROVED 的借用单数)
    total_qty = device.total_qty if device.total_qty else 1
    occupied = BorrowRecord.objects.filter(
        device_id=device.id, status__in=["PENDING", "APPROVED"]
    ).count()
    available_qty = total_qty - occupied
    if available_qty <= 0:
        raise BusinessError(f"该设备库存不足：总库存 {total_qty}，已全部被预约或借出")

    # 时间合法性
    try:
        start = datetime.strptime(payload.startTime, DT_FMT)
        due = datetime.strptime(payload.dueTime, DT_FMT)
    except ValueError:
        raise BusinessError("时间格式不合法，应为 yyyy-MM-dd HH:mm:ss")
    if start >= due:
        raise BusinessError("借用时间不合法：开始时间须早于截止时间")
    if start.timestamp() < datetime.now().timestamp() - 60:
        raise BusinessError("开始时间不能早于当前时间")

    # 逾期未还禁止申请（配置 overdue.limit.apply）
    if SysConfig.get_value("overdue.limit.apply", "1") == "1":
        overdue_count = BorrowRecord.objects.filter(user_id=uid, status="OVERDUE").count()
        if overdue_count > 0:
            raise BusinessError(f"您存在 {overdue_count} 笔逾期未还记录，禁止提交新的借用申请")

    # 逾期次数超上限禁止申请（配置 overdue.max.times）
    user = SysUser.objects.filter(id=uid).first()
    max_times = int(SysConfig.get_value("overdue.max.times", "3"))
    if user and user.overdue_count >= max_times:
        raise BusinessError(f"您的逾期次数已达 {user.overdue_count} 次（上限 {max_times}），禁止借用")

    record = BorrowRecord.objects.create(
        record_no=_gen_record_no(),
        user_id=uid,
        device_id=device.id,
        start_time=start,
        due_time=due,
        purpose=payload.purpose or None,
        status="PENDING",
    )
    return record.id


def my_records(uid: int, status: str, page_num: int, page_size: int) -> dict:
    """我的借用记录分页（B5 页面数据源）"""
    qs = BorrowRecord.objects.select_related("user", "device", "device__lab").filter(user_id=uid)
    if status:
        qs = qs.filter(status=status)
    total = qs.count()
    rows = qs.order_by("-id")[(page_num - 1) * page_size: page_num * page_size]
    return {"total": total, "records": [borrow_vo(r) for r in rows]}


@transaction.atomic
def return_device(uid: int, role_code: str, record_id: int, payload) -> None:
    """归还登记（B1 权限 + B2 状态放宽）"""
    record = BorrowRecord.objects.select_related("device").filter(id=record_id).first()
    if record is None:
        raise BusinessError("借用单不存在")

    # B1：申请人本人 或 管理员
    is_admin = role_code in ("LAB_ADMIN", "SUPER_ADMIN")
    if uid != record.user_id and not is_admin:
        raise ForbiddenError("仅申请人本人或管理员可执行归还")

    # B2：APPROVED（未取直接归还）/ BORROWED / OVERDUE 均可归还
    if record.status not in ("APPROVED", "BORROWED", "OVERDUE"):
        raise BusinessError(f"当前状态为 {record.status}，不可归还")

    record.status = "RETURNED"
    record.actual_return_time = datetime.now()
    record.return_condition = payload.condition
    record.return_remark = payload.remark or None
    record.compensation = payload.compensation
    record.save(update_fields=[
        "status", "actual_return_time", "return_condition", "return_remark", "compensation", "update_time",
    ])

    device = record.device
    if device and device.status in ("BORROWED", "RESERVED"):
        old_status = device.status
        device.status = "IDLE"
        device.save(update_fields=["status", "update_time"])
        DeviceLog.objects.create(
            device=device,
            device_code=device.code,
            operator_user_id=uid,
            action="STATUS_CHANGE",
            old_status=old_status,
            new_status="IDLE",
            detail=f"归还登记，单号 {record.record_no}",
        )

    # 损坏归还：自动创建报修单
    if payload.condition == "DAMAGED" and device:
        RepairRecord.objects.create(
            device=device,
            reporter_id=uid,
            fault_desc=f"归还时发现损坏（借用单 {record.record_no}）：{payload.remark or '无备注'}",
            status="PENDING",
        )


def occupied_map(device_ids: list) -> dict:
    """批量统计设备的占用单数（PENDING+APPROVED），供设备列表算可借数"""
    rows = (
        BorrowRecord.objects
        .filter(device_id__in=device_ids, status__in=["PENDING", "APPROVED"])
        .values("device_id")
        .annotate(n=Count("id"))
    )
    return {r["device_id"]: r["n"] for r in rows}
