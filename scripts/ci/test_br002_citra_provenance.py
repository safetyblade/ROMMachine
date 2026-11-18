import hashlib
import struct
import unittest

from verify_citra_apk_provenance import details


class CitraProvenanceTest(unittest.TestCase):
    def test_binary_identity_and_abi(self):
        data = bytearray(120)
        data[:6] = b"\x7fELF\x02\x01"
        struct.pack_into("<H", data, 18, 183)
        struct.pack_into("<Q", data, 32, 64)
        struct.pack_into("<HH", data, 54, 56, 1)
        struct.pack_into("<I", data, 64, 1)
        struct.pack_into("<Q", data, 112, 16384)
        data = bytes(data)
        blob = hashlib.sha1(b"blob " + str(len(data)).encode() + b"\0" + data).hexdigest()
        proof = details(data, data, blob)
        self.assertTrue(proof["all_load_segments_16k_alignment"])
        self.assertIsNone(proof["upstream_source_revision"])
        with self.assertRaises(ValueError):
            details(data + b"!", data, blob)
        with self.assertRaises(ValueError):
            details(data, data, "0" * 40)
