"""Writes src/test/resources/osrs/combat/monster_enum_defence.tsv (round 17): per-style defence bonuses, the three ranged
defences, Defence / Magic levels and elemental weakness of the built-in Monster enum creatures, from
weirdgloop/osrs-dps-calc cdn/json/monsters.json. Usage: python3 -I monster_enum_defence.py <monsters.json> <out.tsv>.
Sheep and Pig have no calculator entry (non-combat animals in OSRS) and are left out."""
import json
import sys

ROWS = [  # Monster constant, wiki name, version
    ("MAN", "Man", "1"), ("GOBLIN", "Goblin", "Level 2"), ("AL_KHARID_WARRIOR", "Al Kharid warrior", ""),
    ("BARBARIAN", "Barbarian", "Level 10 (Acelin)"), ("DARK_WIZARD", "Dark wizard", "Level 7"), ("GUARD", "Guard", "Edgeville 1"),
    ("HILL_GIANT", "Hill Giant", ""), ("MOSS_GIANT", "Moss giant", "Level 42"), ("GHOUL", "Ghoul", ""), ("ICE_GIANT", "Ice giant", "1"),
    ("LESSER_DEMON", "Lesser demon", "Level 82"), ("GREATER_DEMON", "Greater demon", "Level 92"), ("BLACK_DEMON", "Black demon", "Level 172"),
    ("CHICKEN", "Chicken", "Falador Farm"), ("COW", "Cow", "1"), ("GIANT_RAT", "Giant rat", "Level 3"), ("SPIDER", "Spider", "Common"),
    ("GIANT_SPIDER", "Giant spider", "Level 27"), ("ZOMBIE", "Zombie", "Level 13, 1"), ("SKELETON", "Skeleton", "Level 22, 1"),
    ("WOLF", "Wolf", "Level 25"), ("VILLAGER", "Man", "1"), ("WITCH", "Dark wizard", "Level 20"),
]


def main():
    mons = json.load(open(sys.argv[1]))
    out = ["# Monster enum defences from weirdgloop/osrs-dps-calc monsters.json (round 17, generator combat/generator/monster_enum_defence.py).",
           "\t".join(["monster", "wiki", "def", "magic", "stab", "slash", "crush", "magicDef", "light", "standard", "heavy", "weakness"])]
    for const, name, version in ROWS:
        m = next(x for x in mons if x["name"] == name and x["version"] == version)
        d = m["defensive"]
        w = m.get("weakness")
        out.append("\t".join(str(v) for v in [const, name + "|" + version, m["skills"]["def"], m["skills"]["magic"], d["stab"], d["slash"],
                                              d["crush"], d["magic"], d["light"], d["standard"], d["heavy"],
                                              "%s:%d" % (w["element"], w["severity"]) if w else "-"]))
    open(sys.argv[2], "w").write("\n".join(out) + "\n")


main()
