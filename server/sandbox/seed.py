"""首次部署 seed 官方账号(ID=0001,role=ADMIN)。

条款一/二/三(策划书 6.1/6.3):
- 官方账号由系统预置,数量唯一,不可注册、不可被覆盖
- 账号名称与密码由部署者设定(本地 Mock 走 config,生产走带外机制下发)
"""
import time

from db import _conn
from config import CFG
from password import hash_password


def seed_official() -> None:
    conn = _conn()
    row = conn.execute(
        "SELECT id FROM user WHERE id=?", (CFG.official_id,)
    ).fetchone()
    if row:
        conn.close()
        return
    conn.execute(
        """INSERT INTO user
           (id, account_name, display_name, avatar_url, password_hash,
            role, status, provider, operator_granted_at, granted_by, created_at)
           VALUES (?, ?, ?, ?, ?, 'ADMIN', 'NORMAL', 'INTERNAL', NULL, NULL, ?)""",
        (
            CFG.official_id,
            CFG.official_account_name,
            "AURA Team",
            None,
            hash_password(CFG.official_password),
            time.time(),
        ),
    )
    conn.commit()
    conn.close()
