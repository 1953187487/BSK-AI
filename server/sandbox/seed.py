"""首次部署 seed 官方账号(ID=0001,role=ADMIN)。

条款一/二/三(策划书 6.1/6.3):
- 官方账号由系统预置,数量唯一,不可注册、不可被覆盖
- 账号名称与密码由部署者设定;密码明文永不在源码/配置/产物中出现(策划书 13.9),
  仅首次 seed 时由 config 本地生成并一次性打印到 stdout(带外下发),随后立即改密
"""
import sys
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
    # 仅首次 seed 时一次性输出密码,供带外机制(当前对话)下发给项目所有者;
    # 不写日志文件、不入仓、不进配置。收到后立即强制改密。
    print(
        f"[AURA seed] 官方账号 {CFG.official_id} ({CFG.official_account_name}) 已预置, "
        f"初始密码: {CFG.official_password}\n"
        f"          请立即通过带外机制下发并登录后强制改密(此值不再出现在任何文件/日志/提交中)。",
        file=sys.stderr,
    )
