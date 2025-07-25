#!/usr/bin/env -S uv run --script
#
# /// script
# requires-python = ">=3.13"
# dependencies = ["jsonschema>=4.22"]
# ///

from __future__ import annotations

import argparse
import json
import subprocess
import sys
from pathlib import Path

from jsonschema import ValidationError, exceptions, validators

# ---------- Git からリポジトリルートを取得 ----------
def git_root() -> Path:
    """`git rev-parse --show-toplevel` でリポジトリルートを返す"""
    try:
        out = subprocess.run(
            ["git", "rev-parse", "--show-toplevel"],
            check=True,
            capture_output=True,
            text=True,
        )
        return Path(out.stdout.strip()).resolve()
    except (subprocess.CalledProcessError, FileNotFoundError) as err:
        sys.exit(f"❌  Failed to determine git root: {err}")

ROOT = git_root()
# ------------------------------------------------------

DIST_DIR = ROOT / "jsonschema" / "dist"
DEFAULT_TARGET = DIST_DIR / "central-dogma-decaton-properties-example.json"
SCHEMA_GLOB = "decaton-processor-properties-schema-*.json"


def compile_validator(schema_path: Path) -> validators.Validator:
    schema = json.loads(schema_path.read_text(encoding="utf-8"))
    ValidatorCls = validators.validator_for(schema)
    ValidatorCls.check_schema(schema)  # raise if schema is invalid
    return ValidatorCls(schema)


def validate_against_all(target_file: Path) -> bool:
    data = json.loads(target_file.read_text(encoding="utf-8"))
    ok = True
    for schema_path in sorted(DIST_DIR.glob(SCHEMA_GLOB)):
        try:
            compile_validator(schema_path).validate(data)
            print(f"✅  {schema_path.name} ... ok")
        except (exceptions.SchemaError, ValidationError) as err:
            print(f"❌  {schema_path.name}\n   {err}\n", file=sys.stderr)
            ok = False
    return ok


def parse_args() -> Path:
    p = argparse.ArgumentParser(
        description="Validate Decaton property JSON against all bundled schemas."
    )
    p.add_argument(
        "target",
        nargs="?",
        type=Path,
        default=DEFAULT_TARGET,
        help=f"JSON file to validate (default: {DEFAULT_TARGET.relative_to(ROOT)})",
    )
    args = p.parse_args()
    if not args.target.exists():
        p.error(f"Target JSON not found: {args.target}")
    return args.target.resolve()


def main() -> None:
    sys.exit(0 if validate_against_all(parse_args()) else 1)


if __name__ == "__main__":
    main()
