"""Track the actual full constant read set of Xenia's temporary VS interpreter."""
from pathlib import Path

OLD = '''    if (current_float_constant_map_vertex_[i] !=
        float_constant_map_vertex.float_bitmap[i]) {
      current_float_constant_map_vertex_[i] =
          float_constant_map_vertex.float_bitmap[i];'''
NEW = '''    // The interpreter dynamically indexes all 256 registers, even those not
    // in the compiled shader's compact static map (for example bone matrices).
    const uint64_t required_map = interpreter_placeholder
                                      ? UINT64_MAX
                                      : float_constant_map_vertex.float_bitmap[i];
    if (current_float_constant_map_vertex_[i] != required_map) {
      current_float_constant_map_vertex_[i] = required_map;'''


def fix(text):
    if text.count(OLD) != 1:
        raise ValueError("Pinned Xenia interpreter constant-map source drift")
    return text.replace(OLD, NEW)


def apply(cpp):
    path = Path(cpp) / 'xenia/src/xenia/gpu/vulkan/vulkan_command_processor.cc'
    path.write_text(fix(path.read_text()))
