# Prompt for continuing in a new Claude Code session

Paste this into a new Claude Code (claude.ai/code) session that has the `WreatheR6/RuneCraft` repository attached.

---

You're continuing an ongoing project: RuneCraft, a NeoForge 1.21.1 Minecraft mod that recreates all of Old School RuneScape in a custom Gielinor dimension. Levels and XP should be faithful to OSRS, and so should quests, the world, bosses, raids, skilling and models.

Work on the branch `claude/busy-cray-anf3uw`. Pushing to it runs CI, which builds the jar, boots a real server and client, and publishes the jar to the `dev-latest` GitHub release.

Read these first:
1. `docs/WORKFLOW.md`: the audit/merge loop, the verification tools, the GitHub data sources that work (the OSRS wiki itself is blocked), the agent brief and the backlog (the round log is docs/ROUND_LOG.md).
2. `docs/CONTENT_GUIDE.md`: how content modules, items, structures and assets fit together.
3. `README.md`.

Then continue the loop described in `docs/WORKFLOW.md`:
- Pick 4 independent scopes from the backlog, or audit areas not yet covered against the wiki data.
- Run one sub-agent per scope in its own git worktree, using the agent brief from WORKFLOW.md plus the scope.
- Merge each finished branch with `tools/loop/merge_branch.sh <branch>`. Resolve code conflicts by hand, keeping both sides' intent, and nothing is merged until `tools/loop/verify.sh` prints VERIFY OK.
- Push, then wait for CI to go green and confirm `dev-latest` shows the new commit. If CI fails, read the job log, fix the problem and push again.
- At the end of each round, update `docs/ROUND_LOG.md` and the backlog in `docs/WORKFLOW.md`, commit and push.
- Keep going round after round until I tell you to stop or set a usage limit. Give me a short summary after each round.

Rules:
- Commit often, and push only to `claude/busy-cray-anf3uw`.
- Don't open pull requests unless I ask.
- Keep ids stable, and mark guessed values `// approx`.
- Enums whose ordinals are saved or sent to the client (for example `MenuType` and `AgilityCourse`) only get new values appended at the end.

What I've asked for along the way:
- Models and textures as true to OSRS as possible, at the quality of the Jad concept in `docs/reference/jad_concept.webp`.
- Enemies only spawn where they do in OSRS.
- Detailed, faithful buildings.
- The skills panel at the bottom right.
- A larger jar is fine.
