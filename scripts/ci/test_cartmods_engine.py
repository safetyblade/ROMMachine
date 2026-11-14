#!/usr/bin/env python3
"""Compile real production Kotlin file engines and run synthetic JVM checks.

Uses the Gradle cache, or KOTLIN_COMPILER_CLASSPATH and KOTLIN_STDLIB.
This does not replace Android, Room migration, APK or gameplay checks.
"""
import os
from pathlib import Path
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[2]
CACHE = Path(os.environ.get('GRADLE_USER_HOME', Path.home() / '.gradle')) / 'caches/modules-2/files-2.1'


def jar(group, artifact, version):
    matches = list((CACHE / group / artifact / version).glob('*/*.jar'))
    if not matches:
        raise SystemExit(f'Missing cached {group}:{artifact}:{version}; run Gradle dependency resolution first')
    return str(matches[0])


def main():
    stdlib = os.environ.get('KOTLIN_STDLIB') or jar('org.jetbrains.kotlin', 'kotlin-stdlib', '2.0.21')
    compiler = os.environ.get('KOTLIN_COMPILER_CLASSPATH') or os.pathsep.join([
        jar('org.jetbrains.kotlin', 'kotlin-compiler-embeddable', '2.0.21'),
        stdlib,
        jar('org.jetbrains.kotlin', 'kotlin-script-runtime', '2.0.21'),
        jar('org.jetbrains.kotlin', 'kotlin-reflect', '2.0.21'),
        jar('org.jetbrains.intellij.deps', 'trove4j', '1.0.20200330'),
        jar('org.jetbrains', 'annotations', '13.0'),
        jar('org.jetbrains.kotlinx', 'kotlinx-coroutines-core-jvm', '1.6.4'),
    ])
    java = str(Path(os.environ['JAVA_HOME']) / 'bin/java') if 'JAVA_HOME' in os.environ else 'java'
    sources = sorted((ROOT / 'retrograde-app-shared/src/main/java/com/swordfish/lemuroid/lib/mods').glob('*.kt'))
    with tempfile.TemporaryDirectory(prefix='cartmods-jvm-') as output:
        subprocess.run([java, '-cp', compiler, 'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler',
                        '-no-stdlib', '-no-reflect', '-jvm-target', '17', '-classpath', stdlib,
                        '-d', output, *map(str, sources), str(ROOT / 'lemuroid-app/src/test/java/com/swordfish/lemuroid/app/mods/ModEngineChecks.kt')], check=True)
        subprocess.run([java, '-cp', os.pathsep.join([output, stdlib]), 'com.swordfish.lemuroid.app.mods.ModEngineChecks'], check=True)


if __name__ == '__main__':
    main()
