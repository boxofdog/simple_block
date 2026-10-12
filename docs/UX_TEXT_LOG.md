# Player-facing dev-text cleanup log

Capture intrusive debug/tutorial text seen during playtesting. **Not a current implementation assignment.** Keep player-readable state through deliberate inspection and visuals, rather than deleting all useful feedback.

| ID | Observed text / presentation | Later consideration | Status |
|---|---|---|---|
| UX-001 | `Cable end put down` | Routine action narration can likely go | Open |
| UX-002 | `Cable end picked up: use an anchor to run the cable through it (32 blocks on the drum...)` | Replace verbose instructions with restrained feedback/inspection | Open |
| UX-003 | `Connecting rod fitted; it grips a cable the cart passes under` | Let visible rod communicate purpose; optional short confirmation | Open |
| UX-004 | Floating cargo label e.g. `Empty 0 / 256` | **Design decided:** ordinary freight cart's contents appear as a material surface that rises in discrete stages like a cauldron or composter, proportional to 0–256 items. Create a new texture (editable/owner-paintable art); omit persistent floating labels but keep exact count/type available on deliberate inspection. Exact stage count and material rendering are implementation details. | Specified — pending implementation after M05 |
| UX-005 | M05 action bar: `Loop closed: 28 blocks of return cable` / `Loop opened: the return cable is back on the drum` | Confirmation of a costly action; could shrink once the return strand and wheel are obviously visible | Open (new in M05) |
| UX-006 | M05 action bar, crank on an open route: `Close the loop to haul` | Kept short on purpose (brief: discoverable without lengthy text); review after playtest | Open (new in M05) |
| UX-007 | M05 refusals: `A loop needs a straight route: the cable turns at x, y, z`, `The return cable would run through Stone at x, y, z`, `Not enough cable for the return: it needs 26 blocks, the drum has 9` | Refusal feedback; the coordinates may be more than a player needs | Open (new in M05) |
| UX-008 | M05 engine and couplings: `Needs coal`, `The firebox is full`, `Empty the cart first`, `Too far apart to couple`, `Both ends of this cart are coupled` | Short refusals; probably fine | Open (new in M05) |

For new items, record exact wording, where it appears, and ideally evidence screenshot. Vanilla recipe-unlock popups aren't mod dev-text. Error/refusal feedback is not automatically superfluous.
