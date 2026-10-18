"""业务异常（对应原 common/BusinessException.java）"""


class BusinessError(Exception):
    """业务校验失败 → HTTP 200 + code 400 + msg"""

    def __init__(self, msg: str):
        self.msg = msg
        super().__init__(msg)


class ForbiddenError(Exception):
    """无权限 → HTTP 200 + code 403 + msg"""

    def __init__(self, msg: str = "无权限执行此操作"):
        self.msg = msg
        super().__init__(msg)
