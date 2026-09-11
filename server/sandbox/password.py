"""密码哈希(PBKDF2-HMAC-SHA256 本地实现,接口兼容后续换真 BCrypt)。

策划书 13.9 要求 BCrypt 加盐哈希(rounds=12)。
本地 Mock 阶段用 PBKDF2(标准库),字段名仍叫 password_hash,后续可平移。
"""
import hashlib
import secrets


def hash_password(plain: str) -> str:
    salt = secrets.token_hex(16)
    dk = hashlib.pbkdf2_hmac("sha256", plain.encode(), salt.encode(), 100_000)
    return f"pbkdf2_sha256${dk.hex()}${salt}"


def verify_password(plain: str, stored: str) -> bool:
    try:
        algo, dk_hex, salt = stored.split("$")
        assert algo == "pbkdf2_sha256"
        dk = hashlib.pbkdf2_hmac("sha256", plain.encode(), salt.encode(), 100_000)
        return secrets.compare_digest(dk.hex(), dk_hex)
    except Exception:
        return False


def password_complexity_ok(plain: str) -> bool:
    """官方账号密码规则:≥12 位、大小写+数字+符号。"""
    if len(plain) < 12:
        return False
    import re
    if not re.search(r"[a-z]", plain):
        return False
    if not re.search(r"[A-Z]", plain):
        return False
    if not re.search(r"\d", plain):
        return False
    if not re.search(r"[!@#$%^&*(),.?\":{}|<>]", plain):
        return False
    return True
