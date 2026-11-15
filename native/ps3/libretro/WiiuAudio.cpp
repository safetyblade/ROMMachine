#include "WiiuAudio.h"
#include <algorithm>
#include "WiiuRuntimeMetrics.h"

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
    fifo_.reset();
    primed_ = false;
    played_once_ = false;
    error_.store(0);
    WiiuMetrics::audio_accepted.store(0);
    WiiuMetrics::audio_callbacks.store(0);
    WiiuMetrics::audio_played.store(0);
    WiiuMetrics::audio_underflow.store(0);
    WiiuMetrics::audio_overflow.store(0);
    WiiuMetrics::audio_xruns.store(0);
    WiiuMetrics::audio_error.store(0);
    WiiuMetrics::audio_rate.store(sampleRate);
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
    AAudioStreamBuilder_setDataCallback(builder, &WiiuAudio::render, this);
    AAudioStreamBuilder_setErrorCallback(builder, &WiiuAudio::disconnected, this);
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
    const auto burst = std::max(1, AAudioStream_getFramesPerBurst(stream_));
    prebuffer_ = std::min(4096, burst * 4);
    AAudioStream_setBufferSizeInFrames(stream_, burst * 2);
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
    const auto result = error_.load();
    if (result < 0) { failed(error, "CALLBACK", result); return 0; }
    const auto consumed = fifo_.push(stereo, frames);
    submitted_ += consumed;
    WiiuMetrics::audio_accepted.fetch_add(consumed);
    WiiuMetrics::audio_overflow.fetch_add(frames - consumed);
    WiiuMetrics::audio_buffered.store(static_cast<int>(fifo_.available()));
    WiiuMetrics::audio_xruns.store(AAudioStream_getXRunCount(stream_));
    return consumed;
}

aaudio_data_callback_result_t WiiuAudio::render(AAudioStream*, void* user, void* output, int32_t frames) {
    auto* self = static_cast<WiiuAudio*>(user);
    WiiuMetrics::audio_callbacks.fetch_add(1);
    auto* stereo = static_cast<int16_t*>(output);
    std::fill_n(stereo, static_cast<size_t>(frames) * 2, int16_t{0});
    if (WiiuMetrics::audio_paused.load()) {
        self->fifo_.discard();
        self->primed_ = false;
        WiiuMetrics::audio_buffered.store(0);
        return AAUDIO_CALLBACK_RESULT_CONTINUE;
    }
    if (!self->primed_ && self->fifo_.available() >= static_cast<size_t>(self->prebuffer_))
        self->primed_ = true;
    if (!self->primed_) {
        if (self->played_once_) WiiuMetrics::audio_underflow.fetch_add(frames);
        return AAUDIO_CALLBACK_RESULT_CONTINUE;
    }
    self->played_once_ = true;
    const auto count = self->fifo_.pop(stereo, frames);
    WiiuMetrics::audio_played.fetch_add(count);
    WiiuMetrics::audio_underflow.fetch_add(static_cast<size_t>(frames) - count);
    WiiuMetrics::audio_buffered.store(static_cast<int>(self->fifo_.available()));
    if (count < static_cast<size_t>(frames)) self->primed_ = false;
    return AAUDIO_CALLBACK_RESULT_CONTINUE;
}

void WiiuAudio::disconnected(AAudioStream*, void* user, aaudio_result_t error) {
    static_cast<WiiuAudio*>(user)->error_.store(error);
    WiiuMetrics::audio_error.store(error);
}

void WiiuAudio::stop() {
    std::lock_guard<std::recursive_mutex> lock(mutex_);
    if (!stream_) return;
    AAudioStream_requestStop(stream_);
    AAudioStream_close(stream_);
    stream_ = nullptr;
}
