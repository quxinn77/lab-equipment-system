-- =====================================================================
-- 实验室设备借用管理系统 数据库结构  MySQL 8.0  utf8mb4
-- =====================================================================
CREATE DATABASE IF NOT EXISTS lab_equipment DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE lab_equipment;

-- ----------------------------
-- 1. 角色表
-- ----------------------------
DROP TABLE IF EXISTS `role`;
CREATE TABLE `role` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `code`        VARCHAR(32)  NOT NULL COMMENT '角色编码 STUDENT/LAB_ADMIN/SUPER_ADMIN',
  `name`        VARCHAR(64)  NOT NULL COMMENT '角色名称',
  `description` VARCHAR(255) DEFAULT NULL,
  `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code` (`code`)
) ENGINE=InnoDB COMMENT='角色表';

-- ----------------------------
-- 2. 权限表 + 角色权限关联
-- ----------------------------
DROP TABLE IF EXISTS `permission`;
CREATE TABLE `permission` (
  `id`    BIGINT       NOT NULL AUTO_INCREMENT,
  `code`  VARCHAR(64)  NOT NULL COMMENT '权限编码 如 device:list device:create',
  `name`  VARCHAR(64)  NOT NULL,
  `module` VARCHAR(32) NOT NULL COMMENT '所属模块',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_perm_code` (`code`)
) ENGINE=InnoDB COMMENT='权限表';

DROP TABLE IF EXISTS `role_permission`;
CREATE TABLE `role_permission` (
  `id`            BIGINT NOT NULL AUTO_INCREMENT,
  `role_id`       BIGINT NOT NULL,
  `permission_id` BIGINT NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_rp_role` (`role_id`)
) ENGINE=InnoDB COMMENT='角色权限关联表';

-- ----------------------------
-- 3. 用户表
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `username`    VARCHAR(64)  NOT NULL COMMENT '学号/工号 唯一',
  `password`    VARCHAR(100) NOT NULL COMMENT 'BCrypt 加密',
  `real_name`   VARCHAR(64)  NOT NULL COMMENT '姓名',
  `college`     VARCHAR(128) DEFAULT NULL COMMENT '学院',
  `phone`       VARCHAR(20)  DEFAULT NULL,
  `email`       VARCHAR(128) DEFAULT NULL,
  `role_id`     BIGINT       NOT NULL DEFAULT 1 COMMENT '角色',
  `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '1启用 0禁用',
  `overdue_count` INT         NOT NULL DEFAULT 0 COMMENT '逾期次数',
  `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB COMMENT='用户表';

