"""角色权限装饰器（对应原 annotation/RequireRole.java + AOP 校验）"""
from functools import wraps

from apps.common.exceptions import ForbiddenError


def require_role(*role_codes):
    """
    用法：@require_role("SUPER_ADMIN") 或 @require_role("LAB_ADMIN", "SUPER_ADMIN")
    校验 request.role_code（由 AuthBearer 注入），不匹配则抛 403
    """

    def decorator(func):
        @wraps(func)
        def wrapper(request, *args, **kwargs):
            if getattr(request, "role_code", None) not in role_codes:
                raise ForbiddenError()
            return func(request, *args, **kwargs)

        return wrapper

    return decorator
