"""设备接口：只读列表 / 详情（登录即可，B6 页面数据源）"""
from ninja import Router
from django.db.models import Q

from apps.borrows.services import occupied_map
from apps.common.responses import ok
from apps.devices.models import Device

router = Router()


def device_vo(d: Device, available_qty: int) -> dict:
    return {
        "id": d.id,
        "code": d.code,
        "name": d.name,
        "model": d.model,
        "spec": d.spec,
        "brand": d.brand,
        "categoryId": d.category_id,
        "categoryName": d.category.name if d.category else None,
        "labId": d.lab_id,
        "labName": d.lab.name if d.lab else None,
        "status": d.status,
        "totalQty": d.total_qty or 1,
        "availableQty": max(available_qty, 0),
        "purchaseDate": d.purchase_date.strftime("%Y-%m-%d") if d.purchase_date else None,
        "originalValue": float(d.original_value) if d.original_value is not None else None,
        "imageUrl": d.image_url,
        "remark": d.remark,
    }


@router.get("")
def list_devices(
    request,
    pageNum: int = 1,
    pageSize: int = 10,
    keyword: str = None,
    labId: int = None,
    categoryId: int = None,
    status: str = None,
):
    """设备分页检索；可借数 = 总库存 - (PENDING + APPROVED 单数)，动态计算"""
    qs = Device.objects.select_related("lab", "category").filter(deleted=0)
    if keyword:
        qs = qs.filter(Q(name__icontains=keyword) | Q(code__icontains=keyword))
    if labId:
        qs = qs.filter(lab_id=labId)
    if categoryId:
        qs = qs.filter(category_id=categoryId)
    if status:
        qs = qs.filter(status=status)

    total = qs.count()
    rows = list(qs.order_by("id")[(pageNum - 1) * pageSize: pageNum * pageSize])
    occ = occupied_map([d.id for d in rows])
    data = {
        "total": total,
        "records": [device_vo(d, (d.total_qty or 1) - occ.get(d.id, 0)) for d in rows],
    }
    return ok(data)