-- ----------------------------
-- 4. 实验室表
-- ----------------------------
DROP TABLE IF EXISTS `lab`;
CREATE TABLE `lab` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `code`        VARCHAR(32)  NOT NULL COMMENT '实验室编号',
  `name`        VARCHAR(128) NOT NULL COMMENT '实验室名称',
  `location`    VARCHAR(255) DEFAULT NULL COMMENT '位置',
  `manager`     VARCHAR(64)  DEFAULT NULL COMMENT '负责人姓名',
  `description` VARCHAR(255) DEFAULT NULL,
  `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
  `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_lab_code` (`code`)
) ENGINE=InnoDB COMMENT='实验室表';

-- ----------------------------
-- 5. 设备分类表
-- ----------------------------
DROP TABLE IF EXISTS `device_category`;
CREATE TABLE `device_category` (
  `id`          BIGINT      NOT NULL AUTO_INCREMENT,
  `name`        VARCHAR(64) NOT NULL COMMENT '分类名称: 仪器/工具/耗材',
  `remark`      VARCHAR(255) DEFAULT NULL,
  `create_time` DATETIME    DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB COMMENT='设备分类表';

-- ----------------------------
-- 6. 设备表
-- ----------------------------
DROP TABLE IF EXISTS `device`;
CREATE TABLE `device` (
  `id`            BIGINT         NOT NULL AUTO_INCREMENT,
  `code`          VARCHAR(64)    NOT NULL COMMENT '设备编号 唯一',
  `name`          VARCHAR(128)   NOT NULL COMMENT '设备名称',
  `model`         VARCHAR(128)   DEFAULT NULL COMMENT '型号',
  `spec`          VARCHAR(128)   DEFAULT NULL COMMENT '规格',
  `brand`         VARCHAR(128)   DEFAULT NULL COMMENT '品牌',
  `category_id`   BIGINT         DEFAULT NULL COMMENT '分类',
  `lab_id`        BIGINT         DEFAULT NULL COMMENT '存放实验室',
  `purchase_date` DATE           DEFAULT NULL COMMENT '购置日期',
  `original_value` DECIMAL(12,2)  DEFAULT NULL COMMENT '原值',
  `image_url`     VARCHAR(500)   DEFAULT NULL COMMENT '图片地址',
  `status`        VARCHAR(16)    NOT NULL DEFAULT 'IDLE' COMMENT 'IDLE空闲/BORROWED借出/REPAIRING维修中/SCRAPPED报废/RESERVED预留',
  `remark`        VARCHAR(255)   DEFAULT NULL,
  `deleted`       TINYINT        NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `create_time`   DATETIME       DEFAULT CURRENT_TIMESTAMP,
  `update_time`   DATETIME       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_device_code` (`code`),
  KEY `idx_device_status` (`status`),
  KEY `idx_device_lab` (`lab_id`),
  KEY `idx_device_category` (`category_id`)
) ENGINE=InnoDB COMMENT='设备表';

-- ----------------------------
-- 7. 设备变更日志表
-- ----------------------------
DROP TABLE IF EXISTS `device_log`;
CREATE TABLE `device_log` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `device_id`   BIGINT       NOT NULL,
  `device_code` VARCHAR(64)  DEFAULT NULL,
  `operator_id` BIGINT       DEFAULT NULL,
  `operator`    VARCHAR(64)  DEFAULT NULL COMMENT '操作人',
  `action`      VARCHAR(32)  NOT NULL COMMENT 'CREATE/UPDATE/DELETE/STATUS_CHANGE',
  `old_status`  VARCHAR(16)  DEFAULT NULL,
  `new_status`  VARCHAR(16)  DEFAULT NULL,
  `detail`      VARCHAR(500) DEFAULT NULL COMMENT '变更说明',
  `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_dl_device` (`device_id`)
) ENGINE=InnoDB COMMENT='设备变更日志表';

-- ----------------------------
-- 8. 借用记录表
-- ----------------------------
DROP TABLE IF EXISTS `borrow_record`;
CREATE TABLE `borrow_record` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT,
  `record_no`     VARCHAR(32)  NOT NULL COMMENT '申请编号',
  `user_id`       BIGINT       NOT NULL COMMENT '申请人',
  `device_id`     BIGINT       NOT NULL COMMENT '设备',
  `start_time`    DATETIME     NOT NULL COMMENT '借用开始时间',
  `due_time`      DATETIME     NOT NULL COMMENT '归还截止时间',
  `actual_return_time` DATETIME DEFAULT NULL COMMENT '实际归还时间',
  `purpose`       VARCHAR(500) DEFAULT NULL COMMENT '用途',
  `status`        VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING待审批/APPROVED审批通过/REJECTED已驳回/BORROWED已借出/RETURNED已归还/OVERDUE逾期未还',
  `approver_id`   BIGINT       DEFAULT NULL COMMENT '审批人',
  `approver`      VARCHAR(64)  DEFAULT NULL,
  `approve_time`  DATETIME     DEFAULT NULL,
  `approve_remark` VARCHAR(255) DEFAULT NULL COMMENT '审批备注/驳回理由',
  `return_condition` VARCHAR(16) DEFAULT NULL COMMENT '完好INTACT/损坏DAMAGED',
  `return_remark` VARCHAR(500) DEFAULT NULL COMMENT '归还备注',
  `compensation`  DECIMAL(12,2) DEFAULT NULL COMMENT '赔偿金额',
  `create_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP,
  `update_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_record_no` (`record_no`),
  KEY `idx_br_user` (`user_id`),
  KEY `idx_br_device` (`device_id`),
  KEY `idx_br_status` (`status`)
) ENGINE=InnoDB COMMENT='借用记录表';

-- ----------------------------
-- 9. 逾期记录表
-- ----------------------------
DROP TABLE IF EXISTS `overdue_record`;
CREATE TABLE `overdue_record` (
  `id`            BIGINT   NOT NULL AUTO_INCREMENT,
  `borrow_id`     BIGINT   NOT NULL,
  `user_id`       BIGINT   NOT NULL,
  `device_id`     BIGINT   NOT NULL,
  `due_time`      DATETIME NOT NULL,
  `overdue_days`  INT      DEFAULT 0 COMMENT '逾期天数',
  `handled`       TINYINT  NOT NULL DEFAULT 0 COMMENT '0未处理 1已处理',
  `create_time`   DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_od_user` (`user_id`)
) ENGINE=InnoDB COMMENT='逾期记录表';

