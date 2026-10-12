#!/usr/bin/env python3
"""Flags fully qualified references to package-private top-level classes from another package.

tools/check_compile.py can't see these: javac reports them only after resolving Minecraft types,
which aren't available offline.
"""
import os
import re
import sys

root = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "java")
private = {}
sources = {}
for d, _, files in os.walk(root):
    for f in files:
        if not f.endswith(".java"):
            continue
        path = os.path.join(d, f)
        text = open(path, encoding="utf-8").read()
        pkg = re.search(r"^package ([\w.]+);", text, re.M)
        pkg = pkg.group(1) if pkg else ""
        sources[path] = (pkg, text)
        name = f[:-5]
        if re.search(r"^(?:final |abstract )*(?:class|interface|enum|record) " + name + r"\b", text, re.M):
            private[pkg + "." + name] = pkg

errors = 0
for path, (pkg, text) in sources.items():
    for fq, owner in private.items():
        if owner == pkg:
            continue
        cls = fq.rsplit(".", 1)[1]
        if re.search(re.escape(fq) + r"\b", text) or re.search(r"^import " + re.escape(fq) + ";", text, re.M):
            print(f"{os.path.relpath(path, root)}: {fq} is not public")
            errors += 1
print(f"{errors} visibility error(s)")
sys.exit(1 if errors else 0)
