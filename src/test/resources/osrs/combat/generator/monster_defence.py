import json, re, sys, collections
table, monsters_json, out = sys.argv[1], sys.argv[2], sys.argv[3]
ms = json.load(open(monsters_json))


def key(m):
    s, d = m['skills'], m['defensive']
    return (s['hp'], s['atk'], s['str'], s['def'], s['magic'], s['ranged'], d['stab'], d['slash'], d['crush'], d['magic'], d['standard'])


def val(c):
    w = c.get('weakness') or {}
    return (c['defensive']['light'], c['defensive']['heavy'], w.get('element'), w.get('severity') or 0)


by = collections.defaultdict(list)
for m in ms:
    by[key(m)].append(m)
comment = ''
rows = []
stats = collections.Counter()
for line in open(table):
    t = line.strip()
    if t.startswith('//'):
        comment = t[2:].strip()
        continue
    mm = re.match(r'row\("([a-z0-9_]+)",\s*(.*)\);', t)
    if not mm:
        continue
    rid = mm.group(1)
    nums = [int(x) for x in re.findall(r'-?\d+', mm.group(2).split('"')[0])]
    level, hp, att, st, df, mage, rng, speed, ds, dsl, dc, dm, dr = nums[:13]
    cands = by.get((hp, att, st, df, mage, rng, ds, dsl, dc, dm, dr), [])
    name = re.sub(r'\s*\(.*$', '', comment).strip().lower()
    named = [c for c in cands if c['name'].lower() == name]
    pick = named or cands
    if not pick:
        byname = [c for c in ms if c["name"].lower() == name]
        if byname and len(collections.Counter(val(c) for c in byname)) == 1:
            pick = byname
            stats["byname"] += 1
        else:
            stats["nomatch"] += 1
            print("NOMATCH", rid, comment, file=sys.stderr)
            continue
    exact = [c for c in pick if (c['name'] + (' (' + c['version'] + ')' if c['version'] else '')).lower() == comment.lower()]
    if exact:
        pick = exact
    vals = collections.Counter(val(c) for c in pick)
    if len(vals) != 1:
        stats['ambiguous'] += 1
    light, heavy, el, sev = vals.most_common(1)[0][0]
    stats['matched'] += 1
    if light == dr and heavy == dr and not el:
        continue
    p = pick[0]
    rows.append((rid, light, heavy, el, sev, p['name'] + (' (' + p['version'] + ')' if p['version'] else '')))
print(dict(stats), len(rows), file=sys.stderr)
HEAD = '''package com.runecraft.osrs.data;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * The parts of a monster's defence that {@link MonsterStatsTable} has no column for, from the OSRS Wiki DPS calculator's
 * {@code monsters.json} (weirdgloop/osrs-dps-calc): the light and heavy ranged defence (the table's ranged defence is the
 * "standard" one, used by bows; thrown weapons roll against light, crossbows, ballistae and chinchompas against heavy) and
 * the elemental weakness of the 2024 rework (element and severity in percent: a matching elemental spell gets that much
 * extra accuracy and damage). Rows were matched to the table's rows by name, version and stats (round 15, generator
 * {@code src/test/resources/osrs/combat/generator/monster_defence.py}); a creature that is not listed has light = heavy =
 * standard and no weakness. Applied together with the table's row by {@link MonsterStatsTable#apply}.
 */
public final class MonsterDefenceTable {
    /** Light / heavy ranged defence and elemental weakness (null element: none). */
    public record Row(String id, int lightRanged, int heavyRanged, String weakness, int weaknessPercent) {
    }

    private static final Map<String, Row> ROWS = new HashMap<>();

    static {
        rows();
    }

    private MonsterDefenceTable() {
    }

    /** The row for a creature id, or null. */
    public static Row row(String id) {
        return ROWS.get(id);
    }

    public static Map<String, Row> rows() {
        return Collections.unmodifiableMap(ROWS);
    }

    private static void x(String id, int light, int heavy, String weakness, int percent) {
        if (ROWS.put(id, new Row(id, light, heavy, weakness, percent)) != null) {
            throw new IllegalStateException("Duplicate monster defence row " + id);
        }
    }

    private static void rows() {
'''
MANUAL = [
    ('mountain_troll', 200, 200, 'fire', 50, 'Mountain troll (Level 69)'),
    ('brutal_blue_dragon', 50, 10, 'water', 50, 'Brutal blue dragon (Catacombs of Kourend)'),
    ('nazastarool', 0, 0, 'fire', 40, 'Nazastarool (Zombie)'),
    ('vardorvis', 580, 580, 'fire', 35, 'Vardorvis (Post-quest)'),
]
with open(out, 'w') as f:
    f.write(HEAD)
    for r in rows:
        f.write('        x("%s", %d, %d, %s, %d); // %s\n' % (r[0], r[1], r[2], ('"%s"' % r[3]) if r[3] else 'null', r[4], r[5]))
    f.write('        // Rows whose comment or stats in MonsterStatsTable match no monsters.json entry (matched by hand).\n')
    for r in MANUAL:
        f.write('        x("%s", %d, %d, "%s", %d); // %s\n' % r)
    f.write('    }\n}\n')
