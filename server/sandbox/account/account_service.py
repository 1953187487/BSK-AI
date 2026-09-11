"""OAuth 账号服务:查/建账号、绑定/解绑、注销、登录历史。

执行策划书 13.3/13.4/13.5 的核心规则:
- 注册 = QQ/微信(OAuth),自动创建 role=USER,分配随机 ID
- 规则一:provider_uid 全局唯一,重复登录返回同一账号
- 规则二:单端唯一会话(由 SessionStore 保证,见 auth 层)
- 规则三:(provider, provider_uid) 唯一约束,抢占他人方式 → PROVIDER_ALREADY_BOUND
- 账号合并:同设备 + 有效 Token → 手动绑定(微信挂到现有账号)
- 官方账号(ADMIN)禁止绑定 OAuth 方式(条款二,完全隔离)
"""
import time
from typing import Any

from db import _conn, SESSIONS, next_random_user_id
from error_codes import err


def _fetch_user_by_oauth(provider: str, provider_uid: str) -> dict | None:
    conn = _conn()
    row = conn.execute(
        """SELECT u.* FROM user u
           JOIN identity_provider ip ON ip.user_id = u.id
           WHERE ip.provider=? AND ip.provider_uid=?""",
        (provider, provider_uid),
    ).fetchone()
    conn.close()
    return dict(row) if row else None


def create_or_login(
    provider: str,
    provider_uid: str,
    unionid: str,
    nickname: str,
    avatar: str | None,
    ip: str,
    device: str,
) -> dict[str, Any]:
    """OAuth 登录入口:首次建号,重复登录返回同一账号(规则一)。"""
    conn = _conn()
    user_row = _fetch_user_by_oauth(provider, provider_uid)
    now = time.time()

    if user_row:
        user = dict(user_row)
        if user["status"] != "NORMAL":
            conn.close()
            raise err("ACCOUNT_BANNED", "账号已被封禁")
        # 同人 QQ+微信 自动绑定提示(规则 13.4 场景4):unionid 相同则已有其它 provider
        # 此处仅返回,绑定提示由前端按 unionid 决定,服务端不强制
        conn.execute(
            "INSERT OR REPLACE INTO login_history (user_id, provider, ip, device, created_at) VALUES (?,?,?,?,?)",
            (user["id"], provider, ip, device, now),
        )
        conn.commit()
        conn.close()
        return {"user": user, "created": False}

    # 规则一:建号
    new_id = next_random_user_id()
    conn.execute(
        """INSERT INTO user
           (id, account_name, display_name, avatar_url, role, status, provider, created_at)
           VALUES (?, ?, ?, ?, 'USER', 'NORMAL', ?, ?)""",
        (new_id, f"user_{new_id[:8]}", nickname or "新用户", avatar, provider, now),
    )
    conn.execute(
        """INSERT INTO identity_provider
           (provider, provider_uid, user_id, access_token, refresh_token, expires_at, created_at)
           VALUES (?, ?, ?, ?, ?, ?, ?)""",
        (
            provider,
            provider_uid,
            new_id,
            None,
            None,
            None,
            now,
        ),
    )
    conn.execute(
        "INSERT INTO login_history (user_id, provider, ip, device, created_at) VALUES (?,?,?,?,?)",
        (new_id, provider, ip, device, now),
    )
    conn.commit()

    user = _fetch_user_by_oauth(provider, provider_uid)
    conn.close()
    return {"user": user, "created": True}


def bind_provider(
    user_id: str, provider: str, provider_uid: str
) -> dict[str, Any]:
    """手动绑定另一登录方式到现有账号(账号合并)。

    规则三:(provider, provider_uid) 唯一,若已被其它账号占用 → PROVIDER_ALREADY_BOUND。
    条款二:官方账号(ADMIN)禁止绑定 OAuth 方式。
    """
    conn = _conn()
    user = conn.execute("SELECT * FROM user WHERE id=?", (user_id,)).fetchone()
    if not user:
        conn.close()
        raise err("ACCOUNT_NOT_FOUND", "账号不存在")
    if user["role"] == "ADMIN":
        conn.close()
        raise err("PROVIDER_ALREADY_BOUND", "官方账号不可绑定登录方式(条款二隔离)")

    exists = conn.execute(
        "SELECT user_id FROM identity_provider WHERE provider=? AND provider_uid=?",
        (provider, provider_uid),
    ).fetchone()
    if exists and exists["user_id"] != user_id:
        conn.close()
        raise err("PROVIDER_ALREADY_BOUND", "该登录方式已绑定其他账号")

    now = time.time()
    conn.execute(
        """INSERT OR REPLACE INTO identity_provider
           (provider, provider_uid, user_id, created_at)
           VALUES (?, ?, ?, ?)""",
        (provider, provider_uid, user_id, now),
    )
    conn.commit()
    conn.close()
    return {"bound": True, "provider": provider}


def unbind_provider(user_id: str, provider: str, provider_uid: str) -> dict[str, Any]:
    """解绑登录方式。至少保留一种登录方式;官方账号不可操作(本就无绑定)。"""
    conn = _conn()
    bound = conn.execute(
        "SELECT COUNT(*) AS c FROM identity_provider WHERE user_id=?", (user_id,)
    ).fetchone()["c"]
    if bound <= 1:
        conn.close()
        raise err("PROVIDER_NOT_SUPPORTED", "至少保留一种登录方式,无法解绑")
    cur = conn.execute(
        "DELETE FROM identity_provider WHERE user_id=? AND provider=? AND provider_uid=?",
        (user_id, provider, provider_uid),
    )
    conn.commit()
    conn.close()
    return {"unbound": cur.rowcount > 0}


def delete_account(user_id: str) -> dict[str, Any]:
    """注销账号:删账号及关联数据,不可恢复(个保法撤回)。"""
    conn = _conn()
    u = conn.execute("SELECT role FROM user WHERE id=?", (user_id,)).fetchone()
    if not u:
        conn.close()
        raise err("ACCOUNT_NOT_FOUND", "账号不存在")
    if u["role"] == "ADMIN":
        conn.close()
        raise err("INVALID_TARGET", "官方账号不可注销(条款:不可删除、不可被注册覆盖)")
    conn.execute("DELETE FROM user WHERE id=?", (user_id,))
    conn.execute("DELETE FROM identity_provider WHERE user_id=?", (user_id,))
    conn.execute("DELETE FROM login_history WHERE user_id=?", (user_id,))
    conn.commit()
    SESSIONS.clear_user(user_id)
    conn.close()
    return {"deleted": True}


def recent_login_history(user_id: str, limit: int = 50) -> list[dict[str, Any]]:
    conn = _conn()
    rows = conn.execute(
        "SELECT provider, ip, device, created_at FROM login_history WHERE user_id=? ORDER BY created_at DESC LIMIT ?",
        (user_id, limit),
    ).fetchall()
    conn.close()
    return [dict(r) for r in rows]


def bound_providers(user_id: str) -> list[str]:
    conn = _conn()
    rows = conn.execute(
        "SELECT provider FROM identity_provider WHERE user_id=?", (user_id,)
    ).fetchall()
    conn.close()
    return [r["provider"] for r in rows]
