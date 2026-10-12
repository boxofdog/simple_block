# Player-facing dev-text cleanup log

Capture intrusive debug/tutorial text seen during playtesting. **Not a current implementation assignment.** Keep player-readable state through deliberate inspection and visuals, rather than deleting all useful feedback.

| ID | Observed text / presentation | Later consideration | Status |
|---|---|---|---|
| UX-001 | `Cable end put down` | Routine action narration can likely go | Open |
| UX-002 | `Cable end picked up: use an anchor to run the cable through it (32 blocks on the drum...)` | Replace verbose instructions with restrained feedback/inspection | Open |
| UX-003 | `Connecting rod fitted; it grips a cable the cart passes under` | Let visible rod communicate purpose; optional short confirmation | Open |
| UX-004 | Floating cargo label e.g. `Empty 0 / 256` | **Design decided:** ordinary freight cart's contents appear as a material surface that rises in discrete stages like a cauldron or composter, proportional to 0–256 items. Create a new texture (editable/owner-paintable art); omit persistent floating labels but keep exact count/type available on deliberate inspection. Exact stage count and material rendering are implementation details. | Specified — pending implementation after M05 |

For new items, record exact wording, where it appears, and ideally evidence screenshot. Vanilla recipe-unlock popups aren't mod dev-text. Error/refusal feedback is not automatically superfluous.
