#include "input.h"
#include "libretro.h"
#include <android/keycodes.h>
#include <cassert>
#include <limits>

int main() {
    libretrodroid::Input input;
    auto pressure = [&](unsigned port, unsigned button) {
        return input.getInputState(port, RETRO_DEVICE_ANALOG, RETRO_DEVICE_INDEX_ANALOG_BUTTON, button);
    };
    auto clicked = [&](unsigned button) {
        return input.getInputState(0, RETRO_DEVICE_JOYPAD, 0, button);
    };
    assert(pressure(0, RETRO_DEVICE_ID_JOYPAD_L2) == 0);
    input.onMotionEvent(0, 4, 0.5f, 0.94f);
    assert(pressure(0, RETRO_DEVICE_ID_JOYPAD_L2) == 16383);
    assert(pressure(0, RETRO_DEVICE_ID_JOYPAD_R2) > 30000);
    assert(!clicked(RETRO_DEVICE_ID_JOYPAD_L2));
    assert(!clicked(RETRO_DEVICE_ID_JOYPAD_R2));
    assert(pressure(1, RETRO_DEVICE_ID_JOYPAD_L2) == 0);
    input.onMotionEvent(0, 4, 0.95f, 1.0f);
    assert(clicked(RETRO_DEVICE_ID_JOYPAD_L2));
    assert(clicked(RETRO_DEVICE_ID_JOYPAD_R2));
    input.onMotionEvent(0, 4, 0.2f, 0.0f);
    assert(!clicked(RETRO_DEVICE_ID_JOYPAD_L2));
    input.onKeyEvent(0, 0, AKEYCODE_BUTTON_L2);
    assert(pressure(0, RETRO_DEVICE_ID_JOYPAD_L2) == 32767);
    assert(clicked(RETRO_DEVICE_ID_JOYPAD_L2));
    input.onKeyEvent(0, 1, AKEYCODE_BUTTON_L2);
    assert(pressure(0, RETRO_DEVICE_ID_JOYPAD_L2) == 6553);
    input.onMotionEvent(0, 4, -1.0f, 2.0f);
    assert(pressure(0, RETRO_DEVICE_ID_JOYPAD_L2) == 0);
    assert(pressure(0, RETRO_DEVICE_ID_JOYPAD_R2) == 32767);
    input.onMotionEvent(0, 4, std::numeric_limits<float>::quiet_NaN(), std::numeric_limits<float>::infinity());
    assert(pressure(0, RETRO_DEVICE_ID_JOYPAD_L2) == 0);
    assert(pressure(0, RETRO_DEVICE_ID_JOYPAD_R2) == 0);
    input.onMotionEvent(0, 1, 0.5f, -1.0f);
    assert(input.getInputState(0, RETRO_DEVICE_ANALOG, RETRO_DEVICE_INDEX_ANALOG_LEFT, RETRO_DEVICE_ID_ANALOG_X) == 16383);
    assert(input.getInputState(0, RETRO_DEVICE_ANALOG, RETRO_DEVICE_INDEX_ANALOG_LEFT, RETRO_DEVICE_ID_ANALOG_Y) == -32767);
    input.onKeyEvent(0, 0, AKEYCODE_BUTTON_A);
    assert(input.getInputState(0, RETRO_DEVICE_JOYPAD, 0, RETRO_DEVICE_ID_JOYPAD_A));
    input.onMotionEvent(-1, 4, 1, 1);
    input.onMotionEvent(4, 4, 1, 1);
    input.onKeyEvent(4, 0, AKEYCODE_BUTTON_L2);
    assert(pressure(4, RETRO_DEVICE_ID_JOYPAD_L2) == 0);
}
