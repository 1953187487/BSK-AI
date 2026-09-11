"""沙盒服务器配置。

对齐策划书 13.9:
- 官方账号密码明文永不在任何地方出现;只在部署时本地随机生成并通过
  带外机制(当前对话)下发,源码/配置/构建产物均不含明文默认值。
- QQ/微信 的 AppID/Secret 也不进公开仓库,仅由环境变量提供;缺省给 mock
  值只用于本地离线(AURA_OAUTH_MOCK=1)场景,生产必须注入真实值。
"""
import os
import pathlib
import secrets
import string
from dataclasses import dataclass

BASE_DIR = pathlib.Path(__file__).resolve().parent
DATA_DIR = BASE_DIR / "data"
DATA_DIR.mkdir(exist_ok=True)


def _generate_official_password() -> str:
    """首次部署时本地随机生成官方账号密码(仅部署者可见,不落盘、不入仓)。

    复杂度:16 位,含大小写、数字、符号(策划书 13.7 密码规则)。
    生成后通过带外机制(当前对话)单独下发给项目所有者,之后立即强制改密。
    """
    alphabet = (
        string.ascii_uppercase
        + string.ascii_lowercase
        + string.digits
        + "!@#$%^&*"
    )
    return "".join(secrets.choice(alphabet) for _ in range(16))


@dataclass
class Config:
    host: str = "127.0.0.1"
    port: int = 18080
    db_path: str = str(DATA_DIR / "aura.db")

    jwt_secret: str = os.environ.get("AURA_JWT_SECRET", "aura-sandbox-local-secret")
    token_expire_days: int = 7
    refresh_expire_days: int = 30

    pbkdf2_iterations: int = 100_000
    max_failures: int = 5
    lock_minutes: int = 15

    official_id: str = os.environ.get("AURA_OFFICIAL_ID", "0001")
    official_account_name: str = os.environ.get("AURA_OFFICIAL_ACCOUNT_NAME", "aura_official")
    # 明文默认值已移除(策划书 13.9)。部署者经环境变量 AURA_OFFICIAL_PASSWORD 注入;
    # 未注入时首次部署本地随机生成并仅在 stdout 输出一次,不进源码/配置/产物。
    official_password: str = os.environ.get("AURA_OFFICIAL_PASSWORD", "") or _generate_official_password()

    operator_max_count: int = 1
    operator_allow_self_revoke: bool = False

    qq_appid: str = os.environ.get("QQ_APPID", "mock_qq_appid")
    qq_appkey: str = os.environ.get("QQ_APPKEY", "mock_qq_appkey")
    wechat_appid: str = os.environ.get("WECHAT_APPID", "mock_wechat_appid")
    wechat_appsecret: str = os.environ.get("WECHAT_APPSECRET", "mock_wechat_appsecret")

    redis_host: str = os.environ.get("REDIS_HOST", "127.0.0.1")
    redis_port: int = int(os.environ.get("REDIS_PORT", "6379"))

    def __post_init__(self) -> None:
        os.makedirs(self.db_path and str(pathlib.Path(self.db_path).parent), exist_ok=True)


CFG = Config()
