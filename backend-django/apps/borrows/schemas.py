"""借用域 Schema（字段名对齐前端契约 camelCase）"""
from ninja import Schema


class BorrowApplyIn(Schema):
    deviceId: int
    startTime: str  # "yyyy-MM-dd HH:mm:ss"
    dueTime: str
    purpose: str = ""


class ReturnIn(Schema):
    condition: str  # INTACT / DAMAGED
    remark: str = ""
    compensation: float = None
