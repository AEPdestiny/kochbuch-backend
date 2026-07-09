#!/usr/bin/env python3
"""Validate that the German recipe seed files only differ in allowed text values."""

from __future__ import annotations

import json
import re
import sys
from pathlib import Path
from typing import Any

URL_PATTERN = re.compile(r"https?://[^\s\"'<>()]+")
ALLOWED_LANGUAGE_VALUES = {"en", "de"}


def fail(path: str, message: str) -> None:
    raise AssertionError(f"{path}: {message}")


def validate(en: Any, de: Any, path: str = "$", key: str | None = None) -> None:
    if key == "language":
        if en != "en" or de != "de":
            fail(path, f"language must be en/de, got {en!r}/{de!r}")
        return

    if type(en) is not type(de):
        fail(path, f"type changed from {type(en).__name__} to {type(de).__name__}")
    if isinstance(en, dict):
        if list(en.keys()) != list(de.keys()):
            fail(path, "object keys or their order changed")
        for child_key in en:
            validate(en[child_key], de[child_key], f"{path}.{child_key}", child_key)
    elif isinstance(en, list):
        if len(en) != len(de):
            fail(path, f"array length changed from {len(en)} to {len(de)}")
        for index, (left, right) in enumerate(zip(en, de)):
            validate(left, right, f"{path}[{index}]", key)
    elif isinstance(en, str):
        if key and ("url" in key.lower() or key.lower() in {"image", "sourceurl", "spoonacularsourceurl"}):
            if en != de:
                fail(path, "URL or image value changed")
        if URL_PATTERN.findall(en) != URL_PATTERN.findall(de):
            fail(path, "embedded URL changed")
    elif en != de:
        fail(path, f"non-text value changed from {en!r} to {de!r}")


def main() -> int:
    root = Path(__file__).resolve().parents[1] / "src" / "main" / "resources" / "recipes"
    for category in ("breakfast", "lunch", "dinner", "snack"):
        en_file = root / "en" / f"{category}.json"
        de_file = root / "de" / f"{category}.json"
        en = json.loads(en_file.read_text(encoding="utf-8"))
        de = json.loads(de_file.read_text(encoding="utf-8"))
        validate(en, de)
        print(f"OK: {category}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
