"""账号域服务：登录认证 / 用户信息序列化"""
import bcrypt

from apps.accounts.models import LoginLog, SysUser


def verify_password(plain: str, hashed: str) -> bool:
    """校验 BCrypt 密码（兼容 Java 版 BCryptPasswordEncoder 生成的 $2a$ 前缀）"""
    try:
        return bcrypt.checkpw(plain.encode("utf-8"), hashed.encode("utf-8"))
    except ValueError:
        return False


def user_info(user: SysUser) -> dict:
    """用户信息转 dict（camelCase 契约）"""
    return {
        "id": user.id,
        "username": user.username,
        "realName": user.real_name,
        "college": user.college,
        "phone": user.phone,
        "email": user.email,
        "roleId": user.role_id,
        "roleCode": user.role.code,
        "roleName": user.role.name,
        "overdueCount": user.overdue_count,
    }


def write_login_log(user, username: str, ip: str, success: bool) -> None:
    LoginLog.objects.create(
        user=user if user else None,
        username=username,
        ip=ip or None,
        status=1 if success else 0,
    )


def authenticate(username: str, password: str, ip: str = "") -> SysUser:
    """
    登录校验（对应原 AuthServiceImpl.login）：
    成功返回用户；失败抛 BusinessException 并写登录日志
    """
    from apps.common.exceptions import BusinessError

    user = SysUser.objects.select_related("role").filter(username=username).first()
    if user is None or not verify_password(password, user.password):
        write_login_log(None, username, ip, success=False)
        raise BusinessError("用户名或密码错误")
    if user.status != 1:
        write_login_log(user, username, ip, success=False)
        raise BusinessError("账号已被禁用，请联系管理员")
    write_login_log(user, username, ip, success=True)
    return user
