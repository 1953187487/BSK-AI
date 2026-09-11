"""OAuth 服务端 service:state 管理 + Provider 工厂。

state 防伪(策划书 13.6):authorize 生成 state,回调校验,防 CSRF。
Provider 真实调开放平台(见 providers 包),本地离线走 mock。
"""
import time
import uuid
from typing import Any

from config import CFG
from providers import QQProvider, WeChatProvider


class _PendingState(dict[str, dict[str, Any]]):
    def __init__(self) -> None:
        self._data: dict[str, dict[str, Any]] = {}
        self._last_clean = time.time()

    def put(self, state: str, provider: str, redirect: str) -> None:
        if time.time() - self._last_clean > 600:
            self._clean()
        self._data[state] = {"provider": provider, "redirect": redirect, "created_at": time.time()}

    def pop(self, state: str, provider: str) -> str | None:
        info = self._data.pop(state, None)
        if not info or info["provider"] != provider:
            return None
        return info["redirect"]

    def _clean(self) -> None:
        now = time.time()
        self._data = {k: v for k, v in self._data.items() if now - v["created_at"] < 600}
        self._last_clean = now


_states = _PendingState()


def get_provider(name: str, redirect: str = "aura://oauth/qq"):
    if name.upper() == "QQ":
        return QQProvider(CFG.qq_appid, CFG.qq_appkey, redirect)
    if name.upper() == "WECHAT":
        return WeChatProvider(CFG.wechat_appid, CFG.wechat_appsecret, redirect)
    from error_codes import err
    raise err("PROVIDER_NOT_SUPPORTED", f"不支持的登录方式: {name}")


def create_state(provider: str, redirect: str) -> str:
    state = uuid.uuid4().hex
    _states.put(state, provider, redirect)
    return state


def verify_state(state: str, provider: str) -> str:
    """返回 redirect_uri,校验失败抛 STATE_MISMATCH。"""
    from error_codes import err
    redirect = _states.pop(state, provider)
    if redirect is None:
        raise err("STATE_MISMATCH", "state 不匹配(防 CSRF)")
    return redirect


def mock_code(provider: str, fake_uid: str) -> str:
    """仅 mock 模式(AURA_OAUTH_MOCK=1)下由客户端侧使用,返回可被 exchange 识别的 code。

    真实模式:code 由开放平台生成,客户端经 SDK 拿到,服务端无需感知。
    mock 模式下,callback 入参直接接受 fake code,Provider.exchange 内部按 code 派生 uid。
    """
    return fake_uid
