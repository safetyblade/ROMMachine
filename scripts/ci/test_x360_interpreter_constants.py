"""Exercise actual pinned map-selection and register-write code, without a GPU.

Only the surrounding types are a fixture. The tested method bodies come from
Xenia, not a second implementation of its dirty tracking.
"""
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
import urllib.request

sys.path.insert(0, str(Path(__file__).parent / 'xbox360'))
from fix_interpreter_constants import fix

PIN = '779680ade075d2d2a4c589c6a8d57a8e26c116c0'
REL = 'emulator-core/src/main/cpp/xenia/src/xenia/gpu/vulkan/vulkan_command_processor.cc'


def method(text, name):
    start = text.index('void VulkanCommandProcessor::' + name + '(')
    brace = text.index('{', start)
    level = 1
    end = brace + 1
    while level:
        level += (text[end] == '{') - (text[end] == '}')
        end += 1
    return text[start:end]


def fixture(text):
    start = text.index('  const Shader::ConstantRegisterMap& float_constant_map_vertex =',
                       text.index('bool VulkanCommandProcessor::UpdateBindings('))
    end = text.index('  uint32_t float_constant_count_pixel = 0;', start)
    mapping = text[start:end]
    return r'''
#include <algorithm>
#include <cstdint>
#include <cstring>
#include <iostream>
constexpr uint32_t XE_GPU_REG_SHADER_CONSTANT_000_X = 0x4000;
namespace xe {
void copy_and_swap_32_unaligned(uint32_t* dst, uint32_t* src, uint32_t n) {
  for (uint32_t i = 0; i < n; ++i) dst[i] = __builtin_bswap32(src[i]);
}
}
struct SpirvShaderTranslator {
  enum { kConstantBufferFloatVertex, kConstantBufferFloatPixel };
};
struct Shader {
  struct ConstantRegisterMap { uint64_t float_bitmap[4]{1,0,0,0}; uint32_t float_count = 1; };
};
struct VertexShader {
  Shader::ConstantRegisterMap map;
  const Shader::ConstantRegisterMap& constant_register_map() const { return map; }
};
struct Registers { uint32_t values[0x5000]{}; };
class VulkanCommandProcessor {
public:
  Registers registers;
  Registers* register_file_ = &registers;
  bool frame_open_ = true, float_constants_vertex_are_full_ = false;
  uint64_t current_float_constant_map_vertex_[4]{}, current_float_constant_map_pixel_[4]{};
  uint32_t current_constant_buffers_up_to_date_ = 0;
  void WriteShaderConstantsFromMem(uint32_t start_index, uint32_t* base, uint32_t num_registers);
  void map(VertexShader* vertex_shader, bool interpreter_placeholder) {
''' + mapping + r'''
  }
};
''' + method(text, 'WriteShaderConstantsFromMem') + r'''
int main() {
  VulkanCommandProcessor cp;
  VertexShader shader;
  // Run repeated animation-like register updates, across vec4 and bank edges.
  for (uint32_t frame = 1; frame <= 10000; ++frame) {
    cp.map(&shader, true);
    cp.current_constant_buffers_up_to_date_ = 3;
    uint32_t bone = __builtin_bswap32(frame);
    uint32_t index = frame % 256;
    cp.WriteShaderConstantsFromMem(0x4000 + index * 4, &bone, 1);
    if (cp.current_constant_buffers_up_to_date_ & 1) return 10;
    if (cp.registers.values[0x4000 + index * 4] != frame) return 11;
    // Returning to compiled layout re-uploads and retains sparse optimization.
    cp.map(&shader, false);
    cp.current_constant_buffers_up_to_date_ = 3;
    cp.WriteShaderConstantsFromMem(0x4000 + 255 * 4, &bone, 1);
    if (!(cp.current_constant_buffers_up_to_date_ & 1)) return 12;
    cp.WriteShaderConstantsFromMem(0x4000, &bone, 1);
    if (cp.current_constant_buffers_up_to_date_ & 1) return 13;
  }
}
'''


class InterpreterConstants(unittest.TestCase):
    def test_repeated_full_register_updates_invalidate_interpreter_buffer(self):
        root = os.environ.get('X360_SOURCE_CHECKOUT')
        if root:
            source = (Path(root) / REL).read_text()
        else:
            url = f'https://raw.githubusercontent.com/rfandango/XenDroid/{PIN}/{REL}'
            source = urllib.request.urlopen(url, timeout=60).read().decode()
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory)
            for name, text, expected in [('before', source, 10), ('after', fix(source), 0)]:
                cpp, exe = path / (name + '.cpp'), path / name
                cpp.write_text(fixture(text))
                subprocess.run(['c++', '-std=c++17', '-O2', '-fsanitize=undefined,address',
                                str(cpp), '-o', str(exe)], check=True)
                result = subprocess.run([str(exe)], timeout=30)
                self.assertEqual(result.returncode, expected, name)
