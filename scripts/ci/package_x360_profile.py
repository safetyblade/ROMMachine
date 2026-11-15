#!/usr/bin/env python3
"""Generate a fresh offline Account with pinned Xenia code; never alter the runtime ELF."""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess

PIN = '779680ade075d2d2a4c589c6a8d57a8e26c116c0'
UPSTREAM = 'https://github.com/rfandango/XenDroid.git'
PREFIX = Path('emulator-core/src/main/cpp/xenia')


def run(*args):
    subprocess.run(list(map(str, args)), check=True)


def generate(source, work, output, ndk):
    source, work, output = Path(source), Path(work), Path(output)
    work.mkdir(parents=True, exist_ok=True)
    if not source.exists():
        run('git', 'clone', '--filter=blob:none', '--no-checkout', '--depth', '1', UPSTREAM, source)
        run('git', '-C', source, 'fetch', '--depth', '1', 'origin', PIN)
        run('git', '-C', source, 'sparse-checkout', 'init', '--cone')
        run('git', '-C', source, 'sparse-checkout', 'set',
            PREFIX / 'src/xenia/base', PREFIX / 'src/xenia/kernel/xam', PREFIX / 'src/xenia/kernel/util',
            PREFIX / 'third_party/crypto', PREFIX / 'third_party/fmt')
        run('git', '-C', source, 'checkout', '--detach', PIN)
        run('git', '-C', source, 'submodule', 'update', '--init', '--depth', '1', '--', PREFIX / 'third_party/fmt')
    actual = subprocess.check_output(['git', '-C', str(source), 'rev-parse', 'HEAD'], text=True).strip()
    if actual != PIN:
        raise ValueError('Profile source pin mismatch')
    root = source / PREFIX
    manager = root / 'src/xenia/kernel/xam/profile_manager.cc'
    text = manager.read_text()
    pure = text[text.index('bool ProfileManager::DecryptAccountFile'):text.index('ProfileManager::ProfileManager')]
    paths = [manager, root / 'src/xenia/kernel/xam/xam.h', root / 'src/xenia/kernel/util/crypto_utils.cc',
             root / 'src/xenia/kernel/util/crypto_utils.h', root / 'third_party/crypto/TinySHA1.hpp']
    # Reject local changes in the source routines used to generate/verify the packet.
    for path in paths:
        committed = subprocess.check_output(['git', '-C', str(source), 'show', f'{PIN}:{path.relative_to(source).as_posix()}'])
        if committed != path.read_bytes():
            raise ValueError('Modified profile source: ' + str(path))
    tool = work / 'profile_seed.cpp'
    tool.write_text('''// Host build utility; pure account routines from the pinned BSD-licensed Xenia source.
#include <cassert>
#include <cstring>
#include <fstream>
#include <vector>
#include "xenia/base/string_util.h"
#include "xenia/kernel/xam/xam.h"
#include "xenia/kernel/util/crypto_utils.h"
namespace xe::kernel::xam {
class ProfileManager { public:
    static bool DecryptAccountFile(const uint8_t*, X_XAMACCOUNTINFO*, bool devkit=false);
    static void EncryptAccountFile(const X_XAMACCOUNTINFO*, uint8_t*, bool devkit=false);
};
''' + pure + '''
}
int main(int argc, char** argv) {
    assert(argc == 2);
    using namespace xe::kernel::xam;
    X_XAMACCOUNTINFO account{};
    // X_XAMACCOUNTINFO stores its UTF-16 gamertag in big-endian order.
    const char* tag = "CARTDECK";
    auto* tag_bytes = reinterpret_cast<uint8_t*>(account.gamertag);
    for (size_t i = 0; i < 8; ++i) { tag_bytes[i * 2] = 0; tag_bytes[i * 2 + 1] = tag[i]; }
    account.SetLanguage(xe::XLanguage::kEnglish);
    account.SetCountry(xe::XOnlineCountry::kUnitedStates);
    assert(!account.IsLiveEnabled() && account.GetOnlineXUID() == 0);
    std::vector<uint8_t> packet(sizeof(account) + 0x18);
    ProfileManager::EncryptAccountFile(&account, packet.data());
    X_XAMACCOUNTINFO decoded{};
    assert(ProfileManager::DecryptAccountFile(packet.data(), &decoded));
    assert(std::memcmp(&account, &decoded, sizeof(account)) == 0);
    auto bad = packet;
    bad[0] ^= 1;
    assert(!ProfileManager::DecryptAccountFile(bad.data(), &decoded));
    std::ofstream out(argv[1], std::ios::binary);
    out.write(reinterpret_cast<const char*>(packet.data()), packet.size());
    out.close();
    assert(out.good() && packet.size() == 0x194);
}
''')
    compiler = Path(ndk) / 'toolchains/llvm/prebuilt/linux-x86_64/bin/clang++'
    executable = work / 'profile-seed'
    run(compiler, '--target=x86_64-linux-gnu', '--sysroot=/', '--gcc-toolchain=/usr', '-std=c++20', '-O2', '-mssse3',
        '-include', 'cstring', '-I' + str(root / 'src'), '-I' + str(root), '-I' + str(root / 'third_party/fmt/include'),
        tool, root / 'src/xenia/kernel/util/crypto_utils.cc', '-o', executable)
    output.mkdir(parents=True, exist_ok=True)
    account_file = output / 'Account'
    run(executable.resolve(), account_file.resolve())
    proof = {'source': UPSTREAM, 'revision': PIN, 'gamertag': 'CARTDECK', 'offline_only': True,
             'xuid_bound': False, 'native_encrypt_decrypt_verified': True, 'bytes': account_file.stat().st_size,
             'sha256': hashlib.sha256(account_file.read_bytes()).hexdigest(),
             'source_files': {p.relative_to(root).as_posix(): hashlib.sha256(p.read_bytes()).hexdigest() for p in paths},
             'note': 'Only generated offline account data ships. Host utility and source crypto constants are excluded.'}
    (output / 'profile-source.json').write_text(json.dumps(proof, indent=2) + '\n')
    print(json.dumps(proof, indent=2))


if __name__ == '__main__':
    p = argparse.ArgumentParser()
    p.add_argument('--source', required=True)
    p.add_argument('--work', required=True)
    p.add_argument('--output', required=True)
    p.add_argument('--ndk', required=True)
    a = p.parse_args()
    generate(a.source, a.work, a.output, a.ndk)
