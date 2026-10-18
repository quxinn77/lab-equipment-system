"""
Django settings — 实验室设备借用管理系统（Django 版后端）

对应原 Spring Boot 的 application.yml + application-local.yml：
- 敏感配置（密码、密钥）全部走 .env（等价于 Laravel 的 .env 机制），已被 .gitignore 忽略
- 数据库复用 Java 版建好的 lab_equipment 库 16 张表，通过 inspectdb 映射，不重建
"""
from pathlib import Path

import pymysql
import environ

# 用 PyMySQL 顶替 MySQLdb（Windows 上免编译）
pymysql.install_as_MySQLdb()

BASE_DIR = Path(__file__).resolve().parent.parent

env = environ.Env(
    DEBUG=(bool, True),
    DB_HOST=(str, "127.0.0.1"),
    DB_PORT=(int, 3306),
    DB_NAME=(str, "lab_equipment"),
    DB_USER=(str, "root"),
    DB_PASSWORD=(str, ""),
    SECRET_KEY=(str, "dev-insecure-key"),
    JWT_SECRET=(str, "dev-jwt-secret"),
    JWT_EXPIRE_HOURS=(int, 24),
)
environ.Env.read_env(BASE_DIR / ".env")

SECRET_KEY = env("SECRET_KEY")
DEBUG = env("DEBUG")
ALLOWED_HOSTS = ["*"]

# 纯 JWT API：不使用 Django 内置 admin/auth/session，用户体系走自己的 sys_user 表
INSTALLED_APPS = [
    "corsheaders",
    "apps.accounts",
    "apps.labs",
    "apps.devices",
    "apps.borrows",
    "apps.repairs",
    "apps.reservations",
    "apps.messages",
]

MIDDLEWARE = [
    "django.middleware.security.SecurityMiddleware",
    "corsheaders.middleware.CorsMiddleware",
    "django.middleware.common.CommonMiddleware",
]

ROOT_URLCONF = "config.urls"
WSGI_APPLICATION = "config.wsgi.application"

# ninja 的 Swagger 文档页（/api/docs）渲染模板需要模板引擎，保留最小配置
TEMPLATES = [
    {
        "BACKEND": "django.template.backends.django.DjangoTemplates",
        "DIRS": [],
        "APP_DIRS": True,
        "OPTIONS": {"context_processors": []},
    },
]

DATABASES = {
    "default": {
        "ENGINE": "django.db.backends.mysql",
        "NAME": env("DB_NAME"),
        "USER": env("DB_USER"),
        "PASSWORD": env("DB_PASSWORD"),
        "HOST": env("DB_HOST"),
        "PORT": env("DB_PORT"),
        "OPTIONS": {"charset": "utf8mb4"},
    }
}

# 现有表存的是 Java 版写入的本地时间，必须关闭时区转换，否则时间错乱 8 小时
LANGUAGE_CODE = "zh-hans"
TIME_ZONE = "Asia/Shanghai"
USE_I18N = True
USE_TZ = False

# JWT（对应原 JwtUtil.java）
JWT_SECRET = env("JWT_SECRET")
JWT_EXPIRE_HOURS = env("JWT_EXPIRE_HOURS")

# CORS：放行 Vite 开发服务器
_cors_origins = env("CORS_ORIGINS", default="http://localhost:5173,http://127.0.0.1:5173")
CORS_ALLOWED_ORIGINS = [o.strip() for o in _cors_origins.split(",") if o.strip()]

# 上传文件（对应原 LocalStorageService 的 backend/upload-files/）
MEDIA_URL = "/api/files/"
MEDIA_ROOT = BASE_DIR / "media"

DEFAULT_AUTO_FIELD = "django.db.models.BigAutoField"
