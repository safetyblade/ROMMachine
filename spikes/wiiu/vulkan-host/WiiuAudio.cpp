#include "WiiuAudio.h"
#include <algorithm>

namespace {
bool failed(std::string* error, const char* operation, aaudio_result_t result) {
    if (error) *error = std::string("WIIU_AUDIO_") + operation + ":" +
        AAudio_convertResultToText(result);
    return false;
}
}

bool WiiuAudio::start(int32_t sampleRate, std::string* error) {
    std::lock_guard<std::recursive_mutex> lock(mutex_);
    stop();
    submitted_ = 0;
    if (sampleRate < 8000 || sampleRate > 192000)
        return failed(error, "SAMPLE_RATE", AAUDIO_ERROR_ILLEGAL_ARGUMENT);
    AAudioStreamBuilder* builder = nullptr;
    auto result = AAudio_createStreamBuilder(&builder);
    if (result != AAUDIO_OK) return failed(error, "BUILDER", result);
    AAudioStreamBuilder_setDirection(builder, AAUDIO_DIRECTION_OUTPUT);
    AAudioStreamBuilder_setFormat(builder, AAUDIO_FORMAT_PCM_I16);
    AAudioStreamBuilder_setChannelCount(builder, 2);
    AAudioStreamBuilder_setSampleRate(builder, sampleRate);
    AAudioStreamBuilder_setSharingMode(builder, AAUDIO_SHARING_MODE_SHARED);
    AAudioStreamBuilder_setPerformanceMode(builder, AAUDIO_PERFORMANCE_MODE_LOW_LATENCY);
    result = AAudioStreamBuilder_openStream(builder, &stream_);
    AAudioStreamBuilder_delete(builder);
    if (result != AAUDIO_OK) {
        stop();
        return failed(error, "OPEN", result);
    }
    // Libretro delivers interleaved signed stereo. Never reinterpret a negotiated
    // float/mono/rate-changed stream as that format.
    if (AAudioStream_getFormat(stream_) != AAUDIO_FORMAT_PCM_I16 ||
        AAudioStream_getChannelCount(stream_) != 2 ||
        AAudioStream_getSampleRate(stream_) != sampleRate) {
        stop();
        return failed(error, "FORMAT", AAUDIO_ERROR_INVALID_FORMAT);
    }
    result = AAudioStream_requestStart(stream_);
    if (result != AAUDIO_OK) {
        stop();
        return failed(error, "START", result);
    }
    return true;
}

size_t WiiuAudio::write(const int16_t* stereo, size_t frames, std::string* error) {
    std::lock_guard<std::recursive_mutex> lock(mutex_);
    if (!stream_ || !stereo) return 0;
    size_t consumed = 0;
    while (consumed < frames) {
        // Bound each write, including device disconnection and shutdown latency.
        const int32_t count = static_cast<int32_t>(std::min<size_t>(frames - consumed, 1024));
        const auto result = AAudioStream_write(stream_, stereo + consumed * 2,
                                               count, 20000000);
        if (result < 0) {
            failed(error, "WRITE", result);
            break;
        }
        if (!result) break; // Backpressure is a partial write, never fake consumption.
        consumed += static_cast<size_t>(result);
    }
    submitted_ += consumed;
    return consumed;
}

void WiiuAudio::stop() {
    std::lock_guard<std::recursive_mutex> lock(mutex_);
    if (!stream_) return;
    AAudioStream_requestStop(stream_);
    AAudioStream_close(stream_);
    stream_ = nullptr;
}
