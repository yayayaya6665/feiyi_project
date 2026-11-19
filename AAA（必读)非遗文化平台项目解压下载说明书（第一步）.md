# 非遗文化平台项目说明书

## 一、项目简介

本平台是一个基于 Spring Boot + Vue 3 的非物质文化遗产数字化展示与管理系统。后端提供 RESTful API，前端负责页面渲染与用户交互，数据库使用 MySQL 8.0。

---

## 二、环境准备（必须提前安装）

在开始之前，请确保您的计算机已安装以下软件：

| 软件     | 版本要求              | 官方下载地址                                                 |
| :------- | :-------------------- | :----------------------------------------------------------- |
| Java JDK | 17 或更高             | https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html |
| MySQL    | 8.0 系列              | https://dev.mysql.com/downloads/installer/                   |
| Node.js  | 18.x 或更高（含 npm） | https://nodejs.org/                                          |

安装说明：
- 安装时采用默认选项，连续选择“下一步”即可。
- MySQL 安装过程中请务必牢记您设置的 root 密码，后续配置将多次使用。

---

## 三、获取项目代码

1. 从项目负责人处获取压缩包 `3_feiyi.zip`。
2. 解压至**全英文路径**（例如 `D:\3_feiyi` 或 `C:\work\3_feiyi`），避免使用中文或空格，以免后续工具识别异常。
3. 解压后目录结构应如下：
   ```
   3_feiyi/
   ├── springboot/          # 后端源码（Spring Boot）
   ├── vue3/                # 前端源码（Vue 3）
   ├── heritage_with_data.sql   # 数据库备份（含表结构和初始数据）
   └── 使用说明.txt         # （旧版，可忽略）
   ```

---

## 四、数据库导入

### 4.1 打开命令行终端（CMD）

- 按 `Win + R`，输入 `cmd`，回车。
- 建议以管理员身份运行（右键点击“命令提示符” → “以管理员身份运行”）。

### 4.2 进入项目根目录

```cmd
cd /d D:\3_feiyi
```
（将 `D:\3_feiyi` 替换为您的实际解压路径）

### 4.3 登录 MySQL

```cmd
mysql -u root -p
```
系统提示 `Enter password:`，输入安装 MySQL 时设置的 root 密码（输入时屏幕无反馈，属正常现象）。

### 4.4 创建数据库并导入数据

在 `mysql>` 提示符下，依次执行以下命令（每条命令以分号结尾，按回车执行）：

```sql
CREATE DATABASE IF NOT EXISTS heritage DEFAULT CHARACTER SET utf8mb4;
USE heritage;
SOURCE D:/3_feiyi/heritage_with_data.sql;   -- 注意：路径使用正斜杠 /
```

路径书写说明：
- 必须使用正斜杠 `/`（例如 `D:/3_feiyi/...`），切勿使用反斜杠 `\`，否则 MySQL 会报 `Unknown command` 错误。
- 若路径包含中文或空格，请将路径用双引号括起来，例如 `SOURCE "D:/我的项目/3_feiyi/heritage_with_data.sql";`，但强烈建议使用纯英文路径以避免编码问题。

导入过程约需 10 至 30 秒，请耐心等待。

### 4.5 验证是否成功

```sql
SHOW TABLES;
```
如果列出 28 张表（如 `user`、`course`、`order` 等），则导入成功。若表数量不符或报错，请重新执行 `SOURCE` 命令。

### 4.6 退出 MySQL

```sql
EXIT;
```

---

## 五、修改后端数据库密码配置

1. 进入 `3_feiyi/springboot/src/main/resources/` 目录。
2. 使用记事本（或其他文本编辑器）打开 `application.yml` 文件。
3. 找到以下内容：
   ```yaml
   datasource:
     username: root
     password: 123456
   ```
4. 将 `password` 的值修改为您电脑 MySQL 的 root 密码（例如 `888888`）。
   > 注意：冒号后面必须保留一个空格，例如 `password: 888888`。
5. 保存文件并关闭编辑器。

---

## 六、启动后端服务

1. 打开一个新的 CMD 窗口（不要关闭之前的窗口）。
2. 进入后端目录：
   ```cmd
   cd /d D:\3_feiyi\springboot
   ```
3. 执行启动命令（首次运行会自动下载 Maven 依赖，可能需要 2 至 5 分钟）：
   ```cmd
   .\mvnw.cmd spring-boot:run
   ```
   > 注意：必须加 `.\` 前缀（PowerShell 用户尤其注意），否则会提示命令未找到。

4. 启动成功后，控制台最后几行会显示：
   ```
   Tomcat started on port 8889 (http) with context path '/'
   Started SpringbootApplication in 5.155 seconds
   ```
   此时表示后端已就绪，请保持此窗口持续运行，不要关闭。

### 端口被占用的处理方法

若启动时报错 `Port 8889 was already in use`，说明端口被其他进程占用。可选用以下任一方式解决：

- **方法一（推荐）**：查找并终止占用进程  
  在**新 CMD** 中执行：
  ```cmd
  netstat -ano | findstr :8889
  ```
  记下最后一列的 PID（例如 `41752`），然后执行：
  ```cmd
  taskkill /PID 41752 /F
  ```
  之后重新启动后端。

- **方法二**：修改后端端口  
  在 `application.yml` 中添加 `server.port: 8890`（或其它空闲端口），同时需相应修改前端请求的 baseURL 或代理配置。

---

## 七、启动前端服务

1. 再打开一个新的 CMD 窗口。
2. 进入前端目录：
   ```cmd
   cd /d D:\3_feiyi\vue3
   ```
3. 首次运行需要安装依赖（约 2 至 5 分钟）：
   ```cmd
   npm install
   ```
   若下载缓慢或卡顿，可使用国内镜像：
   ```cmd
   npm install --registry=https://registry.npmmirror.com
   ```
4. 启动前端开发服务器：
   ```cmd
   npm run dev
   ```
5. 成功后会显示：
   ```
   Local: http://localhost:8800/
   ```
   此时表示前端已启动，同样需要保持此窗口运行。

---

## 八、访问系统

1. 打开浏览器（推荐使用 Chrome 或 Edge）。
2. 访问地址：`http://localhost:8800`
3. 若看到网站首页，即表示部署成功。

