// Generates src/test/resources/osrs/combat/dps_reference.tsv from weirdgloop/osrs-dps-calc's PlayerVsNPCCalc.
// Layout (outside the repo): <dir>/src/osrs-dps-calc (git clone), <dir>/deps (npm i esbuild@0.20.2 d3-array@3.2.4
// lodash.mergewith@4.6.2 lodash.isequal@4.5.0), <dir>/gen/{build.mjs,gen.ts}. Then: node gen/build.mjs gen.ts gen.js &&
// node gen/gen.js dps_reference.tsv. build.mjs bundles the calculator, stubbing React/Next/image imports.
import PlayerVsNPCCalc from '@/lib/PlayerVsNPCCalc';
import { getMonsters, INITIAL_MONSTER_INPUTS } from '@/lib/Monsters';
import { calculateEquipmentBonusesFromGear, calculateAttackSpeed } from '@/lib/Equipment';
import { getCombatStylesForCategory } from '@/utils';
import { spells } from '@/types/Spell';
import { getRangedDamageType } from '@/types/PlayerCombatStyle';
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
  c.monsterCurrentHp = c.skills.hp;
  return c;
}

type Loadout = { id: string; gear: string[]; style: string | number; spell?: string; tags?: string };

const MELEE = ['Torva full helm', 'Torva platebody', 'Torva platelegs', 'Amulet of torture', 'Ferocious gloves', 'Primordial boots', 'Infernal cape', 'Ultor ring'];
const MELEE_DEF = [...MELEE, 'Avernic defender'];
const noHead = (g: string[]) => g.filter((x) => x !== 'Torva full helm');
const noNeck = (g: string[]) => g.filter((x) => x !== 'Amulet of torture');
const VOID = ['Void knight top', 'Void knight robe', 'Void knight gloves'];
const EVOID = ['Elite void top', 'Elite void robe', 'Void knight gloves'];
const RANGED = ['Armadyl helmet', 'Armadyl chestplate', 'Armadyl chainskirt', 'Necklace of anguish', 'Zaryte vambraces', 'Pegasian boots', "Ava's accumulator", 'Venator ring'];
const MAGE = ['Ancestral hat', 'Ancestral robe top', 'Ancestral robe bottom', 'Occult necklace', 'Tormented bracelet', 'Eternal boots', 'Imbued saradomin cape', 'Magus ring'];
const noHeadR = (g: string[]) => g.filter((x) => !x.endsWith('helmet') && x !== 'Ancestral hat');
const noNeckR = (g: string[]) => g.filter((x) => x !== 'Necklace of anguish' && x !== 'Occult necklace');
const noBodyLegs = (g: string[]) => g.filter((x) => !/chestplate|chainskirt|robe top|robe bottom/.test(x));

