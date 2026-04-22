#!/usr/bin/env python3
"""
TVBoxOS 环境体检脚本（SessionStart hook）

每次 Claude Code session 启动时自动运行，检查开发环境状态。
"""

import os
import subprocess
from pathlib import Path


def check_gradle_wrapper():
    """检查 Gradle Wrapper 是否存在"""
    if not Path("gradlew").exists():
        print("⚠️  未找到 gradlew，Gradle Wrapper 缺失")
        return False
    return True


def check_local_properties():
    """检查 local.properties（Android SDK 路径配置）"""
    if not Path("local.properties").exists():
        print("⚠️  未找到 local.properties，可能需要配置 Android SDK 路径")
        return False
    return True


def check_git_status():
    """检查 git 状态"""
    try:
        result = subprocess.run(
            ["git", "rev-parse", "--is-inside-work-tree"],
            capture_output=True, text=True, timeout=5
        )
        if result.returncode != 0:
            return True

        # 检查 stash
        result = subprocess.run(
            ["git", "stash", "list"],
            capture_output=True, text=True, timeout=5
        )
        if result.stdout.strip():
            stash_count = len(result.stdout.strip().split("\n"))
            print(f"📌 有 {stash_count} 个 stash 未处理")

        # 检查未提交变更
        result = subprocess.run(
            ["git", "status", "--porcelain"],
            capture_output=True, text=True, timeout=5
        )
        if result.stdout.strip():
            changed = len(result.stdout.strip().split("\n"))
            print(f"📝 有 {changed} 个文件有未提交的变更")

        # 当前分支
        result = subprocess.run(
            ["git", "branch", "--show-current"],
            capture_output=True, text=True, timeout=5
        )
        branch = result.stdout.strip()
        if branch:
            print(f"🌿 当前分支: {branch}")

    except (subprocess.TimeoutExpired, FileNotFoundError):
        pass
    return True


def check_last_session():
    """读取上次 session 日志"""
    dev_log_dir = Path("dev-log")
    if dev_log_dir.exists():
        logs = sorted(dev_log_dir.glob("*.md"), reverse=True)
        if logs:
            latest = logs[0]
            print(f"\n📋 上次 Session 日志: {latest.name}")
            print(f"   (输入 'cat {latest}' 查看详情)")


def main():
    print("🔍 TVBoxOS 环境体检...\n")

    checks = [
        ("Gradle Wrapper", check_gradle_wrapper),
        ("local.properties", check_local_properties),
        ("Git 状态", check_git_status),
    ]

    warnings = []
    for name, check_fn in checks:
        if not check_fn():
            warnings.append(name)

    print()
    if warnings:
        print(f"⚠️  体检完成，{len(warnings)} 项需要注意: {', '.join(warnings)}")
    else:
        print("✅ 环境体检通过，一切就绪！")

    check_last_session()


if __name__ == "__main__":
    main()
