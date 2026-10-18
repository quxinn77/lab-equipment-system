"""站内消息域"""
from django.db import models

from apps.accounts.models import SysUser


class Message(models.Model):
    id = models.BigAutoField(primary_key=True)
    user = models.ForeignKey(SysUser, models.DO_NOTHING, db_column="user_id", related_name="messages", db_comment="接收人")
    title = models.CharField(max_length=128)
    content = models.CharField(max_length=1000, blank=True, null=True)
    type = models.CharField(max_length=16, default="SYSTEM", db_comment="BORROW/REPAIR/OVERDUE/SYSTEM")
    is_read = models.IntegerField(default=0)
    create_time = models.DateTimeField(blank=True, null=True)

    class Meta:
        managed = False
        db_table = "message"
        db_table_comment = "站内消息表"
