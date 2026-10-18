"""设备域：设备 / 分类 / 变更日志"""
from django.db import models

from apps.accounts.models import SysUser
from apps.labs.models import Lab


class DeviceCategory(models.Model):
    id = models.BigAutoField(primary_key=True)
    name = models.CharField(max_length=64, db_comment="分类名称: 仪器/工具/耗材")
    remark = models.CharField(max_length=255, blank=True, null=True)
    create_time = models.DateTimeField(blank=True, null=True)

    class Meta:
        managed = False
        db_table = "device_category"
        db_table_comment = "设备分类表"

    def __str__(self):
        return self.name


class Device(models.Model):
    id = models.BigAutoField(primary_key=True)
    code = models.CharField(max_length=64, unique=True, db_comment="设备编号 唯一")
    name = models.CharField(max_length=128, db_comment="设备名称")
    model = models.CharField(max_length=128, blank=True, null=True, db_comment="型号")
    spec = models.CharField(max_length=128, blank=True, null=True, db_comment="规格")
    brand = models.CharField(max_length=128, blank=True, null=True, db_comment="品牌")
    category = models.ForeignKey(
        DeviceCategory, models.DO_NOTHING, db_column="category_id",
        blank=True, null=True, related_name="devices", db_comment="分类",
    )
    lab = models.ForeignKey(
        Lab, models.DO_NOTHING, db_column="lab_id",
        blank=True, null=True, related_name="devices", db_comment="存放实验室",
    )
    purchase_date = models.DateField(blank=True, null=True, db_comment="购置日期")
    original_value = models.DecimalField(max_digits=12, decimal_places=2, blank=True, null=True, db_comment="原值")
    image_url = models.CharField(max_length=500, blank=True, null=True, db_comment="图片地址")
    status = models.CharField(max_length=16, db_comment="IDLE空闲/BORROWED借出/REPAIRING维修中/SCRAPPED报废/RESERVED预留")
    total_qty = models.IntegerField(default=1, db_comment="总库存")
    remark = models.CharField(max_length=255, blank=True, null=True)
    deleted = models.IntegerField(default=0, db_comment="逻辑删除")
    create_time = models.DateTimeField(blank=True, null=True)
    update_time = models.DateTimeField(blank=True, null=True)

    class Meta:
        managed = False
        db_table = "device"
        db_table_comment = "设备表"

    def __str__(self):
        return f"{self.code} {self.name}"


class DeviceLog(models.Model):
    id = models.BigAutoField(primary_key=True)
    device = models.ForeignKey(Device, models.DO_NOTHING, db_column="device_id", related_name="logs")
    device_code = models.CharField(max_length=64, blank=True, null=True)
    operator_user = models.ForeignKey(
        SysUser, models.DO_NOTHING, db_column="operator_id", blank=True, null=True, related_name="device_logs",
    )
    operator = models.CharField(max_length=64, blank=True, null=True, db_comment="操作人姓名（快照冗余）")
    action = models.CharField(max_length=32, db_comment="CREATE/UPDATE/DELETE/STATUS_CHANGE")
    old_status = models.CharField(max_length=16, blank=True, null=True)
    new_status = models.CharField(max_length=16, blank=True, null=True)
    detail = models.CharField(max_length=500, blank=True, null=True, db_comment="变更说明")
    create_time = models.DateTimeField(blank=True, null=True)

    class Meta:
        managed = False
        db_table = "device_log"
        db_table_comment = "设备变更日志表"
