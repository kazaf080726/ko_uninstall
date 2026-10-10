# KO卸载

> 开机自动卸载 **inte 内核完整性检测模块**（OPPO / 一加 / 真我 ColorOS 系），守护进程持续盯防，内置液态玻璃 WebUI 控制台。
>
> 续承经典模块：`ddc_inte_killer`（冬日渡川）、`Fuck-inte`（无敌WX）、`MAPLE 内核检测`（MapleAutumn）——三位前辈的模块已被作者弃置，本项目延续其使命并全面重构。

**作者：Kazaf** · **版本：v1.00** · **License：MIT**

---

## 特性

- **史诗级重构**：合并三代模块核心逻辑为统一守护进程（隐藏启动、单实例锁、状态机、日志轮转）
- **开机自动卸载**：`inte` / `oplus_integrity` / `oplus_kohashpro` / `oplus_secure_guard(_new)` 白名单精准打击
- **保护名单**：`cdt_integrity`、`msm_core`、`qmi_helpers` 等模块绝不触碰，防止误卸载系统组件
- **守护进程盯防**：卸载后每 N 秒复核，inte 一旦复活立即再次卸载；内置 builtin / 内核保护识别
- **液态玻璃 WebUI**：极光动态背景、磨砂玻璃卡片、弹性按钮动效、状态光环、中英双语
- **内核检测面板（MAPLE）**：集成 3.sh 检测逻辑，逐项显示 `/proc/inte_*` 节点与模块加载状态
- **在线升级**：`updateJson` 指向 GitHub `update.json`，Root 管理器直接检测更新并安装
- **本地服务模式**：一键启动 `busybox httpd`，在任意浏览器打开 `http://127.0.0.1:8080` 即可管理
- **多管理器兼容**：Magisk / KernelSU / SukiSU / KernelSU-Next / RKSU / APatch

---

## 安装

1. 下载 Release 中的 `KOUninstall-v1.00.zip`
2. 打开 Root 管理器（Magisk / KernelSU 等）→ 模块 → 从本地安装
3. 重启生效

> 升级旧模块（`ddc_inte_killer` / `Fuck-inte`）时，安装脚本会自动清理旧守护进程，可直接覆盖安装。

---

## 使用

### WebUI 控制台（推荐）

KernelSU 管理器 → 模块 → KO卸载 → 打开 WebUI。支持：

| 功能 | 说明 |
| --- | --- |
| 状态总览 | 保护状态光环、守护进程、运行时长、检测节点 |
| 内核检测 | MAPLE 面板逐项检查 `/proc/inte_ko`、`/proc/inte_systbl`、`/proc/inte_status`、`/proc/modules`、`/sys/module/inte`、`kallsyms` |
| 模式切换 | 卸载模式（默认）/ 监控模式（只观察不卸载） |
| 复核频率 | 3 / 5 / 10 / 30 / 60 秒 |
| 立即清除 | 手动执行一次卸载 |
| 在线升级 | 检查 update.json，发现新版可跳转下载 |
| 本地服务 | 开启后任意浏览器访问 `http://127.0.0.1:8080` |

### 命令入口

```sh
# 手动清除一次（Root 管理器「执行」按钮等效）
sh /data/adb/modules/ko_uninstall/action.sh

# 查看单行状态协议
sh /data/adb/modules/ko_uninstall/bin/webstatus.sh

# 内核检测（MAPLE）
sh /data/adb/modules/ko_uninstall/bin/detect.sh

# 检查更新
sh /data/adb/modules/ko_uninstall/bin/checkupdate.sh

# 本地服务
sh /data/adb/modules/ko_uninstall/bin/server.sh start|stop|status
```

---

## 在线升级（update.json）

`module.prop` 中声明：

```
updateJson=https://raw.githubusercontent.com/kazaf080726/ko_uninstall/main/update.json
```

Root 管理器（Magisk / KernelSU）会自动读取该文件，检测到新版本后提示更新。发新版流程：

1. 修改 `module.prop` 版本号与 `versionCode`
2. 更新仓库根目录 `update.json`（version / versionCode / zipUrl / changelog）
3. 打包 zip → 在 GitHub 创建 Release（tag 与 zipUrl 一致）

---

## 目录结构

```
ko_uninstall/
├── module.prop              # 模块元数据 (含 updateJson / webuiIcon)
├── customize.sh             # 安装初始化 (旧版清理 / 数据目录 / 权限)
├── post-fs-data.sh          # 早期启动守护
├── service.sh               # 开机完成兜底再启守护
├── action.sh                # Root 管理器「执行」按钮
├── uninstall.sh             # 卸载前清理守护与痕迹
├── bin/
│   ├── common.sh            # 公共函数库 (检测 / 卸载 / 保护名单 / 设备信息)
│   ├── daemon.sh            # 守护进程正文 (隐藏启动)
│   ├── launch_daemon.sh     # 守护启动器 (进程隐藏, 无痕迹)
│   ├── detect.sh            # 内核检测面板 (MAPLE / 3.sh 集成)
│   ├── checkupdate.sh       # 在线升级检测 (update.json)
│   ├── server.sh            # 本地服务模式 (busybox httpd)
│   ├── open_link.sh         # 系统浏览器打开链接
│   └── webstatus.sh         # WebUI 单行状态协议
├── webroot/                 # 液态玻璃 WebUI (KernelSU WebUI)
│   ├── index.html
│   ├── style.css
│   ├── app.js
│   └── icon.png
├── META-INF/                # 安装脚本 (Magisk/KernelSU/APatch)
├── update.json              # 在线升级元数据
├── README.md
└── LICENSE
```

---

## 兼容性与安全说明

- **仅卸载白名单内的内核模块**，其余模块与系统组件一律不碰
- 保护名单中的模块（如 `cdt_integrity`）即使名称相近也绝不触碰
- 守护进程通过 `/dev/.sh` 隐藏运行，不留模块路径痕迹
- 卸载模块后守护进程立即停止，清理所有临时文件

## 致谢

- 冬日渡川 —— `ddc_inte_killer` 原作者
- 无敌WX —— `Fuck-inte` 原作者
- MapleAutumn —— `MAPLE · 多于ko模块检测`（3.sh）

## 免责声明

本项目仅用于个人设备研究与学习。内核模块卸载属于高风险操作，请在确认理解后果后使用，作者不对任何设备损坏负责。相关功能仅针对 OPPO/一加/真我 ColorOS 的 inte 内核完整性检测模块。
