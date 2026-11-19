import sqlite3
import unittest

from b7_catalogue_coverage import PLATFORMS, project


class CatalogueProjectionTest(unittest.TestCase):
    def setUp(self):
        self.db = sqlite3.connect(":memory:")
        self.addCleanup(self.db.close)
        self.db.executescript("""
            CREATE TABLE meta(key TEXT, value TEXT);
            INSERT INTO meta VALUES ('schema', '1'), ('version', '2026.10.09');
            CREATE TABLE platform(id TEXT, name TEXT);
            CREATE TABLE work(id TEXT, kind TEXT);
            CREATE TABLE genre(id TEXT, name TEXT);
            CREATE TABLE release(id TEXT, platform_id TEXT, title TEXT,
                 title_latin TEXT, genre_id TEXT, work_id TEXT, licensed TEXT);
            INSERT INTO work VALUES ('w1', 'game'), ('w2', 'add-on'), ('w3', 'compilation');
            INSERT INTO genre VALUES ('g', 'Strategy');
        """)
        for name, system in PLATFORMS.items():
            self.db.execute("INSERT INTO platform VALUES (?, ?)", (system, name))
        self.db.execute("INSERT INTO platform VALUES ('arcade', 'Arcade')")
        self.systems = set(PLATFORMS.values())

    def insert(self, ident, system="gba", title="Example", latin="", genre="g",
               work="w1", licensed="commercial"):
        self.db.execute("INSERT INTO release VALUES (?, ?, ?, ?, ?, ?, ?)",
                        (ident, system, title, latin, genre, work, licensed))

    def test_filters_and_preserves_regional_release_ids(self):
        self.insert("r0000001")
        self.insert("r0000002", title=" ", latin="Example")
        self.insert("r0000003", licensed="demo")
        self.insert("r0000004", work="w2")
        self.insert("r0000005", system="arcade")
        self.insert("r0000006", system="gbc", work="w3", genre=None)
        version, rows = project(self.db, self.systems)
        self.assertEqual(version, "2026.10.09")
        self.assertEqual([r["releaseId"] for r in rows],
                         ["TS:r0000001", "TS:r0000002", "TS:r0000006"])
        self.assertEqual(rows[1]["title"], "Example")
        self.assertEqual(rows[0]["primaryGenre"], "Strategy")
        self.assertNotIn("primaryGenre", rows[2])
        self.assertNotIn("verifiedSha256", rows[0])

    def test_missing_platform_fails_closed(self):
        self.db.execute("DELETE FROM platform WHERE id = 'gbc'")
        with self.assertRaisesRegex(ValueError, "Platform map"):
            project(self.db, self.systems)

    def test_duplicate_source_id_fails(self):
        self.insert("r0000001")
        self.insert("r0000001", system="gbc")
        with self.assertRaisesRegex(ValueError, "duplicate"):
            project(self.db, self.systems)

    def test_unknown_schema_fails(self):
        self.db.execute("UPDATE meta SET value='2' WHERE key='schema'")
        with self.assertRaisesRegex(ValueError, "schema"):
            project(self.db, self.systems)


if __name__ == "__main__":
    unittest.main()
