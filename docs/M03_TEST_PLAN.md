# Simple Block — M03 Test Plan / Acceptance Matrix

Use existing repo's test harness wherever possible; scripted/integration tests plus a real Minecraft client are expected for a new world entity and its interactions. A compile alone is **not** confirmation. Label each PASS / FAIL / NOT RUN with evidence.

| Test | Scenario | Expected |
|---|---|---|
| T01 | Baseline existing build, tests, updater artifacts | Existing M01/M02 behavior and working packaging preserved |
| T02 | Spawn/craft freight cart and place on straight rail | Working distinct open-topped cart on rails, not a rideable chest cart |
| T03 | Push empty and loaded carts over straight, curved, sloped and powered rails | Traverses vanilla rail configurations, without crashing or losing freight |
| T04 | Load 1, then 63, then multiple stacks of same raw resource | Exact amount added, count reported, player stacks decremented exactly once |
| T05 | Load 255 and attempt 64 more | Stops at **256**, no overflow, uninserted items stay with player |
| T06 | Try copper cargo in a cart already holding raw iron | Rejected with no cargo swap, loss, or mixed resource |
| T07 | Fully unload; load a different eligible resource | Cargo identity clears at zero and can be reused |
| T08 | Load unapproved items including iron block, gear, chest, shulker, bucket or other storage | Rejected; no compound storage or block-compression bypass |
| T09 | Withdraw with nearly full or full personal inventory | Only accepted items transferred; rest remains in cart |
| T10 | Withdraw when M02 shared metal weight cap allows only partial amount | Only the allowed part enters player inventory; excess remains; no negative cargo |
| T11 | Save/quit, reload, leave/re-enter chunk while cart partly/fully loaded | Exact cargo type and quantity persist and reconcile on client/server |
| T12 | Break/destroy loaded cart, then pick up resulting empty cart | Cargo drops as world items, empty cart item does not embed freight; neither loss nor duplication |
| T13 | Kill/remove cart through at least one alternate destruction path (e.g. attack/explosion) | Exactly one safe cargo payout, no duplication or silent losses; document unsupported paths |
| T14 | Stress quick repeated clicks/multiple players if test setup allows | No duplicate transfers, stale state or client-only inventory authority |
| T15 | Attempt mounting and inappropriate storage interactions | Cannot ride; cart isn't a portable inventory or ordinary multi-slot chest |
| T16 | Dedicated-server or integrated-server end-to-end test | Server authoritatively controls cargo and accepts/rejects valid interactions |
| T17 | Existing M01/M02 smoke tests | Beds/phatoms/elytra/shulker behavior, heavy cap, inventory and lighting remain sane |
| T18 | Full build, Minecraft client launch, actual screenshots and release artifact check | Runnable package via existing pipeline, evidence at milestone-specific path |
| T19 | Attempt to craft ender chest; access it from ordinary survival item/loot sources | Inaccessible in survival; no usable recipe/loot path |
| T20 | Obtain/spawn ender chest for a direct restriction test; attempt placement, opening and item transfers | No functioning remote storage; any technical registration cannot be exploited in playable worlds |
| T21 | Attempt chest-minecart recipe, obtaining and placement on rails | Can't obtain/place a usable vanilla chest minecart in normal play |
| T22 | Try inventory interaction, hopper insertion/extraction, chest-minecart spawn/drop bypass | No player-usable vanilla chest-minecart freight or duplicated items |
| T23 | Check vanilla **ordinary minecart** placement and movement | Ordinary cart still works; only chest variety is restricted |
| T24 | Tame/load/travel with donkey, mule and llama cargo | Pack animals retain working inventories and physical cargo transport |
| T25 | Re-run M01 shulker-box controls after ender-chest changes | Existing removals still stand; no accidental broad storage breakage |

**Negative controls:** compare against unmodified vanilla in a separate test instance if useful; **chest minecarts must now be unavailable/unusable in the modded client**. A successful ordinary minecart test is not proof the forbidden chest-minecart path has been closed.

**Manual playtest:** Load a freight cart beside a substantial mine, push it around a small loop, unload at a simple outpost, and describe actual usability/friction. Verify no ender chest or old chest-cart storage route works; verify a donkey or mule can still carry items along the same route. The freight cart does *not* need locomotive speed or coupling yet.

**Evidence / report:** Keep screenshots, short test logs, and source/test references under `docs/evidence/milestone-03/` (or existing repo convention). Report counts and failures honestly; do not record a feature as tested merely because source inspection suggests it works.
