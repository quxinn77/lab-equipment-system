"""账号域 Schema（字段名直接用前端契约的 camelCase）"""
from typing import Optional

from ninja import Schema


class LoginIn(Schema):
    username: str
    password: str


class UserInfoOut(Schema):
    id: int
    username: str
    realName: Optional[str] = None
    college: Optional[str] = None
    phone: Optional[str] = None
    email: Optional[str] = None
    roleId: Optional[int] = None
    roleCode: Optional[str] = None
    roleName: Optional[str] = None
    overdueCount: int = 0


class LoginOut(Schema):
    token: str
    user: UserInfoOut
