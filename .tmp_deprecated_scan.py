import re
import pathlib
root = pathlib.Path('src/main/java')
files = list(root.rglob('*.java'))
defs = []
for f in files:
    lines = f.read_text(encoding='utf-8', errors='ignore').splitlines()
    for i, line in enumerate(lines):
        if '@Deprecated' not in line:
            continue
        for j in range(i + 1, min(i + 10, len(lines))):
            s = lines[j].strip()
            cm = re.search(r'\bclass\s+(\w+)', s)
            if cm:
                defs.append((cm.group(1), f))
                break
            mm = re.search(r'\b(?:public|protected|private)\s+[\w<>,\[\] ?]+\s+(\w+)\s*\(', s)
            if mm:
                defs.append((mm.group(1), f))
                break
seen = set()
unique_defs = []
for sym, f in defs:
    key = (sym, str(f))
    if key in seen:
        continue
    seen.add(key)
    unique_defs.append((sym, f))
for sym, f in sorted(unique_defs):
    pattern = re.compile(r'\b' + re.escape(sym) + r'\s*\(')
    outside = []
    for ff in files:
        lines = ff.read_text(encoding='utf-8', errors='ignore').splitlines()
        for n, line in enumerate(lines, 1):
            if pattern.search(line) and ff != f:
                outside.append((ff, n, line.strip()))
    if outside:
        print(f"{sym} -> {len(outside)}")
        for ff, n, line in outside[:3]:
            print(f"  {ff}:{n}: {line}")
