package com.runecraft.osrs.data;

import com.runecraft.osrs.ItemDef;

import java.util.HashMap;
import java.util.Map;

/**
 * Reference OSRS item data, applied to every item after all modules have registered theirs (the same way
 * {@link EquipmentBonuses} fixes bonuses). Generated from the OSRS Wiki item dump (Flipping-Utilities/parsed-osrs,
 * cross-checked against 0xNeffarion/osrsreboxed-db) by matching item names:
 * <ul>
 *   <li>store value (the price basis for shops; high alchemy = 60%, low alchemy = 40%),</li>
 *   <li>weight in kg (run energy drain),</li>
 *   <li>whether OSRS stacks the item, whether it is tradeable and whether the Grand Exchange lists it,</li>
 *   <li>examine text for items whose module gave none.</li>
 * </ul>
 * Each line is {@code id|value|weightKg|flags|examine}; value -1 keeps the module's value, flags are
 * {@code s} stackable, {@code g} on the Grand Exchange, {@code t} tradeable. Items the wiki doesn't know
 * (RuneCraft-only items) aren't listed and keep their module values.
 */
public final class ItemValues {
    /** Reference data for one item. */
    public record Ref(int value, double weight, boolean stackable, boolean onGe, boolean tradeable, String examine) {
    }

    private static final Map<String, Ref> REFS = new HashMap<>();

