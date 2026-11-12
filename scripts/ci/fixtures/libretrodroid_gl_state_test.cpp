// SPDX-License-Identifier: GPL-3.0-or-later
#include "frontendglstate.h"
#include <cassert>
#include <map>
#include <set>
#include <array>
#include <cstring>
#include <iostream>

struct Attribute {
    GLint enabled = 0, size = 4, type = GL_FLOAT, normalized = GL_FALSE, stride = 0, buffer = 0;
    void* pointer = nullptr;
    bool operator==(const Attribute& b) const {
        return enabled == b.enabled && size == b.size && type == b.type && normalized == b.normalized &&
               stride == b.stride && buffer == b.buffer && pointer == b.pointer;
    }
};
struct Driver {
    GLint program = 17, array = 93, renderbuffer = 19, vao = 71, draw = 22, read = 23, unpack = 25;
    GLint active = GL_TEXTURE0 + 7, alignment = 1, row = 1024;
    std::array<GLint, 4> viewport{3, 5, 1920, 1080};
    std::array<GLfloat, 4> clear{.1f, .2f, .3f, .4f};
    std::array<GLboolean, 4> mask{GL_FALSE, GL_TRUE, GL_FALSE, GL_TRUE};
    std::array<GLint, 8> textures{30, 31, 32, 33, 34, 35, 36, 37};
    std::array<GLint, 8> samplers{40, 41, 42, 43, 44, 45, 46, 47};
    std::set<GLenum> enabled{GL_DEPTH_TEST, GL_BLEND, GL_SCISSOR_TEST, GL_CULL_FACE,
        GL_STENCIL_TEST, GL_POLYGON_OFFSET_FILL, GL_SAMPLE_ALPHA_TO_COVERAGE, GL_RASTERIZER_DISCARD};
    std::map<GLuint, std::array<Attribute, 4>> attributes;
} gl;
EGLContext context = reinterpret_cast<EGLContext>(1);
GLuint nextName = 100;
std::set<GLuint> deletedBuffers, deletedVaos;
std::set<GLuint> populatedBuffers;
bool es3 = true;
unsigned queries = 0;
extern "C" {
EGLContext eglGetCurrentContext() { return context; }
void glGetIntegerv(GLenum key, GLint* p) {
    ++queries;
    switch (key) {
    case GL_CURRENT_PROGRAM: *p = gl.program; break;
    case GL_ARRAY_BUFFER_BINDING: *p = gl.array; break;
    case GL_RENDERBUFFER_BINDING: *p = gl.renderbuffer; break;
    case GL_VERTEX_ARRAY_BINDING: assert(es3); *p = gl.vao; break;
    case GL_FRAMEBUFFER_BINDING: *p = gl.draw; break;
    case GL_READ_FRAMEBUFFER_BINDING: assert(es3); *p = gl.read; break;
    case GL_PIXEL_UNPACK_BUFFER_BINDING: assert(es3); *p = gl.unpack; break;
    case GL_UNPACK_ALIGNMENT: *p = gl.alignment; break;
    case GL_UNPACK_ROW_LENGTH: assert(es3); *p = gl.row; break;
    case GL_ACTIVE_TEXTURE: *p = gl.active; break;
    case GL_TEXTURE_BINDING_2D: *p = gl.textures[gl.active - GL_TEXTURE0]; break;
    case GL_SAMPLER_BINDING: assert(es3); *p = gl.samplers[gl.active - GL_TEXTURE0]; break;
    case GL_VIEWPORT: std::copy(gl.viewport.begin(), gl.viewport.end(), p); break;
    case GL_MAX_VERTEX_ATTRIBS: *p = 4; break;
    default: assert(false);
    }
}
void glGetFloatv(GLenum key, GLfloat* p) { assert(key == GL_COLOR_CLEAR_VALUE); std::copy(gl.clear.begin(), gl.clear.end(), p); }
void glGetBooleanv(GLenum key, GLboolean* p) { assert(key == GL_COLOR_WRITEMASK); std::copy(gl.mask.begin(), gl.mask.end(), p); }
GLboolean glIsEnabled(GLenum cap) { return gl.enabled.count(cap) ? GL_TRUE : GL_FALSE; }
void glEnable(GLenum cap) { gl.enabled.insert(cap); }
void glDisable(GLenum cap) { gl.enabled.erase(cap); }
void glActiveTexture(GLenum value) { gl.active = value; }
void glBindTexture(GLenum target, GLuint value) { assert(target == GL_TEXTURE_2D); gl.textures[gl.active - GL_TEXTURE0] = value; }
void glBindSampler(GLuint unit, GLuint value) { assert(es3); gl.samplers[unit] = value; }
void glBindBuffer(GLenum target, GLuint value) {
    if (target == GL_ARRAY_BUFFER) gl.array = value;
    else { assert(es3 && target == GL_PIXEL_UNPACK_BUFFER); gl.unpack = value; }
}
void glBindRenderbuffer(GLenum target, GLuint value) { assert(target == GL_RENDERBUFFER); gl.renderbuffer = value; }
void glBindVertexArray(GLuint value) { assert(es3); gl.vao = value; }
void glBindFramebuffer(GLenum target, GLuint value) {
    if (target == GL_FRAMEBUFFER) gl.draw = gl.read = value;
    else if (target == GL_DRAW_FRAMEBUFFER) { assert(es3); gl.draw = value; }
    else { assert(es3 && target == GL_READ_FRAMEBUFFER); gl.read = value; }
}
void glUseProgram(GLuint value) { gl.program = value; }
void glViewport(GLint x, GLint y, GLsizei w, GLsizei h) { gl.viewport = {x,y,w,h}; }
void glClearColor(GLfloat r, GLfloat g, GLfloat b, GLfloat a) { gl.clear = {r,g,b,a}; }
void glColorMask(GLboolean r, GLboolean g, GLboolean b, GLboolean a) { gl.mask = {r,g,b,a}; }
void glPixelStorei(GLenum target, GLint value) { if (target == GL_UNPACK_ALIGNMENT) gl.alignment = value; else { assert(es3 && target == GL_UNPACK_ROW_LENGTH); gl.row = value; } }
void glGenBuffers(GLsizei n, GLuint* value) { assert(n == 1); *value = nextName++; }
void glGenVertexArrays(GLsizei n, GLuint* value) { assert(es3 && n == 1); *value = nextName++; }
void glDeleteBuffers(GLsizei n, const GLuint* value) { assert(n == 1); deletedBuffers.insert(*value); }
void glDeleteVertexArrays(GLsizei n, const GLuint* value) { assert(es3 && n == 1); deletedVaos.insert(*value); }
void glBufferData(GLenum target, GLsizeiptr size, const void*, GLenum usage) {
    assert(target == GL_ARRAY_BUFFER && size == 96 && usage == GL_STREAM_DRAW);
    assert(gl.array >= 100); populatedBuffers.insert(gl.array);
}
void glBufferSubData(GLenum target, GLintptr offset, GLsizeiptr size, const void* data) {
    assert(target == GL_ARRAY_BUFFER && size == 48 && (offset == 0 || offset == 48) && data);
    assert(populatedBuffers.count(gl.array));
}
void glVertexAttribPointer(GLuint index, GLint size, GLenum type, GLboolean normalized, GLsizei stride, const void* pointer) {
    gl.attributes[gl.vao][index] = {gl.attributes[gl.vao][index].enabled, size, static_cast<GLint>(type), normalized, stride, gl.array, const_cast<void*>(pointer)};
}
void glEnableVertexAttribArray(GLuint i) { gl.attributes[gl.vao][i].enabled = 1; }
void glDisableVertexAttribArray(GLuint i) { gl.attributes[gl.vao][i].enabled = 0; }
void glGetVertexAttribPointerv(GLuint i, GLenum key, void** p) { assert(key == GL_VERTEX_ATTRIB_ARRAY_POINTER); *p = gl.attributes[gl.vao][i].pointer; }
void glGetVertexAttribiv(GLuint i, GLenum key, GLint* p) {
    const auto& a = gl.attributes[gl.vao][i];
    switch (key) {
    case GL_VERTEX_ATTRIB_ARRAY_ENABLED: *p = a.enabled; break;
    case GL_VERTEX_ATTRIB_ARRAY_SIZE: *p = a.size; break;
    case GL_VERTEX_ATTRIB_ARRAY_TYPE: *p = a.type; break;
    case GL_VERTEX_ATTRIB_ARRAY_STRIDE: *p = a.stride; break;
    case GL_VERTEX_ATTRIB_ARRAY_NORMALIZED: *p = a.normalized; break;
    case GL_VERTEX_ATTRIB_ARRAY_BUFFER_BINDING: *p = a.buffer; break;
    default: assert(false);
    }
}
}

