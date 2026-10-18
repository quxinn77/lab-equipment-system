"""统一响应格式（对应原 common/Result.java）"""

def ok(data=None, msg="success"):
    """成功响应：{"code": 200, "msg": "success", "data": ...}"""
    return {"code": 200, "msg": msg, "data": data}


def fail(msg: str, code: int):
    """失败响应体（HTTP 状态码仍为 200，与 Java 版 GlobalExceptionHandler 行为一致）"""
    return {"code": code, "msg": msg, "data": None}
