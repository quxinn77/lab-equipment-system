"""
JWT 签发 / 校验 / 登出黑名单（对应原 util/JwtUtil.java + B4 登出失效功能）

- 黑名单沿用 B4 的方案：进程内存 Set（单机部署够用，重启后自然清空）
- 载荷：{"uid": 用户id, "roleCode": 角色编码, "exp": 过期时间戳}
"""
import threading
import time

import jwt
from django.conf import settings
from ninja.security import HttpBearer

_blacklist: set = set()
_blacklist_lock = threading.Lock()


def create_token(uid: int, role_code: str) -> str:
    expire = int(time.time()) + settings.JWT_EXPIRE_HOURS * 3600
    payload = {"uid": uid, "roleCode": role_code, "exp": expire}
    return jwt.encode(payload, settings.JWT_SECRET, algorithm="HS256")


def logout_token(token: str) -> None:
    """登出时把 token 加入黑名单，使其立即失效（B4）"""
    with _blacklist_lock:
        _blacklist.add(token)


def is_blacklisted(token: str) -> bool:
    with _blacklist_lock:
        return token in _blacklist


class AuthBearer(HttpBearer):
    """
    全局认证类（对应原 interceptor/JwtInterceptor.java + UserContext）：
    校验通过后把 uid / role_code 挂到 request 上，各接口直接用
    request.uid 取当前用户，等价于原来的 UserContext.userId()
    """

    def authenticate(self, request, token):
        if not token or is_blacklisted(token):
            return None
        try:
            payload = jwt.decode(token, settings.JWT_SECRET, algorithms=["HS256"])
        except jwt.PyJWTError:
            return None
        request.uid = payload.get("uid")
        request.role_code = payload.get("roleCode")
        return token
