"""AURA JWT 签发与校验(对齐策划书 13.6 PKCE/安全)。

本地 Mock:HS256 + 内存黑名单(模拟 Token 撤销)。
- Token 7 天,刷新令牌 30 天
- 登录即官方:官方账号 Token 写入 role=ADMIN + official=true
"""
import time
import uuid
import jwt
from typing import Any

from config import CFG
from error_codes import err
from db import SESSIONS


def issue(user: dict, refresh: bool = False) -> dict[str, Any]:
    now = int(time.time())
    expire_days = CFG.refresh_expire_days if refresh else CFG.token_expire_days
    jti = uuid.uuid4().hex
    payload = {
        "sub": user["id"],
        "role": user["role"],
        "official": user["role"] == "ADMIN",
        "type": "refresh" if refresh else "access",
        "jti": jti,
        "iat": now,
        "exp": now + expire_days * 86400,
    }
    token = jwt.encode(payload, CFG.jwt_secret, algorithm="HS256")
    return {"access_token" if not refresh else "refresh_token": token, "jti": jti}


def decode(token: str, type: str = "access") -> dict[str, Any]:
    try:
        payload = jwt.decode(
            token, CFG.jwt_secret, algorithms=["HS256"], options={"verify_exp": True}
        )
    except jwt.ExpiredSignatureError:
        raise err("TOKEN_EXPIRED", "Token 已过期")
    except jwt.InvalidTokenError:
        raise err("TOKEN_EXPIRED", "Token 无效")
    if payload.get("type") != type:
        raise err("TOKEN_EXPIRED", "Token 类型不匹配")
    return payload


class AuthGuard:
    """FastAPI 依赖:校验 Bearer Token,返回 user dict。

    单端唯一(策划书 13.7):
    - 校验 token 在 Redis 会话看板的 state == ACTIVE,否则 KICKED_OUT
    - role >= min_role
    """

    def __init__(self, min_role: str | None = None) -> None:
        self.min_role = min_role

    def __call__(self, request) -> dict:
        auth = request.headers.get("Authorization", "")
        if not auth.startswith("Bearer "):
            raise err("TOKEN_EXPIRED", "缺少 Authorization")
        token = auth[7:]
        payload = decode(token)

        # 单端唯一:新端登录顶替旧会话后,旧 token 的 session 被置 KICKED
        sess = SESSIONS.get(payload["sub"])
        if sess and sess.get("state") == "KICKED":
            raise err("KICKED_OUT", "被新端登录踢下线")

        from db import _conn
        conn = _conn()
        row = conn.execute("SELECT * FROM user WHERE id=?", (payload["sub"],)).fetchone()
        conn.close()
        if not row:
            raise err("ACCOUNT_NOT_FOUND", "账号不存在")
        user = dict(row)
        if user["status"] != "NORMAL":
            raise err("ACCOUNT_BANNED", "账号已被封禁")
        if self.min_role:
            _order = {"USER": 0, "OPERATOR": 1, "ADMIN": 2}
            if _order[user["role"]] < _order[self.min_role]:
                raise err("PERMISSION_DENIED", "权限不足")
        return user
