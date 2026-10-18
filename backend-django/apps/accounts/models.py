"""账号域：用户 / 角色 / 权限 / 日志 / 系统配置（表已由 Java 版创建，managed=False 只映射不建表）"""
from django.db import models


class Role(models.Model):
    id = models.BigAutoField(primary_key=True)
    code = models.CharField(max_length=32, unique=True, db_comment="角色编码 STUDENT/LAB_ADMIN/SUPER_ADMIN")
    name = models.CharField(max_length=64, db_comment="角色名称")
    description = models.CharField(max_length=255, blank=True, null=True)
    create_time = models.DateTimeField(blank=True, null=True)

    class Meta:
        managed = False
        db_table = "role"
        db_table_comment = "角色表"

    def __str__(self):
        return self.code


class Permission(models.Model):
    id = models.BigAutoField(primary_key=True)
    code = models.CharField(max_length=64, unique=True, db_comment="权限编码 如 device:list device:create")
    name = models.CharField(max_length=64)
    module = models.CharField(max_length=32, db_comment="所属模块")

    class Meta:
        managed = False
        db_table = "permission"
        db_table_comment = "权限表"


class RolePermission(models.Model):
    id = models.BigAutoField(primary_key=True)
    role = models.ForeignKey(Role, models.DO_NOTHING, db_column="role_id")
    permission = models.ForeignKey(Permission, models.DO_NOTHING, db_column="permission_id")

    class Meta:
        managed = False
        db_table = "role_permission"
        db_table_comment = "角色权限关联表"


class SysUser(models.Model):
    id = models.BigAutoField(primary_key=True)
    username = models.CharField(max_length=64, unique=True, db_comment="学号/工号 唯一")
    password = models.CharField(max_length=100, db_comment="BCrypt 加密")
    real_name = models.CharField(max_length=64, db_comment="姓名")
    college = models.CharField(max_length=128, blank=True, null=True, db_comment="学院")
    phone = models.CharField(max_length=20, blank=True, null=True)
    email = models.CharField(max_length=128, blank=True, null=True)
    role = models.ForeignKey(Role, models.DO_NOTHING, db_column="role_id", db_comment="角色")
    status = models.IntegerField(db_comment="1启用 0禁用")
    overdue_count = models.IntegerField(db_comment="逾期次数")
    create_time = models.DateTimeField(blank=True, null=True)
    update_time = models.DateTimeField(blank=True, null=True)

    class Meta:
        managed = False
        db_table = "sys_user"
        db_table_comment = "用户表"

    def __str__(self):
        return f"{self.username}({self.real_name})"


class LoginLog(models.Model):
    id = models.BigAutoField(primary_key=True)
    user = models.ForeignKey(SysUser, models.DO_NOTHING, db_column="user_id", blank=True, null=True)
    username = models.CharField(max_length=64, blank=True, null=True)
    ip = models.CharField(max_length=64, blank=True, null=True)
    status = models.IntegerField(db_comment="1成功 0失败")
    create_time = models.DateTimeField(blank=True, null=True)

    class Meta:
        managed = False
        db_table = "login_log"
        db_table_comment = "登录日志表"


class OperationLog(models.Model):
    id = models.BigAutoField(primary_key=True)
    user = models.ForeignKey(SysUser, models.DO_NOTHING, db_column="user_id", blank=True, null=True)
    username = models.CharField(max_length=64, blank=True, null=True)
    module = models.CharField(max_length=32, db_comment="模块")
    operation = models.CharField(max_length=64, db_comment="操作")
    detail = models.CharField(max_length=500, blank=True, null=True)
    ip = models.CharField(max_length=64, blank=True, null=True)
    create_time = models.DateTimeField(blank=True, null=True)

    class Meta:
        managed = False
        db_table = "operation_log"
        db_table_comment = "操作日志表"


class SysConfig(models.Model):
    id = models.BigAutoField(primary_key=True)
    config_key = models.CharField(max_length=64, unique=True)
    config_value = models.CharField(max_length=255)
    remark = models.CharField(max_length=255, blank=True, null=True)

    class Meta:
        managed = False
        db_table = "sys_config"
        db_table_comment = "系统配置表"

    @classmethod
    def get_value(cls, key: str, default: str = "") -> str:
        """读取配置值（对应原 getConfigValue）"""
        row = cls.objects.filter(config_key=key).first()
        return row.config_value if row else default
