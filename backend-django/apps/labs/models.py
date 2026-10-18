"""实验室域"""
from django.db import models


class Lab(models.Model):
    id = models.BigAutoField(primary_key=True)
    code = models.CharField(max_length=32, unique=True, db_comment="实验室编号")
    name = models.CharField(max_length=128, db_comment="实验室名称")
    location = models.CharField(max_length=255, blank=True, null=True, db_comment="位置")
    manager = models.CharField(max_length=64, blank=True, null=True, db_comment="负责人姓名")
    description = models.CharField(max_length=255, blank=True, null=True)
    status = models.IntegerField(db_comment="1启用 0停用")
    create_time = models.DateTimeField(blank=True, null=True)
    update_time = models.DateTimeField(blank=True, null=True)

    class Meta:
        managed = False
        db_table = "lab"
        db_table_comment = "实验室表"

    def __str__(self):
        return f"{self.code} {self.name}"
