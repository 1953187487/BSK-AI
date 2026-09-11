"""OPERATOR 指定/解除服务(策划书 6.8、13.7⑤,条款七)。

核心约束:
- 仅 ADMIN(ID=0001)拥有指定权;OPERATOR 调 → 403
- 目标必须为 USER + NORMAL(非封禁)
- 同一时刻最多 1 名 OPERATOR;指定前须先解除旧的
- OPERATOR 不可自解除,须 ADMIN 操作
- 指定/解除均写 AdminAuditLog(granted_by=0001、granted_at=now)
"""
import time
from typing import Any

from db import _conn
from error_codes import err
from admin import audit


def operator_count() -> int:
    conn = _conn()
    n = conn.execute(
        "SELECT COUNT(*) AS c FROM user WHERE role='OPERATOR'"
    ).fetchone()["c"]
    conn.close()
    return n


def conn2_fetch(user_id: str) -> dict[str, Any]:
    conn = _conn()
    row = conn.execute(
        "SELECT id, account_name, display_name, role, status FROM user WHERE id=?",
        (user_id,),
    ).fetchone()
    conn.close()
    return dict(row) if row else {}


def grant_operator(admin_user: dict, target_user_id: str) -> dict[str, Any]:
    conn = _conn()
    target = conn.execute("SELECT * FROM user WHERE id=?", (target_user_id,)).fetchone()
    if not target:
        conn.close()
        raise err("USER_NOT_FOUND", "目标用户不存在")
    if target["role"] != "USER":
        conn.close()
        raise err("INVALID_TARGET", f"目标角色为 {target['role']},仅可指定 USER")
    if target["status"] != "NORMAL":
        conn.close()
        raise err("INVALID_TARGET", "目标账号已被封禁,不可指定")
    if operator_count() >= 1:
        conn.close()
        raise err("OPERATOR_ALREADY_EXISTS", "当前已有一名管理员,请先解除后再指定")

    now = time.time()
    conn.execute(
        "UPDATE user SET role='OPERATOR', operator_granted_at=?, granted_by=? WHERE id=?",
        (now, admin_user["id"], target_user_id),
    )
    conn.commit()
    conn.close()

    audit.log(
        admin_user["id"], "GRANT_OPERATOR", target_user_id,
        detail=f"granted_by={admin_user['id']},granted_at={now}",
    )
    return {"granted": True, "user": conn2_fetch(target_user_id)}


def revoke_operator(admin_user: dict, target_user_id: str) -> dict[str, Any]:
    conn = _conn()
    target = conn.execute("SELECT * FROM user WHERE id=?", (target_user_id,)).fetchone()
    if not target:
        conn.close()
        raise err("USER_NOT_FOUND", "目标用户不存在")
    if target["role"] != "OPERATOR":
        conn.close()
        raise err("INVALID_TARGET", f"目标角色为 {target['role']},仅可解除 OPERATOR")
    if target_user_id == admin_user["id"] and target["role"] == "OPERATOR":
        conn.close()
        raise err("CANNOT_REVOKE_SELF", "OPERATOR 不能自行解除,须由 ADMIN 操作")

    now = time.time()
    conn.execute(
        """UPDATE user SET role='USER', operator_granted_at=NULL, granted_by=NULL
           WHERE id=?""",
        (target_user_id,),
    )
    conn.commit()

    # 撤销该 OPERATOR 所有活跃 Token(下次请求被拒)
    from db import SESSIONS
    SESSIONS.clear_user(target_user_id)

    conn.close()
    audit.log(admin_user["id"], "REVOKE_OPERATOR", target_user_id, detail=f"revoked_at={now}")
    return {"revoked": True, "user": conn2_fetch(target_user_id)}
