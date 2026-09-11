"""管理员后台 Controller(对齐策划书 9.3、第十六章、6.8)。

路由:
- GET    /api/admin/accounts                        账号列表(分页+筛选+脱敏)
- GET    /api/admin/accounts/{id}                   账号详情(IP 掩码、无 token)
- DELETE /api/admin/accounts/{id}/session            踢下线
- POST   /api/admin/accounts/{id}/ban               封禁
- POST   /api/admin/accounts/{id}/unban             解封
- POST   /api/admin/accounts/{id}/grant-operator    指定管理员(仅 ADMIN)
- POST   /api/admin/accounts/{id}/revoke-operator   解除管理员(仅 ADMIN)
- DELETE /api/admin/accounts/{id}                   强制注销
- POST   /api/admin/announcements                   发布公告
- GET    /api/admin/sessions                        在线会话看板
- GET    /api/admin/stats                            账号概览统计
- GET    /api/admin/audit                            审计日志

权限:全部需 OPERATOR 及以上;grant/revoke 仅 ADMIN。
"""
import time
import uuid
from typing import Any

from fastapi import APIRouter, Request

from auth import AuthGuard
from db import _conn, SESSIONS
from error_codes import err
from admin import operator_service, audit

router = APIRouter(prefix="/api/admin", tags=["admin"])


def _admin_user(request: Request) -> dict:
    return AuthGuard("OPERATOR")(request)


def _admin_only(request: Request) -> dict:
    u = AuthGuard("ADMIN")(request)
    return u


def _mask_ip(ip: str | None) -> str | None:
    if not ip:
        return None
    parts = ip.split(".")
    if len(parts) == 4:
        return f"{parts[0]}.{parts[1]}.*.*"
    return ip


@router.get("/accounts")
def accounts(
    request: Request,
    keyword: str = "",
    role: str = "",
    status: str = "",
    page: int = 1,
    size: int = 20,
) -> dict[str, Any]:
    _admin_user(request)
    conn = _conn()
    where: list[str] = []
    args: list[Any] = []
    if keyword:
        # keyword 支持 UserID 精确匹配、昵称模糊匹配(不支持 QQ/微信 号搜索)
        where.append("(id=? OR display_name LIKE ?)")
        args.extend([keyword, f"%{keyword}%"])
    if role:
        where.append("role=?")
        args.append(role.upper())
    if status:
        where.append("status=?")
        args.append(status.upper())
    base = "SELECT id, account_name, display_name, role, status, provider, created_at FROM user"
    if where:
        base += " WHERE " + " AND ".join(where)
    total = conn.execute(
        base.replace("SELECT id, account_name, display_name, role, status, provider, created_at", "SELECT COUNT(*) AS c"),
        args,
    ).fetchone()["c"]
    rows = conn.execute(
        base + f" ORDER BY created_at DESC LIMIT ? OFFSET ?",
        args + [size, (max(page, 1) - 1) * size],
    ).fetchall()
    conn.close()
    return {
        "total": total,
        "page": page,
        "size": size,
        "items": [dict(r) for r in rows],  # 已脱敏:无 token / openid / password_hash
    }


@router.get("/accounts/{user_id}")
def account_detail(request: Request, user_id: str) -> dict[str, Any]:
    _admin_user(request)
    conn = _conn()
    row = conn.execute(
        """SELECT u.id, u.account_name, u.display_name, u.role, u.status,
                  u.provider, u.operator_granted_at, u.granted_by, u.created_at
           FROM user u WHERE u.id=?""",
        (user_id,),
    ).fetchone()
    if not row:
        conn.close()
        raise err("USER_NOT_FOUND", "目标用户不存在")
    user = dict(row)

    bound = conn.execute(
        "SELECT provider FROM identity_provider WHERE user_id=?", (user_id,)
    ).fetchall()
    user["bound_providers"] = [r["provider"] for r in bound]

    logins = conn.execute(
        "SELECT provider, device, created_at FROM login_history WHERE user_id=? ORDER BY created_at DESC LIMIT 50",
        (user_id,),
    ).fetchall()
    user["login_history"] = [dict(r) for r in logins]

    sess = SESSIONS.get(user_id)
    user["current_session"] = {
        "state": sess["state"],
        "device": sess["device"],
        "ip": _mask_ip(sess["ip"]),
        "created_at": sess["created_at"],
    } if sess else None

    # 终端审计 + 违规记录:本地 Mock 暂为空,后续接入终端审计表
    user["terminal_audit"] = []
    user["violations"] = []
    conn.close()
    return user


@router.delete("/accounts/{user_id}/session")
def kick_session(request: Request, user_id: str) -> dict[str, Any]:
    admin = _admin_user(request)
    ok = SESSIONS.kick(user_id)
    audit.log(admin["id"], "KICK", user_id, ip=_client_ip(request))
    return {"kicked": ok}


