"""AURA 沙盒服务器数据层:SQLite(账号/审计)+ Redis(会话看板)。

对齐策划书 6.9 / 第十四章 / 第十六章:
- user: 账号主表(id, account_name, display_name, password_hash, role, status, provider, operator_granted_at, granted_by)
- identity_provider: OAuth 身份(provider, provider_uid 唯一约束)
- admin_audit_log: 审计日志
- announcement: 公告
- login_history: 登录历史
- official_login_fail: 官方登录失败计数 + 锁定
- token_blacklist: Token 撤销

会话看板:Redis user_id → session HASH(单端唯一)
官方账号 ID=0001 由 seed.py 首次部署写入,不可注册、不可删除。
"""
import sqlite3
import threading
import time
import uuid

from config import CFG

_lock = threading.Lock()


def _conn() -> sqlite3.Connection:
    conn = sqlite3.connect(CFG.db_path, check_same_thread=False)
    conn.row_factory = sqlite3.Row
    conn.execute("PRAGMA foreign_keys=ON")
    return conn


def init_db() -> None:
    conn = _conn()
    conn.executescript(
        """
        CREATE TABLE IF NOT EXISTS user (
            id TEXT PRIMARY KEY,
            account_name TEXT UNIQUE,
            display_name TEXT,
            avatar_url TEXT,
            password_hash TEXT,
            role TEXT NOT NULL CHECK(role IN ('USER','OPERATOR','ADMIN')),
            status TEXT NOT NULL DEFAULT 'NORMAL'
                CHECK(status IN ('NORMAL','BANNED','DISABLED')),
            provider TEXT,
            operator_granted_at REAL,
            granted_by TEXT,
            created_at REAL NOT NULL
        );

        CREATE TABLE IF NOT EXISTS identity_provider (
            provider TEXT NOT NULL,
            provider_uid TEXT NOT NULL,
            access_token TEXT,
            refresh_token TEXT,
            expires_at REAL,
            user_id TEXT NOT NULL REFERENCES user(id) ON DELETE CASCADE,
            created_at REAL NOT NULL,
            PRIMARY KEY (provider, provider_uid)
        );
        CREATE UNIQUE INDEX IF NOT EXISTS ux_identity_user ON identity_provider(provider, provider_uid);

        CREATE TABLE IF NOT EXISTS admin_audit_log (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            admin_id TEXT,
            action TEXT,
            target_user_id TEXT,
            detail TEXT,
            ip TEXT,
            created_at REAL
        );

        CREATE TABLE IF NOT EXISTS announcement (
            id TEXT PRIMARY KEY,
            title TEXT,
            body TEXT,
            scope TEXT,
            scheduled_at REAL,
            revoked INTEGER DEFAULT 0,
            created_by TEXT,
            created_at REAL
        );

        CREATE TABLE IF NOT EXISTS login_history (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            user_id TEXT,
            provider TEXT,
            ip TEXT,
            device TEXT,
            created_at REAL
        );

        CREATE TABLE IF NOT EXISTS official_login_fail (
            user_id TEXT PRIMARY KEY,
            fail_count INTEGER DEFAULT 0,
            locked_until REAL
        );

        CREATE TABLE IF NOT EXISTS token_blacklist (
            jti TEXT PRIMARY KEY,
            expired_at REAL
        );
        """
    )
    conn.commit()
    conn.close()


def next_random_user_id() -> str:
    return uuid.uuid4().hex[:16]


class SessionStore:
    """会话看板:真 Redis 维护 user_id → session,单端唯一。

    对齐策划书 13.7 单端唯一 + 第十六章会话看板。
    Redis key: aura:session:{user_id} (HASH)
    KICKED_OUT 事件: aura:kicked stream
    """

    def __init__(self) -> None:
        import redis
        try:
            self._r = redis.Redis(host=CFG.redis_host, port=CFG.redis_port, decode_responses=True)
            self._r.ping()
        except Exception:
            raise RuntimeError(f"Redis 不可用({CFG.redis_host}:{CFG.redis_port}),请先启动 redis-server")

    def _key(self, user_id: str) -> str:
        return f"aura:session:{user_id}"

    def issue(self, user_id: str, token: str, refresh: str, device: str, ip: str) -> None:
        self._r.hset(self._key(user_id), mapping={
            "token": token, "refresh": refresh, "device": device,
            "ip": ip, "state": "ACTIVE", "created_at": str(time.time()),
        })
        self._r.expire(self._key(user_id), CFG.token_expire_days * 86400)

    def get(self, user_id: str) -> dict | None:
        h = self._r.hgetall(self._key(user_id))
        return h if h else None

    def kick(self, user_id: str) -> bool:
        s = self.get(user_id)
        if not s:
            return False
        self._r.hset(self._key(user_id), "state", "KICKED")
        self._r.xadd("aura:kicked", {"user_id": user_id, "ts": str(time.time())})
        return True

    def list_active(self) -> list[dict]:
        out = []
        for key in self._r.scan_iter(match="aura:session:*"):
            h = self._r.hgetall(key)
            if h and h.get("state") == "ACTIVE":
                h["user_id"] = key.split(":")[-1]
                out.append(h)
        return out

    def clear_user(self, user_id: str) -> None:
        self._r.delete(self._key(user_id))

    def backend(self) -> str:
        return "redis"


SESSIONS = SessionStore()
