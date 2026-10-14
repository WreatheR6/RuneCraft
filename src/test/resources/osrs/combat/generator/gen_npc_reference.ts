// Generates src/test/resources/osrs/combat/npc_reference.tsv from weirdgloop/osrs-dps-calc's NPCVsPlayerCalc (round 15).
// Same layout and build as gen_dps_reference.ts: node gen/build.mjs gen_npc.ts gen_npc.js && node gen/gen_npc.js npc_reference.tsv.
import NPCVsPlayerCalc from '@/lib/NPCVsPlayerCalc';
import { getMonsters, INITIAL_MONSTER_INPUTS } from '@/lib/Monsters';
import { calculateEquipmentBonusesFromGear } from '@/lib/Equipment';
import { getCombatStylesForCategory } from '@/utils';
import { EquipmentCategory } from '@/enums/EquipmentCategory';
import eq from '../src/osrs-dps-calc/cdn/json/equipment.json';
import * as fs from 'fs';

const equipment: any[] = eq as any[];
const PREF = ['', 'Charged', 'Active', 'Restored', 'Undamaged', 'Unpoisoned', 'Normal', 'Lit', 'New'];
function item(name: string): any {
  const all = equipment.filter((e) => e.name.toLowerCase() === name.toLowerCase());
  if (!all.length) throw new Error('no item ' + name);
  for (const v of PREF) {
    const f = all.find((e) => e.version === v);
    if (f) return f;
  }
  return all[0];
}
const monsters: any[] = getMonsters();
function monster(name: string, version: string): any {
  const m = monsters.find((x) => x.name === name && x.version === version);
  if (!m) throw new Error('no monster ' + name + '|' + version);
  const c = JSON.parse(JSON.stringify(m));
  c.inputs = { ...INITIAL_MONSTER_INPUTS };
  c.inputs.monsterCurrentHp = c.skills.hp;
  return c;
}

// [name, version]: single-style monsters of every attack style.
const MONSTERS: [string, string][] = [
  ['Abyssal demon', 'Catacombs of Kourend'], ['Ankou', 'Level 75'], ['Black demon', 'Level 172'], ['Fire giant', 'Level 104'],
  ['Hill Giant', ''], ['Ice giant', '1'], ['Imp', 'GWD'], ['Moss giant', 'Level 42'], ['Ogre', 'GWD'], ['Sergeant Strongstack', ''],
  ['Phantom Muspah', 'Melee'], ['Scorpia', ''], ['Vardorvis', 'Post-quest'], ['Wyrd', ''], ['Avatar of Creation', ''],
  ['Aviansie', 'Level 131'], ['Zulrah', 'Serpentine'], ['Pestilent Bloat', 'Normal'], ['Javelin Colossus', ''],
  ['Metzli, Teokan of Ranul', ''], ['Dagannoth Prime', ''], ['Dark wizard', 'Level 11'], ['Infernal Mage', ''],
  ['Wingman Skree', ''], ['Verzik Vitur', 'Normal mode, Phase 1'], ['The Maiden of Sugadinti', '50% Health'],
];

type Gear = { id: string; gear: string[]; style: string };
const GEARS: Gear[] = [
  { id: 'naked', gear: ['Abyssal whip'], style: 'Flick' },
  { id: 'rune_def', gear: ['Abyssal whip', 'Rune full helm', 'Rune platebody', 'Rune platelegs', 'Rune kiteshield'], style: 'Deflect' },
  { id: 'torva_lash', gear: ['Abyssal whip', 'Torva full helm', 'Torva platebody', 'Torva platelegs', 'Amulet of torture', 'Ferocious gloves', 'Primordial boots', 'Infernal cape', 'Ultor ring', 'Avernic defender'], style: 'Lash' },
  { id: 'justiciar', gear: ['Ghrazi rapier', 'Justiciar faceguard', 'Justiciar chestguard', 'Justiciar legguards', 'Avernic defender'], style: 'Block' },
  { id: 'bandos_tank', gear: ['Dragon scimitar', 'Neitiznot faceguard', 'Bandos chestplate', 'Bandos tassets', 'Dragon defender', 'Barrows gloves', 'Dragon boots'], style: 'Block' },
  { id: 'armadyl_rapid', gear: ['Twisted bow', 'Dragon arrow', 'Armadyl helmet', 'Armadyl chestplate', 'Armadyl chainskirt', 'Necklace of anguish', 'Zaryte vambraces', 'Pegasian boots', "Ava's assembler"], style: 'Rapid' },
  { id: 'dhide_longrange', gear: ['Magic shortbow', 'Rune arrow', 'Black d\'hide body', 'Black d\'hide chaps', 'Black d\'hide vambraces', 'Snakeskin boots'], style: 'Longrange' },
  { id: 'ancestral', gear: ["Tumeken's shadow", 'Ancestral hat', 'Ancestral robe top', 'Ancestral robe bottom', 'Occult necklace', 'Tormented bracelet', 'Eternal boots', 'Imbued saradomin cape', 'Magus ring'], style: 'Accurate' },
  { id: 'mystic_defensive', gear: ['Trident of the seas', 'Mystic hat', 'Mystic robe top', 'Mystic robe bottom', 'Mystic gloves', 'Mystic boots'], style: 'Longrange' },
  { id: 'torags_half_hp', gear: ["Torag's hammers", "Torag's helm", "Torag's platebody", "Torag's platelegs", 'Amulet of the damned'], style: 'Pound' },
  { id: 'controlled', gear: ['Abyssal tentacle', 'Dragon full helm', 'Dragon platebody', 'Dragon platelegs', 'Dragonfire shield'], style: 'Flick' },
  { id: 'dinhs_block', gear: ["Dinh's bulwark", 'Torva full helm', 'Torva platebody', 'Torva platelegs'], style: 'Block' },
];

