#!/usr/bin/env python3
"""Best-effort compile check without Minecraft/Fabric jars.

Compiles every source file with javac and reports only errors that are NOT caused by the missing
Minecraft / Fabric / Mojang / JOML / LWJGL / SLF4J classes. It catches mistakes between our own
classes (wrong method names, arity, missing imports of our types, syntax errors). It cannot verify
calls into Minecraft itself.

Usage: python3 tools/check_compile.py [--all]
"""
import os
import re
import subprocess
import sys
import tempfile

EXTERNAL = ("net.minecraft", "net.fabricmc", "com.mojang", "org.joml", "org.lwjgl", "org.slf4j")
root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
srcs = []
for d, _, files in os.walk(os.path.join(root, "src/main/java")):
    srcs += [os.path.join(d, f) for f in files if f.endswith(".java")]

# Collect simple names imported from external packages per file.
# Nested types inherited from Minecraft superclasses (used unqualified in subclasses).
external_names = {"Settings", "RemovalReason", "Control", "Builder", "Type", "Entry", "Context", "Location"}
for f in srcs:
    for line in open(f, encoding="utf-8"):
        m = re.match(r"import (?:static )?([\w.]+)\.(\w+);", line.strip())
        if m and m.group(1).startswith(EXTERNAL):
            external_names.add(m.group(2))

# Our classes that extend Minecraft types inherit unknown members: treat them as external for lookups.
changed = True
while changed:
    changed = False
    for f in srcs:
        src = open(f, encoding="utf-8").read()
        for m in re.finditer(r"class (\w+)(?:<[^{]*?>)?\s+extends\s+([\w.]+)", src):
            if m.group(2).split(".")[-1] in external_names and m.group(1) not in external_names:
                external_names.add(m.group(1))
                changed = True

out = tempfile.mkdtemp()
env = dict(os.environ, JAVA_TOOL_OPTIONS="")
proc = subprocess.run(["javac", "--release", "17", "-proc:none", "-Xmaxerrs", "100000", "-d", out, "-encoding", "UTF-8"] + srcs,
                      capture_output=True, text=True, env=env)
text = proc.stderr
blocks = re.split(r"\n(?=/)", text)
kept = []
for b in blocks:
    if ": error:" not in b:
        continue
    first = b.splitlines()[0]
    msg = first.split(": error:", 1)[1].strip()
    if msg.startswith("package ") and any(p in msg for p in EXTERNAL):
        continue
    pm = re.match(r"package ([\w.]+) does not exist", msg)
    if pm and pm.group(1).split(".")[0] in external_names:
        continue
    if msg.startswith("cannot find symbol") or msg.startswith("cannot access"):
        sym = re.search(r"symbol:\s+(?:class|variable|method|interface|package)\s+(\w+)", b)
        loc = re.search(r"location:\s+(?:class|interface|variable \w+ of type|package)\s+([\w.<>?,\s]+)", b)
        if sym and sym.group(1) in external_names:
            continue
        if loc and any(tok in external_names for tok in re.findall(r"\w+", loc.group(1))):
            continue
        if sym and loc is None:
            continue
        if loc and loc.group(1).strip().startswith(EXTERNAL):
            continue
    # Errors cascading from unknown external types.
    if any(k in msg for k in ("does not override or implement", "method does not override",
                               "incompatible types", "cannot be applied", "is not abstract",
                               "no suitable", "cannot infer", "bad operand", "not a statement",
                               "cannot be dereferenced", "has private access", "unreported exception",
                               "invalid method reference", "is not a functional interface",
                               "incomparable types", "cannot return a value", "may not have been initialized",
                               "constant expression required", "an enum switch case label",
                               "not applicable", "already defined", "unreachable")) and "--all" not in sys.argv:
        # Keep only errors that don't involve any Minecraft-derived type.
        detail = "\n".join(b.splitlines()[1:]) + " " + msg
        if any(tok in external_names for tok in re.findall(r"\w+", detail)):
            continue
    if msg.startswith("method does not override") or "does not override or implement" in msg:
        continue
    kept.append(b.strip())

for leftover in os.listdir(root):
    if leftover.startswith("javac.") and leftover.endswith(".args"):
        os.remove(os.path.join(root, leftover))
for k in kept:
    print(k)
    print()
print(f"{len(kept)} relevant error(s) (javac reported {text.count(': error:')} total, mostly missing Minecraft types).")
sys.exit(1 if kept else 0)
