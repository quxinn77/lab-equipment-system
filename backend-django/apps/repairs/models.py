"""报修域"""
from django.db import models

from apps.accounts.models import SysUser
from apps.devices.models import Device


class RepairRecord(models.Model):
    id = models.BigAutoField(primary_key=True)
    device = models.ForeignKey(Device, models.DO_NOTHING, db_column="device_id", related_name="repair_records")
    reporter = models.ForeignKey(
        SysUser, models.DO_NOTHING, db_column="reporter_id", related_name="repair_reports", db_comment="报修人",
    )
    fault_desc = models.CharField(max_length=1000, db_comment="故障描述")
    image_url = models.CharField(max_length=500, blank=True, null=True)
    status = models.CharField(
        max_length=16, default="PENDING",
        db_comment="PENDING待处理/REPAIRING维修中/FINISHED维修完成/SCRAPPED报废",
    )
    handler_user = models.ForeignKey(
        SysUser, models.DO_NOTHING, db_column="handler_id", blank=True, null=True, related_name="repair_handled",
    )
    handler = models.CharField(max_length=64, blank=True, null=True, db_comment="处理人姓名（快照冗余）")
    handle_remark = models.CharField(max_length=500, blank=True, null=True, db_comment="处理说明")
    finish_time = models.DateTimeField(blank=True, null=True)
    create_time = models.DateTimeField(blank=True, null=True)
    update_time = models.DateTimeField(blank=True, null=True)

    class Meta:
        managed = False
        db_table = "repair_record"
        db_table_comment = "报修记录表"