type Profile = { id: string; lvl: number; def: number; magic: number; defBoost: number; magicBoost: number; hpPct: number; prayers: string[] };
const PROFILES: Profile[] = [
  { id: 'max_piety', lvl: 99, def: 99, magic: 99, defBoost: 19, magicBoost: 10, hpPct: 100, prayers: ['piety'] },
  { id: 'max_augury', lvl: 99, def: 99, magic: 99, defBoost: 0, magicBoost: 10, hpPct: 100, prayers: ['augury'] },
  { id: 'max_rigour', lvl: 99, def: 99, magic: 99, defBoost: 13, magicBoost: 0, hpPct: 100, prayers: ['rigour'] },
  { id: 'steel_vigour', lvl: 85, def: 85, magic: 85, defBoost: 5, magicBoost: 5, hpPct: 100, prayers: ['steel_skin', 'mystic_vigour'] },
  { id: 'rock_skin', lvl: 60, def: 60, magic: 40, defBoost: 0, magicBoost: 0, hpPct: 100, prayers: ['rock_skin'] },
  { id: 'deadeye_thick', lvl: 70, def: 70, magic: 70, defBoost: 0, magicBoost: 0, hpPct: 100, prayers: ['thick_skin', 'deadeye'] },
  { id: 'none_half_hp', lvl: 80, def: 80, magic: 60, defBoost: 0, magicBoost: 0, hpPct: 37, prayers: [] },
  { id: 'pure', lvl: 75, def: 1, magic: 94, defBoost: 0, magicBoost: 0, hpPct: 100, prayers: ['mystic_might'] },
];

const PRAYER_KEYS: Record<string, string> = {};

