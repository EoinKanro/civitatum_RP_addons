"""Failure cases for connector snapshots; successful trees are also cloned live."""
import base64
from pathlib import Path
import subprocess
import tempfile
import unittest

from github_checkpoint import GitHubBackup, GitHubBackupError


class ConnectorBackups(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.git("init", "-b", "development")
        self.git("config", "user.name", "Backup test")
        self.git("config", "user.email", "test@local.invalid")
        (self.root / "source.txt").write_bytes("Игрок\r\n".encode())
        self.binary = b"\x00\xffbinary\x00"
        (self.root / "texture.bin").write_bytes(self.binary)
        self.git("add", ".")
        self.git("commit", "-m", "Source")
        self.commit = self.git("rev-parse", "HEAD")
        self.tree = self.git("rev-parse", "HEAD^{tree}")
        self.calls = []

    def git(self, *args):
        return subprocess.check_output(["git", *args], cwd=self.root, stderr=subprocess.DEVNULL).decode().strip()

    def api(self, name, args):
        self.calls.append((name, args))
        if name == "fetch":
            if args["url"].endswith("git/refs"):
                value = [{"ref": "refs/heads/backups", "object": {"sha": "remote-head"}}]
            else:
                value = {"tree": {"sha": self.tree}}
            return {"structuredContent": value}
        raise AssertionError("No remote mutation expected")

    def backup(self, call=None):
        return GitHubBackup(self.root, {"repository_full_name": "owner/repo"}, call or self.api)

    def test_changed_remote_is_not_overwritten(self):
        with self.assertRaisesRegex(GitHubBackupError, "remote branch changed"):
            self.backup().publish(self.commit, "backups", "previous-head")
        self.assertEqual([name for name, _ in self.calls], ["fetch"])

    def test_identical_tree_needs_no_new_commit(self):
        result = self.backup().publish(self.commit, "backups", "remote-head")
        self.assertEqual(result["remote_commit"], "remote-head")
        self.assertTrue(all(name == "fetch" for name, _ in self.calls))

    def test_existing_unknown_branch_requires_inspection(self):
        def call(name, args):
            result = self.api(name, args)
            if not args["url"].endswith("git/refs"):
                result["structuredContent"]["tree"]["sha"] = "different-tree"
            return result
        with self.assertRaisesRegex(GitHubBackupError, "existing branch"):
            self.backup(call).publish(self.commit, "backups")
        self.assertTrue(all(name == "fetch" for name, _ in self.calls))

    def test_binary_encoding_and_exact_utf8_tree_are_verified(self):
        def call(name, args):
            self.calls.append((name, args))
            if name == "create_blob":
                self.assertEqual(args["encoding"], "base64")
                self.assertEqual(base64.b64decode(args["content"]), self.binary)
                return {"structuredContent": {"sha": self.git("hash-object", "texture.bin")}}
            self.assertEqual(name, "create_tree")
            entries = {e["path"]: e for e in args["tree_elements"]}
            self.assertEqual(entries["source.txt"]["content"], "Игрок\r\n")
            self.assertNotIn("sha", entries["source.txt"])
            self.assertNotIn("content", entries["texture.bin"])
            return {"structuredContent": {"sha": self.tree}}
        self.assertEqual(self.backup(call).upload_tree(self.commit), self.tree)

    def test_bad_tree_does_not_create_or_move_commits(self):
        def call(name, args):
            self.calls.append((name, args))
            if name == "create_blob":
                return {"structuredContent": {"sha": self.git("hash-object", "texture.bin")}}
            return {"structuredContent": {"sha": "wrong-tree"}}
        with self.assertRaisesRegex(GitHubBackupError, "tree verification failed"):
            self.backup(call).upload_tree(self.commit)
        self.assertFalse(any(n in ("create_commit", "update_ref", "create_branch") for n, _ in self.calls))

    def test_hosted_errors_do_not_expose_credentials(self):
        def call(name, args):
            raise RuntimeError("secret credential in upstream error")
        with self.assertRaises(GitHubBackupError) as caught:
            self.backup(call).refs()
        self.assertNotIn("secret", str(caught.exception))


if __name__ == "__main__":
    unittest.main(verbosity=2)