### 测试账号

| 角色   | 用户名  | 密码       |
| :----- | :------ | :--------- |
| 管理员 | `admin` | `admin123` |
| 用户   | `test`  | `123456`   |
| 商家   | `123`   | `123456`   |

---

## 九、注意事项

### 9.1 保持两个命令行窗口同时运行
- 一个用于后端（`springboot` 目录），一个用于前端（`vue3` 目录）。
- 关闭任一窗口，对应服务即停止，网站将无法访问。

### 9.2 接口请求与跨域配置
- 后端默认监听端口 **8889**，前端默认监听端口 **8800**。
- 前端通过代理（Proxy）转发 API 请求到后端，无需额外配置 CORS。
- 若修改了后端端口，请同步修改 `vue3/vite.config.js` 中的代理目标地址，否则前端请求将返回 404。

### 9.3 图片显示异常的处理
若页面能加载文字但图片不显示，请按以下顺序排查：

1. 检查后端文件目录：确认 `springboot/files/image/` 路径下是否存在图片文件（数据库存储的是相对路径，如 `/files/image/xxx.jpg`）。
2. 检查静态资源映射：Spring Boot 已默认配置静态资源映射，请勿随意修改。
3. 查看前端图片请求地址：打开浏览器开发者工具（F12）→ Network，查看图片请求的完整 URL，确认是否指向 `http://localhost:8889/files/image/...`。
4. 替换外网占位图：若代码中引用了 `via.placeholder.com` 等外网服务，国内网络可能无法访问，建议替换为本地默认图片。

### 9.4 数据库编码
- 数据库默认字符集为 `utf8mb4`，支持 emoji 和生僻字，请勿更改。

### 9.5 避免端口冲突
- 建议使用 `Ctrl + C` 正常终止进程，待命令行返回提示符后再关闭窗口，防止后台残留僵尸进程导致端口占用。

---

## 十、常见问题及解决办法

| 问题现象                                                | 可能原因                   | 解决方案                                                     |
| :------------------------------------------------------ | :------------------------- | :----------------------------------------------------------- |
| `'mysql' 不是内部或外部命令`                            | MySQL 未添加到系统环境变量 | 搜索“MySQL 环境变量配置”，将 MySQL 的 `bin` 目录加入 Path    |
| `Access denied for user 'root'@'localhost'`             | 数据库密码错误             | 检查 `application.yml` 中的密码是否与安装时设置的一致        |
| 后端启动失败，提示 `Failed to execute goal ...`         | 依赖下载不完整或端口冲突   | 检查网络，重新执行 `.\mvnw.cmd clean` 后再运行；或参照端口占用处理方法 |
| 前端 `npm run dev` 报错 `Error: Cannot find module ...` | 依赖未完整安装             | 删除 `node_modules` 文件夹，重新执行 `npm install`           |
| 页面空白，控制台有 `404` 请求                           | 后端未启动或端口不匹配     | 确认后端已启动且前端代理配置正确                             |
| 页面能打开但图片不显示                                  | 图片文件缺失或路径错误     | 检查 `springboot/files/image` 目录是否存在图片；检查数据库 `image_url` 字段值是否与本地路径对应 |