const loadouts: Loadout[] = [
  // ---- melee
  { id: 'whip_flick', gear: ['Abyssal whip', ...MELEE_DEF], style: 'Flick' },
  { id: 'whip_lash', gear: ['Abyssal whip', ...MELEE_DEF], style: 'Lash' },
  { id: 'whip_deflect', gear: ['Abyssal whip', ...MELEE_DEF], style: 'Deflect' },
  { id: 'rapier_lunge', gear: ['Ghrazi rapier', ...MELEE_DEF], style: 'Lunge' },
  { id: 'scythe_chop', gear: ['Scythe of vitur', ...MELEE], style: 'Chop' },
  { id: 'scythe_jab', gear: ['Scythe of vitur', ...MELEE], style: 'Jab' },
  { id: 'fang_lunge', gear: ["Osmumten's fang", ...MELEE_DEF], style: 'Lunge' },
  { id: 'fang_slash', gear: ["Osmumten's fang", ...MELEE_DEF], style: 'Slash' },
  { id: 'elder_maul', gear: ['Elder maul', ...MELEE], style: 'Pound' },
  { id: 'inq_mace_full', gear: ["Inquisitor's mace", "Inquisitor's great helm", "Inquisitor's hauberk", "Inquisitor's plateskirt", 'Amulet of torture', 'Ferocious gloves', 'Primordial boots', 'Infernal cape', 'Avernic defender', 'Ultor ring'], style: 'Pound' },
  { id: 'inq_mace_body', gear: ["Inquisitor's mace", "Inquisitor's hauberk", 'Amulet of torture', 'Avernic defender'], style: 'Pummel' },
  { id: 'inq_mace_stab', gear: ["Inquisitor's mace", "Inquisitor's great helm", "Inquisitor's hauberk", "Inquisitor's plateskirt", 'Avernic defender'], style: 'Spike' },
  { id: 'dhl', gear: ['Dragon hunter lance', ...MELEE_DEF], style: 'Lunge' },
  { id: 'keris', gear: ['Keris', ...MELEE_DEF], style: 'Lunge' },
  { id: 'arclight', gear: ['Arclight', ...MELEE_DEF], style: 'Slash' },
  { id: 'emberlight', gear: ['Emberlight', ...MELEE_DEF], style: 'Slash' },
  { id: 'silverlight', gear: ['Silverlight', ...MELEE_DEF], style: 'Slash' },
  { id: 'darklight', gear: ['Darklight', ...MELEE_DEF], style: 'Slash' },
  { id: 'obsidian_set_berserker', gear: ['Tzhaar-ket-om', 'Obsidian helmet', 'Obsidian platebody', 'Obsidian platelegs', 'Berserker necklace', 'Fire cape', 'Dragon boots', 'Barrows gloves'], style: 'Pummel' },
  { id: 'obsidian_set', gear: ['Toktz-xil-ak', 'Obsidian helmet', 'Obsidian platebody', 'Obsidian platelegs', 'Amulet of fury', 'Dragon defender'], style: 'Lunge' },
  { id: 'berserker_only', gear: ['Tzhaar-ket-om', ...noNeck(MELEE), 'Berserker necklace'], style: 'Pummel' },
  { id: 'void_melee_whip', gear: ['Abyssal whip', 'Void melee helm', ...VOID, 'Amulet of torture', 'Avernic defender', 'Primordial boots', 'Infernal cape', 'Ultor ring'], style: 'Flick' },
  { id: 'elite_void_melee', gear: ['Ghrazi rapier', 'Void melee helm', ...EVOID, 'Amulet of torture', 'Avernic defender'], style: 'Lunge' },
  { id: 'slayer_helm_whip', gear: ['Abyssal whip', 'Slayer helmet', ...noHead(MELEE_DEF)], style: 'Lash' },
  { id: 'slayer_helm_i_rapier', gear: ['Ghrazi rapier', 'Slayer helmet (i)', ...noHead(MELEE_DEF)], style: 'Lunge' },
  { id: 'black_mask_scim', gear: ['Dragon scimitar', 'Black mask', ...noHead(MELEE_DEF)], style: 'Slash' },
  { id: 'salve_whip', gear: ['Abyssal whip', 'Salve amulet', ...noNeck(MELEE_DEF)], style: 'Flick' },
  { id: 'salve_e_whip', gear: ['Abyssal whip', 'Salve amulet (e)', ...noNeck(MELEE_DEF)], style: 'Flick' },
  { id: 'salve_ei_slayer_whip', gear: ['Abyssal whip', 'Salve amulet(ei)', 'Slayer helmet (i)', ...noNeck(noHead(MELEE_DEF))], style: 'Flick' },
  { id: 'salve_i_rapier', gear: ['Ghrazi rapier', 'Salve amulet(i)', ...noNeck(MELEE_DEF)], style: 'Lunge' },
  { id: 'dharok_half_hp', gear: ["Dharok's greataxe", "Dharok's helm", "Dharok's platebody", "Dharok's platelegs", 'Amulet of torture', 'Ferocious gloves', 'Primordial boots', 'Infernal cape'], style: 'Smash', tags: 'hp-50' },
  { id: 'leaf_bladed_battleaxe', gear: ['Leaf-bladed battleaxe', ...MELEE_DEF], style: 'Hack' },
  { id: 'colossal_blade', gear: ['Colossal blade', ...MELEE], style: 'Slash' },
  { id: 'granite_hammer', gear: ['Granite hammer', ...MELEE_DEF], style: 'Pound' },
  { id: 'dwh_controlled', gear: ['Dragon warhammer', ...MELEE_DEF], style: 'Pound' },
  { id: 'bludgeon', gear: ['Abyssal bludgeon', ...MELEE], style: 'Pound' },
  // ---- ranged
  { id: 'tbow_rapid', gear: ['Twisted bow', 'Dragon arrow', ...RANGED], style: 'Rapid' },
  { id: 'tbow_accurate', gear: ['Twisted bow', 'Dragon arrow', ...RANGED], style: 'Accurate' },
  { id: 'tbow_salve_ei', gear: ['Twisted bow', 'Dragon arrow', 'Salve amulet(ei)', ...noNeckR(RANGED)], style: 'Rapid' },
  { id: 'tbow_slayer_i', gear: ['Twisted bow', 'Dragon arrow', 'Slayer helmet (i)', ...noHeadR(RANGED)], style: 'Rapid' },
  { id: 'bofa_crystal', gear: ['Bow of faerdhinen', 'Crystal helm', 'Crystal body', 'Crystal legs', 'Necklace of anguish', 'Zaryte vambraces', 'Pegasian boots', "Ava's accumulator", 'Venator ring'], style: 'Rapid' },
  { id: 'bofa_crystal_helm', gear: ['Bow of faerdhinen', 'Crystal helm', 'Armadyl chestplate', 'Armadyl chainskirt', 'Necklace of anguish'], style: 'Accurate' },
  { id: 'bofa_plain', gear: ['Bow of faerdhinen', ...RANGED], style: 'Rapid' },
  { id: 'dhcb', gear: ['Dragon hunter crossbow', 'Dragon bolts', ...RANGED], style: 'Rapid' },
  { id: 'dhcb_slayer_i', gear: ['Dragon hunter crossbow', 'Dragon bolts', 'Slayer helmet (i)', ...noHeadR(RANGED)], style: 'Rapid' },
  { id: 'acb_elite_void', gear: ['Armadyl crossbow', 'Dragon bolts', 'Void ranger helm', ...EVOID, 'Necklace of anguish', "Ava's accumulator", 'Pegasian boots'], style: 'Rapid' },
  { id: 'msb_void', gear: ['Magic shortbow', 'Amethyst arrow', 'Void ranger helm', ...VOID, 'Necklace of anguish', "Ava's accumulator"], style: 'Accurate' },
  { id: 'rcb_longrange', gear: ['Rune crossbow', 'Runite bolts', ...RANGED], style: 'Longrange' },
  { id: 'rcb_salve_i', gear: ['Rune crossbow', 'Runite bolts', 'Salve amulet(i)', ...noNeckR(RANGED)], style: 'Accurate' },
  // ---- ranged: thrown weapons roll against light ranged defence, crossbows / ballistae against heavy (round 15)
  { id: 'blowpipe', gear: ['Toxic blowpipe', ...RANGED], style: 'Rapid' },
  { id: 'dragon_knife', gear: ['Dragon knife', ...RANGED], style: 'Rapid' },
  { id: 'dragon_thrownaxe', gear: ['Dragon thrownaxe', ...RANGED], style: 'Accurate' },
  { id: 'toktz_xil_ul', gear: ['Toktz-xil-ul', ...RANGED], style: 'Longrange' },
  { id: 'heavy_ballista', gear: ['Heavy ballista', 'Dragon javelin', ...RANGED], style: 'Accurate' },
  { id: 'dragon_crossbow', gear: ['Dragon crossbow', 'Ruby dragon bolts (e)', ...RANGED], style: 'Rapid' },
  { id: 'zaryte_crossbow', gear: ['Zaryte crossbow', 'Dragon bolts', ...RANGED], style: 'Rapid' },
  { id: 'black_mask_i_msb', gear: ['Magic shortbow', 'Rune arrow', 'Black mask (i)', ...noHeadR(RANGED)], style: 'Rapid' },
  // ---- magic: powered staves
  { id: 'trident_seas', gear: ['Trident of the seas', ...MAGE], style: 'Accurate' },
  { id: 'trident_seas_longrange', gear: ['Trident of the seas', ...MAGE], style: 'Longrange' },
  { id: 'trident_swamp', gear: ['Trident of the swamp', ...MAGE, 'Book of darkness'], style: 'Accurate' },
  { id: 'sang', gear: ['Sanguinesti staff', ...MAGE], style: 'Accurate' },
  { id: 'shadow', gear: ["Tumeken's shadow", ...MAGE], style: 'Accurate' },
  { id: 'shadow_longrange', gear: ["Tumeken's shadow", ...MAGE], style: 'Longrange' },
  { id: 'shadow_void', gear: ["Tumeken's shadow", 'Void mage helm', ...VOID, 'Occult necklace'], style: 'Accurate' },
  { id: 'trident_elite_void', gear: ['Trident of the swamp', 'Void mage helm', ...EVOID, 'Occult necklace', 'Book of darkness'], style: 'Accurate' },
  { id: 'sang_salve_ei', gear: ['Sanguinesti staff', 'Salve amulet(ei)', ...noNeckR(MAGE)], style: 'Accurate' },
  { id: 'sang_salve_i', gear: ['Sanguinesti staff', 'Salve amulet(i)', ...noNeckR(MAGE)], style: 'Accurate' },
  { id: 'trident_slayer_i', gear: ['Trident of the swamp', 'Slayer helmet (i)', ...noHeadR(MAGE)], style: 'Accurate' },
  // ---- magic: spells
  { id: 'kodai_fire_surge', gear: ['Kodai wand', ...MAGE, 'Arcane spirit shield'], style: 'Spell', spell: 'Fire Surge' },
  { id: 'harm_fire_surge_tome', gear: ['Harmonised nightmare staff', ...MAGE, 'Tome of fire'], style: 'Spell', spell: 'Fire Surge' },
  { id: 'kodai_ice_barrage', gear: ['Kodai wand', ...MAGE, 'Book of darkness'], style: 'Spell', spell: 'Ice Barrage' },
  { id: 'kodai_blood_barrage', gear: ['Kodai wand', ...MAGE], style: 'Spell', spell: 'Blood Barrage' },
  { id: 'smoke_staff_fire_wave', gear: ['Smoke battlestaff', ...MAGE], style: 'Spell', spell: 'Fire Wave' },
  { id: 'smoke_staff_ice_burst', gear: ['Smoke battlestaff', ...MAGE], style: 'Spell', spell: 'Ice Burst' },
  { id: 'staff_wind_strike', gear: ['Staff of air'], style: 'Spell', spell: 'Wind Strike' },
  { id: 'staff_fire_bolt', gear: ['Staff of fire', 'Mystic hat', 'Mystic robe top', 'Mystic robe bottom', 'Amulet of fury'], style: 'Spell', spell: 'Fire Bolt' },
  { id: 'staff_earth_blast', gear: ['Staff of earth', 'Mystic hat', 'Mystic robe top', 'Mystic robe bottom'], style: 'Spell', spell: 'Earth Blast' },
  { id: 'dhw_water_surge', gear: ['Dragon hunter wand', ...MAGE], style: 'Spell', spell: 'Water Surge' },
  { id: 'iban_blast', gear: ["Iban's staff", ...MAGE], style: 'Spell', spell: 'Iban Blast' },
  { id: 'slayer_dart', gear: ["Slayer's staff", ...MAGE], style: 'Spell', spell: 'Magic Dart' },
  { id: 'void_mage_fire_surge', gear: ['Kodai wand', 'Void mage helm', ...VOID, 'Occult necklace'], style: 'Spell', spell: 'Fire Surge' },
  // ---- magic: elemental weaknesses (2024 rework) of every element
  { id: 'kodai_wind_surge', gear: ['Kodai wand', ...MAGE], style: 'Spell', spell: 'Wind Surge' },
  { id: 'kodai_earth_surge', gear: ['Kodai wand', ...MAGE], style: 'Spell', spell: 'Earth Surge' },
  { id: 'staff_water_wave', gear: ['Staff of water', 'Mystic hat', 'Mystic robe top', 'Mystic robe bottom'], style: 'Spell', spell: 'Water Wave' },
  { id: 'slayer_i_fire_surge', gear: ['Kodai wand', 'Slayer helmet (i)', ...noHeadR(MAGE)], style: 'Spell', spell: 'Fire Surge' },
];

