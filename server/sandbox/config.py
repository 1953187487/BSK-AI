"""沙盒服务器配置。

本地 Mock 阶段:
- 数据库:SQLite 文件
- 会话:内存 dict + 文件持久化
- 官方账号:首次启动 seed,账号名称与密码由 config/internal/account.yaml 提供
  (该文件仅存私有仓库,不提交;此处用环境变量兜底)
- QQ/微信 OAuth:模拟(不真调开放平台)
"""
import os
import pathlib
from dataclasses import dataclass

BASE_DIR = pathlib.Path(__file__).resolve().parent
DATA_DIR = BASE_DIR / "data"
DATA_DIR.mkdir(exist_ok=True)


@dataclass
class Config:
    host: str = "127.0.0.1"
    port: int = 18080
    db_path: str = str(DATA_DIR / "aura.db")

    jwt_secret: str = "aura-sandbox-local-secret"
    token_expire_days: int = 7
    refresh_expire_days: int = 30

    pbkdf2_iterations: int = 100_000
    max_failures: int = 5
    lock_minutes: int = 15

    official_id: str = os.environ.get("AURA_OFFICIAL_ID", "0001")
    official_account_name: str = os.environ.get("AURA_OFFICIAL_ACCOUNT_NAME", "aura_official")
    official_password: str = os.environ.get("AURA_OFFICIAL_PASSWORD", "changeme-official-Initial!")

    operator_max_count: int = 1
    operator_allow_self_revoke: bool = False

    qq_appid: str = os.environ.get("QQ_APPID", "mock_qq_appid")
    qq_appkey: str = os.environ.get("QQ_APPKEY", "mock_qq_appkey")
    wechat_appid: str = os.environ.get("WECHAT_APPID", "mock_wechat_appid")
    wechat_appsecret: str = os.environ.get("WECHAT_APPSECRET", "mock_wechat_appsecret")

    redis_host: str = "127.0.0.1"
    redis_port: int = 6379

    def __post_init__(self) -> None:
        os.makedirs(self.db_path and str(pathlib.Path(self.db_path).parent), exist_ok=True)


CFG = Config()