-- ----------------------------
-- 10. 设备预约表
-- ----------------------------
DROP TABLE IF EXISTS `reservation`;
CREATE TABLE `reservation` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `user_id`     BIGINT       NOT NULL,
  `device_id`   BIGINT       NOT NULL,
  `start_time`  DATETIME     NOT NULL,
  `end_time`    DATETIME     NOT NULL,
  `status`      VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING待审核/APPROVED通过/REJECTED驳回/CANCELLED取消/EXPIRED过期',
  `remark`      VARCHAR(255) DEFAULT NULL,
  `approver_id` BIGINT       DEFAULT NULL,
  `approve_time` DATETIME    DEFAULT NULL,
  `approve_remark` VARCHAR(255) DEFAULT NULL,
  `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_rv_device` (`device_id`, `start_time`, `end_time`),
  KEY `idx_rv_user` (`user_id`)
) ENGINE=InnoDB COMMENT='设备预约表';

-- ----------------------------
-- 11. 报修记录表
-- ----------------------------
DROP TABLE IF EXISTS `repair_record`;
CREATE TABLE `repair_record` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `device_id`   BIGINT       NOT NULL,
  `reporter_id` BIGINT       NOT NULL COMMENT '报修人',
  `fault_desc`  VARCHAR(1000) NOT NULL COMMENT '故障描述',
  `image_url`   VARCHAR(500) DEFAULT NULL,
  `status`      VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING待处理/REPAIRING维修中/FINISHED维修完成/SCRAPPED报废',
  `handler_id`  BIGINT       DEFAULT NULL COMMENT '处理人',
  `handler`     VARCHAR(64)  DEFAULT NULL,
  `handle_remark` VARCHAR(500) DEFAULT NULL COMMENT '处理说明',
  `finish_time` DATETIME     DEFAULT NULL,
  `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_rr_device` (`device_id`),
  KEY `idx_rr_status` (`status`)
) ENGINE=InnoDB COMMENT='报修记录表';

-- ----------------------------
-- 12. 站内消息表
-- ----------------------------
DROP TABLE IF EXISTS `message`;
CREATE TABLE `message` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `user_id`     BIGINT       NOT NULL COMMENT '接收人',
  `title`       VARCHAR(128) NOT NULL,
  `content`     VARCHAR(1000) DEFAULT NULL,
  `type`        VARCHAR(16)  DEFAULT 'SYSTEM' COMMENT 'BORROW/REPAIR/OVERDUE/SYSTEM',
  `is_read`     TINYINT      NOT NULL DEFAULT 0,
  `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_msg_user` (`user_id`, `is_read`)
) ENGINE=InnoDB COMMENT='站内消息表';

