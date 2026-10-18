"""
NinjaAPI 实例 + 全局异常处理（对应原 config/WebConfig.java + GlobalExceptionHandler.java）

- 默认所有接口需要登录（api 级 auth），公开接口在注册时传 auth=None
- 所有错误统一 HTTP 200 + body 内 code 区分（401/403/400/500），与 Java 版行为一致
"""
import logging

from django.http import Http404
from ninja import NinjaAPI
from ninja.errors import AuthenticationError

from apps.common.auth import AuthBearer
from apps.common.exceptions import BusinessError, ForbiddenError
from apps.common.responses import fail

logger = logging.getLogger("django")

api = NinjaAPI(
    title="实验室设备借用管理系统 API",
    version="1.0",
    auth=AuthBearer(),  # 全局默认：必须登录；公开接口注册时传 auth=None
)


@api.exception_handler(BusinessError)
def business_error_handler(request, exc: BusinessError):
    return api.create_response(request, fail(exc.msg, 400), status=200)


@api.exception_handler(ForbiddenError)
def forbidden_error_handler(request, exc: ForbiddenError):
    return api.create_response(request, fail(exc.msg, 403), status=200)


@api.exception_handler(AuthenticationError)
def auth_error_handler(request, exc: AuthenticationError):
    return api.create_response(request, fail("未登录或登录已过期", 401), status=200)


@api.exception_handler(Exception)
def generic_error_handler(request, exc: Exception):
    if isinstance(exc, Http404):
        return api.create_response(request, fail("资源不存在", 404), status=200)
    logger.exception("系统错误: %s", exc)
    return api.create_response(request, fail("系统错误，请稍后重试", 500), status=200)
