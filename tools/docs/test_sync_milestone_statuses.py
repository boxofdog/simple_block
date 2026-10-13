#!/usr/bin/env python3
"""Standard-library regression tests for milestone status synchronization."""
import json
import pathlib
import tempfile
import unittest

from sync_milestone_statuses import StatusError, read_milestone_statuses, rewrite_roadmap

STATES = ["PLANNED - NOT READY", "PLANNED - READY", "IMPLEMENTING", "PLAYTESTING", "SHIPPED"]
ROADMAP = (
    "# Roadmap\n| ID | Feature | Status | Link |\n|---|---|---|---|\n"
    "| M01 | First | **SHIPPED** | [brief](MILESTONE_01.md) |\n"
    "| M02 | Second | **PLAYTESTING** | [brief](MILESTONE_02.md) |\n"
)


class MilestoneSyncTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.docs = pathlib.Path(self.tmp.name)
        (self.docs / "REPORTING_POLICY.json").write_text(
            json.dumps({"milestone_status_schema": {"allowed_values": STATES}}), encoding="utf-8"
        )
        for mid, status in (("M01", "SHIPPED"), ("M02", "PLAYTESTING")):
            (self.docs / f"MILESTONE_{mid[1:]}.md").write_text(
                f"---\nmilestone: {mid}\nstatus: {status}\n---\n\n# Test\n", encoding="utf-8"
            )

    def test_already_synced(self):
        self.assertEqual(ROADMAP, rewrite_roadmap(ROADMAP, read_milestone_statuses(self.docs)))

    def test_only_one_cell_changed(self):
        path = self.docs / "MILESTONE_02.md"
        path.write_text(path.read_text().replace("PLAYTESTING", "SHIPPED"), encoding="utf-8")
        expected = ROADMAP.replace("**PLAYTESTING**", "**SHIPPED**")
        self.assertEqual(expected, rewrite_roadmap(ROADMAP, read_milestone_statuses(self.docs)))
        self.assertEqual(expected, rewrite_roadmap(expected, read_milestone_statuses(self.docs)))

    def test_invalid_status_rejected(self):
        path = self.docs / "MILESTONE_02.md"
        path.write_text(path.read_text().replace("PLAYTESTING", "PASSED"), encoding="utf-8")
        with self.assertRaises(StatusError):
            read_milestone_statuses(self.docs)

    def test_mismatched_milestone_id_rejected(self):
        path = self.docs / "MILESTONE_02.md"
        path.write_text(path.read_text().replace("milestone: M02", "milestone: M10"), encoding="utf-8")
        with self.assertRaises(StatusError):
            read_milestone_statuses(self.docs)

    def test_duplicate_row_rejected(self):
        with self.assertRaisesRegex(StatusError, "Duplicate"):
            rewrite_roadmap(ROADMAP + "| M02 | repeated | **PLAYTESTING** | x |\n",
                            read_milestone_statuses(self.docs))

    def test_missing_row_rejected(self):
        with self.assertRaisesRegex(StatusError, "Missing"):
            rewrite_roadmap(ROADMAP.split("| M02 |")[0], read_milestone_statuses(self.docs))

    def test_unknown_row_rejected(self):
        with self.assertRaisesRegex(StatusError, "no milestone"):
            rewrite_roadmap(ROADMAP + "| M12 | extra | **SHIPPED** | x |\n",
                            read_milestone_statuses(self.docs))


if __name__ == "__main__":
    unittest.main()