    static {
        load("""
            logs|-1|2|gt|A number of wooden logs.
            oak_logs|-1|2|gt|Logs cut from an oak tree.
            willow_logs|40|2|gt|Logs cut from a willow tree.
            teak_logs|-1|1.36|gt|Some well-cut teak logs.
            maple_logs|80|2|gt|Logs cut from a maple tree.
            mahogany_logs|50|1.36|gt|Some well-cut mahogany logs.
            yew_logs|-1|2|gt|Logs cut from a yew tree.
            magic_logs|320|2|gt|Logs cut from a magic tree.
            redwood_logs|450|2|gt|Logs cut from a redwood tree.
            plank|1|0.8|gt|A plank of wood!
            oak_plank|250|0.8|gt|A plank of sturdy oak.
            teak_plank|500|0.8|gt|A plank of fine teak.
            mahogany_plank|1500|0.8|gt|A plank of expensive mahogany.
            copper_ore|3|2.267|gt|This needs refining.
            tin_ore|3|2.267|gt|This needs refining.
            clay|1|1|gt|Some hard dry clay.
            rune_essence|-1|0.002|gt|An unimbued rune.
            iron_ore|-1|2.267|gt|This needs refining.
            silver_ore|-1|2.267|gt|This needs refining.
            pure_essence|4|0.002|gt|An unimbued rune of extra capability.
            coal|-1|2.267|gt|Hmm a non-renewable energy source!
            gold_ore|-1|2.267|gt|This needs refining.
            mithril_ore|-1|1.814|gt|This needs refining.
            adamantite_ore|-1|2.721|gt|This needs refining.
            runite_ore|-1|2.267|gt|This needs refining.
            amethyst|3900|1.36|gt|Maybe I can make something with this.
            bronze_bar|-1|1.814|gt|It's a bar of bronze.
            iron_bar|-1|1.814|gt|It's a bar of iron.
            silver_bar|-1|1.814|gt|It's a bar of silver.
            steel_bar|-1|1.814|gt|It's a bar of steel.
            gold_bar|-1|1.814|gt|It's a bar of gold.
            mithril_bar|-1|1.587|gt|It's a bar of mithril.
            adamantite_bar|-1|2.041|gt|It's a bar of adamantite.
            runite_bar|-1|1.814|gt|It's a bar of runite.
            bronze_arrowtips|-1|0|sgt|I can make some arrows with these.
            iron_arrowtips|2|0|sgt|I can make some arrows with these.
            steel_arrowtips|6|0|sgt|I can make some arrows with these.
            mithril_arrowtips|16|0|sgt|I can make some arrows with these.
            adamant_arrowtips|40|0|sgt|I can make some arrows with these.
            rune_arrowtips|200|0|sgt|I can make some arrows with these.
            bronze_axe|-1|1.36|gt|A woodcutter's axe.
            bronze_pickaxe|1|2.267|gt|Used for mining.
            iron_axe|-1|1.36|gt|A woodcutter's axe.
            iron_pickaxe|140|2.267|gt|Used for mining.
            steel_axe|-1|1.36|gt|A woodcutter's axe.
            steel_pickaxe|500|2.267|gt|Used for mining.
            black_axe|-1|1.36|gt|A sinister looking axe.
            black_pickaxe|145|0.01|gt|Used for mining.
            mithril_axe|-1|1.133|gt|A powerful axe.
            mithril_pickaxe|1300|1.814|gt|Used for mining.
            adamant_axe|-1|1.587|gt|A powerful axe.
            adamant_pickaxe|3200|2.721|gt|Used for mining.
            rune_axe|-1|1.36|gt|A powerful axe.
            rune_pickaxe|32000|2.267|gt|Used for mining.
            dragon_axe|55000|1.36|gt|A very powerful axe.
            dragon_pickaxe|97950|2.4|gt|If rocks could feel fear, they'd fear this.
            tinderbox|-1|0.035|gt|Useful for lighting a fire.
            knife|-1|0.453|gt|A dangerous looking knife.
            chisel|-1|0.453|gt|Good for detailed crafting.
            hammer|-1|0.907|gt|Good for hitting things!
            needle|-1|0|sgt|Used with a thread to make clothes.
            small_fishing_net|-1|4.535|gt|Useful for catching small fish.
            fishing_rod|-1|1.36|gt|Useful for catching sardine or herring.
            fly_fishing_rod|-1|1.36|gt|Useful for catching salmon or trout.
            lobster_pot|-1|0.255|gt|Useful for catching lobsters.
            harpoon|-1|1.36|gt|Useful for catching really big fish.
            seed_dibber|-1|0.002|gt|Use this to plant seeds with.
            ring_mould|-1|0.453|gt|Used to make rings.
            necklace_mould|-1|0.453|gt|Used to make necklaces.
            amulet_mould|-1|0.453|gt|Used to make amulets.
            bird_snare|5|0|sgt|A simple bird catcher.
            box_trap|32|0|sgt|If a creature goes inside, the box should then slam shut.
            fishing_bait|-1|0|sgt|For use with a fishing rod.
            dark_fishing_bait|3|0|sgt|What on earth would eat this?
            enchanted_gem|-1|0.002||
            mark_of_grace|-1|0|s|You can exchange these for rewards at the Rogues' Den.
            raw_shrimps|-1|0.12|gt|I should try cooking this.
            raw_sardine|-1|0.14|gt|I should try cooking this.
            raw_herring|10|0.55|gt|I should try cooking this.
            raw_anchovies|-1|0.12|gt|I should try cooking this.
            raw_trout|10|0.45|gt|I should try cooking this.
            raw_pike|20|0.55|gt|I should try cooking this.
            raw_salmon|30|0.55|gt|I should try cooking this.
            raw_tuna|40|0.45|gt|I should try cooking this.
            raw_lobster|70|0.4|gt|I should try cooking this.
            raw_swordfish|80|0.5|gt|I should try cooking this.
            raw_monkfish|230|0.45|gt|I should try cooking this.
            raw_shark|170|0.7|gt|I should try cooking this.
            raw_anglerfish|450|0.4|gt|I should try cooking this.
            raw_dark_crab|325|0.35|gt|I should try cooking this.
            shrimps|-1|0.1|gt|Some nicely cooked shrimp.
            sardine|-1|0.12|gt|Some nicely cooked sardines.
            herring|10|0.5|gt|Some nicely cooked herring.
            anchovies|-1|0.1|gt|Some nicely cooked anchovies.
            trout|10|0.4|gt|Some nicely cooked trout.
            pike|20|0.5|gt|Some nicely cooked pike.
            salmon|30|0.5|gt|Some nicely cooked salmon.
            tuna|40|0.4|gt|Wow, this is a big fish.
            lobster|70|0.35|gt|This looks tricky to eat.
            swordfish|80|0.45|gt|I'd better be careful eating this!
            monkfish|230|0.4|gt|A tasty fish.
            shark|170|0.65|gt|I'd better be careful eating this.
            anglerfish|450|0.4|gt|I hope this tastes better than it looks.
            dark_crab|320|0.35|gt|This looks tricky to eat.
            burnt_fish|-1|0.08|t|Oops!
            burnt_meat|-1|0.226|t|Oh dear, it's totally burnt!
            raw_bird_meat|17|0.15|gt|This certainly needs cooking!
            roast_bird_meat|17|0.15|gt|A nicely roasted bird.
            cake|-1|0.3|gt|A plain sponge cake.
            chocolate_slice|-1|0.1|t|I'd rather have a whole cake.
            bones|1|0.5|gt|Bones are for burying!
            big_bones|1|0.8|gt|Ew it's a pile of bones.
            babydragon_bones|1|0.8|gt|Ew it's a pile of bones.
            dragon_bones|160|1.5|gt|These would feed a dog for months!
            wyvern_bones|160|0.5|gt|Bones of a large flying creature!
            lava_dragon_bones|150|1.6|gt|Bones of a lava dragon.
            hydra_bones|160|1.8|gt|The bones of a hydra.
            dagannoth_bones|150|1.5|gt|These would feed a dogfish for months!
            superior_dragon_bones|160|1.8|gt|There's something unnatural about these bones.
            air_rune|-1|0|sgt|One of the 4 basic elemental Runes.
            water_rune|-1|0|sgt|One of the 4 basic elemental Runes.
            earth_rune|-1|0|sgt|One of the 4 basic elemental Runes.
            fire_rune|-1|0|sgt|One of the 4 basic elemental Runes.
            mind_rune|-1|0|sgt|Used for basic level missile spells.
            body_rune|-1|0|sgt|Used for curse spells.
            cosmic_rune|-1|0|sgt|Used for enchant spells.
            chaos_rune|-1|0|sgt|Used for low level missile spells.
            astral_rune|-1|0|sgt|Used for Lunar spells.
            nature_rune|-1|0|sgt|Used for alchemy spells.
            law_rune|-1|0|sgt|Used for teleport spells.
            death_rune|-1|0|sgt|Used for medium level missile spells.
            blood_rune|-1|0|sgt|Used for high level missile spells.
            soul_rune|-1|0|sgt|Used for high level curse spells.
            wrath_rune|500|0|sgt|Used for very high level missile spells.
            staff|-1|1.814|gt|It's a slightly magical stick.
            staff_of_air|-1|2.267|gt|A Magical staff.
            staff_of_water|-1|2.267|gt|A Magical staff.
            staff_of_earth|-1|2.267|gt|A Magical staff.
            staff_of_fire|-1|2.267|gt|A Magical staff.
            ancient_staff|100000|2.267|gt|A magical staff of ancient origin...
            bronze_dagger|10|0.453|gt|Short but pointy.
            bronze_scimitar|-1|1.814|gt|A vicious, curved sword.
            bronze_longsword|-1|1.814|gt|A razor sharp longsword.
            bronze_warhammer|47|1.814|gt|I don't think it's intended for joinery.
            bronze_battleaxe|52|2.721|gt|A vicious looking axe.
            bronze_2h_sword|80|3.628|gt|A two handed sword.
            bronze_full_helm|44|2.721|gt|A full face helmet.
            bronze_platebody|160|9.979|gt|Provides excellent protection.
            bronze_platelegs|80|9.071|gt|These look pretty heavy.
            bronze_kiteshield|68|5.443|gt|A large metal shield.
            iron_dagger|35|0.453|gt|Short but pointy.
            iron_scimitar|-1|1.814|gt|A vicious, curved sword.
            iron_longsword|-1|1.814|gt|A razor sharp longsword.
            iron_warhammer|173|1.814|gt|I don't think it's intended for joinery.
            iron_battleaxe|182|2.721|gt|A vicious looking axe.
            iron_2h_sword|280|3.628|gt|A two handed sword.
            iron_full_helm|154|2.721|gt|A full face helmet.
            iron_platebody|560|9.979|gt|Provides excellent protection.
            iron_platelegs|280|9.071|gt|These look pretty heavy.
            iron_kiteshield|238|5.443|gt|A large metal shield.
            steel_dagger|125|0.453|gt|Short but pointy.
            steel_scimitar|-1|1.814|gt|A vicious, curved sword.
            steel_longsword|-1|1.814|gt|A razor sharp longsword.
            steel_warhammer|640|1.814|gt|I don't think it's intended for joinery.
            steel_battleaxe|650|2.721|gt|A vicious looking axe.
            steel_2h_sword|1000|3.628|gt|A two handed sword.
            steel_full_helm|550|2.721|gt|A full face helmet.
            steel_platebody|2000|9.979|gt|Provides excellent protection.
            steel_platelegs|1000|9.071|gt|These look pretty heavy.
            steel_kiteshield|850|5.443|gt|A large metal shield.
            black_dagger|240|0.453|gt|A vicious black dagger.
            black_scimitar|-1|1.814|gt|A vicious, curved sword.
            black_longsword|-1|1.814|gt|A razor sharp longsword.
            black_warhammer|980|1.814|gt|I don't think it's intended for joinery.
            black_battleaxe|1248|2.721|gt|A vicious looking axe.
            black_2h_sword|1920|3.628|gt|A two handed sword.
            black_full_helm|1056|2.721|gt|A full face helmet.
            black_platebody|3840|9.979|gt|Provides excellent protection.
            black_platelegs|1920|9.071|gt|Big, black and heavy looking.
            black_kiteshield|1632|5.443|gt|A large metal shield.
            mithril_dagger|325|0.396|gt|A dangerous dagger.
            mithril_scimitar|-1|1.587|gt|A vicious, curved sword.
            mithril_longsword|-1|1.587|gt|A razor sharp longsword.
            mithril_warhammer|1660|1.587|gt|I don't think it's intended for joinery.
            mithril_battleaxe|1690|2.267|gt|A vicious looking axe.
            mithril_2h_sword|2600|3.175|gt|A two handed sword.
            mithril_full_helm|1430|2.267|gt|A full face helmet.
            mithril_platebody|5200|8.618|gt|Provides excellent protection.
            mithril_platelegs|2600|7.711|gt|These look pretty heavy.
            mithril_kiteshield|2210|4.535|gt|A large metal shield.
            adamant_dagger|800|0.51|gt|Short and deadly.
            adamant_scimitar|-1|2.041|gt|A vicious, curved sword.
            adamant_longsword|-1|2.041|gt|A razor sharp longsword.
            adamant_warhammer|4120|2.041|gt|I don't think it's intended for joinery.
            adamant_battleaxe|4160|3.175|gt|A vicious looking axe.
            adamant_2h_sword|6400|4.082|gt|A two handed sword.
            adamant_full_helm|3520|3.175|gt|A full face helmet.
            adamant_platebody|16640|11.339|gt|Provides excellent protection.
            adamant_platelegs|6400|10.432|gt|These look pretty heavy.
            adamant_kiteshield|5440|5.896|gt|A large metal shield.
            rune_dagger|8000|0.453|gt|A powerful dagger.
            rune_scimitar|-1|1.814|gt|A vicious, curved sword.
            rune_longsword|-1|1.814|gt|A razor sharp longsword.
            rune_warhammer|41500|1.814|gt|I don't think it's intended for joinery.
            rune_battleaxe|41600|2.721|gt|A vicious looking axe.
            rune_2h_sword|64000|3.628|gt|A two handed sword.
            rune_full_helm|35200|2.721|gt|A full face helmet.
            rune_platebody|65000|9.979|gt|Provides excellent protection.
            rune_platelegs|64000|9.071|gt|These look pretty heavy.
            rune_kiteshield|54400|5.443|gt|A large metal shield.
            dragon_dagger|30000|0.453|gt|A powerful dagger.
            dragon_scimitar|-1|1.814|gt|A vicious, curved sword.
            dragon_longsword|100000|1.814|gt|A very powerful sword.
            dragon_warhammer|120000|2|gt|Spikey, close combat weapon.
            dragon_battleaxe|200000|2.721|gt|A vicious looking axe.
            dragon_2h_sword|220000|3.628|gt|A two-handed Dragon Sword.
            dragon_full_helm|150000|2|gt|Protects your head and looks impressive too.
            dragon_platebody|2000000|11.339|gt|Provides excellent protection.
            dragon_platelegs|270000|9.071|gt|These look pretty heavy.
            dragon_kiteshield|1600000|6.803|gt|An ancient and powerful looking Dragon Kiteshield.
            arrow_shaft|-1|0|sgt|A wooden arrow shaft.
            headless_arrow|-1|0|sgt|A wooden arrow shaft with flights attached.
            bow_string|10|0.014|gt|I need a bow stave to attach this to.
            flax|-1|0.453|gt|I should use this with a spinning wheel.
            shortbow_u|23|0.992|gt|I need to find a string for this.
            longbow_u|60|1.332|gt|I need to find a string for this.
            oak_shortbow_u|-1|0.992|gt|An unstrung oak shortbow; I need a bowstring for this.
            oak_longbow_u|-1|1.332|gt|An unstrung oak longbow; I need a bowstring for this.
            willow_shortbow_u|-1|0.992|gt|An unstrung willow shortbow; I need a bowstring for this.
            willow_longbow_u|-1|1.332|gt|An unstrung willow longbow; I need a bowstring for this.
            maple_shortbow_u|-1|0.992|gt|An unstrung maple shortbow; I need a bowstring for this.
            maple_longbow_u|-1|1.332|gt|An unstrung maple longbow; I need a bowstring for this.
            yew_shortbow_u|-1|0.992|gt|An unstrung yew shortbow; I need a bowstring for this.
            yew_longbow_u|-1|1.332|gt|An unstrung yew longbow; I need a bowstring for this.
            magic_shortbow_u|-1|0.992|gt|An unstrung magic shortbow; I need a bowstring for this.
            magic_longbow_u|-1|1.332|gt|An unstrung magic longbow; I need a bowstring for this.
            shortbow|-1|1.36|gt|Short but effective.
            longbow|-1|1.814|gt|A nice sturdy bow.
            oak_shortbow|-1|1.814|gt|A shortbow made out of oak, still effective.
            oak_longbow|-1|2.267|gt|A nice sturdy bow made out of oak.
            willow_shortbow|-1|1.36|gt|A shortbow made out of willow, still effective.
            willow_longbow|-1|1.814|gt|A nice sturdy bow made out of willow.
            maple_shortbow|-1|1.36|gt|A shortbow made out of Maple, still effective.
            maple_longbow|-1|1.814|gt|A nice sturdy bow made out of Maple.
            yew_shortbow|-1|1.36|gt|A shortbow made out of yew, still effective.
            yew_longbow|-1|1.814|gt|A nice sturdy bow made out of yew.
            magic_shortbow|-1|1.36|gt|Short and magical, but still effective.
            magic_longbow|-1|1.814|gt|A nice sturdy magical bow.
            bronze_arrow|1|0|sgt|Arrows with bronze heads.
            iron_arrow|3|0|sgt|Arrows with iron heads.
            steel_arrow|12|0|sgt|Arrows with steel heads.
            mithril_arrow|32|0|sgt|Arrows with mithril heads.
            adamant_arrow|80|0|sgt|Arrows with adamant heads.
            rune_arrow|400|0|sgt|Arrows with rune heads.
            cowhide|2|2.721|gt|I should take this to the tannery.
            leather|1|2.267|gt|It's a piece of leather.
            ball_of_wool|-1|0.012|gt|Spun from wool.
            silk|-1|1|gt|It's a sheet of silk.
            grey_wolf_fur|-1|3|gt|This would make warm clothing.
            spice|-1|0.2|gt|This could liven up an otherwise bland stew.
            leather_cowl|-1|0.907|gt|Better than no armour!
            leather_body|-1|2.721|gt|Better than no armour!
            leather_chaps|-1|2.721|gt|Better than no armour!
            leather_boots|-1|0.34|gt|Comfortable leather boots.
            uncut_opal|20|0.003|gt|This would be worth more cut.
            uncut_jade|30|0.003|gt|This would be worth more cut.
            uncut_red_topaz|40|0.003|gt|This would be worth more cut.
            uncut_sapphire|-1|0.003|gt|This would be worth more cut.
            uncut_emerald|-1|0.003|gt|This would be worth more cut.
            uncut_ruby|-1|0.003|gt|This would be worth more cut.
            uncut_diamond|-1|0.003|gt|This would be worth more cut.
            uncut_dragonstone|-1|0.003|gt|This would be worth more cut.
            uncut_onyx|200000|0.003|gt|This would be worth more cut.
            uncut_zenyte|75000|0.003|gt|This would be worth more cut.
            opal|100|0.002|gt|A semi precious stone.
            jade|150|0.002|gt|A semi precious stone.
            red_topaz|200|0.002|gt|A semi precious stone.
            sapphire|250|0.002|gt|This looks valuable.
            emerald|500|0.002|gt|This looks valuable.
            ruby|1000|0.002|gt|This looks valuable.
            diamond|2000|0.002|gt|This looks valuable.
            dragonstone|10000|0.002|gt|This looks valuable.
            onyx|200000|0.002|gt|This looks valuable.
            zenyte|75000|0.002|gt|This looks valuable.
            gold_ring|-1|0.004|gt|A valuable ring.
            gold_necklace|-1|0.007|gt|I wonder if this is valuable.
            gold_amulet|-1|0.007|gt|A plain gold amulet.
            sapphire_ring|-1|0.006|gt|A valuable ring.
            emerald_ring|-1|0.006|gt|A valuable ring.
            ruby_ring|-1|0.006|gt|A valuable ring.
            diamond_ring|-1|0.006|gt|A valuable ring.
            vial|-1|0.015|gt|An empty glass vial.
            vial_of_water|-1|0.02|gt|A glass vial containing water.
            grimy_guam_leaf|13|0.007|gt|It needs cleaning.
            grimy_marrentill|14|0.007|gt|It needs cleaning.
            grimy_tarromin|15|0.007|gt|It needs cleaning.
            grimy_harralander|16|0.007|gt|It needs cleaning.
            grimy_ranarr_weed|30|0.007|gt|It needs cleaning.
            grimy_toadflax|19|0.007|gt|It needs cleaning.
            grimy_irit_leaf|17|0.007|gt|It needs cleaning.
            grimy_avantoe|18|0.007|gt|It needs cleaning.
            grimy_kwuarm|20|0.007|gt|It needs cleaning.
            grimy_snapdragon|21|0.007|gt|It needs cleaning.
            grimy_cadantine|22|0.007|gt|It needs cleaning.
            grimy_lantadyme|23|0.007|gt|It needs cleaning.
            grimy_dwarf_weed|24|0.007|gt|It needs cleaning.
            grimy_torstol|25|0.007|gt|It needs cleaning.
            guam_leaf|-1|0.007|gt|A bitter green herb.
            marrentill|-1|0.007|gt|A herb used in poison cures.
            tarromin|-1|0.007|gt|A useful herb.
            harralander|-1|0.007|gt|A useful herb.
            ranarr_weed|-1|0.007|gt|A useful herb.
            toadflax|48|0.007|gt|A useful herb.
            irit_leaf|-1|0.007|gt|A useful herb.
            avantoe|-1|0.007|gt|A useful herb.
            kwuarm|-1|0.007|gt|A powerful herb.
            snapdragon|-1|0.007|gt|A powerful herb.
            cadantine|-1|0.007|gt|A powerful herb.
            lantadyme|68|0.007|gt|A powerful herb.
            dwarf_weed|-1|0.007|gt|A powerful herb.
            torstol|-1|0.007|gt|A powerful herb.
            guam_potion_unf|-1|0.056|gt|I need another ingredient to finish this Guam potion.
            marrentill_potion_unf|-1|0.056|gt|I need another ingredient to finish this Marrentill potion.
            tarromin_potion_unf|-1|0.056|gt|I need another ingredient to finish this Tarromin potion.
            harralander_potion_unf|-1|0.056|gt|I need another ingredient to finish this Harralander potion.
            ranarr_potion_unf|-1|0.056|gt|I need another ingredient to finish this Ranarr potion.
            toadflax_potion_unf|48|0.056|gt|I need another ingredient to finish this Toadflax potion.
            irit_potion_unf|-1|0.056|gt|I need another ingredient to finish this Irit potion.
            avantoe_potion_unf|-1|0.056|gt|I need another ingredient to finish this Avantoe potion.
            kwuarm_potion_unf|-1|0.056|gt|I need another ingredient to finish this Kwuarm potion.
            snapdragon_potion_unf|-1|0.056|gt|I need another ingredient to finish this Snapdragon potion.
            cadantine_potion_unf|-1|0.056|gt|I need another ingredient to finish this Cadantine potion.
            lantadyme_potion_unf|68|0.056|gt|I need another ingredient to finish this Lantadyme potion.
            dwarf_weed_potion_unf|-1|0.056|gt|I need another ingredient to finish this Dwarf Weed potion.
            torstol_potion_unf|25|0.056|gt|I need another ingredient to finish this Torstol potion.
            eye_of_newt|-1|0.007|gt|It seems to be looking at me.
            unicorn_horn_dust|-1|0.007|gt|Finely ground horn of Unicorn.
            limpwurt_root|7|0.007|gt|The root of a limpwurt plant.
            red_spiders_eggs|7|0.007|gt|Ewww!
            chocolate_dust|2|0.15|gt|It's ground up chocolate.
            white_berries|-1|0.007|gt|Sour berries, used in potions.
            wine_of_zamorak|1|0.085|gt|An evil wine for an evil god.
            potato_cactus|1|0.5|gt|How am I supposed to eat that?!
            crushed_nest|200|0.002|gt|A crushed bird's nest.
            attack_potion|15|0.035|gt|4 doses of Attack potion.
            antipoison|360|0.035|gt|4 doses of antipoison potion.
            strength_potion|14|0.035|gt|4 doses of Strength potion.
            restore_potion|110|0.035|gt|4 doses of restore potion.
            energy_potion|146|0.035|gt|4 doses of energy potion.
            defence_potion|150|0.035|gt|4 doses of Defence potion.
            prayer_potion|190|0.035|gt|4 doses of Prayer restore potion.
            super_attack|225|0.035|gt|4 doses of super Attack potion.
            super_strength|275|0.035|gt|4 doses of super Strength potion.
            super_restore|300|0.035|gt|4 doses of super restore potion.
            super_defence|330|0.035|gt|4 doses of super Defence potion.
            ranging_potion|360|0.035|gt|4 doses of ranging potion.
            magic_potion|300|0.035|gt|4 doses of Magic potion.
            saradomin_brew|200|0.035|gt|4 doses of Saradomin brew.
            guam_seed|1|0|sgt|A guam seed - plant in a herb patch.
            marrentill_seed|2|0|sgt|A marrentill seed - plant in a herb patch.
            tarromin_seed|3|0|sgt|A tarromin seed - plant in a herb patch.
            harralander_seed|4|0|sgt|A harralander seed - plant in a herb patch.
            ranarr_seed|50|0|sgt|A ranarr seed - plant in a herb patch.
            toadflax_seed|10|0|sgt|A toadflax seed - plant in a herb patch.
            irit_seed|5|0|sgt|An irit seed - plant in a herb patch.
            avantoe_seed|8|0|sgt|An avantoe seed - plant in a herb patch.
            kwuarm_seed|11|0|sgt|A kwuarm seed - plant in a herb patch.
            snapdragon_seed|60|0|sgt|A snapdragon seed - plant in a herb patch.
            cadantine_seed|7|0|sgt|A cadantine seed - plant in a herb patch.
            lantadyme_seed|9|0|sgt|A lantadyme seed - plant in a herb patch.
            dwarf_weed_seed|6|0|sgt|A dwarf weed seed - plant in a herb patch.
            torstol_seed|70|0|sgt|A torstol seed - plant in a herb patch.
            potato_seed|2|0|sgt|A potato seed - plant in an allotment.
            onion_seed|3|0|sgt|An onion seed - plant in an allotment.
            cabbage_seed|3|0|sgt|A cabbage seed - plant in an allotment.
            tomato_seed|4|0|sgt|A tomato seed - plant in an allotment.
            sweetcorn_seed|8|0|sgt|A sweetcorn seed - plant in an allotment.
            strawberry_seed|18|0|sgt|A strawberry seed - plant in an allotment.
            watermelon_seed|56|0|sgt|A watermelon seed - plant in an allotment.
            snape_grass_seed|94|0|sgt|A snape grass seed - plant in an allotment.
            potato|-1|0.5|gt|This could be used to make a good stew.
            onion|3|0.15|gt|A strong smelling onion.
            cabbage|1|0.453|gt|Yuck I don't like cabbage.
            tomato|4|0.08|gt|This would make good ketchup.
            sweetcorn|-1|0.028|gt|Raw sweetcorn.
            strawberry|17|0.028|gt|A freshly picked strawberry.
            watermelon|-1|0.113|gt|A juicy watermelon.
            snape_grass|10|0.007|gt|Strange spiky grass.
            giant_key|-1|0|s|
            mossy_key|-1|0|s|
            hill_giant_club|60000|3.628|gt|A brutal weapon popular among giants.
            bryophytas_essence|7000|0.001|gt|Ready to infuse a Battlestaff with the power of Nature.
            ahrims_hood|13000|0.907|gt|Ahrim the Blighted's leather hood.
            ahrims_robetop|50000|4.535|gt|Ahrim the Blighted's armoured robe top.
            ahrims_robeskirt|47000|11.339|gt|Ahrim the Blighted's armoured robe skirt.
            ahrims_staff|85000|2.267|gt|Ahrim the Blighted's quarterstaff.
            dharoks_helm|103000|1.814|gt|Dharok the Wretched's helm.
            dharoks_platebody|280000|9.979|gt|Dharok the Wretched's platebody armour.
            dharoks_platelegs|275000|9.071|gt|Dharok the Wretched's plate leg armour.
            dharoks_greataxe|-1|3.175|gt|Dharok the Wretched's greataxe.
            guthans_helm|103000|2.721|gt|Guthan the Infested's helm.
            guthans_platebody|280000|9.979|gt|Guthan the Infested's platebody armour.
            guthans_chainskirt|275000|8.164|gt|Guthan the Infested's chainskirt.
            guthans_warspear|100000|2.267|gt|Guthan the Infested's warspear.
            karils_coif|13000|0.907|gt|Karil the Tainted's coif.
            karils_leathertop|50000|6.803|gt|Karil the Tainted's leather body armour.
            karils_leatherskirt|47000|5.443|gt|Karil the Tainted's leather skirt.
            karils_crossbow|160000|3.628|gt|Karil the Tainted's repeating crossbow.
            torags_helm|103000|2.721|gt|Torag the Corrupted's helm.
            torags_platebody|280000|9.979|gt|Torag the Corrupted's platebody armour.
            torags_platelegs|275000|9.071|gt|Torag the Corrupted's plate leg armour.
            torags_hammers|160000|3.628|gt|Torag the Corrupted's twin hammers.
            veracs_helm|103000|1.36|gt|Verac the Defiled's helm.
            veracs_brassard|280000|4.989|gt|Verac the Defiled's brassard.
            veracs_plateskirt|275000|4.535|gt|Verac the Defiled's plate skirt.
            veracs_flail|160000|2.267|gt|Verac the Defiled's flail.
            bandos_chestplate|290010|12|gt|A sturdy chestplate.
            bandos_tassets|289910|8|gt|A sturdy pair of tassets.
            bandos_boots|289010|6|gt|Some sturdy boots.
            zamorakian_spear|100005|3|gt|An evil spear.
            steam_battlestaff|17000|2.267|gt|It's a slightly magical stick.
            staff_of_the_dead|1000005|1.5|gt|A ghastly weapon with evil origins.
            fire_cape|65007|1.814||A cape of fire.
            tokkul|-1|0|s|It's a token of some kind made from obsidian.
            tzrek_jad|-1|0.015||
            pot_of_flour|-1|1.36|gt|There is flour in this pot.
            ghostspeak_amulet|-1|0.01||It lets me talk to ghosts.
            ghosts_skull|-1|0.08||Ooooh spooky!
            black_bead|4|0.003|gt|A small round black bead.
            red_bead|4|0.003|gt|A small round red bead.
            yellow_bead|4|0.003|gt|A small round yellow bead.
            white_bead|4|0.003|gt|A small round white bead.
            rats_tail|3|0.003||A bit of rat.
            cadava_berries|1|0.007|gt|Poisonous berries.
            cadava_potion|1|0.025||I'm meant to give this to Juliet.
            message_from_juliet|-1|0.01||A crumpled note.
            research_package|30|0.907||This contains some vital research.
            research_notes|-1|0.028||They seem to be written in some kind of code.
            air_talisman|-1|0.015|gt|A mysterious power emanates from the talisman...
            silverlights_key|100|1||The Wizard Traiborn gave me this key to Silverlight's case (Used in the Demon Slayer quest).
            stake|8|0.085||A very pointy stick.
            garlic|3|0.028|gt|A clove of garlic.
            beer|-1|0.55|gt|A glass of frothy ale.
            silverlight|-1|1.814||The magical sword 'Silverlight'.
            amulet_of_accuracy|-1|0.01|gt|It increases my aim.
            dragon_med_helm|-1|1.36|gt|Makes the wearer pretty intimidating.
            dragon_plateskirt|-1|9.071|gt|This looks pretty heavy.
            dragon_sq_shield|-1|3.175|gt|An ancient and powerful looking Dragon Square shield.
            dragon_mace|-1|1.814|gt|A spiky mace.
            dragon_spear|-1|2.267|gt|A dragon tipped spear.
            dragon_halberd|250000|3.175|gt|A dragon halberd.
            ferret|40|1.5||It likes ferreting around.
            grey_chinchompa|140|0|sgt|Handle with care.
            red_chinchompa|160|0|sgt|Even more volatile than its vegetarian counterpart.
            black_chinchompa|240|0|sgt|Just plain nasty.
            green_dragonhide|135|3.175|gt|The scaly rough hide from a Green Dragon.
            blue_dragonhide|140|3.175|gt|The scaly rough hide from a Blue Dragon.
            red_dragonhide|145|3.175|gt|The scaly rough hide from a Red Dragon.
            black_dragonhide|150|3.175|gt|The scaly rough hide from a Black Dragon.
            anti_dragon_shield|-1|5.443|gt|
            antifire_potion|330|0.035|gt|4 doses of anti-firebreath potion.
            extended_antifire|440|0.035|gt|4 doses of extended anti-firebreath potion.
            abyssal_whip|-1|0.453|gt|A weapon from the Abyss.
            clue_scroll_beginner|20|0.02||
            clue_scroll_easy|20|0.02||
            clue_scroll_medium|20|0.02||
            clue_scroll_hard|20|0.02||
            clue_scroll_elite|20|0.02||
            clue_scroll_master|20|0.02||
            raw_mackerel|15|0.35|gt|I should try cooking this.
            mackerel|15|0.3|gt|Some nicely cooked mackerel.
            raw_cod|10|0.45|gt|I should try cooking this.
            cod|10|0.4|gt|Some nicely cooked cod.
            raw_slimy_eel|1|2|gt|A slime covered eel - yuck!
            raw_cave_eel|15|0.5|gt|It's incredibly slimy.
            cave_eel|15|0.5|gt|It's a bit slimy.
            raw_bass|40|0.4|gt|I should try cooking this.
            bass|40|0.35|gt|Wow, this is a big fish.
            raw_lava_eel|-1|0.226||A very strange eel.
            lava_eel|-1|0.198||Strange, it looks cooler now it's been cooked.
            raw_karambwan|200|0.7|gt|A raw green octopus.
            cooked_karambwan|250|0.65|gt|Cooked octopus. It looks very nutritious.
            raw_sea_turtle|200|0.4|gt|A rare catch.
            sea_turtle|200|0.35|gt|Tasty!
            raw_manta_ray|200|0.45|gt|A rare catch.
            manta_ray|200|0.4|gt|A rare catch.
            raw_giant_krill|170|0.7|gt|I should try cooking this.
            giant_krill|170|0.65|gt|Some nicely cooked krill.
            raw_haddock|170|0.7|gt|I should try cooking this.
            haddock|170|0.65|gt|Some nicely cooked haddock.
            raw_yellowfin|170|0.7|gt|I should try cooking this.
            yellowfin|170|0.65|gt|Some nicely cooked yellowfin.
            raw_halibut|170|0.7|gt|I should try cooking this.
            halibut|170|0.65|gt|Some nicely cooked halibut.
            raw_bluefin|170|0.7|gt|I should try cooking this.
            bluefin|170|0.65|gt|Some nicely cooked bluefin.
            raw_marlin|170|0.7|gt|I should try cooking this.
            marlin|170|0.65|gt|Some nicely cooked marlin.
            jug|-1|0.453|gt|This jug is empty.
            jug_of_water|-1|1|gt|It's full of water.
            empty_pot|-1|0.453|gt|This pot is empty.
            bowl_of_water|4|0.907|gt|It's a bowl of water.
            cake_tin|-1|0.1|gt|Useful for baking cakes.
            empty_cup|-1|0.05|gt|An empty cup.
            cup_of_hot_water|10|0.05|gt|It's hot!
            cup_of_tea|-1|0.15|gt|A nice cup of tea.
            grapes|-1|0.25|gt|Good grapes for wine making.
            unfermented_wine|10|1.5||
            jug_of_wine|-1|1.5|gt|It's full of wine.
            jug_of_bad_wine|-1|1.5||Oh dear, this wine is terrible!
            seaweed|-1|0.2|gt|Slightly damp seaweed.
            soda_ash|-1|0.056|gt|One of the ingredients for making glass.
            sinew|2|0.002|gt|I can use this to make a crossbow string.
            chocolate_bar|-1|0.15|gt|Mmmmmmm chocolate.
            pineapple_ring|-1|0.03|gt|Exotic fruit.
            pot_of_cream|2|0.2|gt|Fresh cream.
            pat_of_butter|4|0.2|gt|A pat of freshly churned butter.
            cheese|-1|0.2|gt|It's got holes in it.
            burnt_bread|-1|0.35|t|Nice crispy bread. Possibly too crispy.
            burnt_pie|-1|0.35|t|I think I left it on the stove too long.
            burnt_pizza|-1|0.7|t|Oh dear!
            burnt_cake|-1|0.15|t|Argh what a mess!
            burnt_stew|-1|1.3|t|Eew, it's horribly burnt.
            bread_dough|4|0.6|gt|Some uncooked dough.
            pastry_dough|-1|0.17|gt|Potentially pastry.
            pizza_base|-1|0.1|gt|I need to add some tomato next.
            bread|-1|0.5|gt|Nice crispy bread.
            pie_shell|4|0.25|gt|I need to find a filling for this pie.
            uncooked_redberry_pie|6|0.5|gt|This would be much more appetising cooked.
            redberry_pie|12|0.45|gt|Looks tasty.
            half_a_redberry_pie|6|0.225|t|So tasty I kept some for later.
            uncooked_meat_pie|8|0.5|gt|This would be much healthier cooked.
            meat_pie|15|0.45|gt|Not for vegetarians.
            half_a_meat_pie|8|0.225|t|Half of it is suitable for vegetarians.
            uncooked_apple_pie|16|0.5|gt|This would be much tastier cooked.
            apple_pie|30|0.45|gt|Mmm Apple pie.
            half_a_apple_pie|15|0.225|t|Mmm half an apple pie.
            uncooked_garden_pie|12|0.001|gt|Needs cooking before I eat it.
            garden_pie|24|0.001|gt|What I wouldn't give for a good steak about now...
            half_a_garden_pie|12|0.001|t|What I wouldn't give for a good steak about now...
            uncooked_fish_pie|50|0.001|gt|Raw fish is risky, better cook it.
            fish_pie|100|0.001|gt|Bounty of the sea.
            half_a_fish_pie|50|0.001|t|Bounty of the sea.
            uncooked_mushroom_pie|16|0.5|gt|This would be much tastier cooked.
            mushroom_pie|30|0.45|gt|Mmm mushroom pie.
            half_a_mushroom_pie|15|0.225|t|Mmm half a mushroom pie.
            uncooked_admiral_pie|155|0.001|gt|This would taste a lot better cooked.
            admiral_pie|310|0.001|gt|Much tastier than a normal fish pie.
            half_a_admiral_pie|155|0.001|t|Much tastier than a normal fish pie.
            uncooked_dragonfruit_pie|40|0.5|gt|This would be much tastier cooked.
            dragonfruit_pie|70|0.45|gt|Mmm dragonfruit pie.
            half_a_dragonfruit_pie|35|0.225|t|Mmm half a dragonfruit pie.
            uncooked_wild_pie|91|0.001|gt|Good as it looks, I'd better cook it.
            wild_pie|182|0.001|gt|A triumph of man over nature.
            half_a_wild_pie|91|0.001|t|A triumph of man over nature.
            uncooked_summer_pie|70|0.001|gt|Fresh fruit may be good for you, but I should really cook this.
            summer_pie|140|0.001|gt|All the fruits of a very small forest.
            half_a_summer_pie|70|0.001|t|All the fruits of a very small forest.
            incomplete_pizza|10|0.6|t|I need to add some cheese next.
            uncooked_pizza|25|0.8|t|This needs cooking.
            plain_pizza|40|0.8|gt|A cheese and tomato pizza.
            half_plain_pizza|20|0.4|t|Half of this plain pizza has been eaten.
            meat_pizza|50|0.9|gt|A pizza with bits of meat on it.
            half_meat_pizza|25|0.45|t|Half of this meat pizza has been eaten.
            anchovy_pizza|60|0.9|gt|A pizza with anchovies.
            half_anchovy_pizza|30|0.45|t|Half of this anchovy pizza has been eaten.
            pineapple_pizza|100|0.9|gt|A tropicana pizza.
            half_pineapple_pizza|50|0.45|t|Half of this pineapple pizza has been eaten.
            incomplete_stew|4|1|t|I need to add some meat too.
            uncooked_stew|10|1.5|t|I need to cook this.
            uncooked_curry|-1|1.5|t|I need to cook this.
            stew|-1|1.5|gt|It's a meat and potato stew.
            curry|20|1.5|gt|It's a spicy hot curry.
            uncooked_cake|20|0.5|t|Now all I need to do is cook it.
            chocolate_cake|-1|0.3|gt|This looks very tasty.
            two_thirds_chocolate_cake|-1|0.2|t|Someone has eaten a big chunk of this cake.
            potato_with_butter|8|0.5|gt|A baked potato with butter.
            potato_with_cheese|8|0.5|gt|A baked potato with butter and cheese.
            tuna_and_corn|113|0.5|gt|A bowl of cooked tuna and sweetcorn.
            tuna_potato|113|0.5|gt|A baked potato with tuna and sweetcorn.
            roe|20|0.05|gt|Roe, or cheap fishy eggs.
            caviar|50|0.05|gt|Caviar, or expensive fishy eggs.
            cooking_gauntlets|-1|0.226||
            bracelet_mould|-1|0.453|gt|Used to make bracelets.
            holy_mould|-1|0.453|gt|Used to make holy symbols of Saradomin.
            unholy_mould|200|0.453||Used to make unholy symbols.
            tiara_mould|100|1|gt|A mould for tiaras.
            sickle_mould|10|0.453|gt|Used to make sickles.
            bolt_mould|25|0.453|gt|A mould for creating silver crossbow bolts.
            glassblowing_pipe|-1|0.056|gt|Used to form molten glass into useful items.
            hard_leather|1|3.175|gt|It's a piece of hard leather.
            snake_hide|35|0.5|gt|Scaly but not slimy! It could be a useful material if it were tanned.
            snakeskin|50|0.5|gt|Nicely tanned skin from a snake.
            green_dragon_leather|50|3.175|gt|It's a piece of prepared green dragonhide.
            blue_dragon_leather|70|3.175|gt|It's a piece of prepared blue dragonhide.
            red_dragon_leather|90|3.175|gt|It's a piece of prepared red dragonhide.
            black_dragon_leather|110|3.175|gt|It's a piece of prepared black dragonhide.
            leather_gloves|-1|0.226|gt|
            leather_vambraces|-1|0.226|gt|Better than no armour!
            coif|-1|0.907|gt|Light weight head protection.
            hard_leather_body|-1|3.628|gt|Harder than normal leather.
            studded_body|-1|5.443|gt|Those studs should provide a bit more protection.
            studded_chaps|-1|4.535|gt|Those studs should provide a bit more protection.
            snakeskin_vambraces|434|0.283|gt|Made from 100% real snake.
            snakeskin_boots|250|0.34|gt|Made from snakes.
            snakeskin_bandana|300|0.907|gt|Lightweight head protection.
            snakeskin_chaps|1175|3.628|gt|Made from 100% real snake.
            snakeskin_body|1250|6.803|gt|Made from 100% real snakeskin.
            green_dhide_vambraces|2500|0.283|gt|
            green_dhide_body|7800|6.803|gt|Made from 100% real dragonhide.
            green_dhide_chaps|3900|5.443|gt|Made from 100% real dragonhide.
            green_dhide_shield|5500|8|gt|A solid maple shield covered in green dragon leather.
            blue_dhide_vambraces|3000|0.283|gt|
            blue_dhide_body|9360|6.803|gt|Made from 100% real dragonhide.
            blue_dhide_chaps|4320|5.443|gt|Made from 100% real dragonhide.
            blue_dhide_shield|7500|8|gt|A solid yew shield covered in blue dragon leather.
            red_dhide_vambraces|3600|0.283|gt|
            red_dhide_body|11230|6.803|gt|Made from 100% real dragonhide.
            red_dhide_chaps|5180|5.443|gt|Made from 100% real dragonhide.
            red_dhide_shield|11000|8|gt|A solid magic wood shield covered in red dragon leather.
            black_dhide_vambraces|4320|0.283|gt|
            black_dhide_body|13480|6.803|gt|Made from 100% real dragonhide.
            black_dhide_chaps|6220|5.443|gt|Made from 100% real dragonhide.
            black_dhide_shield|28334|8|gt|A solid redwood shield covered in black dragon leather.
            battlestaff|-1|2.267|gt|It's a slightly magical stick.
            unpowered_orb|-1|0.453|gt|I'd prefer it if it was powered.
            water_orb|-1|0.453|gt|A magic glowing orb.
            water_battlestaff|-1|2.267|gt|It's a slightly magical stick.
            earth_orb|-1|0.453|gt|A magic glowing orb.
            earth_battlestaff|-1|2.267|gt|It's a slightly magical stick.
            fire_orb|-1|0.453|gt|A magic glowing orb.
            fire_battlestaff|-1|2.267|gt|It's a slightly magical stick.
            air_orb|-1|0.453|gt|A magic glowing orb.
            air_battlestaff|-1|2.267|gt|It's a slightly magical stick.
            soft_clay|-1|0.907|gt|Clay soft enough to mould.
            unfired_pot|-1|0.68|gt|I need to put this in a pottery oven.
            unfired_pie_dish|3|0.68|gt|I need to put this in a pottery oven.
            pie_dish|-1|0.1|gt|Deceptively pie shaped.
            unfired_bowl|2|0.68|gt|I need to put this in a pottery oven.
            bowl|4|0.453|gt|Useful for mixing things.
            unfired_plant_pot|-1|0.907|gt|An unfired plant pot.
            plant_pot|1|0.907|t|A plant pot filled with soil.
            unfired_pot_lid|10|0.198|gt|This needs firing, then it should fit on a normal-sized pot.
            pot_lid|15|0.198|gt|This should fit on a normal-sized pot.
            filled_plant_pot|1|0.907|gt|A plant pot filled with soil.
            molten_glass|3|0.907|gt|Hot glass ready to be blown into useful objects.
            beer_glass|2|0.05|gt|I need to fill this with beer.
            empty_candle_lantern|-1|0.907|gt|Put a candle in to complete it.
            empty_oil_lamp|25|0.453|gt|An oil lamp with no oil in it.
            empty_fishbowl|1|1.36|gt|An empty fishbowl.
            lantern_lens|70|0.028|gt|A roughly circular disc of glass.
            empty_light_orb|350|0.001|gt|A component of cave goblin Magic.
            gold_bracelet|550|0.25|gt|I wonder if this is valuable.
            sapphire_necklace|1050|0.01|gt|I wonder if this is valuable.
            sapphire_bracelet|1150|0.25|gt|I wonder if this is valuable.
            sapphire_amulet_u|900|0.004|gt|It needs a string so I can wear it.
            sapphire_amulet|900|0.007|gt|I wonder if I can get this enchanted.
            emerald_necklace|1425|0.01|gt|I wonder if this is valuable.
            emerald_bracelet|1525|0.25|gt|I wonder if this is valuable.
            emerald_amulet_u|1275|0.004|gt|It needs a string so I can wear it.
            emerald_amulet|1275|0.007|gt|I wonder if I can get this enchanted.
            ruby_necklace|2175|0.01|gt|I wonder if this is valuable.
            ruby_bracelet|2325|0.25|gt|I wonder if this is valuable.
            ruby_amulet_u|2025|0.004|gt|It needs a string so I can wear it.
            ruby_amulet|2025|0.007|gt|I wonder if I can get this enchanted.
            diamond_necklace|3675|0.01|gt|I wonder if this is valuable.
            diamond_bracelet|3825|0.25|gt|I wonder if this is valuable.
            diamond_amulet_u|3525|0.004|gt|It needs a string so I can wear it.
            diamond_amulet|3525|0.007|gt|I wonder if I can get this enchanted.
            dragonstone_ring|17625|0.006|gt|A valuable ring.
            dragonstone_necklace|18375|0.01|gt|I wonder if this is valuable.
            dragonstone_bracelet|19125|0.25|gt|I wonder if this is valuable.
            dragonstone_amulet_u|17625|0.004|gt|It needs a string so I can wear it.
            dragonstone_amulet|17625|0.007|gt|I wonder if I can get this enchanted.
            onyx_ring|201000|0.006|gt|A valuable ring.
            onyx_necklace|201000|0.01|gt|I wonder if this is valuable.
            onyx_bracelet|201000|0.25|gt|Yep, this is most definitely valuable!
            onyx_amulet_u|201000|0.004|gt|It needs a string so I can wear it.
            onyx_amulet|201000|0.007|gt|I wonder if I can get this enchanted.
            zenyte_ring|201000|0.006|gt|A fiery glow emanates from this ring.
            zenyte_necklace|202000|0.01|gt|A fiery glow emanates from this necklace.
            zenyte_bracelet|201000|0.25|gt|A fiery glow emanates from this bracelet.
            zenyte_amulet_u|75000|0.004|gt|It needs a string so I can wear it.
            zenyte_amulet|202000|0.01|gt|A fiery glow emanates from this amulet.
            opal_ring|1050|0.006|gt|A valuable ring.
            opal_necklace|1050|0.01|gt|I wonder if this is valuable.
            opal_bracelet|1150|0.25|gt|I wonder if this is valuable.
            opal_amulet_u|900|0.004|gt|It needs a string so I can wear it.
            opal_amulet|900|0.007|gt|I wonder if I can get this enchanted.
            jade_ring|1275|0.006|gt|A valuable ring.
            jade_necklace|1275|0.01|gt|I wonder if this is valuable.
            jade_bracelet|1300|0.25|gt|I wonder if this is valuable.
            jade_amulet_u|1050|0.004|gt|It needs a string so I can wear it.
            jade_amulet|1050|0.007|gt|I wonder if I can get this enchanted.
            topaz_ring|1425|0.006|gt|A valuable ring.
            topaz_necklace|1425|0.01|gt|I wonder if this is valuable.
            topaz_bracelet|1500|0.25|gt|I wonder if this is valuable.
            topaz_amulet_u|1275|0.004|gt|It needs a string so I can wear it.
            topaz_amulet|1250|0.007|gt|I wonder if I can get this enchanted.
            unstrung_symbol|200|0.004|gt|It needs a string so I can wear it.
            unblessed_symbol|-1|0.007|gt|A symbol of Saradomin.
            unstrung_emblem|200|0.004|gt|It needs a string so I can wear it.
            unpowered_symbol|-1|0.007|gt|An unblessed symbol of Zamorak.
            tiara|-1|1|gt|Makes me feel like a Princess.
            silver_sickle|-1|1.587|gt|It's a silver sickle.
            crossbow_string|1|0.002|gt|A string for a crossbow.
            rope|-1|1.36|gt|A coil of rope.
            bolt_of_cloth|500|1|gt|A bolt of ordinary cloth.
            clockwork|500|1|gt|A clockwork mechanism.
            bird_house|5|0.453|gt|Feed and catch the birds by placing this in the right spot.
            oak_bird_house|5|0.453|gt|Feed and catch the birds by placing this in the right spot.
            willow_bird_house|5|0.453|gt|Feed and catch the birds by placing this in the right spot.
            teak_bird_house|5|0.453|gt|Feed and catch the birds by placing this in the right spot.
            maple_bird_house|5|0.453|gt|Feed and catch the birds by placing this in the right spot.
            mahogany_bird_house|5|0.453|gt|Feed and catch the birds by placing this in the right spot.
            yew_bird_house|5|0.453|gt|Feed and catch the birds by placing this in the right spot.
            magic_bird_house|5|0.453|gt|Feed and catch the birds by placing this in the right spot.
            redwood_bird_house|5|0.453|gt|Feed and catch the birds by placing this in the right spot.
            bronze_med_helm|24|1.814|gt|A medium sized helmet.
            bronze_sword|26|1.814|gt|A razor sharp sword.
            bronze_mace|18|1.814|gt|A spiky mace.
            bronze_sq_shield|48|3.628|gt|A medium square shield.
            bronze_chainbody|60|6.803|gt|A series of connected metal rings.
            bronze_claws|15|0.907|gt|A set of fighting claws.
            bronze_plateskirt|80|8.164|gt|Designer leg protection.
            iron_med_helm|84|1.814|gt|A medium sized helmet.
            iron_sword|91|1.814|gt|A razor sharp sword.
            iron_mace|63|1.814|gt|A spiky mace.
            """);
        load("""
            iron_sq_shield|168|3.628|gt|A medium square shield.
            iron_chainbody|210|6.803|gt|A series of connected metal rings.
            iron_claws|50|0.907|gt|A set of fighting claws.
            iron_plateskirt|280|8.164|gt|Designer leg protection.
            steel_med_helm|300|1.814|gt|A medium sized helmet.
            steel_sword|325|1.814|gt|A razor sharp sword.
            steel_mace|225|1.814|gt|A spiky mace.
            steel_sq_shield|600|3.628|gt|A medium square shield.
            steel_chainbody|750|6.803|gt|A series of connected metal rings.
            steel_claws|175|0.907|gt|A set of fighting claws.
            steel_plateskirt|1000|8.164|gt|Designer leg protection.
            black_med_helm|576|1.814|gt|A medium sized helmet.
            black_sword|624|1.814|gt|A razor sharp sword.
            black_mace|432|1.814|gt|A spiky mace.
            black_sq_shield|1152|3.628|gt|A medium square shield.
            black_chainbody|1440|6.803|gt|A series of connected metal rings.
            black_claws|360|0.907|gt|A set of fighting claws.
            black_plateskirt|1920|8.164|gt|Big, black and heavy looking.
            mithril_med_helm|780|1.36|gt|A medium sized helmet.
            mithril_sword|845|1.587|gt|A razor sharp sword.
            mithril_mace|585|1.587|gt|A spiky mace.
            mithril_sq_shield|1560|3.175|gt|A medium square shield.
            mithril_chainbody|1950|5.896|gt|A series of connected metal rings.
            mithril_claws|475|0.907|gt|A set of fighting claws.
            mithril_plateskirt|2600|7.257|gt|Designer leg protection.
            adamant_med_helm|1920|2.267|gt|A medium sized helmet.
            adamant_sword|2080|2.041|gt|A razor sharp sword.
            adamant_mace|1440|2.041|gt|A spiky mace.
            adamant_sq_shield|3840|4.082|gt|A medium square shield.
            adamant_chainbody|4800|7.711|gt|A series of connected metal rings.
            adamant_claws|1200|0.907|gt|A set of fighting claws.
            adamant_plateskirt|6400|9.071|gt|Designer leg protection.
            rune_med_helm|19200|1.814|gt|A medium sized helmet.
            rune_sword|20800|1.814|gt|A razor sharp sword.
            rune_mace|14400|1.814|gt|A spiky mace.
            rune_sq_shield|38400|3.628|gt|A medium square shield.
            rune_chainbody|50000|6.803|gt|A series of connected metal rings.
            rune_claws|12000|0.907|gt|A set of fighting claws.
            rune_plateskirt|64000|8.164|gt|Designer leg protection.
            bronze_bolts_unf|-1|0|sgt|Unfeathered bronze crossbow bolts.
            bronze_dart_tip|-1|0|sgt|A deadly looking dart tip made of bronze - needs feathers for flight.
            bronze_nails|2|0|sgt|Keeps things in place fairly permanently.
            bronze_javelin_heads|25|0|sgt|Needs a shaft.
            bronze_knife|1|0|sgt|
            bronze_limbs|20|1|gt|A pair of bronze crossbow limbs.
            iron_bolts_unf|1|0|sgt|Unfeathered iron crossbow bolts.
            iron_dart_tip|-1|0|sgt|A deadly looking dart tip made of iron - needs feathers for flight.
            iron_nails|4|0|sgt|Keeps things in place fairly permanently.
            iron_javelin_heads|35|0|sgt|Needs a shaft.
            iron_knife|3|0|sgt|
            iron_limbs|70|1|gt|A pair of iron crossbow limbs.
            steel_bolts_unf|1|0|sgt|Unfeathered steel crossbow bolts.
            steel_dart_tip|5|0|sgt|A deadly-looking dart tip made of steel - needs feathers for flight.
            steel_nails|3|0|sgt|Keeps things in place fairly permanently.
            steel_javelin_heads|90|0|sgt|Needs a shaft.
            steel_knife|11|0|sgt|
            steel_limbs|250|2|gt|A pair of steel crossbow limbs.
            mithril_bolts_unf|1|0|sgt|Unfeathered mithril crossbow bolts.
            mithril_dart_tip|12|0|sgt|A deadly looking dart tip made of mithril - needs feathers for flight.
            mithril_nails|18|0|sgt|Keeps things in place fairly permanently.
            mithril_javelin_heads|165|0|sgt|Needs a shaft.
            mithril_knife|27|0|sgt|
            mithril_limbs|650|2|gt|A pair of mithril crossbow limbs.
            adamant_bolts_unf|1|0|sgt|Unfeathered adamantite crossbow bolts.
            adamant_dart_tip|36|0|sgt|A deadly-looking dart tip made of adamant - needs feathers for flight.
            adamant_nails|45|0|sgt|Keeps things in place fairly permanently.
            adamant_javelin_heads|440|0|sgt|Needs a shaft.
            adamant_knife|66|0|sgt|
            adamant_limbs|1600|3|gt|A pair of adamantite crossbow limbs.
            rune_bolts_unf|1|0|sgt|Unfeathered runite crossbow bolts.
            rune_dart_tip|175|0|sgt|A deadly-looking dart tip made of runite - needs feathers for flight.
            rune_nails|220|0|sgt|Keeps things in place fairly permanently.
            rune_javelin_heads|1350|0|sgt|Needs a shaft.
            rune_knife|167|0|sgt|
            rune_limbs|16000|3|gt|A pair of runite crossbow limbs.
            steel_studs|150|1.36|gt|A set of studs for leather armour.
            cannonball|-1|0|sgt|
            ammo_mould|-1|4.535||Used to make cannon ammunition.
            blurite_ore|3|2.267||Definitely blue.
            blurite_bar|18|1.814||It's a bar of blurite.
            blurite_bolts_unf|1|0|s|Unfeathered blurite crossbow bolts.
            blurite_limbs|33|1||A pair of blurite crossbow limbs.
            wooden_stock|20|3|gt|A wooden crossbow stock.
            oak_stock|27|3|gt|An oak crossbow stock.
            willow_stock|53|3|gt|A willow crossbow stock.
            teak_stock|77|3|gt|A teak crossbow stock.
            maple_stock|100|3|gt|A maple crossbow stock.
            mahogany_stock|133|3|gt|A mahogany crossbow stock.
            yew_stock|167|3|gt|A yew crossbow stock.
            magic_stock|280|3|gt|A magic crossbow stock.
            bronze_crossbow_u|40|8|gt|An unstrung bronze crossbow.
            bronze_crossbow|73|4|gt|A bronze crossbow.
            blurite_crossbow_u|60|8||An unstrung blurite crossbow.
            blurite_crossbow|93|4||A blurite crossbow.
            iron_crossbow_u|123|8|gt|An unstrung iron crossbow.
            iron_crossbow|157|4|gt|An iron crossbow.
            steel_crossbow_u|327|8|gt|An unstrung steel crossbow.
            steel_crossbow|360|5|gt|A steel crossbow.
            mithril_crossbow_u|750|8|gt|An unstrung mithril crossbow.
            mithril_crossbow|783|5|gt|A mithril crossbow.
            adamant_crossbow_u|1733|8|gt|An unstrung adamantite crossbow.
            adamant_crossbow|1767|6|gt|An adamantite crossbow.
            rune_crossbow_u|16167|8|gt|An unstrung runite crossbow.
            rune_crossbow|16200|6|gt|A runite crossbow.
            dragon_crossbow_u|30000|8|gt|An unstrung dragon crossbow.
            dragon_crossbow|54000|6|gt|A dragon crossbow.
            bronze_bolts|-1|0|sgt|Bronze crossbow bolts.
            blurite_bolts|2|0|s|Blurite crossbow bolts.
            iron_bolts|2|0|sgt|Iron crossbow bolts.
            silver_bolts_unf|1|0|sgt|Unfeathered silver crossbow bolts.
            silver_bolts|5|0|sgt|Silver crossbow bolts.
            steel_bolts|8|0|sgt|Steel crossbow bolts.
            mithril_bolts|-1|0|sgt|Mithril crossbow bolts.
            adamant_bolts|58|0|sgt|Adamantite crossbow bolts.
            rune_bolts|300|0|sgt|Runite crossbow bolts.
            opal_bolt_tips|7|0|sgt|Opal bolt tips.
            opal_bolts|7|0|sgt|Opal tipped Bronze crossbow bolts.
            jade_bolt_tips|-1|0|sgt|Jade bolt tips.
            jade_bolts|12|0|s|Jade tipped Blurite crossbow bolts.
            topaz_bolt_tips|13|0|sgt|Red Topaz bolt tips.
            topaz_bolts|22|0|sgt|Topaz tipped Steel crossbow bolts.
            sapphire_bolt_tips|17|0|sgt|Sapphire bolt tips.
            sapphire_bolts|37|0|sgt|Sapphire tipped Mithril crossbow bolts.
            emerald_bolt_tips|33|0|sgt|Emerald bolt tips.
            emerald_bolts|53|0|sgt|Emerald tipped Mithril crossbow bolts.
            ruby_bolt_tips|67|0|sgt|Ruby bolt tips.
            ruby_bolts|125|0|sgt|Ruby tipped Adamantite crossbow bolts.
            diamond_bolt_tips|133|0|sgt|Diamond bolt tips.
            diamond_bolts|192|0|sgt|Diamond tipped Adamantite crossbow bolts.
            dragonstone_bolt_tips|667|0|sgt|Dragonstone bolt tips.
            dragonstone_bolts|967|0|sgt|Dragonstone tipped Runite crossbow bolts.
            onyx_bolt_tips|1000|0|sgt|Onyx bolt tips.
            onyx_bolts|13633|0|sgt|Onyx tipped Runite crossbow bolts.
            unfinished_broad_bolts|55|0|sgt|Add feathers to make broad-tipped crossbow bolts.
            broad_bolts|55|0|sgt|Crossbow bolts with broad tips.
            amethyst_arrowtips|245|0|sgt|I can make some arrows with these.
            amethyst_arrow|490|0|sgt|Arrows with amethyst heads.
            amethyst_bolt_tips|160|0|sgt|Amethyst bolt tips.
            amethyst_javelin_heads|1520|0|sgt|Needs a shaft.
            amethyst_dart_tip|200|0|sgt|A deadly-looking dart tip made of amethyst - needs feathers for flight.
            dragon_arrowtips|500|0|sgt|Dragon talons, usable as arrowheads.
            dragon_arrow|-1|0|sgt|An arrow made using a dragon's talon.
            bronze_dart|1|0|sgt|A deadly throwing dart with a bronze tip.
            iron_dart|2|0|sgt|A deadly throwing dart with an iron tip.
            steel_dart|10|0|sgt|A deadly throwing dart with a steel tip.
            mithril_dart|25|0|sgt|A deadly throwing dart with a mithril tip.
            adamant_dart|65|0|sgt|A deadly throwing dart with an adamant tip.
            rune_dart|350|0|sgt|A deadly throwing dart with a rune tip.
            amethyst_dart|400|0|sgt|A deadly throwing dart with an amethyst tip.
            javelin_shaft|-1|0|sgt|It's not very strong, but it could be pointy if you put a pointy tip on it.
            bronze_javelin|4|0|sgt|A bronze tipped javelin.
            iron_javelin|6|0|sgt|An iron tipped javelin.
            steel_javelin|24|0|sgt|A steel tipped javelin.
            mithril_javelin|64|0|sgt|A mithril tipped javelin.
            adamant_javelin|160|0|sgt|An adamant tipped javelin.
            rune_javelin|400|0|sgt|A rune tipped javelin.
            amethyst_javelin|420|0|sgt|An amethyst tipped javelin.
            oak_shield|80|3.175|gt|A solid oaken shield.
            willow_shield|160|3.175|gt|A solid willow shield.
            maple_shield|320|3.175|gt|A solid maple shield.
            yew_shield|640|3.175|gt|A solid yew shield.
            magic_shield|1280|3.175|gt|A solid magic wood shield.
            redwood_shield|1280|3.175|gt|A solid redwood shield.
            celastrus_bark|450|1|gt|A length of celastrus wood, ready for fletching.
            pestle_and_mortar|-1|0.056|gt|I can grind things for potions in this.
            mort_myre_fungus|1|0.01|gt|A mushroom from the swamps of Mort Myre.
            amylase_crystal|20|0|sgt|Good for enhancing a super energy potion.
            goat_horn|12|0.453|gt|Not much good for blowing.
            goat_horn_dust|12|0.453|gt|Finely ground goat horn.
            toads_legs|2|0.15|gt|They're a gnome delicacy apparently.
            kebbit_teeth_dust|160|0.02|gt|Previously a kebbit-sized set of dentures.
            yew_roots|1|0.001|gt|The roots of the Yew tree.
            magic_roots|1|0.001|gt|The roots of the Magic tree.
            oak_roots|1|0.001|gt|The roots of the Oak tree.
            willow_roots|1|0.001|gt|The roots of the Willow tree.
            maple_roots|1|0.001|gt|The roots of the Maple tree.
            coconut_milk|1|0.035|gt|A vial filled with coconut milk
            blue_dragon_scale|-1|0.01|gt|A large shiny scale.
            dragon_scale_dust|52|0.007|gt|Finely ground scale of Dragon.
            lava_scale_shard|20|0|sgt|Ground-up lava dragon scale.
            crushed_superior_dragon_bones|50|0.007|gt|Ground up superior dragon bones.
            crystal_dust|1000|0|s|Crystal crushed down to the finest form imaginable, useful for making potions.
            cactus_spine|1|0.028|gt|Don't prick yourself with this.
            nightshade|30|0.015||Deadly.
            poison_ivy_berries|65|0.007|gt|They look sweet and juicy, but only a fool would eat them.
            jangerberries|1|0.007|gt|They don't look very ripe.
            volcanic_ash|200|0|sgt|One of the ingredients for making ultra compost.
            vial_of_blood|100|0.1|gt|A glass vial containing blood.
            antidote_plus_unf|14|0.035||A vial of coconut milk and Toadflax.
            antidote_plus_plus_unf|14|0.035||A vial of coconut milk and Irit Leaf.
            cadantine_blood_potion_unf|165|0.056|gt|I need another ingredient to finish this blood potion.
            super_combat_potion|250|0.035|gt|4 doses of super combat potion.
            stamina_potion|400|0.035|gt|4 doses of stamina potion.
            super_energy|-1|0.035|gt|4 doses of super energy potion.
            superantipoison|360|0.035|gt|4 doses of super antipoison potion.
            antidote_plus|360|0.035|gt|4 doses of extra strong antipoison potion.
            antidote_plus_plus|360|0.035|gt|4 doses of super strong antipoison potion.
            anti_venom|440|0.035|gt|4 doses of antivenom potion.
            anti_venom_plus|444|0.035|gt|4 doses of super antivenom potion.
            combat_potion|215|0.035|gt|4 doses of combat potion.
            agility_potion|200|0.035|gt|4 doses of Agility potion.
            fishing_potion|250|0.035|gt|4 doses of Fishing potion.
            hunter_potion|15|0.035|gt|4 doses of Hunter potion.
            zamorak_brew|200|0.035|gt|4 doses of Zamorak brew.
            bastion_potion|360|0.135|gt|4 doses of bastion potion.
            battlemage_potion|360|0.135|gt|4 doses of battlemage potion.
            divine_super_attack|250|0.035|gt|4 doses of divine super attack potion.
            divine_super_strength|250|0.035|gt|4 doses of divine super strength potion.
            divine_super_defence|250|0.035|gt|4 doses of divine super defence potion.
            divine_ranging|250|0.035|gt|4 doses of divine ranging potion.
            divine_magic|250|0.035|gt|4 doses of divine magic potion.
            divine_super_combat|250|0.035|gt|4 doses of divine super combat potion.
            divine_bastion|720|0.135|gt|4 doses of divine bastion potion.
            divine_battlemage|720|0.135|gt|4 doses of divine battlemage potion.
            super_antifire|800|0.035|gt|4 doses of super anti-firebreath potion.
            extended_super_antifire|800|0.035|gt|4 doses of extended super anti-firebreath potion.
            weapon_poison|144|0.025|gt|For use on daggers and projectiles.
            weapon_poison_plus|288|0.035|gt|A vial of extra strong weapon poison, for spears and daggers.
            weapon_poison_plus_plus|432|0.035|gt|A vial of super strong weapon poison, for spears and daggers.
            guthix_rest|50|0.05|gt|A cup of Guthix Rest.
            compost_potion|120|0.035|gt|Pour this on compost to turn it into super-compost.
            spade|-1|1.814|gt|A slightly muddy spade.
            rake|-1|1.36|gt|Use this to clear weeds.
            secateurs|-1|0.453|gt|Good for pruning away diseased leaves.
            gardening_trowel|12|0.056|gt|Not suitable for archaeological digs.
            weeds|-1|0.007|gt|A handful of weeds.
            plant_cure|40|0.035|gt|
            compost|20|3|gt|Good for plants, helps them grow.
            supercompost|85|3|gt|Super-good for the smallest or largest of plants.
            ultracompost|85|3|gt|Ultra-good for the smallest or largest of plants.
            coconut_shell|-1|0.028|t|All the milk has been removed.
            bird_nest|453|0.002||It's a bird's nest with an egg in it.
            bird_nest_seeds|453|0.002||
            bird_nest_ring|453|0.002||
            acorn|6|0|sgt|Plant this in a plantpot of soil to grow a sapling.
            oak_sapling|1|0.907|gt|This sapling is ready to be replanted in a tree patch.
            willow_seed|16|0|sgt|Plant this in a plantpot of soil to grow a sapling.
            willow_sapling|1|0.907|gt|This sapling is ready to be replanted in a tree patch.
            maple_seed|48|0|sgt|Plant this in a plantpot of soil to grow a sapling.
            maple_sapling|1|0.907|gt|This sapling is ready to be replanted in a tree patch.
            yew_seed|143|0|sgt|Plant this in a plantpot of soil to grow a sapling.
            yew_sapling|1|0.907|gt|This sapling is ready to be replanted in a tree patch.
            magic_seed|422|0|sgt|Plant this in a plantpot of soil to grow a sapling.
            magic_sapling|1|0.907|gt|This sapling is ready to be replanted in a tree patch.
            teak_seed|48|0|sgt|Plant this in a plantpot of soil to grow a sapling.
            teak_sapling|1|0.907|gt|This sapling is ready to be replanted in a hardwood tree patch.
            mahogany_seed|48|0|sgt|Plant this in a plantpot of soil to grow a sapling.
            mahogany_sapling|1|0.907|gt|This sapling is ready to be replanted in a tree patch.
            redwood_tree_seed|650|0|sgt|Plant this in a plantpot of soil to grow a sapling.
            redwood_sapling|1|0.907|gt|This sapling is ready to be replanted in a Redwood patch.
            apple_seed|13|0|sgt|Plant in a plantpot of soil to grow a sapling.
            apple_sapling|1|0.907|gt|This sapling is ready to be replanted in a fruit tree patch.
            banana_seed|20|0|sgt|Plant in a plantpot of soil to grow a sapling.
            banana_sapling|1|0.907|gt|This sapling is ready to be replanted in a fruit tree patch.
            orange_seed|31|0|sgt|Plant in a plantpot of soil to grow a sapling.
            orange_sapling|1|0.907|gt|This sapling is ready to be replanted in a fruit tree patch.
            curry_seed|39|0|sgt|Plant in a plantpot of soil to grow a sapling.
            curry_sapling|1|0.907|gt|This sapling is ready to be replanted in a fruit tree patch.
            pineapple_seed|74|0|sgt|Plant in a plantpot of soil to grow a sapling.
            pineapple_sapling|1|0.907|gt|This sapling is ready to be replanted in a fruit tree patch.
            papaya_seed|115|0|sgt|Plant in a plantpot of soil to grow a sapling.
            papaya_sapling|1|0.907|gt|This sapling is ready to be replanted in a fruit tree patch.
            palm_seed|254|0|sgt|Plant in a plantpot of soil to grow a sapling.
            palm_sapling|1|0.907|gt|This sapling is ready to be replanted in a fruit tree patch.
            dragonfruit_seed|430|0|sgt|Plant in a plantpot of soil to grow a sapling.
            dragonfruit_sapling|1|0.907|gt|This sapling is ready to be replanted in a fruit tree patch.
            calquat_tree_seed|340|0|sgt|Plant in a plantpot of soil to grow a sapling.
            calquat_sapling|1|0.907|gt|This sapling is ready to be replanted in a Calquat tree patch.
            celastrus_seed|540|0|sgt|For growing a Celastrus tree.
            celastrus_sapling|1|0.907|gt|This sapling is ready to be replanted in a Celastrus patch.
            spirit_seed|1|0|s|Plant this in a plantpot of soil to grow a sapling.
            spirit_sapling|1|0.907||This sapling is ready to be replanted in a Spirit patch.
            redberry_seed|4|0|sgt|A redberry bush seed - plant in a bush patch.
            cadavaberry_seed|9|0|sgt|A cadavaberry bush seed - plant in a bush patch.
            dwellberry_seed|25|0|sgt|A dwellberry bush seed - plant in a bush patch.
            jangerberry_seed|60|0|sgt|A jangerberry bush seed - plant in a bush patch.
            whiteberry_seed|133|0|sgt|A whiteberry bush seed - plant in a bush patch.
            poison_ivy_seed|166|0|sgt|A poison ivy bush seed - plant in a bush patch.
            barley_seed|2|0|sgt|A barley seed - plant in a hops patch.
            hammerstone_seed|2|0|sgt|A Hammerstone hop seed - plant in a hops patch.
            asgarnian_seed|3|0|sgt|An Asgarnian hop seed - plant in a hops patch.
            jute_seed|5|0|sgt|A jute plant seed - plant in a hops patch.
            yanillian_seed|6|0|sgt|A Yanillian hop seed - plant in a hops patch.
            krandorian_seed|8|0|sgt|A Krandorian hop seed - plant in a hops patch.
            wildblood_seed|14|0|sgt|A Wildblood hop seed - plant in a hops patch.
            cactus_seed|99|0|sgt|A Cactus seed - plant in a cactus patch.
            potato_cactus_seed|140|0|sgt|A Potato cactus seed - plant in a cactus patch.
            seaweed_spore|20|0|sgt|A seaweed spore - plant in a seaweed patch.
            mushroom_spore|86|0|sgt|A Bittercap mushroom spore - plant in a mushroom patch.
            belladonna_seed|177|0|sgt|Also known as Deadly Nightshade - plant in a belladonna patch.
            grape_seed|74|0|sgt|A grape seed for growing in a vinery.
            redberries|-1|0.007|gt|Very bright red berries.
            dwellberries|-1|0.014|gt|Some rather pretty blue berries.
            cooking_apple|1|0.085|gt|Keeps the doctor away.
            banana|-1|0.028|gt|Mmm this looks tasty.
            orange|-1|0.11|gt|A fresh orange.
            curry_leaf|19|0.028|gt|I could make a spicy curry with this.
            pineapple|2|0.15|gt|It can be cut up into something more manageable with a knife.
            papaya_fruit|64|0.028|gt|Looks delicious.
            coconut|87|0.028|gt|It's a coconut.
            dragonfruit|95|0.2|gt|A large ripe dragonfruit.
            calquat_fruit|54|0.028|gt|This is the largest fruit I've ever seen.
            barley|4|0.028|gt|A handful of Barley.
            hammerstone_hops|4|0.028|gt|A handful of Hammerstone Hops.
            asgarnian_hops|5|0.028|gt|A handful of Asgarnian Hops.
            jute_fibre|6|0.226|gt|I can weave this to make sacks.
            yanillian_hops|7|0.028|gt|A handful of Yanillian Hops.
            krandorian_hops|10|0.028|gt|A handful of Krandorian Hops.
            wildblood_hops|15|0.028|gt|A handful of Wildblood Hops.
            mushroom|38|0.028|gt|A Bittercap Mushroom
            cave_nightshade_flower|-1|0.015||Deadly.
            butterfly_net|20|0.226|gt|For catching butterflies...
            impling_jar|1|0.02|gt|It's got little holes in the top.
            baby_impling_jar|50|2.267|gt|
            young_impling_jar|50|2.267|gt|
            gourmet_impling_jar|50|2.267|gt|
            earth_impling_jar|50|2.267|gt|
            essence_impling_jar|50|2.267|gt|
            eclectic_impling_jar|50|2.267|gt|
            nature_impling_jar|50|2.267|gt|
            magpie_impling_jar|50|2.267|gt|
            ninja_impling_jar|50|2.267|gt|
            dragon_impling_jar|50|2.267|gt|
            lucky_impling_jar|50|2.267||
            swamp_lizard|50|4|gt|A very slimy and generally disgusting green lizard.
            orange_salamander|100|4|gt|Slightly slimy but kind of cute.
            red_salamander|150|4|gt|Slightly slimy but certainly striking.
            black_salamander|200|4|gt|Slightly slimy and somewhat menacing.
            tecu_salamander|200|6|gt|I thought this would be slimier.
            raw_wild_kebbit|1|0.34|gt|Better cook this carefully.
            cooked_wild_kebbit|4|0.283|gt|Mmm, this looks tasty.
            raw_larupia|1|0.34|gt|I should probably cook this first.
            cooked_larupia|4|0.283|gt|Mmm, this looks tasty.
            raw_barb_tailed_kebbit|1|0.34|gt|Ouch!
            cooked_barb_tailed_kebbit|4|0.283|gt|Mmm, this looks tasty.
            raw_graahk|1|0.34|gt|I should probably cook this first.
            cooked_graahk|4|0.283|gt|Mmm, this looks tasty.
            raw_kyatt|1|0.34|gt|I should probably cook this first.
            cooked_kyatt|4|0.283|gt|Mmm, this looks tasty.
            raw_pyre_fox|1|0.34|gt|I should probably cook this first.
            cooked_pyre_fox|4|0.283|gt|Mmm, this looks tasty.
            kebbit_spike|90|0.02|gt|These bone spikes are both very tough and very sharp.
            kebbit_teeth|160|0.02|gt|A kebbit-sized set of dentures.
            fox_fur|10|3|gt|This would make warm clothing.
            polar_kebbit_fur|10|0.453|gt|A thick fur for a cold climate.
            common_kebbit_fur|-1|0.453|gt|Common fur from a common kebbit.
            feldip_weasel_fur|14|0.453|gt|Not actually from a weasel, but it is, at least, furry.
            desert_devil_fur|17|0.453|gt|Sandy coloured kebbit fur.
            long_kebbit_spike|150|0.04|gt|These bone spikes are both very tough and very sharp.
            larupia_fur|80|2|gt|It's a perfect-looking larupia fur.
            graahk_fur|120|2|gt|It's a perfect-looking graahk fur.
            kyatt_fur|160|2|gt|It's a perfect-looking kyatt fur.
            sandstone_1kg|1|1|gt|A tiny chunk of sandstone.
            sandstone_2kg|1|2|gt|A small chunk of sandstone.
            sandstone_5kg|1|5|gt|A medium-sized chunk of sandstone.
            sandstone_10kg|1|10|gt|A large chunk of sandstone.
            granite_500g|1|0.5|gt|A tiny chunk of granite.
            granite_2kg|1|2|gt|A small chunk of granite.
            granite_5kg|1|5|gt|A medium-sized chunk of granite.
            limestone|-1|2.267|gt|Some limestone.
            pay_dirt|20|1.814||Prospector Percy has a machine that can wash this.
            golden_nugget|100|0|s|
            bucket_of_sand|2|2.5|gt|One of the ingredients for making glass.
            prospector_helmet|40|0.907||Helps you when you're mining.
            prospector_jacket|60|1.36||Helps you when you're mining.
            prospector_legs|50|0.907||Helps you when you're mining.
            prospector_boots|30|0.907||Helps you when you're mining.
            air_tiara|-1|1|gt|A tiara infused with the properties of air.
            mind_talisman|4|0.015|gt|A mysterious power emanates from the talisman...
            mind_tiara|-1|1|gt|A tiara infused with the properties of the mind.
            water_talisman|4|0.015|gt|A mysterious power emanates from the talisman...
            water_tiara|-1|1|gt|A tiara infused with the properties of water.
            earth_talisman|4|0.015|gt|A mysterious power emanates from the talisman...
            earth_tiara|-1|1|gt|A tiara infused with the properties of the earth.
            fire_talisman|4|0.015|gt|A mysterious power emanates from the talisman...
            fire_tiara|-1|1|gt|A tiara infused with the properties of fire.
            body_talisman|4|0.015|gt|A mysterious power emanates from the talisman...
            body_tiara|-1|1|gt|A tiara infused with the properties of the body.
            cosmic_talisman|4|0.015|gt|A mysterious power emanates from the talisman...
            cosmic_tiara|-1|1|gt|A tiara infused with the properties of the cosmos.
            chaos_talisman|4|0.015|gt|A mysterious power emanates from the talisman...
            chaos_tiara|-1|1|gt|A tiara infused with the properties of chaos.
            astral_tiara|-1|1||A tiara infused with the properties of the heavens.
            nature_talisman|4|0.015|gt|A mysterious power emanates from the talisman...
            nature_tiara|-1|1|gt|A tiara infused with the properties of nature.
            law_talisman|4|0.015||A mysterious power emanates from the talisman...
            law_tiara|-1|1||A tiara infused with the properties of law.
            death_talisman|4|0.015|gt|A mysterious power emanates from the talisman...
            death_tiara|-1|1|gt|A tiara infused with the properties of death.
            wrath_talisman|4|0.015|gt|A mysterious power emanates from the talisman...
            wrath_tiara|500|1|gt|A tiara infused with the properties of wrath.
            blood_talisman|4|0.015|gt|A mysterious power emanates from the talisman...
            blood_tiara|500|1|gt|A tiara infused with the properties of blood.
            soul_talisman|4|0.015||A mysterious power emanates from the talisman...
            mist_rune|20|0|sgt|A combined Air and Water Rune.
            dust_rune|-1|0|sgt|A combined Air and Earth Rune.
            mud_rune|20|0|sgt|A combined Earth and Water Rune.
            smoke_rune|20|0|sgt|A combined Air and Fire Rune.
            steam_rune|20|0|sgt|A combined Water and Fire Rune.
            lava_rune|20|0|sgt|A combined Earth and Fire Rune.
            small_pouch|150|1||
            medium_pouch|300|1||
            large_pouch|600|1||
            giant_pouch|1200|1||
            colossal_pouch|1200|1||
            limestone_brick|20|1.36|gt|A well carved limestone brick.
            marble_block|250000|13.607|gt|A beautifully carved marble block.
            gold_leaf|100000|1|gt|A very delicate sheet of gold.
            magic_stone|750000|1|gt|A magic stone to make high-level furniture.
            wooden_chair_flatpack|10|1|gt|
            rocking_chair_flatpack|10|1|gt|
            oak_chair_flatpack|10|1|gt|
            oak_armchair_flatpack|10|1|gt|
            teak_armchair_flatpack|10|1|gt|
            mahogany_armchair_flatpack|10|1|gt|
            oak_bookcase_flatpack|10|1|gt|
            mahogany_bookcase_flatpack|10|1|gt|
            wood_dining_table_flatpack|10|1|gt|
            oak_dining_table_flatpack|10|1|gt|
            carved_oak_table_flatpack|10|1|gt|
            teak_table_flatpack|10|1|gt|
            carved_teak_table_flatpack|10|1|gt|
            mahogany_table_flatpack|10|1|gt|
            opulent_table_flatpack|10|1|gt|
            wooden_bed_flatpack|10|1|gt|
            oak_bed_flatpack|10|1|gt|
            large_oak_bed_flatpack|10|1|gt|
            teak_bed_flatpack|10|1|gt|
            large_teak_bed_flatpack|10|1|gt|
            lockpick|-1|0.01|gt|For picking tough locks.
            achey_tree_logs|-1|1.36|gt|These logs are longer than normal.
            arctic_pine_logs|35|1.36|gt|Log cut from an arctic pine.
            sulliuscep_cap|3|0.907|gt|A Sulliuscep mushroom cap. Edible? Probably...
            split_log|90|1.814|gt|Used to repair bridges.
            big_fishing_net|-1|8.164|gt|Useful for catching lots of fish.
            barbarian_rod|76|1.36||A heavy barbarian Fishing rod.
            karambwan_vessel|-1|3|gt|A wide bodied and thin necked vessel, encrusted with sea salt.
            oily_fishing_rod|-1|1.36||Useful for catching lava eels.
            raw_karambwanji|19|0|s|Small brightly coloured tropical fish.
            leaping_trout|2|0.5|gt|A sad-looking trout.
            leaping_salmon|3|0.5|gt|Some non-tasty salmon.
            leaping_sturgeon|4|1|gt|A bloated sturgeon.
            sacred_eel|140|0.6||An eel that used to live in a toxic swamp containing Zulrah's discarded scales.
            infernal_eel|165|0.6||An infernal eel that has now hardened. Perhaps it could be cracked open.
            casket|-1|5|gt|I hope there's treasure in it.
            oyster|200|0.1|gt|Maybe there are pearls inside?
            empty_oyster|5|0.08|t|Aww, it's empty.
            oyster_pearl|-1|0.002|gt|I could work wonders with a chisel on this pearl.
            infernal_axe|99801|1.814||
            crystal_axe|520000|0.907||
            infernal_pickaxe|99801|2.4||
            crystal_pickaxe|520000|1.36||
            infernal_harpoon|99801|2.267||
            crystal_harpoon|520000|0.907||
            spyglass|-1|0.2||For making cows that are small and/or far away seem big and/or close up.
            small_salvage|200|3||
            fishy_salvage|200|3||
            barracuda_salvage|200|3||
            large_salvage|200|3||
            fremennik_salvage|200|3||
            graceful_hood|35|0.6||
            graceful_cape|40|0.8||
            graceful_top|55|0.9||
            graceful_legs|60|0.9||
            graceful_gloves|30|0.1||
            graceful_boots|40|0.4||
            fiendish_ashes|1|0.056|gt|
            vile_ashes|1|0.056|gt|
            malicious_ashes|1|0.056|gt|
            earmuffs|-1|0.113|gt|
            nose_peg|-1|0.001|gt|
            facemask|-1|0.113|gt|
            spiny_helmet|-1|2.267|gt|
            mirror_shield|-1|2.267|gt|
            insulated_boots|-1|1.36|gt|
            boots_of_stone|200|3.175|gt|
            boots_of_brimstone|20000|3.175|gt|
            slayer_gloves|-1|0.226||
            witchwood_icon|-1|0.007||
            rock_hammer|-1|2.267|gt|
            bag_of_salt|-1|0|sgt|
            ice_cooler|-1|0|s|
            unlit_bug_lantern|-1|2.267|gt|
            bug_lantern|-1|2.267||
            slayer_bell|-1|0.028|gt|
            slayer_ring_8|1000|0.01|gt|
            herb_sack|1|1||
            leaf_bladed_spear|-1|2.267||
            leaf_bladed_sword|-1|1.814|gt|
            leaf_bladed_battleaxe|96000|2.721|gt|
            broad_arrowheads|55|0|sgt|
            broad_arrows|-1|0|s|
            slayer_staff|-1|1.814|gt|
            black_mask|3000|10|gt|
            black_mask_i|3000|10||
            slayer_helmet|40000|2.267||
            slayer_helmet_i|90000|2.267||
            abyssal_ashes|1|0.056|gt|
            dark_bow|120002|1.814|gt|
            granite_legs|66000|15.875|gt|These look pretty heavy.
            draconic_visage|750000|1.814|gt|
            wyvern_visage|750000|0.907|gt|
            dragon_boots|20000|1|gt|These will protect my feet.
            brine_sabre|26000|1.814|gt|A salty sword.
            dragon_sword|72001|1.814|gt|A razor sharp sword.
            dragon_harpoon|55000|2.267|gt|
            dust_battlestaff|15500|2.267|gt|It's a slightly magical stick.
            mist_battlestaff|15500|2.267|gt|It's a slightly magical stick.
            basilisk_jaw|500000|0.005|gt|
            drakes_tooth|100000|0.01|gt|The tooth of a ferocious drake. They could be combined with holy sandals.
            drakes_claw|100000|0.001|gt|
            lava_scale|50|0.01|gt|
            dragon_limbs|35000|3.2|gt|A pair of dragon crossbow limbs.
            dragon_metal_slice|100000|2.721|gt|A badly damaged slice of dragon metal.
            dragon_metal_lump|150000|2.721|gt|A badly damaged lump of dragon metal.
            rune_boots|-1|1.36|gt|
            mystic_robe_top_dark|-1|2.721|gt|
            loop_half_of_key|100|0.005|gt|
            tooth_half_of_key|100|0.005|gt|
            imbued_heart|500000|0.35|gt|
            eternal_gem|9500|0.004|gt|
            brimstone_key|80000|0|s|
            larran_key|90|0|sgt|
            armadyl_helmet|289000|0.5|gt|
            armadyl_chestplate|290000|4|gt|
            armadyl_chainskirt|289900|1|gt|
            armadyl_crossbow|990000|6|gt|
            saradomin_sword|130000|3|gt|
            saradomins_light|34600|0.25|gt|
            armadyl_hilt|500000|2.5|gt|
            bandos_hilt|500000|2.5|gt|
            saradomin_hilt|500000|2.5|gt|
            zamorak_hilt|500000|2.5|gt|
            ancient_hilt|500000|2.5|gt|
            godsword_shard_1|250000|2.5|gt|
            godsword_shard_2|250000|2.5|gt|
            godsword_shard_3|250000|2.5|gt|
            godsword_shards_1_2|500000|5|t|
            godsword_shards_1_3|500000|5|t|
            godsword_shards_2_3|500000|5|t|
            godsword_blade|750000|7.5|gt|
            armadyl_godsword|-1|10|gt|
            bandos_godsword|-1|10|gt|
            saradomin_godsword|-1|10|gt|
            zamorak_godsword|-1|10|gt|
            ancient_godsword|-1|10|gt|
            frozen_key_piece_armadyl|5000|0.01||
            frozen_key_piece_bandos|5000|0.01||
            frozen_key_piece_saradomin|5000|0.01||
            frozen_key_piece_zamorak|5000|0.01||
            frozen_key|-1|0.001||
            ecumenical_key|102500|0.015||
            torva_full_helm|200000|2.721|gt|
            torva_platebody|600000|9.979|gt|
            torva_platelegs|400000|9.071|gt|
            zaryte_vambraces|200000|1|gt|
            nihil_horn|2000000|0.006|gt|
            nihil_shard|5000|0|sgt|
            zaryte_crossbow|990000|6.001|gt|
            tyrannical_ring|50000|1|gt|
            treasonous_ring|50000|0.4|gt|
            ring_of_the_gods|50000|0.3|gt|
            odium_shard_1|31000|0.907|gt|
            malediction_shard_1|31000|0.907|gt|
            odium_shard_2|31000|0.907|gt|
            malediction_shard_2|31000|0.907|gt|
            odium_shard_3|31000|0.907|gt|
            malediction_shard_3|31000|0.907|gt|
            odium_ward|45000|3.628|gt|
            malediction_ward|45000|3.628|gt|
            voidwaker_hilt|800000|0.514|gt|
            voidwaker_blade|800000|0.9|gt|
            voidwaker_gem|800000|0.3|gt|
            voidwaker|2000000|1.814|gt|
            claws_of_callisto|80000|2|gt|
            fangs_of_venenatis|80000|2|gt|
            skull_of_vetion|80000|2|gt|
            kbd_heads|50000|10||
            revenant_ether|250|0|sgt|
            ancient_emblem|-1|0.1|gt|
            ancient_totem|-1|0.1|gt|
            ancient_statuette|-1|0.1|gt|
            ancient_medallion|-1|0.1|gt|
            ancient_effigy|-1|0.1|gt|
            ancient_relic|-1|0.1|gt|
            bracelet_of_ethereum|72000|0.25|gt|
            craws_bow|120000|3.175||
            viggoras_chainmace|120000|2||
            thammarons_sceptre|120000|0.198||
            webweaver_bow|175000|3.175||
            ursine_chainmace|175000|2||
            accursed_sceptre|175000|0.198||
            pet_kreearra|-1|0.015||
            pet_zilyana|-1|0.015||
            pet_general_graardor|-1|0.015||
            pet_kril_tsutsaroth|-1|0.015||
            nexling|-1|0.015||
            callisto_cub|-1|0.015||
            pet_venenatis_spiderling|-1|0.015||
            vetion_jr|-1|0.015||
            pet_scorpias_offspring|-1|0.015||
            pet_chaos_elemental|-1|0.015||
            prince_black_dragon|-1|0.015||
            mole_claw|600|0.001|gt|
            mole_skin|400|0.001|gt|
            pet_baby_mole|-1|0.015||
            kq_head|50000|10||
            dragon_chainbody|-1|6.803|gt|
            jar_of_sand|-1|0.02|gt|
            pet_kalphite_princess|-1|0.015||
            berserker_ring|10000|0.004|gt|
            warrior_ring|10000|0.004|gt|
            seers_ring|10000|0.004|gt|
            archers_ring|10000|0.004|gt|
            mud_battlestaff|17000|2.267|gt|
            seercull|8000|1.36|gt|
            pet_dagannoth_rex|-1|0.015||
            pet_dagannoth_prime|-1|0.015||
            pet_dagannoth_supreme|-1|0.015||
            sarachnis_cudgel|120001|0.453|gt|
            jar_of_eyes|-1|0.02|gt|
            pet_sraracha|-1|0.015||
            zulrahs_scales|20|0|sgt|
            tanzanite_fang|110000|0.35|gt|
            magic_fang|102000|0.3|gt|
            serpentine_visage|105000|1|gt|
            tanzanite_mutagen|9500|0.05||
            magma_mutagen|9500|0.05||
            toxic_blowpipe|120000|0.5||
            trident_of_the_seas|68500|1.814||
            trident_of_the_swamp|78500|1.814||
            serpentine_helm|110000|0.9||
            tanzanite_helm|110500|0.9||
            magma_helm|110500|0.9||
            jar_of_swamp|-1|0.02|gt|
            pet_snakeling|-1|0.015||
            vorkaths_head|50000|10||
            skeletal_visage|1500000|1.814|gt|
            dragonbone_necklace|80000|0.005|gt|
            dragonfire_ward|2000000|4.082|gt|
            jar_of_decay|-1|0.04|gt|
            pet_vorki|-1|0.015||
            kraken_tentacle|83340|0.005|gt|
            abyssal_tentacle|289800|0.453||
            jar_of_dirt|-1|0.02|gt|
            pet_kraken|-1|0.015||
            occult_necklace|45000|0.005|gt|
            smoke_battlestaff|15500|2.267|gt|
            jar_of_smoke|-1|0.04|gt|
            pet_smoke_devil|-1|0.015||
            primordial_crystal|45000|0.05|gt|
            pegasian_crystal|45000|0.05|gt|
            eternal_crystal|45000|0.05|gt|
            smouldering_stone|45000|0.05|gt|
            primordial_boots|75000|1.814|gt|A pair of upgraded dragon boots.
            pegasian_boots|75000|1.814|gt|A pair of upgraded ranger boots.
            eternal_boots|75000|1.814|gt|A pair of upgraded infinity boots.
            jar_of_souls|-1|0.02|gt|
            pet_hellpuppy|-1|0.015||
            unsired|140000|0|s|
            abyssal_dagger|115001|0.453|gt|
            abyssal_bludgeon|260000|6|gt|
            bludgeon_claw|230000|2.5||
            bludgeon_spine|230000|2.5||
            bludgeon_axon|230000|2.5||
            abyssal_head|12000|10||
            jar_of_miasma|-1|0.04|gt|
            pet_abyssal_orphan|-1|0.015||
            hydras_eye|100000|0.001||
            hydras_fang|100000|0.001||
            hydras_heart|100000|0.001||
            hydra_claw|150000|0.001|gt|
            hydra_leather|100000|0.226|gt|
            hydra_tail|100000|0.226|gt|
            alchemical_hydra_heads|50000|10||
            brimstone_ring|10000|0.004|gt|
            ferocious_gloves|100000|0.226||
            dragon_hunter_lance|300000|3|gt|
            jar_of_chemicals|-1|0.04|gt|
            pet_ikkle_hydra|-1|0.015||
            granite_maul|50000|4.535|gt|
            granite_hammer|64000|5|gt|
            granite_ring|15000|3|gt|
            granite_gloves|34000|1|gt|
            black_tourmaline_core|41500|1|gt|
            guardian_boots|305000|7|gt|Some sturdy Bandos boots, made sturdier with the blackened tourmaline core of the Grotesque Guardians.
            granite_dust|80|0|s|Granite, in fine form.
            jar_of_stone|-1|1|gt|
            pet_noon|-1|0.015||
            spirit_shield|-1|2|gt|
            holy_elixir|750000|2|gt|
            blessed_spirit_shield|1200000|2|gt|
            spectral_spirit_shield|2000000|2|gt|
            arcane_spirit_shield|2000000|2|gt|
            elysian_spirit_shield|2000000|2|gt|
            spectral_sigil|750000|3|gt|
            arcane_sigil|750000|3|gt|
            elysian_sigil|750000|3|gt|
            jar_of_spirits|-1|0.04|gt|
            pet_dark_core|-1|0.015||
            dark_totem_base|205|0.003||
            dark_totem_middle|205|0.003||
            dark_totem_top|205|0.003||
            dark_totem|450|0.009||
            dark_claw|2500|0.02||
            ancient_shard|204|0|s|
            jar_of_darkness|-1|0.04|gt|
            pet_skotos|-1|0.015||
            bottomless_compost_bucket|45000|1|gt|
            iasor_seed|1000|0|s|
            kronos_seed|1000|0|s|
            attas_seed|1000|0|s|
            crystal_shard|10000|0|s|
            crystal_weapon_seed|-1|0.014|gt|
            crystal_armour_seed|500000|0|gt|
            enhanced_crystal_weapon_seed|500000|0.018|gt|
            crystal_tool_seed|500000|0|gt|
            bow_of_faerdhinen|5000000|1.5||
            blade_of_saeldor|5000000|1.814||
            crystal_helm|-1|0.5||This helmet shines bright, like a diamond.
            crystal_body|-1|2||Sturdier than it looks.
            crystal_legs|-1|1||A bright, shiny set of leg armour.
            gauntlet_cape|20000|0.453||
            pet_youngllef|-1|0.015||
            zalcano_shard|1600|0.5|gt|
            pet_smolcano|-1|0.015||
            nightmare_staff|600000|1.5|gt|
            harmonised_orb|4000000|0.1|gt|
            harmonised_nightmare_staff|4600000|1.6||
            volatile_orb|4000000|0.1|gt|
            volatile_nightmare_staff|4600000|1.6||
            eldritch_orb|4000000|0.1|gt|
            eldritch_nightmare_staff|4600000|1.6||
            inquisitors_great_helm|500000|2.721|gt|The great helmet of the turncloak justiciar.
            inquisitors_hauberk|1000000|9.979|gt|The hauberk of the turncloak justiciar.
            inquisitors_plateskirt|750000|9.071|gt|The plateskirt of the turncloak justiciar.
            inquisitors_mace|5000000|1.814|gt|
            jar_of_dreams|-1|0.04|gt|
            parasitic_egg|-1|0.19||
            pet_little_nightmare|-1|0.015||
            venator_shard|150000|0|sgt|
            venator_bow|750000|4.5||
            ancient_icon|100000|0.02||
            ancient_essence|28|0|sgt|
            pet_muphin|-1|0.015||
            chromium_ingot|50000|3|gt|
            awakeners_orb|75000|0.4|gt|
            ultor_vestige|50000|0.004||
            magus_vestige|50000|0.004||
            venator_vestige|50000|0.004||
            bellator_vestige|50000|0.004||
            ultor_ring|140000|0.012|gt|
            bellator_ring|140000|0.012|gt|
            magus_ring|140000|0.012|gt|
            venator_ring|140000|0.012|gt|
            virtus_mask|200000|1.206|gt|An ancient mage's mask.
            virtus_robe_top|600000|2.895|gt|An ancient mage's robe top.
            virtus_robe_bottom|400000|3.79|gt|An ancient mage's robe bottom.
            executioners_axe_head|50000|0.001||
            leviathans_lure|50000|0.001||
            sirens_staff|50000|0.001||
            eye_of_the_duke|50000|0.001||
            soulreaper_axe|240000|2.825|gt|
            pet_baron|-1|0.015||
            pet_lilviathan|-1|0.015||
            pet_wisp|-1|0.015||
            pet_butch|-1|0.015||
            scurrius_spine|1000|0.3||
            pet_scurry|-1|0.015||
            noxious_point|160000|0.907||
            noxious_blade|160000|0.907||
            noxious_pommel|160000|0.907||
            noxious_halberd|500000|2.721|gt|
            araxyte_fang|200000|0.001||
            coagulated_venom|10000|0.19||
            araxyte_head|60000|10||
            echo_crystal|150000|0.5|gt|
            jar_of_venom|200000|0.04||
            pet_nid|-1|0.015||
            hueycoatl_hide|110|3.175|gt|
            tome_of_earth|20000|1||
            dragon_hunter_wand|300000|2|gt|
            pet_huberte|-1|0.015||
            glacial_temotli|83000|1.814|gt|
            pendant_of_ates|351|0.06||
            frozen_tear|500|0|sgt|
            pet_moxi|-1|0.015||
            deadeye_prayer_scroll|5000|0.015||
            mystic_vigour_prayer_scroll|5000|0.015||
            giantsoul_amulet|5000|0.01||
            fire_element_staff_crown|10000|2.267|gt|
            ice_element_staff_crown|10000|2.267|gt|
            pet_bran|-1|0.015||
            sunfire_fanatic_helm|375000|2.267|gt|A helmet forged from sunfire.
            sunfire_fanatic_cuirass|500000|8.618|gt|A cuirass forged from sunfire.
            sunfire_fanatic_chausses|500000|7.711|gt|Chausses forged from sunfire.
            tonalztics_of_ralos|600000|3||
            dizanas_quiver|80000|0.453||
            sunfire_splinters|100|0|sgt|
            pet_smol_heredit|-1|0.015||
            dexterous_prayer_scroll|80000|0.015|gt|
            arcane_prayer_scroll|80000|0.015|gt|
            twisted_buckler|90000|5.443|gt|
            dragon_hunter_crossbow|80000|7|gt|
            dinhs_bulwark|560000|30|gt|
            ancestral_hat|45000|0.453|gt|
            ancestral_robe_top|120000|2.721|gt|
            ancestral_robe_bottom|80000|1.814|gt|
            dragon_claws|205000|0.907|gt|
            elder_maul|500010|5.443|gt|
            kodai_insignia|150000|0.056|gt|
            kodai_wand|150000|0.198|gt|
            twisted_bow|4000000|1.814|gt|
            olmlet|-1|0.015||
            avernic_defender_hilt|2500000|0.453|gt|
            avernic_defender|2500000|0.453||
            ghrazi_rapier|5000000|1.814|gt|
            sanguinesti_staff|5000000|1.5||
            justiciar_faceguard|2000000|2.721|gt|
            justiciar_chestguard|6000000|9.979|gt|
            justiciar_legguards|4500000|9.071|gt|
            scythe_of_vitur|4000000|3.175||
            lil_zik|-1|0.015||
            osmumtens_fang|1000000|1.814|gt|
            elidinis_ward|5000000|0.708|gt|
            masori_mask|800000|1|gt|
            masori_body|1200000|8|gt|
            masori_chaps|1000000|7|gt|
            tumekens_shadow|7000000|5||
            tumekens_guardian|-1|0.015||
            cox_kindling|-1|0|st|
            cavern_grubs|-1|0|st|
            crondis_water_container|-1|2||
            rfd_bronze_gloves|100|0.226||
            rfd_iron_gloves|250|0.226||
            rfd_steel_gloves|500|0.226||
            rfd_black_gloves|1000|0.226||
            rfd_mithril_gloves|1500|0.226||
            rfd_adamant_gloves|2500|0.226||
            rfd_rune_gloves|5000|0.226||
            rfd_dragon_gloves|100000|0.226||
            barrows_gloves|100000|0.226||
            avas_attractor|769|4.535||
            avas_accumulator|1000|4.535||
            cape_of_legends|-1|1.814||
            steel_gauntlets|-1|0.226||
            chaos_gauntlets|-1|0.226||
            gnome_amulet|-1|0.01||
            excalibur|-1|2.267||
            dramen_staff|-1|1.814||
            wolfbane|300|0.453||
            silver_sickle_b|250|1.587||
            ibans_staff|42500|2.267||
            lunar_staff|-1|2.267||
            ivandis_flail|20000|0.001||
            blisterwood_flail|40000|0.001||
            crandor_map|1|0.015||
            elvargs_head|1|0.01||
            wizards_mind_bomb|2|0.55|gt|
            oil_can|3|0.028||
            pressure_gauge|1|0.056||
            rubber_tube|3|0.028||
            goblin_mail|40|3.628|gt|
            orange_goblin_mail|40|3.628||
            blue_goblin_mail|40|3.628||
            karamjan_rum|30|0.907||
            chest_key|-1|0.01||
            pirate_message|1|0.02||
            key_print|30|0.01||
            bronze_key|-1|0.01||
            blonde_wig|30|0.453||
            skin_paste|30|0.015||
            pink_skirt|2|0.907|gt|
            portrait|3|1.36||
            blurite_sword|200|1.814||
            intel_report|10|0.02||
            weapon_store_key|10|0.01|t|
            phoenix_crossbow|10|3.628|t|
            shield_left_half|110000|2.721|gt|
            shield_right_half|500000|2.721|gt|
            arrav_certificate|-1|0.02||
            treasure_scroll|30|0.02||
            ancient_casket|30|5||
            mm_notes|-1|0.028||
            ogre_artefact|1|0.8||
            dramen_branch|15|2.267||
            book_on_baxtorian|2|0.51||
            glarials_pebble|1|0.01||
            glarials_amulet|1|0.01||
            glarials_urn|1|0.141||
            mithril_seeds|200|0|sgt|
            black_candle|3|0.028||
            bat_bones|1|0.3|gt|
            magic_whistle|10|0.005||
            holy_grail|3|0.085||
            cooked_lava_eel|150|0.198||
            ice_gloves|6|0.226||
            gas_mask|2|0.255||
            warrant|5|0.01||
            magic_scroll_ardougne|1|0.02||
            distillator|1|0.02||
            plague_sample|1|0.05||
            khali_brew|5|0.113||
            khazard_cell_keys|30|0.01||
            carnillean_armour|65|9.071||
            red_cog|1|1||
            blue_cog|1|1||
            black_cog|1|1||
            white_cog|1|1||
            childs_ball|1|0.15||
            door_key_witch|1|0.01||
            orbs_of_protection|1|2||
            bark_sample|2|0.015||
            hazelmeres_scroll|2|0.02||
            invasion_plans|2|0.02||
            daconia_rock|1|0.907||
            narnodes_orders|1|0.02||
            monkey_dentures|10|0.2||
            monkey_talisman|1000|0.085||
            monkey_greegree|500|0.085||
            snake_weed|5|0.007||
            ardrigal|5|0.007||
            sito_foil|5|0.007||
            volencia_moss|5|0.007||
            rogues_purse|5|0.007||
            zadimus_corpse|1|0.5||
            sword_pommel|1|0.085||
            bone_key|1|0.01||
            """);
        load("""
            radimus_notes|1|0.02||
            yommi_tree_seeds|1|0|s|
            enchanted_lyre|1|1.814||
            fremennik_ballad|1|0.005||
            seal_of_passage|1|4.535||
            waking_sleep_vial|1|0.5||
            lunar_moonclan_manual|1|0.51||
            asleifs_necklace|1|0.1||
            corpse_of_woman|1|1||
            bearhead|-1|0.453||
            lighthouse_key|1|0.01||
            ancient_talisman|1|0.085||
            stone_tablet|-1|0.8||
            armadyl_pendant|12|0.01||
            ice_arrows|2|0|s|
            staff_of_armadyl|15|1.814||
            metal_key|1|0.01||
            desert_disguise|1|0.04||
            ana_in_a_barrel|1|32||
            ancient_diary|1|0.51||
            smoke_diamond|250|0.001||
            shadow_diamond|250|0.001||
            ice_diamond|250|0.001||
            blood_diamond|250|0.001||
            abyssal_incantation|30|0.05||
            golden_key|300|0.01||
            iron_key_pip|-1|0.01||
            blessed_water|6|3||
            filliman_journal|1|0.51||
            undead_chicken|100|0.453||
            selected_iron|10|0.25||
            undead_twigs|1|0.453||
            decapitated_head|1|2.721||
            torso_fenk|1|19.05||
            arms_fenk|1|6.35||
            legs_fenk|1|6.35||
            pickled_brain|1|0.907||
            ring_of_charos|-1|0.005||
            petition_form|1|0.001||
            ectophial|1|0.035||
            crystal_mine_key|1|0.01||
            salve_shard|1|0.007||
            plunder|1|0.005||
            blisterwood_logs|200|2||
            combination_iou|1|0.005||
            climbing_boots|12|0.34|gt|
            spiked_boots|30|0.34|gt|
            secret_way_map|1|0.005||
            prison_key|1|0.01||
            troll_disguise|1|0.001||
            troll_potion|1|0.056||
            doll_of_iban|2|0.3||
            ibans_shadow|2|0.025||
            ibans_ashes|2|0.056||
            barrel_bomb|1|32||
            crystal_seed_sote|1|0.014||
            royal_seed_pod|20|0.005||
            kruks_paw|1|0.5||
            childs_blanket|5|0.907||
            tribal_totem|10|3||
            observatory_lens|3|0.003||
            lens_mould|1|1||
            ogre_bow|500|1.814||
            ogre_arrow|25|0|sgt|
            bloated_toad|1|0.75||
            raw_chompy|85|10|gt|
            battered_key|1|0.01||
            elemental_ore|5|2.267||
            elemental_shield|20|0.226|gt|
            zogre_black_prism|1|0.001||
            blindweed|1|0.007||
            origami_balloon|1|0.001||
            consecration_seed|1|0.014||
            crystal_bow_quest|279000|1.814|g|
            light_crystal|4|0.015|gt|
            scorpion_cage|10|0.15||
            fishing_pass|1|0.006||
            fishing_trophy|1|0.085||
            giant_carp|10|0.5||
            dwarf_remains|1|16||
            cannon_ammo_mould|5|4.535||
            cannon_base|187500|10|gt|
            cannon_stand|187500|0.907|gt|
            cannon_barrels|187500|15|gt|
            cannon_furnace|187500|5|gt|
            granite_cannonball|85|0|s|
            nulodions_notes|1|0.028||
            fluffs_kittens|1|0.75||
            enchanted_key|1|0.01||
            outpost_letter|-1|0.005||
            outpost_scroll|-1|0.005||
            outpost_journal|1|0.51||
            enchanted_scroll|1|0.005||
            kharedsts_memoirs|200|0.2||
            stone_bowl|1|1.814||
            desert_disguise_feud|-1|0.07|gt|
            doctors_hat|1|0.3||
            statuette|-1|0.5||
            darklight|500|1.814||
            beacon_ring|-1|0.006||
            initiate_sallet|6000|2.267|gt|
            silverware|1|0.05||
            commorb|1|0.001||
            scrying_orb|30|1||
            barcrawl_card|10|0.014||
            magic_secateurs|-1|0.453||
            star_flower|75|0.001||
            gorak_claws|150|0.001||
            queens_secateurs|1|0.453||
            white_tree_sapling|1|0.907||
            beaten_book|1|0.1||
            mind_shield|3000|0.226|gt|
            golden_feather|300|0.002||
            helm_of_neitiznot|50000|2.267|gt|
            homunculus_key|1|0.005||
            lockpick_kr|20|0.01|gt|
            magic_beans|1|0.01||
            griffin_feather|1|0.001||
            sandy_hand|1|0.5||
            truth_serum|1|0.05||
            ancestral_key|1|0.01||
            keris|10000|0.453||
            rat_pole|1|2.267||
            bone_in_vinegar|-1|1.36||
            dark_essence_fragment|18|0.4|s|
            rfd_rock_cake|1|0.453||
            ham_hood|75|0.453|gt|
            ham_shirt|75|0.907|gt|
            ham_robe|75|0.907|gt|
            ancient_mace|1000|1.814|gt|
            goblin_symbol_book|1|1||
            gold_helmet|-1|31.751||
            arzinian_schematic|1|0.004||
            kelda_seed|2|0|s|
            kelda_hops|4|0.028||
            kelda_stout|2|0.55||
            camulet|-1|0.007||
            slender_blade|30|3.175||
            bow_sword|30|3.175||
            devious_package|1|0.01||
            vardorvis_medallion_piece|100|0.5||
            sucellus_medallion_piece|100|0.5||
            whisperer_medallion_piece|100|0.5||
            ring_of_shadows|75000|0.004||
            karambwan_paste|15|0.01||
            quest_sled|-1|1||
            trollweiss_flower|5|0.028||
            quest_animate_rock_scroll|50|0.01||
            quest_broken_cauldron|1|0.003||
            bonesack|1|0.003||
            ram_skull_helm|-1|3.175||
            quest_prayer_book_fintan|1|0.05||
            barrelchest_anchor|230000|30||
            vs_shield|-1|2.267||
            sourhog_foot|1|0.001||
            quest_quetzal_feed|50|0|sgt|
            bronze_hasta|26|2.267|gt|
            damaged_soul_bearer|1|0.42||
            soul_bearer|8|0.45||
            ring_of_visibility|350|0.004||
            ghostly_hood|30|0.001||
            ghostly_robe_top|30|0.001||
            shadow_sword|-1|3.628||
            book_of_knowledge|1|1||
            tarns_diary|50|0.453||
            imbued_saradomin_cape|80000|0.453||
            imbued_guthix_cape|80000|0.453||
            imbued_zamorak_cape|80000|0.453||
            ring_of_recoil|-1|0.006|gt|
            games_necklace_8|-1|0.01|gt|
            games_necklace_7|-1|0.01|t|
            games_necklace_6|-1|0.01|t|
            games_necklace_5|-1|0.01|t|
            games_necklace_4|-1|0.01|t|
            games_necklace_3|-1|0.01|t|
            games_necklace_2|-1|0.01|t|
            games_necklace_1|-1|0.01|t|
            amulet_of_magic|-1|0.01|gt|
            bracelet_of_clay|1265|0.25|gt|
            ring_of_dueling_8|1275|0.006|gt|
            ring_of_dueling_7|1275|0.006|t|
            ring_of_dueling_6|1275|0.006|t|
            ring_of_dueling_5|1275|0.006|t|
            ring_of_dueling_4|1275|0.006|t|
            ring_of_dueling_3|1275|0.006|t|
            ring_of_dueling_2|1275|0.006|t|
            ring_of_dueling_1|1275|0.006|t|
            binding_necklace|1425|0.01|gt|
            amulet_of_defence|-1|0.01|gt|
            amulet_of_defence_t|-1|0.01|gt|
            castle_wars_bracelet|1675|0.25|gt|
            ring_of_forging|-1|0.006|gt|
            dodgy_necklace|1050|0.01|gt|
            amulet_of_strength|-1|0.01|gt|
            amulet_of_strength_t|-1|0.01|gt|
            inoculation_bracelet|2560|0.25|gt|
            ring_of_life|-1|0.006|gt|
            phoenix_necklace|4050|0.01|gt|
            amulet_of_power|-1|0.01|gt|
            abyssal_bracelet_5|-1|0.25|gt|
            abyssal_bracelet_4|-1|0.25|t|
            abyssal_bracelet_3|-1|0.25|t|
            abyssal_bracelet_2|-1|0.25|t|
            abyssal_bracelet_1|-1|0.25|t|
            ring_of_wealth_5|-1|0.006|gt|
            ring_of_wealth_4|-1|0.006|t|
            ring_of_wealth_3|-1|0.006|t|
            ring_of_wealth_2|-1|0.006|t|
            ring_of_wealth_1|-1|0.006|t|
            ring_of_wealth|17625|0.006|gt|
            skills_necklace_6|20200|0.01|gt|
            skills_necklace_5|20200|0.01|t|
            skills_necklace_4|20200|0.01|gt|
            skills_necklace_3|20200|0.01|t|
            skills_necklace_2|20200|0.01|t|
            skills_necklace_1|20200|0.01|t|
            skills_necklace|20200|0.01|gt|
            amulet_of_glory_6|-1|0.01|gt|
            amulet_of_glory_5|-1|0.01|t|
            amulet_of_glory_4|-1|0.01|gt|
            amulet_of_glory_3|-1|0.01|t|
            amulet_of_glory_2|-1|0.01|t|
            amulet_of_glory_1|-1|0.01|t|
            amulet_of_glory|17625|0.01|gt|
            amulet_of_glory_t|17625|0.01|gt|
            combat_bracelet_6|21040|0.25|gt|
            combat_bracelet_5|21040|0.25|t|
            combat_bracelet_4|21040|0.25|gt|
            combat_bracelet_3|21040|0.25|t|
            combat_bracelet_2|21040|0.25|t|
            combat_bracelet_1|21040|0.25|t|
            combat_bracelet|21040|0.25|gt|
            amulet_of_eternal_glory|-1|0.01|gt|
            ring_of_stone|202000|0.006|gt|
            berserker_necklace|202000|0.01|gt|
            amulet_of_fury|202000|0.01|gt|
            regen_bracelet|202000|0.25|gt|
            ring_of_suffering|201000|0.006|gt|
            necklace_of_anguish|202000|0.01|gt|
            amulet_of_torture|202000|0.01|gt|
            tormented_bracelet|201000|0.25|gt|
            digsite_pendant_5|2400|0.01||
            digsite_pendant_4|2400|0.01||
            digsite_pendant_3|2400|0.01||
            digsite_pendant_2|2400|0.01||
            digsite_pendant_1|2400|0.01||
            necklace_of_passage_5|1275|0.01|gt|
            necklace_of_passage_4|1275|0.01|t|
            necklace_of_passage_3|1275|0.01|t|
            necklace_of_passage_2|1275|0.01|t|
            necklace_of_passage_1|1275|0.01|t|
            burning_amulet_5|1250|0.007|gt|
            burning_amulet_4|1250|0.007|t|
            burning_amulet_3|1250|0.007|t|
            burning_amulet_2|1250|0.007|t|
            burning_amulet_1|1250|0.007|t|
            xerics_talisman|351|0.06||
            drakans_medallion|200|0.01||
            holy_symbol|-1|0.007|gt|
            unholy_symbol|200|0.007|gt|
            lightbearer|1000000|0.05|gt|
            ring_of_nature|8000|0.004|gt|
            ring_of_coins|8000|0.004|gt|
            amulet_of_magic_t|-1|0.01|gt|
            amulet_of_power_t|4025|0.01|gt|
            reward_casket_beginner|-1|0|s|
            reward_casket_easy|-1|0|s|
            reward_casket_medium|-1|0|s|
            reward_casket_hard|-1|0|s|
            reward_casket_elite|-1|0|s|
            reward_casket_master|-1|0|s|
            mole_slippers|1000|0.2|gt|
            frog_slippers|1000|0.2|gt|
            bear_feet|1000|0.2|gt|
            demon_feet|1000|0.2|gt|
            jester_cape|1000|0.453|gt|
            shoulder_parrot|1000|0.453|gt|
            monks_robe_top_t|500|0.907|gt|I feel the gods don't enjoy my materialistic obsessions.
            monks_robe_t|500|0.907|gt|I feel the gods don't enjoy my materialistic obsessions.
            sandwich_lady_hat|200|0.907|gt|
            sandwich_lady_top|500|0.907|gt|
            sandwich_lady_bottom|300|0.907|gt|
            rune_scimitar_ornament_kit_guthix|-1|0.5|gt|Use on a rune scimitar to make it look fancier!
            rune_scimitar_ornament_kit_saradomin|-1|0.5|gt|Use on a rune scimitar to make it look fancier!
            rune_scimitar_ornament_kit_zamorak|-1|0.5|gt|Use on a rune scimitar to make it look fancier!
            black_full_helm_t|1056|2.721|gt|
            black_platebody_t|3840|9.979|gt|
            black_platelegs_t|1920|9.071|gt|
            black_kiteshield_t|1632|5.443|gt|
            black_full_helm_g|1056|2.721|gt|
            black_platebody_g|3840|9.979|gt|
            black_platelegs_g|1920|9.071|gt|
            black_kiteshield_g|1632|5.443|gt|
            studded_body_t|850|5.443|gt|Those studs should provide a bit more protection. Nice trim too!
            studded_chaps_t|750|4.535|gt|Those studs should provide a bit more protection. Nice trim too!
            blue_wizard_hat_t|2|0.453|gt|A silly pointed hat, with colourful trim.
            blue_wizard_robe_t|15|0.907|gt|I can do magic better in this.
            studded_body_g|850|5.443|gt|Those studs should provide a bit more protection. Nice trim too!
            studded_chaps_g|750|4.535|gt|Those studs should provide a bit more protection. Nice trim too!
            blue_wizard_hat_g|2|0.453|gt|A silly pointed hat, with colourful trim.
            blue_wizard_robe_g|15|0.907|gt|I can do magic better in this.
            highwayman_mask|40|0.02|gt|Your money or your life!
            blue_beret|80|0.04|gt|
            black_beret|80|0.04|gt|
            white_beret|80|0.04|gt|
            red_beret|80|0.04|gt|
            red_elegant_shirt|2000|1|gt|
            red_elegant_legs|2000|1|gt|
            golden_chef_hat|2|0.907|gt|
            golden_apron|2|0.453|gt|
            black_cane|600|1.814|gt|A ruby topped cane.
            willow_comp_bow|300|1.8|gt|A powerful bow made from willow wood.
            adamant_full_helm_t|3520|3.175|gt|
            adamant_platebody_t|16640|11.339|gt|
            adamant_platelegs_t|6400|10.432|gt|
            adamant_kiteshield_t|5440|5.896|gt|
            adamant_full_helm_g|3520|3.175|gt|
            adamant_platebody_g|16640|11.339|gt|
            adamant_platelegs_g|6400|10.432|gt|
            adamant_kiteshield_g|5440|5.896|gt|
            green_dhide_body_t|7800|6.803|gt|Made from 100% real dragonhide. With colourful trim!
            green_dhide_chaps_t|3900|5.443|gt|Made from 100% real dragonhide. With colourful trim!
            green_dhide_body_g|7800|6.803|gt|Made from 100% real dragonhide. With colourful trim!
            green_dhide_chaps_g|3900|5.443|gt|Made from 100% real dragonhide. With colourful trim!
            ranger_boots|200|0.283|gt|Lightweight boots ideal for rangers.
            wizard_boots|200|0.283|gt|Slightly magical boots.
            holy_sandals|2000|0.5|gt|Holy footwear!
            red_headband|40|0.04|gt|A minimalist's hat.
            black_headband|40|0.04|gt|A minimalist's hat.
            brown_headband|40|0.04|gt|A minimalist's hat.
            black_elegant_shirt|2000|1|gt|
            black_elegant_legs|2000|1|gt|
            white_elegant_blouse|2000|1|gt|
            white_elegant_skirt|2000|1|gt|
            climbing_boots_g|75000|0.34|gt|Boots made for climbing. Nice trim!
            spiked_manacles|2500|3|gt|Some very spikey metal bands, better make sure I don't cut myself while walking.
            adamant_cane|1440|1.814|gt|A diamond topped cane.
            cat_mask|2400|2.267|gt|
            penguin_mask|1200|1.814|gt|
            leprechaun_hat|1000|2.267|gt|
            crier_hat|-1|0.3|gt|
            rune_full_helm_t|35200|2.721|gt|
            rune_platebody_t|65000|9.979|gt|
            rune_platelegs_t|64000|9.071|gt|
            rune_kiteshield_t|54400|5.443|gt|
            rune_full_helm_g|35200|2.721|gt|
            rune_platebody_g|65000|9.979|gt|
            rune_platelegs_g|64000|9.071|gt|
            rune_kiteshield_g|54400|5.443|gt|
            saradomin_full_helm|35200|2.721|gt|
            saradomin_platebody|65000|9.979|gt|
            saradomin_platelegs|64000|9.071|gt|
            saradomin_kiteshield|54400|5.443|gt|
            zamorak_full_helm|35200|2.721|gt|
            zamorak_platebody|65000|9.979|gt|
            zamorak_platelegs|64000|9.071|gt|
            zamorak_kiteshield|54400|5.443|gt|
            guthix_full_helm|35200|2.721|gt|
            guthix_platebody|65000|9.979|gt|
            guthix_platelegs|64000|9.071|gt|
            guthix_kiteshield|54400|5.443|gt|
            blue_dhide_body_t|9360|6.803|gt|Made from 100% real dragonhide. With colourful trim!
            blue_dhide_chaps_t|4320|5.443|gt|Made from 100% real dragonhide. With colourful trim!
            blue_dhide_body_g|9360|6.803|gt|Made from 100% real dragonhide. With colourful trim!
            blue_dhide_chaps_g|4320|5.443|gt|Made from 100% real dragonhide. With colourful trim!
            red_dhide_body_t|11230|6.803|gt|Made from 100% real dragonhide. With colourful trim!
            red_dhide_chaps_t|5180|5.443|gt|Made from 100% real dragonhide. With colourful trim!
            red_dhide_body_g|11230|6.803|gt|Made from 100% real dragonhide. With colourful trim!
            red_dhide_chaps_g|5180|5.443|gt|Made from 100% real dragonhide. With colourful trim!
            robin_hood_hat|450|0.283|gt|Endorsed by Robin Hood.
            enchanted_hat|15000|0.453|gt|A three pointed hat of magic.
            enchanted_top|120000|2.721|gt|Enchanted Wizards robes.
            enchanted_robe|80000|1.814|gt|Enchanted Wizards robes.
            pirate_hat|1|0.003||
            tan_cavalier|200|0.08|gt|
            dark_cavalier|200|0.001|gt|
            black_cavalier|200|0.08|gt|
            magic_comp_bow|2500|1.8|gt|A powerful bow made from magic wood.
            rune_cane|14400|1.814|gt|A dragonstone topped cane.
            saradomin_page_1|200|0|sgt|
            saradomin_page_2|200|0|sgt|
            saradomin_page_3|200|0|sgt|
            saradomin_page_4|200|0|sgt|
            zamorak_page_1|200|0|sgt|
            zamorak_page_2|200|0|sgt|
            zamorak_page_3|200|0|sgt|
            zamorak_page_4|200|0|sgt|
            guthix_page_1|200|0|sgt|
            guthix_page_2|200|0|sgt|
            guthix_page_3|200|0|sgt|
            guthix_page_4|200|0|sgt|
            bandos_page_1|400|0|sgt|
            bandos_page_2|400|0|sgt|
            bandos_page_3|400|0|sgt|
            bandos_page_4|400|0|sgt|
            armadyl_page_1|400|0|sgt|
            armadyl_page_2|400|0|sgt|
            armadyl_page_3|400|0|sgt|
            armadyl_page_4|400|0|sgt|
            ancient_page_1|400|0|sgt|
            ancient_page_2|400|0|sgt|
            ancient_page_3|400|0|sgt|
            ancient_page_4|400|0|sgt|
            3rd_age_range_coif|50400|1|gt|Fabulously ancient range protection crafted from white dragonhide.
            3rd_age_range_top|50800|4|gt|Fabulously ancient range protection crafted from white dragonhide.
            3rd_age_range_legs|50600|3|gt|Fabulously ancient range protection crafted from white dragonhide.
            3rd_age_vambraces|50500|1|gt|Fabulously ancient range protection crafted from white dragonhide.
            3rd_age_mage_hat|50400|0.4|gt|Fabulously ancient mage protection enchanted in the 3rd Age.
            3rd_age_robe_top|50800|2.7|gt|Fabulously ancient mage protection enchanted in the 3rd Age.
            3rd_age_robe|50600|1.8|gt|Fabulously ancient mage protection enchanted in the 3rd Age.
            3rd_age_amulet|50500|0.003|gt|Fabulously ancient mage protection enchanted in the 3rd Age.
            3rd_age_full_helmet|90000|0.907|gt|Fabulously ancient armour beaten from magical silver.
            3rd_age_platebody|200000|9.979|gt|Fabulously ancient armour beaten from magical silver.
            3rd_age_platelegs|200000|2.721|gt|Fabulously ancient armour beaten from magical silver.
            3rd_age_kiteshield|180000|2.721|gt|Fabulously ancient armour beaten from magical silver.
            3rd_age_cloak|85000|0.005|gt|A beautiful cloak woven by ancient tailors.
            3rd_age_wand|150000|3.175|gt|A beautifully crafted wand infused by ancient wizards.
            3rd_age_bow|150000|3.175|gt|A beautifully crafted bow carved by ancient archers.
            3rd_age_longsword|150000|1|gt|A beautifully crafted sword forged by ancient blacksmiths.
            3rd_age_pickaxe|97950|2.4|gt|A beautifully crafted pickaxe, shaped by ancient smiths.
            3rd_age_axe|55000|1.36|gt|A beautifully crafted axe, shaped by ancient smiths.
            3rd_age_druidic_robe_top|200000|1.5|gt|A fabulously ancient woven robe top as worn by the druids of old.
            3rd_age_druidic_robe_bottoms|200000|1.5|gt|A fabulously ancient woven robe as worn by the druids of old.
            3rd_age_druidic_cloak|200000|0.4|gt|A fabulously ancient vine cloak as worn by the druids of old.
            3rd_age_druidic_staff|200000|2|gt|A fabulously ancient gnarled staff as used by the druids of old.
            gilded_full_helm|35200|2.721|gt|
            gilded_platebody|65000|9.979|gt|
            gilded_platelegs|64000|9.071|gt|
            gilded_kiteshield|54400|5.443|gt|
            gilded_scimitar|25600|1.814|gt|Rune scimitar with gold plate.
            gilded_boots|12500|1.36|gt|Rune boots with gold plate.
            black_dhide_body_t|13480|6.803|gt|Made from 100% real dragonhide. With colourful trim!
            black_dhide_chaps_t|6220|5.443|gt|Made from 100% real dragonhide. With colourful trim!
            black_dhide_body_g|13480|6.803|gt|Made from 100% real dragonhide. With colourful trim!
            black_dhide_chaps_g|6220|5.443|gt|Made from 100% real dragonhide. With colourful trim!
            rangers_tunic|12000|3|gt|Lightweight body armour ideal for rangers.
            rangers_tights|20000|5.443|gt|Lightweight legwear ideal for rangers.
            ranger_gloves|3400|0.226|gt|Lightweight gloves ideal for rangers.
            holy_wraps|9600|0.5|gt|Holy handwear!
            fremennik_kilt|60000|3|gt|
            lava_dragon_mask|10000|2.267|gt|Do I look scary?
            royal_crown|6000|0.453|gt|
            royal_gown_top|5000|1.36|gt|
            royal_gown_bottom|5000|1.36|gt|
            royal_sceptre|5000|4.535|gt|I dub thee...
            musketeer_hat|3500|0.005|gt|
            deerstalker|2300|0.5|gt|
            afro|684|0.453|gt|
            big_pirate_hat|1500|0.001|gt|
            dragon_scimitar_ornament_kit|5000|0.5|gt|Use on the dragon scimitar to trim it with gold.
            ankou_mask|1|0.5|gt|
            ankou_top|1|0.5|gt|
            ankou_legs|1|0.5|gt|
            ankou_socks|1|0.5|gt|
            ankou_gloves|1|0.5|gt|
            mummys_head|1|0.5|gt|
            mummys_body|1|0.5|gt|
            mummys_legs|1|0.5|gt|
            mummys_feet|1|0.5|gt|
            mummys_hands|1|0.5|gt|
            bloodhound|-1|0.015||
            half_moon_spectacles|19450|0.05|gt|
            ale_of_the_gods|850|0.06|gt|Ale of the gods.
            fancy_tiara|8000|0.05|gt|
            left_eye_patch|2|0.006|gt|
            bucket_helm_g|14000|0.5|gt|A helm made from a golden bucket.
            obsidian_cape_r|60000|1.814|gt|A cape of woven obsidian plates.
            samurai_kasa|4000|0.6|gt|
            samurai_shirt|7000|2|gt|
            armadyl_godsword_ornament_kit|5000|0.5|gt|Use on an Armadyl godsword to make it look fancier!
            bandos_godsword_ornament_kit|5000|0.5|gt|Use on a Bandos godsword to make it look fancier!
            saradomin_godsword_ornament_kit|5000|0.5|gt|Use on a Saradomin godsword to make it look fancier!
            zamorak_godsword_ornament_kit|5000|0.5|gt|Use on a Zamorak godsword to make it look fancier!
            occult_ornament_kit|5000|0.5|gt|Use on an occult necklace to make it look fancier!
            torture_ornament_kit|5000|0.5|gt|Use on an amulet of torture to make it look fancier!
            anguish_ornament_kit|5000|0.5|gt|Use on a necklace of anguish to make it look fancier!
            tormented_ornament_kit|5000|0.5|gt|Use on a tormented bracelet to make it look fancier!
            dragon_defender_ornament_kit|5000|0.5|gt|Use on the dragon defender to trim it with gold.
            attack_cape|-1|0.453||
            attack_cape_t|-1|0.453||
            hitpoints_cape|-1|0.453||
            hitpoints_cape_t|-1|0.453||
            mining_cape|-1|0.453||
            mining_cape_t|-1|0.453||
            strength_cape|-1|0.453||
            strength_cape_t|-1|0.453||
            agility_cape|-1|-4||
            agility_cape_t|-1|-4||
            smithing_cape|-1|0.453||
            smithing_cape_t|-1|0.453||
            defence_cape|-1|0.453||
            defence_cape_t|-1|0.453||
            herblore_cape|-1|0.453||
            herblore_cape_t|-1|0.453||
            fishing_cape|-1|0.453||
            fishing_cape_t|-1|0.453||
            ranged_cape|-1|0.453||
            ranged_cape_t|-1|0.453||
            thieving_cape|-1|0.453||
            thieving_cape_t|-1|0.453||
            cooking_cape|-1|0.453||
            cooking_cape_t|-1|0.453||
            prayer_cape|-1|0.453||
            prayer_cape_t|-1|0.453||
            crafting_cape|-1|0.453||
            crafting_cape_t|-1|0.453||
            firemaking_cape|-1|0.453||
            firemaking_cape_t|-1|0.453||
            magic_cape|-1|0.453||
            magic_cape_t|-1|0.453||
            fletching_cape|-1|0.453||
            fletching_cape_t|-1|0.453||
            woodcutting_cape|-1|0.453||
            woodcutting_cape_t|-1|0.453||
            runecraft_cape|-1|0.453||
            runecraft_cape_t|-1|0.453||
            slayer_cape|-1|0.453||
            slayer_cape_t|-1|0.453||
            farming_cape|-1|0.453||
            farming_cape_t|-1|0.453||
            construction_cape|-1|0.453||
            construction_cape_t|-1|0.453||
            hunter_cape|-1|0.453||
            hunter_cape_t|-1|0.453||
            sailing_cape|-1|0.453||
            sailing_cape_t|-1|0.453||
            max_cape|99000|0.453||
            quest_point_cape|-1|0.453||
            achievement_diary_cape|-1|0.453||
            ardougne_cloak_1|-1|0.453||
            ardougne_cloak_2|-1|0.453||
            ardougne_cloak_3|-1|0.453||
            ardougne_cloak_4|-1|0.453||
            desert_amulet_1|-1|0.01||
            desert_amulet_2|-1|0.01||
            desert_amulet_3|-1|0.01||
            desert_amulet_4|-1|0.01||
            falador_shield_1|-1|5.443||
            falador_shield_2|-1|5.443||
            falador_shield_3|-1|5.443||
            falador_shield_4|-1|5.443||
            fremennik_sea_boots_1|-1|0.4||
            fremennik_sea_boots_2|-1|0.4||
            fremennik_sea_boots_3|-1|0.4||
            fremennik_sea_boots_4|-1|0.4||
            kandarin_headgear_1|-1|0.01||
            kandarin_headgear_2|-1|0.01||
            kandarin_headgear_3|-1|0.01||
            kandarin_headgear_4|-1|0.01||
            karamja_gloves_1|-1|0.226||
            karamja_gloves_2|-1|0.226||
            karamja_gloves_3|-1|0.226||
            karamja_gloves_4|-1|0.226||
            radas_blessing_1|-1|0.453||
            radas_blessing_2|-1|0.453||
            radas_blessing_3|-1|0.453||
            radas_blessing_4|-1|0.453||
            explorers_ring_1|-1|0.006||
            explorers_ring_2|-1|0.006||
            explorers_ring_3|-1|0.006||
            explorers_ring_4|-1|0.006||
            morytania_legs_1|-1|9.071||
            morytania_legs_2|-1|9.071||
            morytania_legs_3|-1|9.071||
            morytania_legs_4|-1|9.071||
            varrock_armour_1|-1|1||
            varrock_armour_2|-1|1||
            varrock_armour_3|-1|1||
            varrock_armour_4|-1|1||
            western_banner_1|-1|2.267||
            western_banner_2|-1|2.267||
            western_banner_3|-1|2.267||
            western_banner_4|-1|2.267||
            wilderness_sword_1|-1|1.814||
            wilderness_sword_2|-1|1.814||
            wilderness_sword_3|-1|1.814||
            wilderness_sword_4|-1|1.814||
            ensouled_goblin_head|52|0.453|gt|
            ensouled_monkey_head|104|0.453|gt|
            ensouled_imp_head|156|0.453|gt|
            ensouled_minotaur_head|228|0.453|gt|
            ensouled_scorpion_head|247|0.453|gt|
            ensouled_bear_head|260|0.453|gt|
            ensouled_unicorn_head|267|0.453|gt|
            ensouled_dog_head|286|0.453|gt|
            ensouled_chaos_druid_head|299|0.453|gt|
            ensouled_giant_head|319|0.453|gt|
            ensouled_ogre_head|338|0.453|gt|
            ensouled_elf_head|358|0.453|gt|
            ensouled_troll_head|384|0.453|gt|
            ensouled_horror_head|403|0.453|gt|
            ensouled_kalphite_head|433|0.453|gt|
            ensouled_dagannoth_head|455|0.453|gt|
            ensouled_bloodveld_head|494|0.453|gt|
            ensouled_tzhaar_head|507|0.453|gt|
            ensouled_demon_head|527|0.453|gt|
            ensouled_hellhound_head|536|0.453|gt|
            ensouled_aviansie_head|553|0.453|gt|
            ensouled_abyssal_head|585|0.453|gt|
            ensouled_dragon_head|650|0.453|gt|
            varrock_teleport_tablet|1|0|sgt|
            lumbridge_teleport_tablet|1|0|sgt|
            falador_teleport_tablet|1|0|sgt|
            camelot_teleport_tablet|1|0|sgt|
            ardougne_teleport_tablet|1|0|sgt|
            watchtower_teleport_tablet|1|0|sgt|
            trollheim_teleport_tablet|1|0|s|
            kourend_castle_teleport_tablet|1|0|sgt|
            paddewwa_teleport_tablet|1|0|sgt|
            senntisten_teleport_tablet|1|0|sgt|
            kharyrll_teleport_tablet|1|0|sgt|
            lassar_teleport_tablet|1|0|sgt|
            dareeyak_teleport_tablet|1|0|sgt|
            carrallanger_teleport_tablet|1|0|sgt|
            annakarl_teleport_tablet|1|0|sgt|
            ghorrock_teleport_tablet|1|0|sgt|
            moonclan_teleport_tablet|1|0|sgt|
            waterbirth_teleport_tablet|1|0|sgt|
            barbarian_teleport_tablet|1|0|sgt|
            khazard_teleport_tablet|1|0|sgt|
            fishing_guild_teleport_tablet|1|0|sgt|
            catherby_teleport_tablet|1|0|sgt|
            ice_plateau_teleport_tablet|1|0|sgt|
            arceuus_library_teleport_tablet|1|0|sgt|
            draynor_manor_teleport_tablet|1|0|sgt|
            salve_graveyard_teleport_tablet|1|0|sgt|
            fenkenstrains_castle_teleport_tablet|1|0|sgt|
            west_ardougne_teleport_tablet|1|0|sgt|
            harmony_island_teleport_tablet|1|0|sgt|
            cemetery_teleport_tablet|1|0|sgt|
            barrows_teleport_tablet|1|0|sgt|
            ape_atoll_teleport_tablet|1|0|sgt|
            nardah_teleport|10|0|sgt|
            digsite_teleport|10|0|sgt|
            feldip_hills_teleport|10|0|sgt|
            lunar_isle_teleport|10|0|sgt|
            mortton_teleport|10|0|sgt|
            pest_control_teleport|10|0|sgt|
            piscatoris_teleport|10|0|sgt|
            tai_bwo_wannai_teleport|10|0|sgt|
            iorwerth_camp_teleport|10|0|sgt|
            mos_leharmless_teleport|10|0|sgt|
            lumberyard_teleport|10|0|sgt|
            zul_andra_teleport|10|0|sgt|
            key_master_teleport|10|0|sgt|
            revenant_cave_teleport|10|0|sgt|
            watson_teleport|10|0|sgt|
            saradomin_staff|-1|2.267||
            guthix_staff|-1|2.267||
            zamorak_staff|-1|2.267||
            ancient_sceptre|211000|2.267||
            saradomin_cape|10|0.453||
            guthix_cape|10|0.453||
            zamorak_cape|10|0.453||
            infernal_cape|80000|1.814||
            jal_nib_rek|-1|0.015||
            tzrek_zuk|-1|0.015||
            obsidian_helmet|56320|3.628|gt|Provides excellent protection.
            obsidian_platebody|84000|9.979|gt|Provides excellent protection.
            obsidian_platelegs|67000|9.071|gt|Provides excellent protection.
            toktz_ket_xil|45000|3.628|gt|A spiked shield of obsidian.
            obsidian_cape|60000|1.814|gt|
            tzhaar_ket_om|50001|3.628|gt|
            tzhaar_ket_em|30000|1.814|gt|A mace of obsidian.
            toktz_xil_ak|40000|1.814|gt|A razor sharp sword of obsidian.
            toktz_xil_ek|25000|0.453|gt|A large knife of obsidian.
            toktz_mej_tal|35000|1.814|gt|A staff of obsidian.
            void_knight_top|48000|6.803||
            void_knight_robe|45000|8.164||
            void_knight_gloves|6052|0.453||
            void_melee_helm|9851|0.453||A melee helm from the order of the Void Knights.
            void_ranger_helm|9851|0.453||A ranger helm from the order of the Void Knights.
            void_mage_helm|9851|0.453||A mage helm from the order of the Void Knights.
            elite_void_top|48500|6.803||
            elite_void_robe|45500|8.164||
            void_knight_mace|12000|1.814||A mace used by the order of the Void Knights.
            bruma_root|1|2||
            bruma_kindling|1|2||
            bruma_herb|1|0.007||
            supply_crate|-1|5.15||
            pyromancer_hood|500|0.18||
            pyromancer_garb|800|0.2||
            pyromancer_robe|700|0.18||
            pyromancer_boots|200|1.36||
            warm_gloves|200|0.453||
            bruma_torch|-1|0.003||
            tome_of_fire|20000|1||
            burnt_page|200|0|sgt|
            phoenix|-1|0.015||
            raw_harpoonfish|1|0.5||An all natural harpoon substitute.
            harpoonfish|1|0.45||Looks like it would pack a punch.
            fish_barrel|720|1.39||
            tackle_box|850|0.95||
            big_harpoonfish|3500|0.7||
            spirit_flakes|2|0|s|
            soaked_page|170|0|sgt|
            tome_of_water|19500|1||
            tiny_tempor|-1|0.015||
            guardian_fragments|20|0|s|
            guardian_essence|30|0.402||
            elemental_guardian_stone|30|0|s|
            catalytic_guardian_stone|30|0|s|
            abyssal_pearls|50|0|s|
            intricate_pouch|50|0.396||
            hat_of_the_eye|400|0.4||
            robe_top_of_the_eye|350|0.6||
            robe_bottoms_of_the_eye|350|0.6||
            boots_of_the_eye|250|0.6||
            abyssal_needle|750|0.002||
            abyssal_lantern|100|0.5||
            catalytic_talisman|2000|0.02|gt|
            elemental_talisman|500|0.015|gt|
            abyssal_protector|-1|0.015||
            fighter_torso|65006|4||
            penance_gloves|65001|0.34||
            penance_skirt|65005|3||A skirt made from the skin of a Penance Ranger.
            granite_body|80000|22.679|gt|Provides excellent protection.
            fighter_hat|65002|2||A Penance Fighter hat.
            ranger_hat|65002|2||A Penance Ranger hat.
            runner_hat|65002|1||A Penance Runner hat.
            healer_hat|65002|2||A Penance Healer hat.
            runner_boots|65003|0.34||Boots made from a Penance Runner.
            pet_penance_queen|-1|0.015||
            castle_wars_ticket|-1|0|s|
            castle_wars_bandage|1|0.001|t|
            saradomin_halo|150000|0.3||
            zamorak_halo|150000|0.3||
            castlewars_brace|1675|0.25|gt|
            warrior_guild_token|-1|0|s|
            bronze_defender|1|0.453||
            iron_defender|1|0.453||
            steel_defender|1|0.453||
            black_defender|1|0.453||
            mithril_defender|1|0.453||
            adamant_defender|1|0.453||
            rune_defender|65004|0.453||
            dragon_defender|68007|0.453||
            infinity_hat|17000|0.453|gt|A mystic hat.
            infinity_top|140000|2.267|gt|Mystical robes.
            infinity_bottoms|90000|1.814|gt|Mystical robes.
            infinity_boots|12000|0.453|gt|Mystical boots.
            infinity_gloves|12000|0.453|gt|
            beginner_wand|1200|0.283|gt|A beginner level wand.
            apprentice_wand|1500|0.255|gt|An apprentice level wand.
            teacher_wand|2000|0.226|gt|A teacher level wand.
            master_wand|100001|0.198|gt|A master level wand.
            mages_book|500|1|gt|The magical book of the Mage.
            bones_to_peaches_tablet|-1|0|sgt|
            peach|-1|0.01||A tasty fruit.
            graveyard_bones|1|0.001||
            ivory_comb|10|0.01|gt|
            pottery_scarab|10|0.01|gt|
            pottery_statuette|10|0.1|gt|
            stone_seal|20|0.01|gt|
            stone_scarab|20|0.01|gt|
            stone_statuette|20|0.1|gt|
            gold_seal|30|0.01|gt|
            golden_scarab|30|0.01|gt|
            golden_statuette|30|0.1|gt|
            pharaohs_sceptre|100|2||
            hallowed_mark|4800|0|s|
            hallowed_crystal_shard|1000|0|s|
            hallowed_focus|10000|0.001||
            hallowed_symbol|10000|0.007||
            hallowed_hammer|10000|0.02||
            hallowed_grapple|100|0.001||
            hallowed_ring|10000|0.004||
            dark_dye|300|0.02||
            dark_acorn|5000|0.002||
            strange_old_lockpick|32000|0.002||
            ring_of_endurance|64000|0.006||
            mysterious_page|-1|0.005||
            agility_arena_ticket|-1|0|s|
            pirates_hook|89|0.3|gt|
            fruit_batta|2|0.25|gt|It actually smells quite good.
            toad_batta|2|0.25|gt|It actually smells quite good.
            worm_hole|2|0.35|gt|It actually smells quite good.
            chocolate_bomb|2|0.35|gt|Full of creamy, chocolately goodness.
            tangled_toads_legs|2|0.35|gt|It actually smells quite good.
            veg_ball|2|0.35|gt|This looks pretty healthy.
            aluft_aloft_box|-1|0.1||
            gnome_scarf|1000|0.01|gt|
            gnome_goggles|1000|0.01|gt|Tally Ho!
            mint_cake|250|0.1|gt|
            gnome_restaurant_token|-1|0.001||
            pieces_of_eight|-1|0|s|
            bitternut|1|0.003||
            sweetgrubs|1|0.003|t|
            bottle_of_rum|-1|0.907||A banana has been stuffed into the neck of this bottle.
            blue_naval_shirt|100|0.003||...You can sail the seven seas...
            blue_tricorn_hat|50|0.003||I could never look square in this.
            blue_navy_slacks|100|0.003||Not for slackers.
            green_naval_shirt|100|0.003||...You can sail the seven seas...
            green_tricorn_hat|50|0.003||I could never look square in this.
            green_navy_slacks|100|0.003||Not for slackers.
            red_naval_shirt|100|0.003||...You can sail the seven seas...
            red_tricorn_hat|50|0.003||I could never look square in this.
            red_navy_slacks|100|0.003||Not for slackers.
            purple_naval_shirt|100|0.003||...You can sail the seven seas...
            purple_tricorn_hat|50|0.003||I could never look square in this.
            purple_navy_slacks|100|0.003||Not for slackers.
            the_stuff|5|0.001||
            loar_remains|1|1.36|gt|
            phrin_remains|1|1.36|gt|
            riyl_remains|1|1.36|gt|
            asyn_remains|1|1.36|gt|
            fiyr_remains|1|1.36|gt|
            bronze_shade_key|-1|0.01||
            steel_shade_key|-1|0.01||
            silver_shade_key|-1|0.01||
            flamtaer_hammer|10000|1.36|gt|
            golovanova_seed|4|0|s|
            golovanova_fruit|3|0|s|
            bologano_seed|5|0|s|
            bologano_fruit|3|0|s|
            logavano_seed|6|0|s|
            logavano_fruit|3|0|s|
            gricollers_can|1200|0.98||
            farmers_strawhat|50|0.907||
            farmers_jacket|60|0.907||
            farmers_boro_trousers|50|0.907||
            farmers_boots|50|0.907||
            seed_box|10|1||
            smiths_tunic|380|0.62||
            smiths_trousers|340|0.71||Is that a bit of dried blood on the waist?
            smiths_boots|280|0.42||They feel a size too small.
            smiths_gloves|390|0.09||
            colossal_blade|225000|30||
            double_ammo_mould|6|4.535||
            kovacs_grog|5|0.55|gt|
            carpenters_helmet|320|1.02||
            carpenters_shirt|380|0.62||
            carpenters_trousers|340|0.71||
            carpenters_boots|280|0.42||
            plank_sack|370|5.25||
            amys_saw|280|0.8||
            hosidius_blueprints|30|0.1||
            angler_hat|50|0.5||
            angler_top|50|1||
            angler_waders|50|1||
            angler_boots|50|1||
            swamp_paste|30|0|sgt|
            rogue_mask|375|0.453||
            rogue_top|625|0.453||Black banded leather armour, a rogue's dream!
            rogue_trousers|500|0.453||Black banded leather armour, a rogue's dream!
            rogue_boots|250|0.453||Black banded leather boots, a rogue's dream!
            rogue_gloves|250|0.453||
            mystic_jewel|-1|0.002||
            magic_staff|-1|2.267|gt|
            wooden_shield|-1|2.267|gt|
            thread|-1|0|sgt|
            candle|-1|0.028|gt|
            machete|-1|1.36|gt|
            kebab|3|0.25|gt|
            waterskin_4|-1|1.1|gt|
            shantay_pass_ticket|-1|0|sgt|
            asgarnian_ale|-1|0.55|gt|
            dwarven_stout|2|0.55|gt|
            cider|-1|0.55|gt|
            lemon|-1|0.1|gt|
            lime|-1|0.1|gt|
            equa_leaves|2|0.014|gt|
            gnome_spice|2|0.15|gt|
            gianne_dough|-1|0.5|gt|
            cocktail_shaker|-1|0.15|gt|
            choc_ice|-1|0.001|gt|
            bear_fur|10|3|gt|
            noose_wand|-1|1.814|gt|
            rabbit_snare|15|0|sgt|
            teasing_stick|50|0.453|gt|
            watering_can|1|0.9||
            white_apron|-1|0.453|gt|
            brown_apron|-1|0.453|gt|
            chefs_hat|-1|0.907|gt|
            black_skirt|-1|0.907|gt|
            blue_skirt|-1|0.907|gt|
            red_cape|-1|0.453|gt|
            priest_gown_top|-1|1.814|gt|
            priest_gown_bottom|-1|1.814|gt|
            wizard_hat|-1|0.453|gt|
            desert_shirt|-1|0.907|gt|
            desert_robe|-1|0.907|gt|
            desert_boots|-1|0.226|gt|
            lizardman_fang|60|0|sgt|
            xerics_talisman_inert|350|0.06||
            book_of_the_dead|80|0.2||
            lovakite_ore|80|1.814||
            lovakite_bar|100|1.36||
            dynamite|50|1.36|gt|
            shayzien_helm_5|70|2.721||Dress like a tier 5 Shayzien soldier.
            shayzien_platebody_5|80|9.979||Dress like a tier 5 Shayzien soldier.
            shayzien_greaves_5|75|9.071||Dress like a tier 5 Shayzien soldier.
            shayzien_boots_5|65|1.36||Dress like a tier 5 Shayzien soldier.
            guild_hunter_headwear|500|1.36||The hat of the Hunters.
            guild_hunter_top|800|1.36||The top of the Hunters.
            guild_hunter_legs|700|1.36||The legs of the Hunters.
            guild_hunter_boots|200|1.36||The boots of the Hunters.
            quetzal_whistle_basic|10|0.004||
            quetzal_whistle_enhanced|10|0.004||
            quetzal_whistle_perfected|10|0.004||
            hunters_loot_sack|-1|0||
            butterfly_jar|-1|0.02|gt|
            sunlight_moth|1|0.02|gt|
            moonlight_moth|1|0.02|gt|
            sunlight_antler|31|0||
            moonlight_antler|31|0.005||
            sunlight_antelope_fur|10|3|gt|
            moonlight_antelope_fur|10|3|gt|
            raw_sunlight_antelope|1|0.34|gt|I should probably cook this first.
            cooked_sunlight_antelope|4|0.283|gt|Mmm, this looks tasty.
            raw_moonlight_antelope|1|0.34|gt|I should probably cook this first.
            cooked_moonlight_antelope|4|0.283|gt|Mmm, this looks tasty.
            sunlight_antler_bolts|31|0|sgt|
            moonlight_antler_bolts|31|0|sgt|
            hunters_sunlight_crossbow|1300|6|gt|
            blood_moon_helm|103000|1|gt|The helm of the Blood Moon.
            blood_moon_chestplate|290010|5.443|gt|The chestplate of the Blood Moon.
            blood_moon_tassets|289910|4.082|gt|The tassets of the Blood Moon.
            dual_macuahuitl|290010|3.628|gt|
            blue_moon_helm|103000|0.453|gt|The helm of the Blue Moon.
            blue_moon_chestplate|290010|3.175|gt|The chestplate of the Blue Moon.
            blue_moon_tassets|289910|1.36|gt|The tassets of the Blue Moon.
            blue_moon_spear|100000|3|gt|
            eclipse_moon_helm|103000|0.907|gt|The helm of the Eclipse Moon.
            eclipse_moon_chestplate|290010|3|gt|The chestplate of the Eclipse Moon.
            eclipse_moon_tassets|289910|1|gt|The tassets of the Eclipse Moon.
            eclipse_atlatl|160000|3.628|gt|
            atlatl_dart|490|0|sgt|
            fancy_boots|1|0.01||
            fighting_boots|1|0.01||
            pet_heron|-1|0.015||
            pet_rock_golem|-1|0.015||
            pet_beaver|-1|0.015||
            pet_baby_chinchompa|-1|0.015||
            pet_giant_squirrel|-1|0.015||
            pet_tangleroot|-1|0.015||
            pet_rocky|-1|0.015||
            pet_rift_guardian|-1|0.015||
            pet_herbi|-1|0.015||
            pet_chompy_chick|-1|0.015||
            teleport_to_house|1|0|sgt|
            numulite|10|0|sgt|
            unidentified_small_fossil|100|0.141||
            unidentified_medium_fossil|200|0.283||
            unidentified_large_fossil|300|0.566||
            unidentified_rare_fossil|1250|0.283||
            yak_hide|-1|0.453|gt|
            yak_hair|2|0.001|gt|
            raw_yak_meat|2|0.34|gt|
            suqah_hide|60|3.175||
            suqah_tooth|20|0.01||
            """);
        // Monster-drop items added with the drop-table follow-up (same parsed-osrs snapshot).
        load("""
            berserker_helm|60000|2.721|gt|This helmet is worn by berserkers.
            warrior_helm|60000|2.721|gt|This helmet is worn by warriors.
            spined_gloves|650|3.175|gt|Fremennik gloves stitched together from spined dagannoth hide.
            spined_boots|650|3.175|gt|Some finely crafted Fremennik boots, made from spined dagannoth hide.
            rock_shell_gloves|650|3.175|gt|Fremennik gloves stitched together from rock crab shell shards.
            rock_shell_boots|650|3.175|gt|Some Fremennik boots, made from the shards of a rock crab's shell.
            skeletal_gloves|650|3.175|gt|Fremennik gloves stitched together from wallasalki bones fragments.
            skeletal_boots|650|3.175|gt|Some finely crafted Fremennik boots, made from the bones of a wallasalki.
            fremennik_gloves|500|0.453|gt|These will keep my hands warm!
            fremennik_brown_shirt|250|0.907|gt|The latest in Fremennik fashion.
            fremennik_brown_cloak|250|0.453|gt|The latest fashion in Rellekka.
            ancient_ceremonial_mask|50000|0.3|gt|A ceremonial mask dedicated to an ancient god.
            ancient_ceremonial_top|100000|0.9|gt|Some ceremonial robes dedicated to an ancient god.
            ancient_ceremonial_legs|80000|0.8|gt|Some ceremonial robes dedicated to an ancient god.
            ancient_ceremonial_gloves|40000|0.2|gt|Some ceremonial gloves dedicated to an ancient god.
            ancient_ceremonial_boots|40000|0.3|gt|Some ceremonial boots dedicated to an ancient god.
            granite_helm|46000|4.535|gt|A stone helmet.
            granite_shield|56000|6.803|gt|A solid stone shield.
            mystic_lava_staff|45000|2.267|gt|It's a slightly magical stick.
            black_robe|13|0.907|gt|I can do magic better in this.
            purple_gloves|500|0.453|gt|These will keep my hands warm!
            red_gloves|500|0.453|gt|These will keep my hands warm!
            teal_gloves|500|0.453|gt|These will keep my hands warm!
            yellow_gloves|500|0.453|gt|These will keep my hands warm!
            monkey_bones|1|0.5|gt|These are small monkey bones.
            small_ninja_monkey_bones|1|0.5||These are smallish monkey bones.
            medium_ninja_monkey_bones|1|0.5||These are medium sized monkey bones.
            gorilla_bones|1|0.8||These are quite large ape bones.
            bearded_gorilla_bones|1|0.8||These are quite large ape bones.
            small_zombie_monkey_bones|1|0.5||These are smallish monkey bones. They smell extremely nauseating.
            large_zombie_monkey_bones|1|0.5||These are smallish monkey bones. They smell extremely nauseating.
            jogre_bones|1|0.8|gt|Fairly big bones which smell distinctly of Jogre.
            wyrm_bones|160|1.8|gt|I wonder how Wyrms have bones...
            drake_bones|160|1.8|gt|The bones of a ferocious Drake.
            long_bone|5|1||A Construction bone.
            curved_bone|7|1||A curved Construction bone.
            oyster_pearls|1400|0.004|gt|I could work wonders with a chisel on these pearls.
            unicorn_horn|20|0.007|gt|This horn has restorative properties.
            spined_helm|60000|2.721|gt|A helm fit for any Fremennik ranger.
            rock_shell_helm|35200|2.721|gt|Protective headwear made from crabs. Better than that sounds.
            skeletal_helm|10000|1.36|gt|Make your foes cower by wearing a skull as a helmet!
            fremennik_boots|500|0.34|gt|Very stylish!
            dagannoth_hide|60|3.175|gt|A sturdy piece of dagannoth hide.
            circular_hide|60|3.175|gt|A toughened chunk of dagannoth hide.
            flattened_hide|60|3.175|gt|A tattered chunk of dagannoth hide.
            stretched_hide|60|3.175|gt|A weathered chunk of dagannoth hide.
            rock_shell_chunk|60|3.175|gt|A spherical chunk of rock-shell.
            rock_shell_shard|60|3.175|gt|A curved piece of rock-shell.
            rock_shell_splinter|60|3.175|gt|A slim piece of rock-shell.
            skull_piece|60|3.175|gt|A fearsome looking skull.
            ribcage_piece|60|3.175|gt|A slightly damaged ribcage.
            fibula_piece|60|3.175|gt|An interesting looking bone shard.
            edible_seaweed|2|0.25|gt|Slightly damp seaweed.
            ugthanki_kebab|20|0.5|gt|A fresh kebab made from ugthanki meat.
            cockatrice_head|2000|10||I should get it stuffed!
            basilisk_head|4000|10||I should get it stuffed!
            kurask_head|6000|10||I should get it stuffed!
            crawling_hand|1000|10||I should get it stuffed!
            xerician_fabric|3|0.06|gt|An old scrap of cloth, containing remnants of Xeric's forgotten magicks.
            contract_of_bloodied_blows|75000|0.4|gt|A contract issued by Yama. Present it to him to discuss some new terms.
            contract_of_glyphic_attenuation|75000|0.4|gt|A contract issued by Yama. Present it to him to discuss some new terms.
            contract_of_divine_severance|75000|0.4|gt|A contract issued by Yama. Present it to him to discuss some new terms.
            contract_of_forfeit_breath|75000|0.4|gt|A contract issued by Yama. Present it to him to discuss some new terms.
            contract_of_sensory_clouding|75000|0.4|gt|A contract issued by Yama. Present it to him to discuss some new terms.
            """);
    }

