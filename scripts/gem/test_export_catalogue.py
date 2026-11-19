import copy
import hashlib
import json
from pathlib import Path
import unittest
from export_catalogue import build

ROOT = Path(__file__).resolve().parent
# Standalone local copy may sit alongside extracted inputs; repo runs against seed/ and assets.
SEED = ROOT / 'seed' if (ROOT / 'seed').is_dir() else ROOT
ASSET = ROOT.parents[1] / 'lemuroid-app/src/main/assets/gem/catalogue-v1.json' if (ROOT / 'seed').is_dir() else ROOT / 'seed.json'

class ExportCatalogueTest(unittest.TestCase):
    def setUp(self):
        self.reviewed = json.loads((SEED / 'reviewed.json').read_text(encoding='utf-8'))
        self.approval = json.loads((SEED / 'approval.json').read_text(encoding='utf-8'))
    def test_published_seed_reproduces_exact_bytes(self):
        data = build(self.reviewed, self.approval)
        self.assertEqual(ASSET.read_bytes(), data)
        self.assertEqual('2cf0d7bab4c9ac3b2a899b9aa23a6a04a544c22574ffd9f2418a6207311f3761', hashlib.sha256(data).hexdigest())
        parsed = json.loads(data)
        self.assertEqual(16, parsed['releaseCount'])
        self.assertTrue(all(not r['verifiedSha256'] and not r['verifiedSerials'] for r in parsed['releases']))
    def test_deterministic_row_order(self):
        data = build(self.reviewed, self.approval)
        self.reviewed['releases'].reverse()
        self.assertEqual(data, build(self.reviewed, self.approval))
    def test_missing_approval(self):
        self.approval['approvedRecords'].pop()
        with self.assertRaises(ValueError): build(self.reviewed, self.approval)
    def test_extra_approval(self):
        self.approval['approvedRecords'].append(['titlescreen','none','TS:none'])
        with self.assertRaises(ValueError): build(self.reviewed, self.approval)
    def test_duplicate_release(self):
        self.reviewed['releases'].append(copy.deepcopy(self.reviewed['releases'][0]))
        with self.assertRaises(ValueError): build(self.reviewed, self.approval)
    def test_reject_editorial_field(self):
        self.reviewed['releases'][0]['roxyRating'] = 9
        with self.assertRaises(ValueError): build(self.reviewed, self.approval)
    def test_reject_invalid_hash(self):
        self.reviewed['releases'][0]['verifiedSha256'] = ['123']
        with self.assertRaises(ValueError): build(self.reviewed, self.approval)
    def test_reject_unrecognised_system(self):
        self.reviewed['releases'][0]['systemId'] = 'invented'
        with self.assertRaises(ValueError): build(self.reviewed, self.approval)
    def test_reject_version_conflict(self):
        self.approval['catalogueVersion'] = 'different'
        with self.assertRaises(ValueError): build(self.reviewed, self.approval)

if __name__ == '__main__':
    unittest.main()
