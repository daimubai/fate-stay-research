# Deployment / 部署

当前支持本地 Spring Boot 可执行 JAR 和 Vite 开发服务器，启动命令见根目录 README。

尚无 Dockerfile、Compose、数据库或生产部署配置。生产前端构建命令为 `npm run build`，产物位于 `frontend/dist`。Vite 开发代理不会写入构建产物，生产托管需单独配置 `/api` 到后端的反向代理。`npm run preview` 仅用于检查静态构建资源。