function main() {
  // eslint-disable-next-line @typescript-eslint/no-var-requires
  const { Prayer, PrayerMap } = require('@/enums/Prayer');
  for (const [k, v] of Object.entries(PrayerMap as Record<string, any>)) {
    PRAYER_KEYS[v.name.toLowerCase().replace(/ /g, '_')] = k;
  }
  const rows: string[] = [];
  rows.push('# NPC-vs-player reference from weirdgloop/osrs-dps-calc NPCVsPlayerCalc (src/lib), bundled with esbuild and run under node (round 15).');
  rows.push('# Inputs: the monster (style, attack level for that style, attack bonus, strength level/bonus for the style), the player (boosted Defence and Magic,');
  rows.push('# max/current HP, prayers, weapon stance, summed defence bonus against the style). Outputs: NPC attack roll, player defence roll, hit chance, the max hit formula.');
  rows.push(['id', 'monster', 'npcStyle', 'npcLevel', 'npcBonus', 'npcStrLevel', 'npcStrBonus', 'gear', 'loadout', 'stance', 'def', 'magic', 'hpMax', 'hpCur', 'prayers',
    'defBonus', 'npcAttackRoll', 'playerDefenceRoll', 'hitChance', 'npcMaxHit'].join('\t'));
  let n = 0;
  for (const prof of PROFILES) {
    for (let gi = 0; gi < GEARS.length; gi++) {
      const lo = GEARS[gi];
      for (let mi = 0; mi < MONSTERS.length; mi++) {
        if ((mi + gi) % 3 !== 0 && prof.id !== 'max_piety') continue;
        const [mn, mv] = MONSTERS[mi];
        const m = monster(mn, mv);
        if (!m.style || !['stab', 'slash', 'crush', 'ranged', 'magic'].includes(m.style)) throw new Error('style of ' + mn + ': ' + m.style);
        const pieces = lo.gear.map(item);
        const equip: any = { head: null, cape: null, neck: null, ammo: null, weapon: null, body: null, shield: null, legs: null, hands: null, feet: null, ring: null };
        for (const p of pieces) equip[p.slot] = p;
        const styles = getCombatStylesForCategory(equip.weapon.category || EquipmentCategory.NONE);
        const style = styles.find((s: any) => s.name === lo.style);
        if (!style) throw new Error('style ' + lo.style + ' for ' + lo.id + ' in ' + JSON.stringify(styles));
        const hpCur = Math.max(1, Math.trunc(prof.lvl * (lo.id === 'torags_half_hp' ? 50 : prof.hpPct) / 100));
        const player: any = {
          name: 'x', style,
          skills: { atk: prof.lvl, def: prof.def, hp: prof.lvl, magic: prof.magic, prayer: 99, ranged: prof.lvl, str: prof.lvl, mining: 99, herblore: 99 },
          boosts: { atk: 0, def: prof.defBoost, hp: hpCur - prof.lvl, magic: prof.magicBoost, prayer: 0, ranged: 0, str: 0, mining: 0, herblore: 0 },
          equipment: equip, attackSpeed: 4,
          prayers: prof.prayers.map((p: string) => { const k = PRAYER_KEYS[p]; if (k === undefined) throw new Error('prayer ' + p); return Number.isNaN(Number(k)) ? (Prayer as any)[k] : Number(k); }),
          bonuses: { str: 0, ranged_str: 0, magic_str: 0, prayer: 0 },
          defensive: { stab: 0, slash: 0, crush: 0, magic: 0, ranged: 0 },
          offensive: { stab: 0, slash: 0, crush: 0, magic: 0, ranged: 0 },
          buffs: { potions: [], onSlayerTask: true, inWilderness: false, kandarinDiary: true, chargeSpell: false, markOfDarknessSpell: false, forinthrySurge: false, soulreaperStacks: 0, baAttackerLevel: 0, chinchompaDistance: 4, usingSunfireRunes: false },
          spell: null,
        };
        const calcd = calculateEquipmentBonusesFromGear(player, m);
        player.bonuses = calcd.bonuses; player.offensive = calcd.offensive; player.defensive = calcd.defensive;
        const calc: any = new NPCVsPlayerCalc(player, m, { loadoutName: 'x', detailedOutput: false } as any);
        const s = m.style;
        const melee = ['stab', 'slash', 'crush'].includes(s);
        const npcLevel = melee ? m.skills.atk : s === 'ranged' ? m.skills.ranged : m.skills.magic;
        const npcBonus = melee ? m.offensive.atk : s === 'ranged' ? m.offensive.ranged : m.offensive.magic;
        const strLevel = melee ? m.skills.str : s === 'ranged' ? m.skills.ranged : m.skills.magic;
        const strBonus = melee ? m.offensive.str : s === 'ranged' ? m.offensive.ranged_str : m.offensive.magic_str;
        // The formula max hit (magic monsters use the wiki's hardcoded value instead and Vardorvis scales, so they're left out).
        const max = s === 'magic' || mn === 'Vardorvis' ? '-' : calc.getNPCMaxHit(); // Vardorvis's Strength scales with his hitpoints
        // Summed defence bonus of the gear against the monster's style.
        const raw = player.defensive[s];
        n++;
        rows.push([`${prof.id}/${lo.id}/${mn}${mv ? ' (' + mv + ')' : ''}`, `${mn}|${mv}`, s, npcLevel, npcBonus, strLevel, strBonus, lo.gear.join(','), lo.id,
          style.stance, prof.def + prof.defBoost, prof.magic + prof.magicBoost, prof.lvl, hpCur, prof.prayers.join(',') || '-', raw,
          calc.getNPCMaxAttackRoll(), calc.getPlayerDefenceRoll(), calc.getHitChance().toFixed(6), max].join('\t'));
      }
    }
  }
  fs.writeFileSync(process.argv[2], rows.join('\n') + '\n');
  console.log('rows', n);
}

main();
