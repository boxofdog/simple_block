# Player-facing dev-text cleanup log

Capture intrusive debug/tutorial text seen during playtesting. **Not a current implementation assignment** except where explicitly absorbed into a future milestone. **M11 owner direction (2026-10-13) supersedes the older cart-inspection exception:** freight-cart **look-at and click-originated cargo/flavor text are both to be removed**; physical fill states and cargo transfer are the normal feedback, with exact internal counts still used for logic/tests. Keep non-cart useful feedback where appropriate.

| ID | Observed text / presentation | Later consideration | Status |
|---|---|---|---|
| UX-001 | `Cable end put down` | Routine action narration can likely go | Open |
| UX-002 | `Cable end picked up: use an anchor to run the cable through it (32 blocks on the drum...)` | Replace verbose instructions with restrained feedback/inspection | Open |
| UX-003 | `Connecting rod fitted; it grips a cable the cart passes under` | Let visible rod communicate purpose; optional short confirmation | Open |
| UX-004 | Floating freight-cart cargo label, e.g. `Empty 0 / 256` or `Iron 128 / 256` when looking | **M11 REQUIRED:** remove auto-generated cargo/status name tags even when crosshair-targeted; render actual item-form-aware progressive fill levels instead. Keep vanilla owner-named cart names, without appended counts. No numeric hover substitute. | **Scoped in M11; not yet implemented** |
| UX-005 | M05 action bar: `Loop closed: 28 blocks of return cable` / `Loop opened: the return cable is back on the drum` | Confirmation of a costly action; could shrink once the return strand and wheel are obviously visible | Open (new in M05) |
| UX-006 | M05 action bar, crank on an open route: `Close the loop to haul` | Kept short on purpose (brief: discoverable without lengthy text); review after playtest | Open (new in M05) |
| UX-007 | M05 refusals: `A loop needs a straight route: the cable turns at x, y, z`, `The return cable would run through Stone at x, y, z`, `Not enough cable for the return: it needs 26 blocks, the drum has 9` | Refusal feedback; the coordinates may be more than a player needs | Open (new in M05) |
| UX-008 | M05 engine and couplings: `Needs coal`, `The firebox is full`, `Empty the cart first`, `Too far apart to couple`, `Both ends of this cart are coupled` | Short refusals; probably fine; M11 removes **MinecartCargo-generated** click narration, not all train/coupling feedback | Open (new in M05) |
| UX-009 | Minecart click/status overlays: `MinecartCargo.interact` sends `status()`, `Full`, `No room`, `Too heavy`, `This cart carries...` and load/unload messages | **M11 REQUIRED:** remove freight-cart-originated status/flavor/actionbar text from all click cases, not only from empty-hand inspection. Gameplay actions and real safeguards remain; no replacement counters, GUI or hover prompts. | **Scoped in M11; not yet implemented** |

For new items, record exact wording, where it appears, and ideally evidence screenshot. Vanilla recipe-unlock popups aren't mod dev-text. Error/refusal feedback is not automatically superfluous.