@router.post("/accounts/{user_id}/ban")
def ban(request: Request, user_id: str) -> dict[str, Any]:
    admin = _admin_user(request)
    conn = _conn()
    row = conn.execute("SELECT role FROM user WHERE id=?", (user_id,)).fetchone()
    if not row:
        conn.close()
        raise err("USER_NOT_FOUND", "目标用户不存在")
    if row["role"] == "ADMIN":
        conn.close()
        raise err("INVALID_TARGET", "官方账号不可封禁")
    conn.execute("UPDATE user SET status='BANNED' WHERE id=?", (user_id,))
    conn.commit()
    conn.close()
    audit.log(admin["id"], "BAN", user_id, ip=_client_ip(request))
    return {"banned": True}


@router.post("/accounts/{user_id}/unban")
def unban(request: Request, user_id: str) -> dict[str, Any]:
    admin = _admin_user(request)
    conn = _conn()
    conn.execute("UPDATE user SET status='NORMAL' WHERE id=?", (user_id,))
    conn.commit()
    conn.close()
    audit.log(admin["id"], "UNBAN", user_id, ip=_client_ip(request))
    return {"unbanned": True}


@router.delete("/accounts/{user_id}")
def force_delete(request: Request, user_id: str) -> dict[str, Any]:
    admin = _admin_user(request)
    from account import account_service
    account_service.delete_account(user_id)
    audit.log(admin["id"], "DELETE", user_id, ip=_client_ip(request))
    return {"deleted": True}


@router.post("/accounts/{user_id}/grant-operator")
def grant_operator(request: Request, user_id: str) -> dict[str, Any]:
    admin = _admin_only(request)  # 仅 ADMIN 可指定
    r = operator_service.grant_operator(admin, user_id)
    return r


@router.post("/accounts/{user_id}/revoke-operator")
def revoke_operator(request: Request, user_id: str) -> dict[str, Any]:
    admin = _admin_only(request)  # 仅 ADMIN 可解除
    r = operator_service.revoke_operator(admin, user_id)
    return r


@router.post("/announcements")
def announce(request: Request, body: dict) -> dict[str, Any]:
    admin = _admin_user(request)
    aid = uuid.uuid4().hex
    conn = _conn()
    conn.execute(
        """INSERT INTO announcement (id, title, body, scope, scheduled_at, revoked, created_by, created_at)
           VALUES (?, ?, ?, ?, ?, 0, ?, ?)""",
        (
            aid,
            body.get("title", ""),
            body.get("body", ""),
            body.get("scope", "ALL"),
            body.get("scheduled_at"),
            admin["id"],
            time.time(),
        ),
    )
    conn.commit()
    conn.close()
    audit.log(admin["id"], "ANNOUNCE", None, detail=f"announcement_id={aid}", ip=_client_ip(request))
    return {"announcement_id": aid}


@router.get("/sessions")
def sessions(request: Request) -> dict[str, Any]:
    _admin_user(request)
    actives = SESSIONS.list_active()
    conn = _conn()
    items = []
    for s in actives:
        # 会话看板以 token 的 jti 关联;本地 Mock 通过解码 token 拿 user_id
        try:
            from auth import decode
            payload = decode(s["token"])
            uid = payload["sub"]
        except Exception:
            uid = None
        row = conn.execute(
            "SELECT id, display_name, role FROM user WHERE id=?", (uid,)
        ).fetchone() if uid else None
        items.append({
            "user_id": uid,
            "user": dict(row) if row else None,
            "state": s["state"],
            "device": s["device"],
            "ip": _mask_ip(s["ip"]),
            "created_at": s["created_at"],
        })
    conn.close()
    return {"active_count": len(actives), "items": items}


@router.get("/stats")
def stats(request: Request) -> dict[str, Any]:
    _admin_user(request)
    conn = _conn()
    total = conn.execute("SELECT COUNT(*) AS c FROM user").fetchone()["c"]
    users = conn.execute("SELECT COUNT(*) AS c FROM user WHERE role='USER'").fetchone()["c"]
    operators = conn.execute("SELECT COUNT(*) AS c FROM user WHERE role='OPERATOR'").fetchone()["c"]
    admins = conn.execute("SELECT COUNT(*) AS c FROM user WHERE role='ADMIN'").fetchone()["c"]
    banned = conn.execute("SELECT COUNT(*) AS c FROM user WHERE status='BANNED'").fetchone()["c"]
    online = len(SESSIONS.list_active())
    conn.close()
    return {
        "total_accounts": total,
        "user_count": users,
        "operator_count": operators,
        "admin_count": admins,
        "banned_count": banned,
        "online_count": online,
    }


@router.get("/audit")
def audit_logs(request: Request, limit: int = 100) -> dict[str, Any]:
    _admin_user(request)
    return {"items": audit.recent(limit)}


def _client_ip(request: Request) -> str:
    return request.client.host if request.client else "127.0.0.1"
