"""AURA 沙盒服务器入口(FastAPI)。

启动:
  cd server/sandbox
  python3 -m uvicorn main:app --host 127.0.0.1 --port 18080

职责(策划书第十四章):
- 本地桥接(监听 127.0.0.1,不暴露公网)
- 终端会话管理(预留)
- 账号/管理员 API(本实现)
- 审计落盘(SQLite)
- 配置缓存
- OTA 检查(预留)
"""
from contextlib import asynccontextmanager

from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse

from db import init_db
from seed import seed_official
from account.oauth_controller import router as auth_router
from admin.controller import router as admin_router
from error_codes import err, all_codes


@asynccontextmanager
async def lifespan(app: FastAPI):
    init_db()
    seed_official()
    yield


app = FastAPI(title="AURA Sandbox Server", version="2.1.0-sandbox", lifespan=lifespan)
app.include_router(auth_router)
app.include_router(admin_router)


@app.exception_handler(Exception)
async def error_handler(request: Request, exc: Exception):
    """统一错误响应格式 {code, message}。"""
    if hasattr(exc, "code"):
        return JSONResponse(
            status_code=getattr(exc, "http_status", 400),
            content={"code": exc.code, "message": exc.message},
        )
    import traceback
    traceback.print_exc()
    return JSONResponse(status_code=500, content={"code": "INTERNAL", "message": str(exc)})


@app.get("/healthz")
def healthz() -> dict:
    return {"ok": True, "service": "aura-sandbox", "version": "2.1.0"}


@app.get("/api/error-codes")
def error_codes() -> dict:
    return all_codes()
