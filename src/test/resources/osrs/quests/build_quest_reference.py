"""Builds the quest reference snapshot from the OSRS wiki dump (Kiyogitpy/osrs-wiki-data) and quest-helper.

usage: python3 -I build_quest_reference.py <wiki raw dir> <quest-helper checkout> <mod quest dump.tsv> <out.tsv>
(mod quest dump: one line per ContentRegistry quest, "questId<TAB>displayName", used only to map names to ids)
"""
import os
import re
import sys

RAW, QH, MOD, OUT = sys.argv[1:5]

SKILLS = ["Attack", "Hitpoints", "Mining", "Strength", "Agility", "Smithing", "Defence", "Herblore", "Fishing",
          "Ranged", "Thieving", "Cooking", "Prayer", "Crafting", "Firemaking", "Magic", "Fletching", "Woodcutting",
          "Runecraft", "Slayer", "Farming", "Construction", "Hunter", "Sailing"]


def read(title):
    first = title[0].upper() if title[0].isalpha() else title[0]
    p = os.path.join(RAW, first, title.replace("/", "_") + ".txt")
    if not os.path.exists(p):
        return None
    with open(p, encoding="utf-8") as f:
        return f.read()


def field(text, template, name):
    i = text.find("{{" + template)
    if i < 0:
        return None
    params, cur, depth, j = {}, [], 0, i + 2
    parts = []
    while j < len(text):
        two = text[j:j + 2]
        if two in ("{{", "[["):
            depth += 1
            cur.append(two)
            j += 2
            continue
        if two in ("}}", "]]"):
            if depth == 0 and two == "}}":
                break
            depth -= 1
            cur.append(two)
            j += 2
            continue
        if text[j] == "|" and depth == 0:
            parts.append("".join(cur))
            cur = []
        else:
            cur.append(text[j])
        j += 1
    parts.append("".join(cur))
    for part in parts[1:]:
        if "=" in part:
            k, v = part.split("=", 1)
            params[k.strip()] = v.strip()
    return params.get(name)


def norm(name):
    name = name.replace("&", "and").replace("(miniquest)", "")
    return re.sub(r"[^a-z0-9]", "", name.lower())


# ------------------------------------------------------------ quest list
pages = []
for d in os.listdir(RAW):
    if not os.path.isdir(os.path.join(RAW, d)):
        continue
    for fn in os.listdir(os.path.join(RAW, d)):
        p = os.path.join(RAW, d, fn)
        try:
            t = open(p, encoding="utf-8").read()
        except Exception:
            continue
        if "{{Infobox Quest" not in t and "{{Infobox Miniquest" not in t:
            continue
        title = fn[:-4]
        if "_" in title or "Full guide" in title or "Quick guide" in title:
            continue
        if "alpha content" in t or "beta content" in t:
            continue
        pages.append((title, t))

misc = read("Miniquests") or ""

# ------------------------------------------------------------ skill requirements table
req_table = {}
t = read("Quests_Requirements by quest")
for row in t.split("\n|-"):
    m = re.match(r"\s*\n?\|\s*\[\[([^]|]+)", row)
    if not m:
        continue
    reqs = {}
    qp = 0
    combat = 0
    for s, lv in re.findall(r"\{\{SCP\|([A-Za-z]+)\|(\d+)", row):
        if s == "Quest":
            qp = int(lv)
        elif s == "Combat":
            combat = int(lv)
        elif s in SKILLS:
            reqs[s] = max(reqs.get(s, 0), int(lv))
    req_table[m.group(1).strip()] = (reqs, qp, combat)

# ------------------------------------------------------------ set XP table
xp_table = {}
t = read("Quest experience rewards")
set_part = t.split("==Skill choice==")[0]
for sec in re.split(r"\n===\{\{plink\|", set_part)[1:]:
    skill = sec.split("|")[0]
    for row in sec.split("|- data-rowid=")[1:]:
        qm = re.search(r"\n\|\[\[([^]|]+)", row)
        xm = re.search(r"\{\{\+=\|[a-z]+\|([\d,\.]+)", row)
        if qm and xm:
            q = qm.group(1).strip()
            xp_table.setdefault(q, {})[skill] = xp_table.get(q, {}).get(skill, 0) + float(xm.group(1).replace(",", ""))

