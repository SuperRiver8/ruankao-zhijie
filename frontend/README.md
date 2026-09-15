# 前端项目

`customer` 是用户端，端口 5173；`admin` 是管理后台，端口 5174。两个目录分别维护源码、package.json、锁文件和 Vite 配置，构建产物分别位于各自的 `dist`。

在项目根目录执行：

```shell
npm --prefix frontend/customer ci
npm --prefix frontend/customer run dev
```

管理后台另开终端：

```shell
npm --prefix frontend/admin ci
npm --prefix frontend/admin run dev
```

两个开发服务器均将 `/api` 转发到 `http://localhost:8080`。也可以在 frontend 下继续使用 `npm run dev`、`npm run dev:admin`；`npm run build` 顺序构建两个项目。

认证接口分别使用 /api/customer/auth 与 /api/admin/auth，刷新及退出也独立。后台没有公开注册入口；默认管理员由 001 初始化 SQL 创建。两个项目的 OpenAPI 类型来自同一后端文档。开发后端启动后，在各项目执行 npm run types:generate 更新类型（默认 http://localhost:8080/v3/api-docs）。
