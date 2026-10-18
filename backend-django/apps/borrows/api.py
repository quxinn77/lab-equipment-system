"""借用接口：申请 / 我的记录 / 归还（B1/B2/B3）"""
from ninja import Router

from apps.borrows.schemas import BorrowApplyIn, ReturnIn
from apps.borrows.services import apply, my_records, return_device
from apps.common.responses import ok

router = Router()


@router.post("")
def submit_apply(request, payload: BorrowApplyIn):
    """提交借用申请（学生）→ 返回单据 id"""
    record_id = apply(request.uid, payload)
    return ok(record_id, msg="申请已提交，等待审批")


@router.get("/my")
def my_borrows(request, pageNum: int = 1, pageSize: int = 10, status: str = None):
    """我的借用记录分页（B5 页面数据源）"""
    return ok(my_records(request.uid, status, pageNum, pageSize))


@router.put("/{record_id}/return")
def do_return(request, record_id: int, payload: ReturnIn):
    """归还登记（B1 申请人本人或管理员；B2 允许 APPROVED 直接归还）"""
    return_device(request.uid, request.role_code, record_id, payload)
    return ok(msg="归还成功")