# ------------------------------------------------------------ quest-helper item rewards
qh_items, qh_qp, qh_xp = {}, {}, {}
qh_dir = os.path.join(QH, "src/main/java/com/questhelper/helpers")
for root, _, files in os.walk(qh_dir):
    for fn in files:
        if not fn.endswith(".java"):
            continue
        src = open(os.path.join(root, fn), encoding="utf-8").read()
        key = norm(fn[:-5])
        m = re.search(r"new QuestPointReward\((\d+)\)", src)
        if m:
            qh_qp[key] = int(m.group(1))
        xs = re.findall(r"new ExperienceReward\(Skill\.([A-Z]+),\s*([\d_\.]+)", src)
        if xs:
            qh_xp[key] = {k.capitalize(): float(v.replace("_", "")) for k, v in xs}
        if "getItemRewards" not in src:
            continue
        m = re.search(r"getItemRewards\(\)\s*\{(.*?)\n\t\}", src, re.S)
        if not m:
            continue
        items = re.findall(r'new ItemReward\("([^"]+)",\s*[A-Za-z_.0-9]+,\s*(\d+)\)', m.group(1))
        qh_items[norm(fn[:-5])] = items


def links(s):
    out = [l.split("|")[0].split("#")[0].strip() for l in re.findall(r"\[\[([^]]+)\]\]", s)]
    # Recipe for Disaster subquests count as (partial) Recipe for Disaster.
    return [("Recipe for Disaster" if l.startswith("Recipe for Disaster/") else l) for l in out]


COMPLETION = re.compile(r"[Cc]ompletion|[Cc]ompleted|following quests|[Cc]omplete ")
PARTIAL = re.compile(r"Recipe for Disaster/|[Ss]tart|[Pp]artial|[Pp]art of|up to|until|[Pp]rogress")


def plain(line):
    return re.sub(r"\{\{(Questreqstart|Boostable)[^}]*\}\}|<ref.*?(</ref>|/>)|<!--.*?-->", "", line)


def parse_reqs(reqtext, quest_titles):
    """Direct quest prerequisites (top level of the completion tree) and the quest's own skill requirements."""
    out, started, skills = [], [], {}
    minqp = combat = 0
    skip_below = None
    for line in reqtext.split("\n"):
        m = re.match(r"\s*(\*+)", line)
        d = len(m.group(1)) if m else 1
        if skip_below is not None:
            if m and d > skip_below:
                if d == skip_below + 1:
                    for l in links(line):
                        if l in quest_titles:
                            (started if PARTIAL.search(plain(line)) else out).append(l)
                continue
            skip_below = None
        if COMPLETION.search(line) or any(l in quest_titles for l in links(line)):
            for l in links(line):
                if l in quest_titles and "\'\'\'or\'\'\'" not in line:
                    (started if PARTIAL.search(plain(line)) else out).append(l)
            skip_below = d
            if not re.search(r"\{\{SCP\|(?!Quest\}\})", line):
                continue
        if re.search(r" OR |[Ee]ither|any one of|one of the following", plain(line)):
            continue
        for s_, lv in re.findall(r"\{\{SCP\|([A-Za-z]+)\|(\d+)", plain(line)):
            if s_ == "Quest":
                minqp = int(lv)
            elif s_ == "Combat":
                combat = int(lv)
            elif s_ in SKILLS:
                skills[s_] = max(skills.get(s_, 0), int(lv))
    return out, started, skills, minqp, combat


