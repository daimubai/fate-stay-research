# API

`GET /api/v1/health`：公开的服务连通性接口，无需登录。

```json
{"status":"UP","application":"fate-stay-research"}
```

`GET /actuator/health`：公开的 Spring Boot 存活检查，只返回健康状态。

其他路径默认禁止访问；尚无业务 API 或账户认证。Springdoc 依赖已声明，但文档路径尚未放行。

前端使用相对路径请求 `/api/v1/health`，由 Vite 开发代理转发至 `http://127.0.0.1:8080`。无需跨域配置。生产环境需单独配置反向代理。
