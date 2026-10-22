# Round 44 (interrupted, not merged)

Round 44 was stopped part-way to save usage. Each patch left here is unreviewed work from one scope's worktree, diffed
against the integration branch at 3623edf7, and is not in the build. To resume, apply a patch on a fresh branch
(`git apply --3way docs/wip/round44/scopeN.patch`), finish it, run `tools/loop/verify.sh`, then review and merge as usual.

- **real-walks** (playability), done: its 3 commits were resumed and finished on their own branch (RealPlayer, GridPath and
  RealReaction, NoHealChecks, the walk-out budget, prayers through the keys, Zulrah's clouds on the strike floor) and merge
  with it, so its patch was removed. Don't re-apply an old copy of `scope1.patch`: it would duplicate that code.
- `scope2.patch`, **slayer2**: per-area task-only gating and new task-only rooms; Task Storage, Duly Noted, the rune
  pouch, shop items and helmets; Vannaka in the Edgeville Dungeon, Konar on the summit, Larran's chest on the ship, bell
  radius, Otto repairs hastas; Konar names a location; Mortimer and five missing superiors. The last commit ("WIP slayer2")
  was mid-edit when stopped and has not been compiled; the real-player Slayer self-test was not written yet.
- `scope3.patch`, **questpolish**: quest lamps, tomes and skill choices; the Balance Elemental drain, Stone of Jas fight
  and Vanstrom rework; The Golem and Hazeel Cult puzzles per quest-helper; WGS chasm and temple in the Lumbridge Swamp
  Caves, the DS2 tomb in the Shayzien Crypts, Legends' lost crystal pieces, Surok's special. 3 commits; not reviewed.
- **bosspolish** never started (Yama's familiar contract, underground lairs, GWD/Nex numbers, Scurrius, Araxxor eggs,
  Nightmare outside damage, Duke's vat, Sol's empty drop table): see the scope text in docs/ROUND_LOG.md, round 44.
