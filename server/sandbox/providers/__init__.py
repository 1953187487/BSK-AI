"""真实 OAuth Provider 层(对接 QQ / 微信官方开放平台接口)。

策划书 13.1/13.2:
- QQ:graph.qq.com 换 token + 拉 userInfo → 查/建账号;不存 QQ 号,只存 openid
- 微信:api.weixin.qq.com 换 token + 拉 userInfo;unionid 用于同人识别

真实调用:
- QQ access_token: https://graph.qq.com/oauth2.0/token
- QQ 拉信息:       https://graph.qq.com/user/get_user_info
- 微信 access_token: https://api.weixin.qq.com/sns/oauth2/access_token
- 微信拉信息:       https://api.weixin.qq.com/sns/api/users (带 access_token+openid)
- 微信 unionid:    https://api.weixin.qq.com/sns/userinfo

本地 Mock 模式:设置 env AURA_OAUTH_MOCK=1 后,不调外部接口,
用确定性 hash 模拟 openid(便于 CI / 离线测试)。

提供统一接口:
  authorize_url(state, redirect) -> str
  exchange(code) -> {provider_uid, unionid, nickname, avatar, access_token, expires_at}
"""
import hashlib
import os
import time
from typing import Any

import httpx

MOCK = os.environ.get("AURA_OAUTH_MOCK") == "1"


class OAuthError(Exception):
    def __init__(self, code: str, message: str) -> None:
        self.code = code
        self.message = message
        super().__init__(message)


class BaseProvider:
    provider: str = "UNKNOWN"

    def __init__(self, appid: str, appsecret: str, redirect: str) -> None:
        self.appid = appid
        self.appsecret = appsecret
        self.redirect = redirect

    def authorize_url(self, state: str) -> str:
        raise NotImplementedError

    def exchange(self, code: str) -> dict[str, Any]:
        raise NotImplementedError


class QQProvider(BaseProvider):
    provider = "QQ"

    def authorize_url(self, state: str) -> str:
        return (
            f"https://graph.qq.com/oauth2.0/authorize"
            f"?response_type=code&client_id={self.appid}"
            f"&redirect_uri={self.redirect}&state={state}"
            f"&scope=get_user_info"
        )

    def exchange(self, code: str) -> dict[str, Any]:
        if MOCK:
            uid = "qq_" + hashlib.sha1(f"{self.appid}:{code}".encode()).hexdigest()[:20]
            return {
                "provider_uid": uid, "unionid": uid, "nickname": "QQ 用户(mock)",
                "avatar": None, "access_token": "mock", "expires_at": time.time() + 3600,
            }
        # 真实:code 换 access_token
        tok = httpx.post(
            "https://graph.qq.com/oauth2.0/token",
            data={
                "grant_type": "authorization_code",
                "client_id": self.appid,
                "client_secret": self.appsecret,
                "code": code,
                "redirect_uri": self.redirect,
            },
            timeout=10,
        ).json()
        if "access_token" not in tok:
            raise OAuthError("CODE_EXPIRED", f"QQ 换 token 失败: {tok.get('message', tok)}")
        # 拉用户信息(不存 QQ 号,只存 openid)
        info = httpx.get(
            f"https://graph.qq.com/user/get_user_info",
            params={"access_token": tok["access_token"], "openid": tok["openid"]},
            timeout=10,
        ).json()
        if "nickname" not in info and "ret" not in info:
            raise OAuthError("CODE_EXPIRED", f"QQ 拉信息失败: {info}")
        return {
            "provider_uid": tok["openid"],
            "unionid": tok["openid"],
            "nickname": info.get("nickname", "QQ 用户"),
            "avatar": info.get("figureurl_64"),
            "access_token": tok["access_token"],
            "expires_at": time.time() + int(tok.get("expires_in", 3600)),
        }


class WeChatProvider(BaseProvider):
    provider = "WECHAT"

    def authorize_url(self, state: str) -> str:
        return (
            f"https://open.weixin.qq.com/connect/qrconnect"
            f"?appid={self.appid}&redirect_uri={self.redirect}"
            f"&response_type=code&scope=snsapi_userinfo&state={state}#wechat_redirect"
        )

    def exchange(self, code: str) -> dict[str, Any]:
        if MOCK:
            uid = "wx_" + hashlib.sha1(f"{self.appid}:{code}".encode()).hexdigest()[:20]
            unionid = "wx_union_" + hashlib.sha1(uid.encode()).hexdigest()[:12]
            return {
                "provider_uid": uid, "unionid": unionid, "nickname": "微信用户(mock)",
                "avatar": None, "access_token": "mock", "expires_at": time.time() + 3600,
            }
        tok = httpx.get(
            "https://api.weixin.qq.com/sns/oauth2/access_token",
            params={
                "appid": self.appid, "secret": self.appsecret,
                "code": code, "grant_type": "authorization_code",
            },
            timeout=10,
        ).json()
        if "access_token" not in tok:
            raise OAuthError("CODE_EXPIRED", f"微信换 token 失败: {tok.get('errmsg', tok)}")
        info = httpx.get(
            "https://api.weixin.qq.com/sns/userinfo",
            params={"access_token": tok["access_token"], "openid": tok["openid"]},
            timeout=10,
        ).json()
        unionid = info.get("unionid") or tok.get("unionid") or "wx_union_" + hashlib.sha1(tok["openid"].encode()).hexdigest()[:12]
        return {
            "provider_uid": tok["openid"],
            "unionid": unionid,
            "nickname": info.get("nickname", "微信用户"),
            "avatar": info.get("headimgurl"),
            "access_token": tok["access_token"],
            "expires_at": time.time() + int(tok.get("expires_in", 3600)),
        }
