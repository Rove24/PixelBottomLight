# Pixel 底色 (PixelBottomLight)

[![API](https://img.shields.io/badge/API-102%20(Modern)-blue.svg)](https://github.com/libxposed/api)
[![Target](https://img.shields.io/badge/Target-SystemUI-orange.svg)]()
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

一个轻量级的现代化 Xposed / LSPosed 模块，将一加、OPPO 等搭载 ColorOS / OxygenOS 设备的单色语音助手侧滑光弧，替换为 Google Pixel 原生的经典四色光流（蓝、红、黄、绿）。
<img width="1180" height="2560" alt="IMG_20260916_095233" src="https://github.com/user-attachments/assets/32618883-4d49-47f7-a9ae-1fbb2e470570" />

---

## ✨ 特性

- 🎨 **Pixel 原生经典四色**：解除系统单色导航栏主题锁，还原 Google 经典的蓝（#4285F4）、红（#EA4335）、黄（#FBBC05）、绿（#34A853）分段渲染。
- 🟢 **圆角光弧端点**：开启原生两端圆角（`Cap.ROUND`），告别单调平头白条。
- 🪶 **超轻量纯后台**：无多余 UI 界面，APK 仅约 14 KB，无多余依赖和后台驻留。
- ⚡ **高性能零损耗**：采用继承链反射解析与静态缓存机制，杜绝每帧重复反射，滑动平滑流畅不掉帧。
- 🔒 **现代规范支持**：全面基于 **libxposed API 102** 规范开发，原生支持静态作用域（LSPosed 免手动勾选 SystemUI）。

---

## 📱 兼容性

- **支持系统**：ColorOS 13 / 14 / 15、OxygenOS 及采用类似 AOSP 语音助手架构的系统。
- **框架支持**：LSPosed（推荐启用并支持 API 102 现代架构的版本）。
- **作用域**：`com.android.systemui`（模块已配置静态作用域，安装激活后自动关联）。

---

## 📥 安装与使用

1. 在 [Releases](../../releases) 页面下载最新的 APK。
2. 安装 APK，在 LSPosed 管理器中启用模块（已配置静态作用域，自动勾选“系统界面 SystemUI”）。
3. 作用域生效后，**重启 SystemUI 或重启手机**。
4. 从屏幕底角侧滑呼出语音助手，即可看到 Pixel 四色底色光弧。

---

## 🛠️ 构建

本项目使用 Gradle 构建：

```bash
git clone https://github.com/your-username/PixelBottomLight.git
cd PixelBottomLight
./gradlew assembleRelease
