"""管理员审计日志(策划书 13.7⑤、6.8③:监察者亦被监察)。

所有管理员操作(踢线/封禁/解封/注销/指定/解除/公告)写入 AdminAuditLog。
指定 / 解除额外含 granted_by(=0001)、granted_at。
"""
import time
from typing import Any

from db import _conn


def log(
    admin_id: str,
    action: str,
    target_user_id: str | None,
    detail: str = "",
    ip: str = "127.0.0.1",
    result: str = "OK",
) -> None:
    conn = _conn()
    conn.execute(
        """INSERT INTO admin_audit_log (admin_id, action, target_user_id, detail, ip, created_at)
           VALUES (?, ?, ?, ?, ?, ?)""",
        (admin_id, action, target_user_id, f"{detail}|result={result}", ip, time.time()),
    )
    conn.commit()
    conn.close()


def recent(limit: int = 100) -> list[dict[str, Any]]:
    conn = _conn()
    rows = conn.execute(
        """SELECT admin_id, action, target_user_id, detail, ip, created_at
           FROM admin_audit_log ORDER BY id DESC LIMIT ?""",
        (limit,),
    ).fetchall()
    conn.close()
    return [dict(r) for r in rows]
