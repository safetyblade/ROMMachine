#!/usr/bin/env python3
"""PS2-NEXT-C debug JNI callback contract from audited ARMSX2 source.

Pins method signatures from ARMSX2 7a999782377e61fb96eca65e476b5ec0d182058e:
platforms/android/app/src/main/cpp/native-lib.cpp and
common/HTTPDownloaderAndroid.cpp. This guards debug bridge source, not
a running native library or full production Java/HTTP feature compatibility.
"""
import pathlib
import re
import unittest

REPO = pathlib.Path(__file__).resolve().parents[2]
JAVA = REPO / "lemuroid-app/src/debug/java/kr/co/iefriends/pcsx2/NativeApp.java"
HTTP = REPO / "lemuroid-app/src/debug/java/kr/co/iefriends/pcsx2/HttpClient.java"
BIOS = REPO / "lemuroid-app/src/debug/java/com/armsx2/BiosInfo.kt"

# C++ GetStaticMethodID entries from pinned native-lib.cpp and their JNI
# descriptors, intentionally exact to catch silent API signature drift.
CALLBACKS = {
    "vmSetPaused": "(Z)V",
    "onPadRumble": "(III)V",
    "playSound": "(Ljava/lang/String;)V",
    "onAutoTuneCommitted": "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V",
    "onAutoTuneDisplayScale": "(I)V",
    "findSiblingChds": "(Ljava/lang/String;)[Ljava/lang/String;",
    "openContentUri": "(Ljava/lang/String;)I",
    "createDirectoryPath": "(Ljava/lang/String;)Z",
    "createFilePath": "(Ljava/lang/String;)Z",
}
JAVA_TYPES = {
    "boolean": "Z", "int": "I", "void": "V", "String": "Ljava/lang/String;",
    "String[]": "[Ljava/lang/String;", "byte[]": "[B",
    "Response": "Lkr/co/iefriends/pcsx2/HttpClient$Response;",
}


def java_methods(source):
    # Source assertions only. Never assume Java compiles from a regex parse.
    parsed = {}
    for match in re.finditer(
        r"public\s+static\s+(?!native\b)(\w+(?:\[\])?)\s+"
        r"(\w+)\s*\(([^)]*)\)", source, re.MULTILINE,
    ):
        return_type, name, params = match.groups()
        args = []
        for param in params.split(","):
            words = param.strip().split()
            if words:
                args.append(JAVA_TYPES.get(words[0], "UNKNOWN"))
        parsed[name] = "(" + "".join(args) + ")" + JAVA_TYPES.get(return_type, "UNKNOWN")
    return parsed


class PinnedPs2JniContract(unittest.TestCase):
    def test_pinned_native_app_callbacks(self):
        source = JAVA.read_text(encoding="utf-8")
        methods = java_methods(source)
        for name, descriptor in CALLBACKS.items():
            with self.subTest(name=name):
                self.assertEqual(methods.get(name), descriptor)

    def test_disabled_network_http_binding(self):
        source = HTTP.read_text(encoding="utf-8")
        self.assertIn("package kr.co.iefriends.pcsx2;", source)
        self.assertIn("static final class Response", source)
        self.assertRegex(source, r"public\s+int\s+statusCode")
        self.assertRegex(source, r"public\s+String\s+contentType")
        self.assertRegex(source, r"public\s+byte\[\]\s+data")
        self.assertEqual(
            java_methods(source).get("doRequest"),
            "(Ljava/lang/String;Ljava/lang/String;[BLjava/lang/String;I)"
            "Lkr/co/iefriends/pcsx2/HttpClient$Response;",
        )
        self.assertNotIn("HttpURLConnection", source)
        self.assertNotIn("new URL(", source)
        self.assertIn("return new Response();", source)

    def test_bios_info_constructor_fields(self):
        content = BIOS.read_text(encoding="utf-8")
        for declared in (
            "val version: Int", "val region: Int",
            "val description: String", "val zone: String",
        ):
            self.assertIn(declared, content)

    def test_native_methods_do_not_imply_complete_host(self):
        native = JAVA.read_text(encoding="utf-8")
        self.assertIn("public static native boolean runVMThread", native)
        self.assertIn("return -1;", native)  # Unsupported SAF stays unavailable.
        self.assertIn("return new String[0];", native)  # No arbitrary sibling discs.


if __name__ == "__main__":
    unittest.main()
