"""Isolated Git repositories exercise recovery and non-destructive checkpoints."""
import contextlib
import importlib.util
import io
import json
from pathlib import Path
import subprocess
import tempfile
import unittest
from unittest import mock

SPEC = importlib.util.spec_from_file_location("checkpoint", Path(__file__).with_name("git_checkpoint.py"))
backup = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(backup)


class Checkpoints(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.folder = Path(self.temp.name)
        self.repo = self.folder / "source"
        self.remote = self.folder / "remote.git"
        self.repo.mkdir()
        self.original_root = backup.ROOT
        backup.ROOT = self.repo
        self.addCleanup(setattr, backup, "ROOT", self.original_root)
        self.git("init", "-b", "development")
        self.git("config", "user.name", "Checkpoint test")
        self.git("config", "user.email", "test@local.invalid")
        (self.repo / "src").mkdir()
        (self.repo / "src/one.java").write_text("baseline\n")
        (self.repo / "notes.txt").write_text("baseline notes\n")
        self.git("add", ".")
        self.git("-c", "commit.gpgsign=false", "commit", "-m", "Baseline")
        self.head = self.git("rev-parse", "HEAD")
        self.git("init", "--bare", str(self.remote))
        self.git("remote", "add", "origin", str(self.remote))

    def git(self, *args):
        p = subprocess.run(["git", *args], cwd=self.repo, capture_output=True, text=True, check=True)
        return p.stdout.strip()

    def once(self):
        with contextlib.redirect_stdout(io.StringIO()):
            return backup.checkpoint()

    def status(self):
        return json.loads((backup.state_dir() / "status.json").read_text())

    def test_snapshot_preserves_branch_index_and_excludes_unrelated_changes(self):
        (self.repo / "src/one.java").write_text("changed\n")
        (self.repo / "src/two.java").write_text("new\n")
        (self.repo / "notes.txt").write_text("user staged\n")
        self.git("add", "notes.txt")
        index = self.git("diff", "--cached")
        record = self.once()
        commit = record["commit"]
        self.assertEqual(self.git("rev-parse", "HEAD"), self.head)
        self.assertEqual(self.git("diff", "--cached"), index)
        self.assertEqual(self.git("show", commit + ":src/one.java"), "changed")
        self.assertEqual(self.git("show", commit + ":src/two.java"), "new")
        self.assertEqual(self.git("show", commit + ":notes.txt"), "baseline notes")
        self.assertTrue(record["remote_verified"])
        self.assertEqual(self.git("ls-remote", "origin", "refs/heads/" + backup.BRANCH).split()[0], commit)

    def test_unchanged_source_creates_no_empty_commit(self):
        one = self.once()
        two = self.once()
        self.assertEqual(one["commit"], two["commit"])
        self.assertTrue(two["remote_verified"])

    def test_deleted_source_recorded_and_previous_source_recoverable(self):
        one = self.once()["commit"]
        (self.repo / "src/one.java").unlink()
        two = self.once()["commit"]
        self.assertNotIn("src/one.java", self.git("ls-tree", "-r", "--name-only", two))
        self.assertEqual(self.git("show", one + ":src/one.java"), "baseline")

    def test_failed_upload_retains_local_snapshot_and_last_verified_commit(self):
        old = self.once()["commit"]
        self.git("remote", "set-url", "origin", str(self.folder / "missing.git"))
        (self.repo / "src/one.java").write_text("unsent\n")
        with self.assertRaises(backup.BackupError):
            self.once()
        record = self.status()
        self.assertFalse(record["remote_verified"])
        self.assertEqual(record["last_verified_commit"], old)
        self.assertEqual(self.git("show", record["commit"] + ":src/one.java"), "unsent")

    def test_divergent_remote_is_not_overwritten(self):
        old = self.once()["commit"]
        tree = self.git("rev-parse", old + "^{tree}")
        remote_only = self.git("-c", "commit.gpgsign=false", "commit-tree", tree, "-p", old, "-m", "Another writer")
        self.git("push", "origin", remote_only + ":refs/heads/" + backup.BRANCH)
        (self.repo / "src/one.java").write_text("local change\n")
        with self.assertRaises(backup.BackupError):
            self.once()
        actual = self.git("ls-remote", "origin", "refs/heads/" + backup.BRANCH).split()[0]
        self.assertEqual(actual, remote_only)
        self.assertFalse(self.status()["remote_verified"])

    def test_checkpoint_lock_prevents_concurrent_writes(self):
        with backup.lock(backup.state_dir()):
            with self.assertRaises(backup.BackupError):
                self.once()

    def test_timer_600_seconds_and_inactive_lease(self):
        directory = backup.state_dir()
        clock = [0]

        def sleep(seconds):
            clock[0] += seconds
            if clock[0] >= 1900:
                (directory / "stop").write_text("stop")

        with mock.patch.object(backup.time, "time", side_effect=lambda: clock[0]), \
             mock.patch.object(backup.time, "monotonic", side_effect=lambda: clock[0]), \
             mock.patch.object(backup.time, "sleep", side_effect=sleep), \
             mock.patch.object(backup, "checkpoint") as checkpoint, \
             contextlib.redirect_stdout(io.StringIO()):
            backup.watch_locked(directory, "origin", backup.BRANCH)
        # Immediate upload and t=600; t=1200 and t=1800 skipped after lease expiry.
        self.assertEqual(checkpoint.call_count, 2)
        self.assertEqual(backup.INTERVAL, 600)
        self.assertFalse((directory / "watch.json").exists())

    def test_timer_does_not_start_without_successful_initial_push(self):
        directory = backup.state_dir()
        with mock.patch.object(backup, "checkpoint", side_effect=backup.BackupError("offline")), \
             contextlib.redirect_stdout(io.StringIO()):
            with self.assertRaises(backup.BackupError):
                backup.watch_locked(directory, "origin", backup.BRANCH)
        self.assertFalse((directory / "watch.json").exists())


if __name__ == "__main__":
    unittest.main(verbosity=2)
