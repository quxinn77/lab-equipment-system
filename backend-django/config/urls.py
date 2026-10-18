from django.urls import path

from apps.accounts.api import router as accounts_router
from apps.borrows.api import router as borrows_router
from apps.common.api import api
from apps.devices.api import router as devices_router

api.add_router("/auth", accounts_router, tags=["认证"])
api.add_router("/borrows", borrows_router, tags=["借用/归还"])
api.add_router("/devices", devices_router, tags=["设备"])

urlpatterns = [
    path("api/", api.urls),
]