titles = {t for t, _ in pages}
title_norm = {norm(t): t for t in titles}
rows = []
for title, text in sorted(pages):
    members = (field(text, "Infobox Miniquest" if "{{Infobox Miniquest" in text else "Infobox Quest", "members") or "").lower().startswith("yes")
    diff = (field(text, "Quest details", "difficulty") or "").split("<")[0].strip()
    qp_s = field(text, "Quest rewards", "qp")
    mini = "{{Infobox Miniquest" in text
    note = []
    future = "{{Future Content" in text or "[[Category:Future content" in text
    if future:
        note.append("wiki page predates release")
    if qp_s and re.sub(r"[^\d]", "", qp_s):
        qp = int(re.sub(r"[^\d]", "", qp_s))
    elif norm(title) in qh_qp:
        qp = qh_qp[norm(title)]
        note.append("qp from quest-helper")
    else:
        qp = 0
    reqtext = field(text, "Quest details", "requirements") or ""
    pre, started, skills, minqp, combat = parse_reqs(reqtext, titles)
    pre = [x for x in dict.fromkeys(pre) if x != title]
    started = [x for x in dict.fromkeys(started) if x != title and x not in pre]
    if not reqtext.strip() and title in req_table:
        skills, minqp, combat = req_table[title]
        note.append("skills from requirements table")
    rewards = field(text, "Quest rewards", "rewards")
    if rewards is None:
        m = re.search(r"\n==\s*Rewards?\s*==\n(.*?)(?=\n==[^=])", text, re.S)
        rewards = m.group(1) if m else ""
    choice = bool(re.search(r"lamp|choice|choose|tome|book of|\bany\b|antique|either", rewards, re.I))
    xp = xp_table.get(title)
    if xp is None:
        page_xp = {}
        for line in rewards.split("\n"):
            if line.startswith("**") or re.search(r"lamp|choice|choose|\bany\b|additional|during the quest", line, re.I):
                continue
            for s_, v in re.findall(r"\{\{SCP\|([A-Za-z]+)\|([\d,\.]+)", line):
                if s_ in SKILLS and "xperience" in line:
                    page_xp[s_] = float(v.replace(",", ""))
        if page_xp:
            xp = page_xp
            note.append("xp from quest page")
    if xp is None and norm(title) in qh_xp:
        xp = qh_xp[norm(title)]
        note.append("xp from quest-helper")
    xp = xp or {}
    q2 = qh_xp.get(norm(title))
    if q2 is not None and q2 != xp:
        print("XP disagree", title, "wiki", xp, "qh", q2)
    items = qh_items.get(norm(title), [])
    rows.append(dict(title=title, members=members, diff=diff, qp=qp, skills=skills, minqp=minqp, combat=combat,
                     pre=pre, started=started, note=note, future=future, xp=xp, choice=choice, items=items, mini=mini))

# ------------------------------------------------------------ mod id mapping
mod = {}
for line in open(MOD, encoding="utf-8"):
    c = line.rstrip("\n").split("\t")
    mod[norm(c[1])] = c[0]
ALIAS = {"recipefordisaster": "recipe_for_disaster"}
NOTES = {"Recipe for Disaster": "staged: subquest gates and XP are triggers; skills/quests cumulative over subquests",
         "Learning the Ropes": "Tutorial Island quest (22 October 2025)"}
for r in rows:
    if r["title"] in NOTES:
        r["note"].append(NOTES[r["title"]])
for r in rows:
    n = norm(r["title"])
    r["id"] = mod.get(n) or mod.get(n.replace("the", "", 1)) or mod.get("the" + n) or ALIAS.get(n) or ""

rows = [r for r in rows if r["id"] or not r["future"]]
ids = {r["title"]: r["id"] for r in rows}


def idlist(ts):
    return ",".join(ids.get(t) or "?" + t for t in ts)


with open(OUT, "w", encoding="utf-8") as f:
    f.write("# OSRS quest reference snapshot (October 2026), generated by build_quest_reference.py (next to this file) from\n")
    f.write("# the OSRS Wiki dump (Kiyogitpy/osrs-wiki-data: quest pages,\n")
    f.write("# Quests/Requirements by quest, Quest experience rewards) and Zoinkwiz/quest-helper (item rewards).\n")
    f.write("# Columns: id (mod quest id, '-' if missing from the mod), name, type (F2P/P2P/MINI), difficulty, qp,\n")
    f.write("# skills (Skill:level;...), min_qp, combat, quests (direct prerequisites, mod ids), started (partial prereqs),\n")
    f.write("# xp (Skill:amount;... fixed XP only), xp_choice (lamp/choice XP exists), items (quest-helper item rewards), note.\n")
    f.write("id\tname\ttype\tdifficulty\tqp\tskills\tmin_qp\tcombat\tquests\tstarted\txp\txp_choice\titems\tnote\n")
    for r in rows:
        typ = "MINI" if r["mini"] else ("P2P" if r["members"] else "F2P")
        sk = ";".join(f"{s.upper()}:{v}" for s, v in sorted(r["skills"].items()))
        xp = ";".join(f"{s.upper()}:{v:g}" for s, v in sorted(r["xp"].items()))
        it = ";".join(f"{n}:{c}" for n, c in r["items"]).replace("\t", " ")
        f.write("\t".join([r["id"] or "-", r["title"], typ, r["diff"] or "-", str(r["qp"]), sk or "-", str(r["minqp"]),
                           str(r["combat"]), idlist(r["pre"]) or "-", idlist(r["started"]) or "-", xp or "-",
                           "yes" if r["choice"] else "no", it or "-", "; ".join(r["note"]) or "-"]) + "\n")
print(len(rows), "quests;", sum(1 for r in rows if not r["id"]), "unmapped;",
      "qp total", sum(r["qp"] for r in rows))