-- ----------------------------
-- 13. 操作日志表
-- ----------------------------
DROP TABLE IF EXISTS `operation_log`;
CREATE TABLE `operation_log` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `user_id`     BIGINT       DEFAULT NULL,
  `username`    VARCHAR(64)  DEFAULT NULL,
  `module`      VARCHAR(32)  NOT NULL COMMENT '模块',
  `operation`   VARCHAR(64)  NOT NULL COMMENT '操作',
  `detail`      VARCHAR(500) DEFAULT NULL,
  `ip`          VARCHAR(64)  DEFAULT NULL,
  `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_ol_time` (`create_time`)
) ENGINE=InnoDB COMMENT='操作日志表';

-- ----------------------------
-- 14. 登录日志表
-- ----------------------------
DROP TABLE IF EXISTS `login_log`;
CREATE TABLE `login_log` (
  `id`          BIGINT      NOT NULL AUTO_INCREMENT,
  `user_id`     BIGINT      DEFAULT NULL,
  `username`    VARCHAR(64) DEFAULT NULL,
  `ip`          VARCHAR(64) DEFAULT NULL,
  `status`      TINYINT     NOT NULL DEFAULT 1 COMMENT '1成功 0失败',
  `create_time` DATETIME    DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB COMMENT='登录日志表';

-- ----------------------------
-- 15. 系统配置表
-- ----------------------------
DROP TABLE IF EXISTS `sys_config`;
CREATE TABLE `sys_config` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT,
  `config_key`   VARCHAR(64)  NOT NULL,
  `config_value` VARCHAR(255) NOT NULL,
  `remark`       VARCHAR(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB COMMENT='系统配置表';

-- =====================================================================
-- 初始化数据
-- =====================================================================
INSERT INTO `role` (`id`,`code`,`name`,`description`) VALUES
(1,'STUDENT','学生','普通用户：查看设备、申请借用、预约、报修'),
(2,'LAB_ADMIN','实验室管理员','审批借用、管理设备、处理归还与报修'),
(3,'SUPER_ADMIN','超级管理员','管理账号、实验室、角色与全局统计');

INSERT INTO `permission` (`code`,`name`,`module`) VALUES
('device:list','设备查询','device'),
('device:create','设备新增','device'),
('device:update','设备编辑','device'),
('device:delete','设备删除','device'),
('device:import','设备导入','device'),
('borrow:apply','借用申请','borrow'),
('borrow:approve','借用审批','borrow'),
('borrow:return','归还登记','borrow'),
('reservation:apply','预约申请','reservation'),
('reservation:approve','预约审核','reservation'),
('repair:report','故障报修','repair'),
('repair:handle','报修处理','repair'),
('user:manage','用户管理','user'),
('lab:manage','实验室管理','lab'),
('stats:view','统计报表','stats'),
('log:view','日志查看','log');

-- 学生权限
INSERT INTO `role_permission` (role_id, permission_id)
SELECT 1, id FROM `permission` WHERE code IN
('device:list','borrow:apply','reservation:apply','repair:report');
-- 实验室管理员
INSERT INTO `role_permission` (role_id, permission_id)
SELECT 2, id FROM `permission` WHERE code IN
('device:list','device:create','device:update','device:delete','device:import',
 'borrow:approve','borrow:return','reservation:approve','repair:handle',
 'stats:view','log:view');
-- 超级管理员全部权限
INSERT INTO `role_permission` (role_id, permission_id)
SELECT 3, id FROM `permission`;

INSERT INTO `sys_config` (`config_key`,`config_value`,`remark`) VALUES
('overdue.limit.apply','1','存在逾期未还时是否禁止新申请 1是 0否'),
('overdue.max.times','3','逾期达到该次数后禁止借用');

INSERT INTO `device_category` (`id`,`name`,`remark`) VALUES
(1,'仪器','精密测量仪器'),
(2,'工具','常用工具'),
(3,'耗材','一次性耗材'),
(4,'计算设备','计算机与服务器设备');

INSERT INTO `lab` (`id`,`code`,`name`,`location`,`manager`,`description`) VALUES
(1,'LAB-001','物理实验室','理科楼 A301','王建国','力学与光学基础实验'),
(2,'LAB-002','化学分析实验室','理科楼 B205','李文静','化学分析与检测'),
(3,'LAB-003','电子工程实验室','工科楼 C102','张宏宇','电路与嵌入式开发'),
(4,'LAB-004','计算机实验室','信息楼 302','陈思远','软件开发与测试');

INSERT INTO `device` (`code`,`name`,`model`,`spec`,`brand`,`category_id`,`lab_id`,`purchase_date`,`original_value`,`status`,`remark`) VALUES
('DEV-0001','数字示波器','DS1054Z','50MHz 4通道','RIGOL',1,3,'2022-03-15',2999.00,'IDLE',NULL),
('DEV-0002','信号发生器','DG1022','25MHz 双通道','RIGOL',1,3,'2022-03-15',1599.00,'IDLE',NULL),
('DEV-0003','万用表','Fluke-15B+','自动量程','Fluke',2,3,'2021-09-01',699.00,'IDLE',NULL),
('DEV-0004','直流稳压电源','DP832','三路输出','RIGOL',1,3,'2022-05-20',2899.00,'IDLE',NULL),
('DEV-0005','笔记本电脑','ThinkPad T14','i7 16G 512G','Lenovo',4,4,'2023-01-10',6499.00,'IDLE',NULL),
('DEV-0006','台式工作站','P360','i9 32G 1T','Lenovo',4,4,'2023-06-18',9999.00,'IDLE',NULL),
('DEV-0007','电子天平','FA2004','0.1mg 200g','舜宇恒平',1,2,'2021-11-05',1899.00,'IDLE',NULL),
('DEV-0008','pH 计','PHS-3C','0.01级','雷磁',1,2,'2021-11-05',1280.00,'IDLE',NULL),
('DEV-0009','分光光度计','722N','可见光','优尼柯',1,2,'2020-04-22',4500.00,'REPAIRING','光源故障维修中'),
('DEV-0010','离心机','TGL-16','16000rpm','湘仪',1,2,'2020-08-30',7800.00,'IDLE',NULL),
('DEV-0011','游标卡尺','0-150mm','0.02mm','成量',2,1,'2021-03-12',85.00,'IDLE',NULL),
('DEV-0012','螺旋测微器','0-25mm','0.01mm','成量',2,1,'2021-03-12',120.00,'IDLE',NULL),
('DEV-0013','光具座','CXJ-1','1.2m','大恒',2,1,'2020-10-01',680.00,'BORROWED',NULL),
('DEV-0014','激光干涉仪','IFM-100','0.001μm','大恒',1,1,'2019-12-20',52000.00,'IDLE','贵重设备 需管理员审批'),
('DEV-0015','电烙铁焊台','T12','90W','快克',2,3,'2022-08-14',299.00,'IDLE',NULL),
('DEV-0016','逻辑分析仪','LA1010','16通道 100MHz','Kingst',1,3,'2023-02-27',699.00,'IDLE',NULL),
('DEV-0017','投影仪','EB-X51','3600流明','Epson',4,4,'2021-05-09',3899.00,'SCRAPPED','灯泡损坏已报废'),
('DEV-0018','服务器','R740','2*XEON 64G','Dell',4,4,'2022-11-30',35000.00,'IDLE','机房托管'),
('DEV-0019','烧杯套装','BZ-500','500ml*12','蜀牛',3,2,'2023-03-01',96.00,'IDLE',NULL),
('DEV-0020','防护眼镜','YG-3','防化','3M',3,2,'2023-03-01',45.00,'IDLE',NULL);
