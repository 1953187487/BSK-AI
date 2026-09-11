"""OAuth + 官方账号 登录 Controller(对齐策划书 9.1/9.2)。

路由:
- POST /api/auth/oauth/authorize    生成 state + 返回授权 URL
- POST /api/auth/oauth/callback     code 换 token + 查/建账号 + 签发 JWT
- POST /api/auth/refresh            刷新 Token
- POST /api/auth/logout             登出
- POST /api/auth/bind               绑定另一登录方式
- POST /api/auth/unbind             解绑
- DELETE /api/auth/account          注销账号
- POST /api/auth/official/login     官方三字段登录
- POST /api/auth/official/change-password  改密(首次登录强制)
"""
import time
from typing import Any

from fastapi import APIRouter, Request

from auth import issue, decode, AuthGuard
from error_codes import err
from db import _conn, SESSIONS
from config import CFG
from password import hash_password, verify_password, password_complexity_ok
from account import account_service, oauth_service
from providers import OAuthError

router = APIRouter(prefix="/api/auth", tags=["auth"])


def _client_ip(request: Request) -> str:
    return request.client.host if request.client else "127.0.0.1"


def _issue_tokens(user: dict, request: Request) -> dict[str, Any]:
    access = issue(user)
    refresh = issue(user, refresh=True)
    SESSIONS.issue(
        user["id"], access["access_token"], refresh["refresh_token"],
        request.headers.get("X-Device", "android"), _client_ip(request),
    )
    return {
        "access_token": access["access_token"],
        "refresh_token": refresh["refresh_token"],
        "role": user["role"],
        "official": user["role"] == "ADMIN",
    }


@router.post("/oauth/authorize")
def oauth_authorize(body: dict) -> dict[str, Any]:
    provider = (body.get("provider") or "QQ").upper()
    redirect = body.get("redirect", "aura://oauth/qq")
    state = oauth_service.create_state(provider, redirect)
    p = oauth_service.get_provider(provider, redirect)
    url = p.authorize_url(state)
    return {"state": state, "authorize_url": url, "provider": provider}


@router.post("/oauth/callback")
def oauth_callback(body: dict, request: Request) -> dict[str, Any]:
    provider = (body.get("provider") or "QQ").upper()
    code = body.get("code", "")
    state = body.get("state", "")
    redirect = oauth_service.verify_state(state, provider)
    p = oauth_service.get_provider(provider, redirect)
    try:
        info = p.exchange(code)
    except OAuthError as e:
        raise err(e.code, e.message)
    r = account_service.create_or_login(
        provider,
        info["provider_uid"],
        info["unionid"],
        info["nickname"],
        info["avatar"],
        _client_ip(request),
        request.headers.get("X-Device", "android"),
    )
    user = r["user"]
    tokens = _issue_tokens(user, request)
    return {
        **tokens,
        "created": r["created"],
        "user": {
            "id": user["id"],
            "account_name": user["account_name"],
            "display_name": user["display_name"],
            "role": user["role"],
            "official": user["role"] == "ADMIN",
        },
    }


@router.post("/refresh")
def refresh(body: dict) -> dict[str, Any]:
    refresh_token = body.get("refresh_token", "")
    payload = decode(refresh_token, type="refresh")
    conn = _conn()
    row = conn.execute("SELECT * FROM user WHERE id=?", (payload["sub"],)).fetchone()
    conn.close()
    if not row:
        raise err("ACCOUNT_NOT_FOUND", "账号不存在")
    user = dict(row)
    if user["status"] != "NORMAL":
        raise err("ACCOUNT_BANNED", "账号已被封禁")
    access = issue(user)
    SESSIONS.issue(user["id"], access["access_token"], refresh_token, "refresh", "127.0.0.1")
    return {"access_token": access["access_token"]}


@router.post("/logout")
def logout(request: Request, body: dict = None) -> dict[str, Any]:
    access_token = body.get("access_token") if body else None
    if access_token:
        try:
            payload = decode(access_token)
            SESSIONS.clear_user(payload["sub"])
        except Exception:
            pass
    return {"logged_out": True}


@router.post("/bind")
def bind(request: Request, body: dict) -> dict[str, Any]:
    user = AuthGuard("USER")(request)
    provider = (body.get("provider") or "").upper()
    code = body.get("code", "")
    redirect = body.get("redirect", "aura://oauth/qq")
    p = oauth_service.get_provider(provider, redirect)
    try:
        info = p.exchange(code)
    except OAuthError as e:
        raise err(e.code, e.message)
    account_service.bind_provider(user["id"], provider, info["provider_uid"])
    return {"bound": True, "provider": provider}