const MONSTERS: [string, string][] = [
  ['Abyssal demon', 'Standard'], ['Vorkath', 'Post-quest'], ['Kalphite Queen', 'Crawling'], ['Greater demon', 'Level 100'],
  ['Ankou', 'Level 75'], ['General Graardor', ''], ['Black dragon', 'Level 227'], ['Kalphite Soldier', 'Kalphite Lair'],
  ['Aberrant spectre', ''], ['Gargoyle', 'Upstairs'], ['Cerberus', ''], ["K'ril Tsutsaroth", ''], ['Callisto', ''],
  ['Turoth', 'Dad'], ['Dagannoth Rex', ''], ['Skeletal Wyvern', '1'],
  // round 15: distinct light / standard / heavy ranged defences and the four elemental weaknesses
  ['Corporeal Beast', ''], ['Frost dragon', ''], ['Chaos Fanatic', ''], ['Aviansie', 'Level 131'],
];

type Profile = { id: string; lvl: number; boost: { atk: number; str: number; ranged: number; magic: number }; prayers: { melee: string[]; ranged: string[]; magic: string[] }; monsters: number };
const PROFILES: Profile[] = [
  { id: 'max', lvl: 99, boost: { atk: 19, str: 19, ranged: 13, magic: 10 }, prayers: { melee: ['piety'], ranged: ['rigour'], magic: ['augury'] }, monsters: 99 },
  { id: 'mid', lvl: 80, boost: { atk: 11, str: 11, ranged: 8, magic: 4 }, prayers: { melee: ['incredible_reflexes', 'ultimate_strength'], ranged: ['eagle_eye'], magic: ['mystic_might'] }, monsters: 6 },
  { id: 'vig', lvl: 90, boost: { atk: 5, str: 5, ranged: 5, magic: 5 }, prayers: { melee: ['chivalry'], ranged: ['deadeye'], magic: ['mystic_vigour'] }, monsters: 3 },
  { id: 'low', lvl: 15, boost: { atk: 0, str: 0, ranged: 0, magic: 0 }, prayers: { melee: ['clarity_of_thought', 'burst_of_strength'], ranged: ['sharp_eye'], magic: ['mystic_will'] }, monsters: 2 },
  { id: 'none', lvl: 70, boost: { atk: 0, str: 0, ranged: 0, magic: 0 }, prayers: { melee: [], ranged: [], magic: [] }, monsters: 3 },
];

