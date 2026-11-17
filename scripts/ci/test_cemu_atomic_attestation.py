"""Regression for the actual ABI-valid, but pre-atomic-fix, CI-01 Cemu artifact."""
import copy
import hashlib
import pathlib
import unittest
from wiiu_cemu_contract import require_guest_atomic64, ATOMIC64_PATH, ATOMIC64_SOURCE_SHA256


class CemuPatchAttestation(unittest.TestCase):
    def proof(self):
        helper = pathlib.Path(__file__).resolve().parents[2] / 'native/wiiu/GuestAtomic64.h'
        return {'source_patches': [{'path': ATOMIC64_PATH, 'after_sha256': ATOMIC64_SOURCE_SHA256,
                                   'helper_sha256': hashlib.sha256(helper.read_bytes()).hexdigest()}]}

    def test_tested_15980240_artifact_with_only_usb_patch_is_rejected(self):
        with self.assertRaisesRegex(ValueError, 'rebuild only cemu'):
            require_guest_atomic64({'core_sha256': '15980240cc76f97bf270c31d1dfcf39a7cc9f9b40acc4d2c75fe30134ca669d5',
                                    'native_abi_pass': True, 'source_patches': [{'path': 'CMakeLists.txt'}]})

    def test_matching_patch_and_helper_are_accepted(self):
        require_guest_atomic64(self.proof())

    def test_changed_helper_or_cpp_or_duplicate_patch_is_rejected(self):
        for field in ['after_sha256', 'helper_sha256']:
            proof = self.proof()
            proof['source_patches'][0][field] = '0' * 64
            with self.assertRaises(ValueError):
                require_guest_atomic64(proof)
        proof = self.proof()
        proof['source_patches'].append(copy.deepcopy(proof['source_patches'][0]))
        with self.assertRaises(ValueError):
            require_guest_atomic64(proof)
