#!/usr/bin/env python3
"""Replay synthetic notification wording through a local Android emulator.

The fixtures mirror private audit patterns but contain no real codes, links, or
personal data. The emulator's shell package is temporarily configured as an
OTP-only source so the listener follows the same gate as email notifications.
"""

import argparse
import json
import shlex
import subprocess
import time
from datetime import datetime


APP_CLI = "./scripts/fluid-capsule-cli.sh"
CASES = [
    ("ordinary-link", "Delivery update", "Your package is ready. Track it at https://example.test/track/1234", "FILTERED"),
    ("misleading-title", "Verify your email", "Your package is ready. Track it at https://example.test/track/1234", "FILTERED"),
    ("guide-link", "Security guide", "Read the verification guide at https://example.test/help", "FILTERED"),
    ("completed", "Verification complete", "Your email address has been verified.", "FILTERED"),
    ("direct-verify", "Verify email", "Please click the following link to verify your email address: https://example.test/verify?token=fake", "PUBLISHED"),
    ("unidays-otp", "UNiDAYS", "482913 is your UNiDAYS passcode\nOne-time passcode\n482913\nHere is your passcode.\nIt will expire in 5 minutes.", "PUBLISHED"),
    ("riot-otp", "Riot Games", "登录代码：482913\n登入代码\n以下是你的登入代码：\n482913\n此代码将很快过期。", "PUBLISHED"),
    ("otp-and-link", "Verify email", "Your verification code is 482913. Or click the link to verify your email: https://example.test/verify?token=fake", "PUBLISHED"),
    ("no-target", "Verify work rights", "Verify your work rights", "FILTERED"),
]


def run(*args):
    return subprocess.run(args, text=True, capture_output=True, check=True).stdout


def cli(serial, *args):
    output = run(APP_CLI, "--serial", serial, *args)
    return json.loads(output.split('data="', 1)[1].rsplit('"', 1)[0])


def history(serial):
    rows = []
    after = 0
    snapshot = None
    while True:
        args = ["history", "export", str(after), "25"]
        if snapshot is not None:
            args.append(str(snapshot))
        page = cli(serial, *args)
        if snapshot is None:
            snapshot = page["snapshotMaxId"]
        rows.extend(page["entries"])
        if not page["hasMore"]:
            return rows
        after = page["nextAfterId"]


def await_decision(serial, title):
    deadline = time.monotonic() + 8
    while time.monotonic() < deadline:
        matches = [row for row in history(serial) if row["title"] == title]
        if matches and matches[-1]["decision"] != "CAPTURED":
            return matches[-1]
        time.sleep(0.2)
    raise RuntimeError(f"No final decision for {title}")


def snooze_previous_shell_notifications(serial):
    keys = run("adb", "-s", serial, "shell", "cmd", "notification", "list").splitlines()
    for key in keys:
        if "|com.android.shell|" in key:
            remote = shlex.join(["cmd", "notification", "snooze", "--for", "600000", key])
            run("adb", "-s", serial, "shell", remote)


def await_actions(serial, labels, source_title=None):
    deadline = time.monotonic() + 5
    while time.monotonic() < deadline:
        dump = run("adb", "-s", serial, "shell", "dumpsys", "notification", "--noredact")
        if all(f'"{label}"' in dump for label in labels) and (
            source_title is None or source_title in dump
        ):
            return True
        time.sleep(0.2)
    return False


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--serial", default="emulator-5554")
    args = parser.parse_args()
    serial = args.serial
    if run("adb", "-s", serial, "shell", "getprop", "ro.kernel.qemu").strip() != "1":
        parser.error("This replay is restricted to an Android emulator")

    status = cli(serial, "status")
    if not status["notificationListenerEnabled"] or not status["notificationPostingEnabled"]:
        parser.error("Install FluidCapsule and enable its notification listener first")
    shell_enabled = "com.android.shell" in status["whitelist"]
    shell_otp_only = "com.android.shell" in status["otpOnlyPackages"]
    history_enabled = status["historyEnabled"]
    nonce = datetime.now().strftime("%Y%m%d%H%M%S")
    failures = []

    try:
        cli(serial, "whitelist", "add", "com.android.shell")
        cli(serial, "whitelist", "otp-only", "com.android.shell", "true")
        cli(serial, "set", "history-enabled", "true")
        snooze_previous_shell_notifications(serial)
        for name, source_title, body, expected in CASES:
            title = f"QA {nonce} {name} {source_title}"
            tag = f"qa-{nonce}-{name}"
            remote = shlex.join(["cmd", "notification", "post", "-t", title, tag, body])
            run("adb", "-s", serial, "shell", remote)
            entry = await_decision(serial, title)
            actual = entry["decision"]
            print(f"{name}: {actual} (expected {expected})")
            if actual != expected:
                failures.append(name)
            if name == "direct-verify":
                if not await_actions(serial, ["打开验证链接"]):
                    failures.append("direct-verify-action")
                    print("direct-verify-action: missing")
                else:
                    print("direct-verify-action: present")
            if name == "otp-and-link":
                if "验证码识别成功" not in entry["decisionDetail"] or not await_actions(
                    serial, ["复制验证码", "打开验证链接"], title,
                ):
                    failures.append("otp-and-link-actions")
                    print("otp-and-link-actions: missing")
                else:
                    print("otp-and-link-actions: present")
    finally:
        cli(serial, "whitelist", "otp-only", "com.android.shell", "true" if shell_otp_only else "false")
        if not shell_enabled:
            cli(serial, "whitelist", "remove", "com.android.shell")
        cli(serial, "set", "history-enabled", "true" if history_enabled else "false")

    if failures:
        raise SystemExit(f"Replay failed: {', '.join(failures)}")


if __name__ == "__main__":
    main()