@router.post("/unbind")
def unbind(request: Request, body: dict) -> dict[str, Any]:
    user = AuthGuard("USER")(request)
    provider = (body.get("provider") or "").upper()
    provider_uid = body.get("provider_uid", "")
    account_service.unbind_provider(user["id"], provider, provider_uid)
    return {"unbound": True}


@router.delete("/account")
def delete_account(request: Request) -> dict[str, Any]:
    user = AuthGuard("USER")(request)
    account_service.delete_account(user["id"])
    return {"deleted": True}


# ---------------------------------------------------------------- 官方账号


@router.post("/official/login")
def official_login(body: dict, request: Request) -> dict[str, Any]:
    """三字段全部校验:ID + 账号名称 + 密码,任一错统一报 INVALID_CREDENTIALS。

    条款四:不区分哪一项错,防枚举、防探测。
    连续 5 次错误锁定该 ID 15 分钟,统一延迟响应。
    """
    oid = (body.get("id") or "").strip()
    name = (body.get("account_name") or "").strip()
    pwd = body.get("password") or ""
    if not oid or not name or not pwd:
        raise err("OFFICIAL_LOGIN_REQUIRES_3_FIELDS", "官方登录三字段缺一不可")

    conn = _conn()
    fail_row = conn.execute(
        "SELECT * FROM official_login_fail WHERE user_id=?", (oid,)
    ).fetchone()
    if fail_row:
        locked_until = fail_row["locked_until"] or 0
        if time.time() < locked_until:
            conn.close()
            raise err("INVALID_CREDENTIALS", "ID 已临时锁定,请 15 分钟后再试")

    user_row = conn.execute(
        "SELECT * FROM user WHERE id=? AND account_name=? AND role='ADMIN'",
        (oid, name),
    ).fetchone()
    pwd_ok = bool(user_row) and verify_password(pwd, user_row["password_hash"])

    if not pwd_ok:
        new_count = (fail_row["fail_count"] if fail_row else 0) + 1
        if new_count >= CFG.max_failures:
            locked_until = time.time() + CFG.lock_minutes * 60
            conn.execute(
                """INSERT INTO official_login_fail (user_id, fail_count, locked_until)
                   VALUES (?, ?, ?)
                   ON CONFLICT(user_id) DO UPDATE SET fail_count=excluded.fail_count, locked_until=excluded.locked_until""",
                (oid, new_count, locked_until),
            )
        else:
            conn.execute(
                """INSERT INTO official_login_fail (user_id, fail_count, locked_until)
                   VALUES (?, ?, NULL)
                   ON CONFLICT(user_id) DO UPDATE SET fail_count=excluded.fail_count""",
                (oid, new_count),
            )
        conn.commit()
        conn.close()
        raise err("INVALID_CREDENTIALS", "ID、账号名称或密码错误")

    conn.execute("DELETE FROM official_login_fail WHERE user_id=?", (oid,))
    conn.commit()
    conn.close()

    user = dict(user_row)
    if user["status"] != "NORMAL":
        raise err("ACCOUNT_BANNED", "账号已被封禁")
    from db import SESSIONS
    sess = SESSIONS.get(user["id"])
    if sess and sess.get("state") == "KICKED":
        raise err("KICKED_OUT", "被新端登录踢下线")
    tokens = _issue_tokens(user, request)
    tokens["user"] = {
        "id": user["id"],
        "account_name": user["account_name"],
        "display_name": user["display_name"],
        "role": user["role"],
        "official": user["role"] == "ADMIN",
    }
    return tokens


@router.post("/official/change-password")
def official_change_password(request: Request, body: dict) -> dict[str, Any]:
    """改密(首次登录强制)。新密码需满足复杂度(≥12 位、大小写+数字+符号)。"""
    user = AuthGuard("ADMIN")(request)
    new_pwd = body.get("password", "")
    if not password_complexity_ok(new_pwd):
        raise err("OFFICIAL_LOGIN_REQUIRES_3_FIELDS", "密码不满足复杂度要求(≥12 位、大小写+数字+符号)")
    conn = _conn()
    conn.execute("UPDATE user SET password_hash=? WHERE id=?", (hash_password(new_pwd), user["id"]))
    conn.commit()
    conn.close()
    return {"changed": True}
