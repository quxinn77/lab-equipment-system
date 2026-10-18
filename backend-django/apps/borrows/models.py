"""借用域：借用记录 / 逾期记录（B 部分核心表）"""
from django.db import models

from apps.accounts.models import SysUser
from apps.devices.models import Device


class BorrowRecord(models.Model):
    id = models.BigAutoField(primary_key=True)
    record_no = models.CharField(max_length=32, unique=True, db_comment="申请编号")
    user = models.ForeignKey(
        SysUser, models.DO_NOTHING, db_column="user_id", related_name="borrow_records", db_comment="申请人",
    )
    device = models.ForeignKey(
        Device, models.DO_NOTHING, db_column="device_id", related_name="borrow_records", db_comment="设备",
    )
    start_time = models.DateTimeField(db_comment="借用开始时间")
    due_time = models.DateTimeField(db_comment="归还截止时间")
    actual_return_time = models.DateTimeField(blank=True, null=True, db_comment="实际归还时间")
    purpose = models.CharField(max_length=500, blank=True, null=True, db_comment="用途")
    status = models.CharField(
        max_length=16, default="PENDING",
        db_comment="PENDING待审批/APPROVED审批通过/REJECTED已驳回/BORROWED已借出/RETURNED已归还/OVERDUE逾期未还",
    )
    approver_user = models.ForeignKey(
        SysUser, models.DO_NOTHING, db_column="approver_id", blank=True, null=True, related_name="approved_borrows",
    )
    approver = models.CharField(max_length=64, blank=True, null=True, db_comment="审批人姓名（快照冗余）")
    approve_time = models.DateTimeField(blank=True, null=True)
    approve_remark = models.CharField(max_length=255, blank=True, null=True, db_comment="审批备注/驳回理由")
    return_condition = models.CharField(max_length=16, blank=True, null=True, db_comment="完好INTACT/损坏DAMAGED")
    return_remark = models.CharField(max_length=500, blank=True, null=True, db_comment="归还备注")
    compensation = models.DecimalField(max_digits=12, decimal_places=2, blank=True, null=True, db_comment="赔偿金额")
    create_time = models.DateTimeField(blank=True, null=True)
    update_time = models.DateTimeField(blank=True, null=True)

    class Meta:
        managed = False
        db_table = "borrow_record"
        db_table_comment = "借用记录表"

    def __str__(self):
        return self.record_no


class OverdueRecord(models.Model):
    id = models.BigAutoField(primary_key=True)
    borrow = models.ForeignKey(BorrowRecord, models.DO_NOTHING, db_column="borrow_id", related_name="overdue_records")
    user = models.ForeignKey(SysUser, models.DO_NOTHING, db_column="user_id", related_name="overdue_records")
    device = models.ForeignKey(Device, models.DO_NOTHING, db_column="device_id", related_name="overdue_records")
    due_time = models.DateTimeField()
    overdue_days = models.IntegerField(blank=True, null=True, db_comment="逾期天数")
    handled = models.IntegerField(default=0, db_comment="0未处理 1已处理")
    create_time = models.DateTimeField(blank=True, null=True)

    class Meta:
        managed = False
        db_table = "overdue_record"
        db_table_comment = "逾期记录表"
