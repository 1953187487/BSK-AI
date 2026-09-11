AURA 沙盒服务器(本地 Mock 骨架)

选型说明:
- 策划书 P7 原定 Spring Boot 3.x / FastAPI(Python 3.11)二选一
- 本地环境无 Java,选 FastAPI 落地
- 端口 127.0.0.1:18080,不暴露公网
- 数据库:SQLite(文件持久化),会话:内存 dict + 文件持久化(模拟 Redis,单端唯一)
- 密码:PBKDF2-HMAC-SHA256(本地 Mock 够用,接口兼容后续换真 BCrypt)
- QQ/微信 OAuth:模拟实现(不真正调开放平台,返回模拟 code/uid,便于本地跑通)
- 官方账号 ID=0001 由系统预置,首次启动 seed,不可注册、不可被覆盖

目录结构:
  server/sandbox/
  ├── main.py                 # 入口,uvicorn 启动
  ├── config.py               # 配置(读 .env 与内部 yaml)
  ├── db.py                   # SQLite 连接与建表
  ├── error_codes.py          # 错误码表(17 项)
  ├── auth.py                 # JWT 签发/校验
  ├── password.py             # 密码哈希(PBKDF2)
  ├── seed.py                 # 首次部署 seed 官方账号
  ├── providers/              # QQ/微信 provider 模拟
  ├── account/                # OAuth + 官方登录 controller/service
  ├── admin/                  # 管理员后台 controller + OperatorService + AdminAuditLog
  └── data/                   # 运行时数据(SQLite 文件、会话文件),gitignore

启动:
  cd server/sandbox
  python3 -m uvicorn main:app --host 127.0.0.1 --port 18080

验证:
  curl http://127.0.0.1:18080/healthz
  curl -X POST http://127.0.0.1:18080/api/auth/official/login -H 'Content-Type: application/json' -d '{"id":"0001","account_name":"aura_official","password":"<由带外机制下发>"}'