const PRAYER_KEYS: Record<string, string> = {};

function slotOf(p: any): string { return p.slot; }

function main() {
  // Prayer enum: map our snake names to the calculator's Prayer enum values.
  // eslint-disable-next-line @typescript-eslint/no-var-requires
  const { Prayer, PrayerMap } = require('@/enums/Prayer');
  for (const [k, v] of Object.entries(PrayerMap as Record<string, any>)) {
    PRAYER_KEYS[v.name.toLowerCase().replace(/ /g, '_')] = k;
  }
  const rows: string[] = [];
  rows.push('# Player-vs-NPC reference from weirdgloop/osrs-dps-calc PlayerVsNPCCalc (src/lib), bundled with esbuild and run under node (rounds 14-15).');
  rows.push('# Inputs are what the calculator saw: boosted levels, prayers, raw summed gear bonuses (before Tumeken\'s shadow / elite void adjustments),');
  rows.push('# the NPC defence level and bonus it rolled against (and its light / standard / heavy ranged defences). Outputs: min/max hit (per hit; effMax adds the Dharok / berserker necklace damage scaling), attack roll, NPC defence roll, hit chance.');
  rows.push(['id', 'profile', 'monster', 'loadout', 'gear', 'type', 'stance', 'spell', 'atk', 'str', 'ranged', 'magic', 'hpMax', 'hpCur', 'prayers',
    'offBonus', 'strBonus', 'rangedStr', 'magicDmgTenths', 'npcDefLevel', 'npcDefBonus', 'npcRangedLight', 'npcRangedStandard', 'npcRangedHeavy', 'npcTbowMagic', 'attrs', 'slayerMonster', 'size', 'weakness',
    'minHit', 'maxHit', 'effMax', 'attackRoll', 'defenceRoll', 'hitChance'].join('\t'));
  let n = 0;
  for (const prof of PROFILES) {
    for (const lo of loadouts) {
      const mons = MONSTERS.slice(0, Math.min(prof.monsters, MONSTERS.length));
      for (const [mn, mv] of (prof.monsters < 99 ? rotate(mons, lo.id) : mons)) {
        const m = monster(mn, mv);
        const pieces = lo.gear.map(item);
        const equip: any = { head: null, cape: null, neck: null, ammo: null, weapon: null, body: null, shield: null, legs: null, hands: null, feet: null, ring: null };
        for (const p of pieces) equip[slotOf(p)] = p;
        const weapon = equip.weapon;
        const styles = getCombatStylesForCategory(weapon.category || EquipmentCategory.NONE);
        let style: any;
        if (lo.spell) style = styles.find((s: any) => s.stance === 'Autocast') || styles.find((s: any) => s.type === 'magic');
        else style = styles.find((s: any) => s.name === lo.style);
        if (!style) throw new Error('style ' + lo.style + ' for ' + lo.id + ' in ' + JSON.stringify(styles));
        const kind = style.type === 'ranged' ? 'ranged' : style.type === 'magic' ? 'magic' : 'melee';
        const hpCur = lo.tags === 'hp-50' ? Math.trunc(prof.lvl / 2) : prof.lvl;
        const player: any = {
          name: 'x', style,
          skills: { atk: prof.lvl, def: prof.lvl, hp: prof.lvl, magic: prof.lvl, prayer: 99, ranged: prof.lvl, str: prof.lvl, mining: 99, herblore: 99 },
          boosts: { atk: prof.boost.atk, def: 0, hp: hpCur - prof.lvl, magic: prof.boost.magic, prayer: 0, ranged: prof.boost.ranged, str: prof.boost.str, mining: 0, herblore: 0 },
          equipment: equip, attackSpeed: 4,
          prayers: (prof.prayers as any)[kind].map((p: string) => { const k = PRAYER_KEYS[p]; if (k === undefined) throw new Error('prayer ' + p); return Number.isNaN(Number(k)) ? (Prayer as any)[k] : Number(k); }),
          bonuses: { str: 0, ranged_str: 0, magic_str: 0, prayer: 0 },
          defensive: { stab: 0, slash: 0, crush: 0, magic: 0, ranged: 0 },
          offensive: { stab: 0, slash: 0, crush: 0, magic: 0, ranged: 0 },
          buffs: { potions: [], onSlayerTask: true, inWilderness: false, kandarinDiary: true, chargeSpell: false, markOfDarknessSpell: false, forinthrySurge: false, soulreaperStacks: 0, baAttackerLevel: 0, chinchompaDistance: 4, usingSunfireRunes: false },
          spell: lo.spell ? spells.find((s: any) => s.name === lo.spell) : null,
        };
        if (lo.spell && !player.spell) throw new Error('spell ' + lo.spell);
        const calcd = calculateEquipmentBonusesFromGear(player, m);
        player.bonuses = calcd.bonuses; player.offensive = calcd.offensive; player.defensive = calcd.defensive;
        player.attackSpeed = calculateAttackSpeed(player, m);
        const calc: any = new PlayerVsNPCCalc(player, m, { loadoutName: 'x', detailedOutput: false } as any);
        const [minHit, maxHit] = calc.getMinAndMax();
        const atkRoll = calc.getMaxAttackRoll();
        const defRoll = calc.getNPCDefenceRoll();
        const hc = calc.getHitChance();
        // The calculator applies these two to the hit distribution (PlayerVsNPCCalc getAttackerDist): same integer maths here.
        let effMax = maxHit;
        if (kind === 'melee' && calc.isWearingDharok()) effMax = Math.trunc(effMax * (10000 + (player.skills.hp - hpCur) * player.skills.hp) / 10000);
        if (kind === 'melee' && calc.isWearingBerserkerNecklace() && calc.isWearingTzhaarWeapon()) effMax = Math.trunc(effMax * 6 / 5);
        // raw sums
        const raw = { off: { stab: 0, slash: 0, crush: 0, magic: 0, ranged: 0 } as any, str: 0, rstr: 0, mstr: 0 };
        for (const p of pieces) {
          for (const k of Object.keys(raw.off)) raw.off[k] += p.offensive[k] || 0;
          raw.str += p.bonuses.str || 0; raw.rstr += p.bonuses.ranged_str || 0; raw.mstr += p.bonuses.magic_str || 0;
        }
        const t = style.type;
        let defBonus: number; let defLevel: number;
        if (t === 'ranged') {
          const rt = getRangedDamageType(weapon.category);
          defBonus = rt === 'mixed' ? Math.trunc((m.defensive.light + m.defensive.standard + m.defensive.heavy) / 3) : m.defensive[rt];
          defLevel = m.skills.def;
        } else if (t === 'magic') { defBonus = m.defensive.magic; defLevel = m.skills.magic; } else { defBonus = m.defensive[t]; defLevel = m.skills.def; }
        const tbowMagic = Math.max(m.skills.magic, m.offensive.magic);
        const weak = m.weakness ? `${m.weakness.element}:${m.weakness.severity}` : '-';
        n++;
        rows.push([`${prof.id}/${lo.id}/${mn}${mv ? ' (' + mv + ')' : ''}`, prof.id, `${mn}|${mv}`, lo.id, lo.gear.join(','), t, style.stance, lo.spell || '-',
          prof.lvl + prof.boost.atk, prof.lvl + prof.boost.str, prof.lvl + prof.boost.ranged, prof.lvl + prof.boost.magic, prof.lvl, hpCur,
          (prof.prayers as any)[kind].join(',') || '-',
          raw.off[t], raw.str, raw.rstr, raw.mstr, defLevel, defBonus, m.defensive.light, m.defensive.standard, m.defensive.heavy, tbowMagic, (m.attributes || []).join(',') || '-', calc.isSlayerMonster() ? 1 : 0, m.size, weak,
          minHit, maxHit, effMax, atkRoll, defRoll, hc.toFixed(6)].join('\t'));
      }
    }
  }
  fs.writeFileSync(process.argv[2], rows.join('\n') + '\n');
  console.log('rows', n);
}

function rotate<T>(arr: T[], key: string): T[] {
  let h = 0;
  for (const c of key) h = (h * 31 + c.charCodeAt(0)) >>> 0;
  const out: T[] = [];
  const all = MONSTERS as any as T[];
  for (let i = 0; i < arr.length; i++) out.push(all[(h + i * 5) % all.length]);
  return out;
}

main();
