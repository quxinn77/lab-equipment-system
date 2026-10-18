"""认证接口：登录 / 登出（B4）/ 当前用户"""
from ninja import Router

from apps.accounts.schemas import LoginIn
from apps.accounts.services import authenticate, user_info
from apps.common.auth import create_token, logout_token
from apps.common.responses import ok

router = Router()


def client_ip(request) -> str:
    return request.META.get("REMOTE_ADDR", "")


@router.post("/login", auth=None)
def login(request, payload: LoginIn):
    user = authenticate(payload.username, payload.password, ip=client_ip(request))
    token = create_token(user.id, user.role.code)
    return ok({"token": token, "user": user_info(user)})


@router.post("/logout")
def logout(request):
    """B4：登出后当前 token 立即失效（内存黑名单）"""
    token = getattr(request, "auth", None)
    if token:
        logout_token(token)
    return ok(msg="退出成功")


@router.get("/info")
def info(request):
    from apps.accounts.models import SysUser

    user = SysUser.objects.select_related("role").get(id=request.uid)
    return ok(user_info(user))
