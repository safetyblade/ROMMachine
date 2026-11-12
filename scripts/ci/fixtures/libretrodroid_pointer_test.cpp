#include <cassert>
#include <cmath>
#include <cstdio>
#include <limits>
#include "videolayout.h"

using libretrodroid::Rect;
using libretrodroid::VideoLayout;

static void near(float actual, float expected) {
    assert(std::isfinite(actual));
    assert(std::fabs(actual - expected) < 0.00002f);
}

// A game's logical point projected through the real renderer's vertices.
// Inverse input mapping must round-trip it, regardless of rotation or letterbox.
static std::pair<float, float> project(VideoLayout& layout, bool bottomLeft, float x, float y) {
    const auto& v = layout.getForegroundVertices();
    const size_t o = bottomLeft ? 2 : 0;
    const size_t r = bottomLeft ? 10 : 4;
    const size_t d = bottomLeft ? 0 : 2;
    return {v[o] + x * (v[r] - v[o]) + y * (v[d] - v[o]),
            -(v[o + 1] + x * (v[r + 1] - v[o + 1]) + y * (v[d + 1] - v[o + 1]))};
}

int main() {
    constexpr float pi = 3.14159265358979323846f;
    for (bool bottomLeft : {false, true}) {
        for (float angle : {0.0f, pi / 2, pi, 3 * pi / 2, pi / 4, -pi / 6}) {
            for (unsigned alignment : {V_ALIGN_CENTER, V_ALIGN_TOP, V_ALIGN_BOTTOM}) {
                VideoLayout layout(bottomLeft, angle, Rect(0.15f, 0.2f, 0.65f, 0.55f), alignment);
                layout.updateScreenSize(1920, 1080);
                layout.updateAspectRatio(4.0f / 3.0f);
                for (float x : {0.0f, 0.1f, 0.5f, 0.9f, 1.0f}) {
                    for (float y : {0.0f, 0.2f, 0.5f, 0.8f, 1.0f}) {
                        const auto point = project(layout, bottomLeft, x, y);
                        const auto result = layout.getRelativePosition(point.first, point.second);
                        near(result.first, x);
                        near(result.second, y);
                    }
                }
                // These points lie outside the game even if a rotated bounding
                // box would still contain them. Hardware shell touches stay out.
                for (const auto& logical : {std::pair<float, float>{-0.05f, 0.5f},
                                           {1.05f, 0.5f}, {0.5f, -0.05f}, {0.5f, 1.05f}}) {
                    const auto point = project(layout, bottomLeft, logical.first, logical.second);
                    const auto result = layout.getRelativePosition(point.first, point.second);
                    assert(result.first < 0 && result.second < 0);
                }
                auto invalid = layout.getRelativePosition(std::numeric_limits<float>::quiet_NaN(), 0);
                assert(invalid.first < 0 && invalid.second < 0);
            }
        }
    }
    VideoLayout uninitialised(false, 0, Rect(0, 0, 1, 1), V_ALIGN_CENTER);
    assert(uninitialised.getRelativePosition(0, 0).first < 0);
    std::puts("LibretroDroid rotated viewport pointer contract: PASS");
}
