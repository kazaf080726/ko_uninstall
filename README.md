# KO卸载

> **卸载 inte.ko —— 一加 / OPPO / 真我 ColorOS 的内核完整性检测模块，让 inte 彻底失效。**
> 开机自动卸载 inte 及其关联 KO 模块（`oplus_integrity` / `oplus_kohashpro` / `oplus_secure_guard(_new)`），守护进程全天候盯防，模块一旦复活立即再次卸载。

**作者：Kazaf** · **版本：v3.1** · **License：MIT**

续承三位前辈的弃置模块：`ddc_inte_killer`（冬日渡川）、`Fuck-inte`（无敌WX）、`MAPLE 内核检测`（MapleAutumn），延续其使命并全面重构。

---

## 它做什么

| 能力 | 说明 |
| --- | --- |
| 卸载 inte.ko | 开机早期卸载 inte 及关联 KO 模块，使其失去检测能力 |
| 防复活 | 守护进程按秒复核，inte 被重新加载即再次卸载；激进模式下自动收紧到 1s |
| 即时响应 | 模式 / 复核间隔 / 屏蔽强度切换 1s 内生效，无需重启；WebUI / APP 直读守护状态缓存，操作零卡顿 |
| 精准打击 | 只碰白名单内的 inte 家族模块，`cdt_integrity` 等系统模块绝不触碰 |
| 深度检测 | 12 维检测：`/proc/inte_*` 节点、`/proc/modules`、`/sys/module`、kallsyms、/dev 节点、.ko 文件、cmdline、refcnt、initstate |
| 开机自保护 | 旧砖模式：连续 3 次未完成开机自动禁用本模块防砖，boot.log 全程记录 |
| 在线升级 | `update.json` 远程更新，Root 管理器直接检测安装 |
| 管理 APP | 原生管理 APP，安装模块时自动安装，直读状态缓存秒级响应，与 WebUI 并存，桌面直控；v3.1 液态玻璃三屏 UI（首页/检测/设置）复刻 KernelSU/SukiSU 管理器视觉，root 缺失全屏提示 |
| 多管理器 | Magisk / KernelSU / SukiSU / KernelSU-Next / RKSU / APatch |

## 安装

1. 下载 Release 中的 `KOUninstall-v3.1.zip`
2. Root 管理器 → 模块 → 从本地安装
3. 重启生效（升级旧版可覆盖安装，会自动清理旧守护；管理 APP 自动安装）

## 兼容与安全

- 仅卸载白名单模块，保护名单（`cdt_integrity` 等）绝不触碰
- 守护进程隐藏运行，无模块路径痕迹
- 卸载模块后守护进程立即停止，清理所有临时文件
- 纯 rmmod 卸载方案，不注入任何挂载/内核模块，无 bootloop 风险

## 致谢

- 冬日渡川 —— `ddc_inte_killer` 原作者
- 无敌WX —— `Fuck-inte` 原作者
- MapleAutumn —— `MAPLE · 多于ko模块检测`（3.sh）

## 免责声明

本项目仅用于个人设备研究与学习。内核模块卸载属于高风险操作，请在确认理解后果后使用，作者不对任何设备损坏负责。