void testHandoff(bool modern) {
    es3 = modern;
    gl = Driver{};
    if (!es3) gl.vao = 0;
    gl.attributes[gl.vao][0] = {1, 3, GL_FLOAT, GL_TRUE, 24, 93, reinterpret_cast<void*>(8)};
    gl.attributes[gl.vao][1] = {0, 2, GL_FLOAT, GL_FALSE, 16, 94, reinterpret_cast<void*>(16)};
    const Driver before = gl;
    {
        libretrodroid::FrontendGLState guard(es3);
        guard.prepare();
        assert(!gl.enabled.count(GL_BLEND) && !gl.enabled.count(GL_SCISSOR_TEST));
        assert(gl.mask == (std::array<GLboolean, 4>{GL_TRUE,GL_TRUE,GL_TRUE,GL_TRUE}));
        libretrodroid::FrontendVertices vertices;
        const float quad[12]{};
        vertices.bind(es3, 0, 1, quad, quad, 12);
        // Regression: client pointers cannot be interpreted inside the core's VBO.
        assert(gl.array != before.array && gl.array >= 100);
        if (es3) assert(gl.vao != before.vao);
        const auto& a = gl.attributes[gl.vao];
        assert(a[0].buffer == gl.array && a[0].pointer == nullptr);
        assert(a[1].buffer == gl.array && a[1].pointer == reinterpret_cast<void*>(48));
        glUseProgram(0); glViewport(0,0,100,100); glClearColor(0,0,0,1);
        glBindFramebuffer(GL_FRAMEBUFFER, 0); glBindRenderbuffer(GL_RENDERBUFFER, 0);
        glActiveTexture(GL_TEXTURE1); glBindTexture(GL_TEXTURE_2D, 0); glPixelStorei(GL_UNPACK_ALIGNMENT, 4);
    }
    assert(gl.program == before.program && gl.vao == before.vao && gl.array == before.array);
    assert(gl.renderbuffer == before.renderbuffer && gl.draw == before.draw);
    if (es3) assert(gl.read == before.read && gl.unpack == before.unpack && gl.row == before.row);
    assert(gl.active == before.active && gl.textures == before.textures && gl.samplers == before.samplers);
    assert(gl.viewport == before.viewport && gl.clear == before.clear && gl.mask == before.mask);
    assert(gl.enabled == before.enabled && gl.alignment == before.alignment);
    assert(gl.attributes[before.vao] == before.attributes.at(before.vao));
}
void testContextLoss() {
    es3 = true;
    context = reinterpret_cast<EGLContext>(1);
    GLuint oldBuffer;
    {
        libretrodroid::FrontendVertices vertices;
        const float quad[12]{};
        vertices.bind(true, 0, 1, quad, quad, 12);
        oldBuffer = gl.array;
        context = reinterpret_cast<EGLContext>(2);
        vertices.bind(true, 0, 1, quad, quad, 12);
        assert(gl.array != static_cast<GLint>(oldBuffer));
    }
    assert(!deletedBuffers.count(oldBuffer)); // lost-context names are not deleted in its replacement
    assert(deletedBuffers.count(nextName - 2)); // replacement resources are reclaimed
}
void testFramebufferReplacement() {
    es3 = true;
    gl = Driver{};
    {
        libretrodroid::FrontendGLState guard(true);
        guard.prepare();
        glBindFramebuffer(GL_FRAMEBUFFER, 500);
        guard.replaceFramebuffer(22, 500);
    }
    assert(gl.draw == 500 && gl.read == 23);
    es3 = false;
    gl = Driver{};
    gl.vao = 0;
    {
        libretrodroid::FrontendGLState guard(false);
        guard.prepare();
        glBindFramebuffer(GL_FRAMEBUFFER, 600);
        guard.replaceFramebuffer(22, 600);
    }
    assert(gl.draw == 600 && gl.read == 600);
}
void testSoftwareAvoidsStateQueries() {
    es3 = false;
    const auto previousQueries = queries;
    {
        libretrodroid::FrontendGLState state(false, false);
        state.prepare();
    }
    assert(queries == previousQueries);
}
int main() {
    testHandoff(true); testHandoff(false); testContextLoss(); testFramebufferReplacement(); testSoftwareAvoidsStateQueries();
    std::cout << "GLES3 hardware core / GLES2 legacy state restoration and context loss PASS\n";
}
