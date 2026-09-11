"""AURA 错误码表(对齐策划书第十章 17 项)。

每个错误码带 HTTP 状态与中文描述,接口统一返回 {"code": "...", "message": "...", "data": ...}。
"""


class ApiError(Exception):
    def __init__(self, code: str, message: str, http_status: int = 400) -> None:
        self.code = code
        self.message = message
        self.http_status = http_status
        super().__init__(message)


# 错误码 → (HTTP, 描述)
_CODES: dict[str, tuple[int, str]] = {
    "NAME_ALREADY_TAKEN": (409, "账号名称已被使用"),
    "PROVIDER_ALREADY_BOUND": (409, "该登录方式已绑定其他账号"),
    "ACCOUNT_NOT_FOUND": (404, "账号不存在"),
    "INVALID_CREDENTIALS": (401, "ID、账号名称或密码错误"),
    "KICKED_OUT": (401, "被新端登录踢下线"),
    "ACCOUNT_BANNED": (403, "账号已被封禁"),
    "PROVIDER_NOT_SUPPORTED": (400, "不支持的登录方式"),
    "STATE_MISMATCH": (400, "state 不匹配(防 CSRF)"),
    "CODE_EXPIRED": (400, "授权 code 已过期"),
    "TOKEN_EXPIRED": (401, "Token 已过期"),
    "PERMISSION_DENIED": (403, "权限不足(非 ADMIN/OPERATOR 调管理员接口)"),
    "OFFICIAL_LOGIN_REQUIRES_3_FIELDS": (400, "官方登录三字段缺一不可"),
    "ID_NAME_MISMATCH": (400, "ID 与账号名称不匹配"),
    "USER_NOT_FOUND": (404, "目标用户不存在"),
    "INVALID_TARGET": (400, "指定管理员的目标无效"),
    "OPERATOR_ALREADY_EXISTS": (409, "当前已有一名管理员,请先解除"),
    "CANNOT_REVOKE_SELF": (403, "OPERATOR 不能自行解除,须由 ADMIN 操作"),
}


def err(code: str, message: str | None = None, http_status: int | None = None) -> ApiError:
    if code not in _CODES:
        return ApiError(code, message or code, http_status or 400)
    default_http, default_msg = _CODES[code]
    return ApiError(code, message or default_msg, http_status or default_http)


def all_codes() -> dict[str, dict]:
    return {c: {"http": h, "message": m} for c, (h, m) in _CODES.items()}
