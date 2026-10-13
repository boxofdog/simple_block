#!/usr/bin/env python3
"""Offline regression tests for documentation automation."""
import json
from pathlib import Path
import tempfile
import unittest
from automation import ci_facts, evidence, generated_block, new_milestone, readme, drift, server_test_summary

class AutomationTests(unittest.TestCase):
    def setUp(self):
        t=tempfile.TemporaryDirectory()
        self.addCleanup(t.cleanup)
        self.root=Path(t.name)
        (self.root/"docs/evidence/milestone-01").mkdir(parents=True)
        (self.root/"docs/evidence/milestone-01/a.png").write_bytes(b"test")
        (self.root/"README.md").write_text("# README\nManual prose\n")
        src=self.root/"src/main/java/io/github/boxofdog/simpleblock/train"
        src.mkdir(parents=True)
        (src/"Engine.java").write_text("class Engine {}")
        (self.root/"docs/ROADMAP.md").write_text("| M01 | First | **SHIPPED** | Brief | Done |\n")
        (self.root/"docs/MILESTONE_01.md").write_text("---\nmilestone: M01\nstatus: SHIPPED\n---\n")
        (self.root/"docs/REPORTING_POLICY.json").write_text(json.dumps(
            {"milestone_status_schema":{"allowed_values":["SHIPPED","PLANNED - NOT READY"]}}))
        for n in ("BACKLOG.md","BUG_LOG.md","UX_TEXT_LOG.md","DESIGN_REGISTER.md"):
            (self.root/"docs"/n).write_text("# "+n+"\n")
    def test_readme_idempotent_and_bounded(self):
        self.assertEqual(readme(self.root,True),0)
        result=(self.root/"README.md").read_text()
        self.assertIn("Manual prose",result)
        self.assertIn("| main | train | 1 |",result)
        self.assertEqual(readme(self.root,False),0)
        self.assertEqual(result,(self.root/"README.md").read_text())
    def test_partial_marker_errors(self):
        from automation import START
        with self.assertRaises(ValueError):
            generated_block(START+" only","data")
    def test_evidence_file_presence_not_pass(self):
        self.assertEqual(evidence(self.root,True),0)
        self.assertEqual(evidence(self.root,False),0)
        self.assertIn("does not prove",(self.root/"docs/evidence/INDEX.md").read_text())
    def test_new_requires_explicit_write(self):
        self.assertEqual(new_milestone("M02","Next",False,self.root),0)
        self.assertFalse((self.root/"docs/MILESTONE_02.md").exists())
        self.assertEqual(new_milestone("M02","Next",True,self.root),0)
        self.assertIn("PLANNED - NOT READY",(self.root/"docs/MILESTONE_02.md").read_text())
        with self.assertRaises(ValueError):
            new_milestone("M02","Overwrite",True,self.root)
    def test_duplicate_ledger_id(self):
        (self.root/"docs/BUG_LOG.md").write_text("| BUG-001 | a |\n| BUG-001 | b |\n")
        self.assertEqual(drift(self.root),1)
    def test_server_log_requires_two_matching_real_markers(self):
        self.assertEqual(server_test_summary(
            "152 GAME TESTS COMPLETE IN 42s\nAll 152 required tests passed :)"),
            {"result":"PASS","passed":152,"total":152})
        self.assertEqual(server_test_summary(
            "152 GAME TESTS COMPLETE\nAll 151 required tests passed"),"UNAVAILABLE")
        self.assertEqual(server_test_summary("152 tests are now running"),"UNAVAILABLE")

    def test_ci_no_inferred_counts_and_sha_gate(self):
        sha="a"*40
        def api(url,token):
            if "/artifacts?" in url:return {"artifacts":[{"name":"simple-block-mod","id":7,"expired":False}]}
            return {"name":"Build","run_number":31,"id":123,"html_url":"https://github.com/x/runs/123",
                "head_sha":sha,"conclusion":"success","status":"completed"}
        facts=ci_facts("boxofdog/simple_block",123,api=api)
        self.assertEqual(facts["tests"],"UNAVAILABLE")
        self.assertTrue(facts["artifacts"][0]["available"])
        path=self.root/"test.json"
        path.write_text(json.dumps({"head_sha":"b"*40,"categories":{}}))
        with self.assertRaisesRegex(ValueError,"SHA"):
            ci_facts("boxofdog/simple_block",123,test_evidence=path,api=api)

if __name__=="__main__":unittest.main()