    private ItemValues() {
    }

    private static void load(String data) {
        for (String line : data.split("\n")) {
            if (line.isBlank()) {
                continue;
            }
            String[] f = line.split("\\|", -1);
            String flags = f[3];
            REFS.put(f[0], new Ref(Integer.parseInt(f[1]), Double.parseDouble(f[2]), flags.indexOf('s') >= 0,
                    flags.indexOf('g') >= 0, flags.indexOf('t') >= 0, f[4].isEmpty() ? null : f[4]));
        }
    }

    /** Reference data for an item, or null for items OSRS doesn't have. */
    public static Ref get(String id) {
        return REFS.get(id);
    }

    /** Whether OSRS knows this item but the Grand Exchange doesn't trade it. */
    public static boolean offGrandExchange(String id) {
        Ref r = REFS.get(id);
        return r != null && !r.onGe();
    }

    /** Weight in kg (0 for unknown items). */
    public static double weight(String id) {
        Ref r = REFS.get(id);
        return r == null ? 0 : r.weight();
    }

    /** Weight while worn: Graceful pieces weigh less than nothing once equipped (OSRS worn weights). */
    public static double wornWeight(String id) {
        // Recoloured Graceful pieces (Varlamore, Agility Arena) weigh the same as the plain ones.
        String plain = id.startsWith("graceful_") ? id.replace("_varlamore", "").replace("_agility_arena", "") : id;
        return switch (plain) {
            case "graceful_hood", "graceful_gloves" -> -3;
            case "graceful_cape", "graceful_legs", "graceful_boots" -> -4;
            case "graceful_top" -> -5;
            default -> weight(id);
        };
    }

    /** Applies the reference value, weight, stackability and examine text to a registered item. */
    public static void apply(ItemDef def) {
        Ref r = REFS.get(def.id());
        if (r == null) {
            return;
        }
        if (r.value() > 0) {
            def.value(r.value());
        }
        def.weight(r.weight());
        if (r.examine() != null && def.examine() == null) {
            def.examine(r.examine());
        }
        // Stackable in OSRS (runes, ammo, tokens, caskets...) but registered as single items: let them stack.
        if (r.stackable() && def.maxStack() == 1 && stackableKind(def.kind())) {
            def.stack(64);
        }
    }

    private static boolean stackableKind(ItemDef.Kind kind) {
        return switch (kind) {
            case SIMPLE, RUNE, COINS, ARROW, SEED, TRAP, TOOL -> true;
            default -> false;
        };
    }

    /** Number of items with reference data. */
    public static int size() {
        return REFS.size();
    }
}
