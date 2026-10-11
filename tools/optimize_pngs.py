#!/usr/bin/env python3
"""Lossless PNG optimiser for the mod's resources (final pass of tools/gen_assets.py).

For every PNG under the given roots it tries several encodings of the *same pixels* and keeps the smallest:

* RGBA (Pillow, ``optimize=True``),
* RGB when every pixel is opaque,
* L / LA when the image is grey,
* an exact palette (``P`` + ``tRNS``) at the smallest bit depth when the image has <= 256 distinct RGBA colours,
* and, when the optional ``pyoxipng`` package is installed, oxipng on the best of the above.

A candidate is accepted only if it decodes to exactly the same RGBA bytes as the original (checked on every file),
so the pass never changes what Minecraft sees. Ancillary chunks (text, time, gamma, ICC...) are dropped. Results are
cached by content hash under ``build/png_cache`` so re-running on unchanged files is quick.

Usage: python3 tools/optimize_pngs.py [ROOT ...]   (default: src/main/resources)
"""
import hashlib
import io
import os
import sys

import numpy as np
from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CACHE = os.path.join(ROOT, "build", "png_cache")

try:  # optional: noticeably better deflate (~10% on the painted model textures)
    import oxipng  # type: ignore
except ImportError:  # pragma: no cover - optional dependency
    oxipng = None


def rgba_bytes(data):
    with Image.open(io.BytesIO(data)) as im:
        im.load()
        return im.size, im.convert("RGBA").tobytes()


def _encode(img, **kw):
    out = io.BytesIO()
    img.save(out, "PNG", optimize=True, **kw)
    return out.getvalue()


def candidates(size, rgba):
    w, h = size
    arr = np.frombuffer(rgba, dtype=np.uint8).reshape(h, w, 4)
    img = Image.frombytes("RGBA", size, rgba)
    yield _encode(img)
    opaque = bool((arr[..., 3] == 255).all())
    grey = bool(((arr[..., 0] == arr[..., 1]) & (arr[..., 1] == arr[..., 2])).all())
    if opaque:
        yield _encode(img.convert("RGB"))
        if grey:
            yield _encode(Image.fromarray(np.ascontiguousarray(arr[..., 0]), "L"))
    elif grey:
        yield _encode(Image.fromarray(np.ascontiguousarray(arr[..., [0, 3]]), "LA"))
    packed = arr.reshape(-1, 4).copy().view(np.uint32).ravel()
    colours, inverse = np.unique(packed, return_inverse=True)
    if len(colours) <= 256:
        pal = colours.view(np.uint8).reshape(-1, 4)
        # opaque-last ordering keeps the tRNS chunk short
        order = np.argsort(pal[:, 3] == 255, kind="stable")
        remap = np.empty_like(order)
        remap[order] = np.arange(len(order))
        pal = pal[order]
        p = Image.fromarray(remap[inverse].astype(np.uint8).reshape(h, w), "P")
        p.putpalette(pal[:, :3].ravel().tolist())
        n = len(pal)
        bits = 1 if n <= 2 else 2 if n <= 4 else 4 if n <= 16 else 8
        kw = {"bits": bits}
        trns = bytes(int(a) for a in pal[:, 3] if a != 255)
        if trns:
            kw["transparency"] = trns
        yield _encode(p, **kw)


def optimise_bytes(data):
    """Smallest lossless encoding of a PNG (the input itself if nothing beats it)."""
    size, ref = rgba_bytes(data)
    best = data
    for cand in candidates(size, ref):
        if len(cand) < len(best) and rgba_bytes(cand) == (size, ref):
            best = cand
    if oxipng is not None:
        try:
            cand = oxipng.optimize_from_memory(best, level=3, strip=oxipng.StripChunks.safe())
        except Exception:  # noqa: BLE001 - optional optimiser; fall back silently
            cand = best
        if len(cand) < len(best) and rgba_bytes(cand) == (size, ref):
            best = cand
    return best


def _cache_path(digest):
    return os.path.join(CACHE, digest[:2], digest + ".png")


def optimise_file(path):
    """Returns (bytes before, bytes after)."""
    with open(path, "rb") as f:
        data = f.read()
    digest = hashlib.sha1(data).hexdigest()
    cached = _cache_path(digest)
    best = None
    if os.path.exists(cached):
        with open(cached, "rb") as f:
            best = f.read()
        if rgba_bytes(best) != rgba_bytes(data):  # stale/corrupt cache entry
            best = None
    if best is None:
        best = optimise_bytes(data)
        os.makedirs(os.path.dirname(cached), exist_ok=True)
        tmp = cached + f".{os.getpid()}.tmp"
        with open(tmp, "wb") as f:
            f.write(best)
        os.replace(tmp, cached)
    if best != data:
        with open(path, "wb") as f:
            f.write(best)
    return len(data), len(best)


def find_pngs(roots):
    out = []
    for r in roots:
        for d, _, files in os.walk(r):
            out.extend(os.path.join(d, n) for n in files if n.lower().endswith(".png"))
    return sorted(out)


def optimise_tree(roots=None, quiet=False):
    roots = roots or [os.path.join(ROOT, "src", "main", "resources")]
    files = find_pngs(roots)
    try:
        import multiprocessing
        with multiprocessing.Pool() as pool:
            results = pool.map(optimise_file, files, chunksize=16)
    except (ImportError, OSError):
        results = [optimise_file(p) for p in files]
    before = sum(r[0] for r in results)
    after = sum(r[1] for r in results)
    if not quiet:
        print(f"  png optimisation     {len(files)} files, {before / 1e6:.2f} MB -> {after / 1e6:.2f} MB"
              f" ({'oxipng' if oxipng else 'pillow only'})")
    return len(files), before, after


if __name__ == "__main__":
    optimise_tree(sys.argv[1:] or None)
