"""预约域"""
from django.db import models

from apps.accounts.models import SysUser
from apps.devices.models import Device


class Reservation(models.Model):
    id = models.BigAutoField(primary_key=True)
    user = models.ForeignKey(SysUser, models.DO_NOTHING, db_column="user_id", related_name="reservations")
    device = models.ForeignKey(Device, models.DO_NOTHING, db_column="device_id", related_name="reservations")
    start_time = models.DateTimeField()
    end_time = models.DateTimeField()
    status = models.CharField(
        max_length=16, default="PENDING",
        db_comment="PENDING待审核/APPROVED通过/REJECTED驳回/CANCELLED取消/EXPIRED过期",
    )
    remark = models.CharField(max_length=255, blank=True, null=True)
    approver_user = models.ForeignKey(
        SysUser, models.DO_NOTHING, db_column="approver_id", blank=True, null=True, related_name="approved_reservations",
    )
    approve_time = models.DateTimeField(blank=True, null=True)
    approve_remark = models.CharField(max_length=255, blank=True, null=True)
    create_time = models.DateTimeField(blank=True, null=True)
    update_time = models.DateTimeField(blank=True, null=True)

    class Meta:
        managed = False
        db_table = "reservation"
        db_table_comment = "设备预约表"
