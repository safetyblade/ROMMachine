#include "WiiuAudio.h"
#include "WiiuRuntimeMetrics.h"
#include <cassert>
#include <array>
#include <thread>

static AAudioStream* device;
static int closes;
static bool in_callback = false;
static bool fail_open = false;
const char* AAudio_convertResultToText(aaudio_result_t) { return "injected device error"; }
aaudio_result_t AAudio_createStreamBuilder(AAudioStreamBuilder** b) { *b = new AAudioStreamBuilder; return 0; }
void AAudioStreamBuilder_setDirection(AAudioStreamBuilder*, int) {}
void AAudioStreamBuilder_setFormat(AAudioStreamBuilder*, int) {}
void AAudioStreamBuilder_setChannelCount(AAudioStreamBuilder*, int) {}
void AAudioStreamBuilder_setSampleRate(AAudioStreamBuilder* b, int r) { b->rate = r; }
void AAudioStreamBuilder_setSharingMode(AAudioStreamBuilder*, int) {}
void AAudioStreamBuilder_setPerformanceMode(AAudioStreamBuilder*, int) {}
void AAudioStreamBuilder_setDataCallback(AAudioStreamBuilder* b, DataCallback c, void* u) { b->data = c; b->user = u; }
void AAudioStreamBuilder_setErrorCallback(AAudioStreamBuilder* b, ErrorCallback c, void* u) { b->error = c; assert(b->user == u); }
aaudio_result_t AAudioStreamBuilder_openStream(AAudioStreamBuilder* b, AAudioStream** s) {
    if (fail_open) return AAUDIO_ERROR_DISCONNECTED;
    device = *s = new AAudioStream{b->data,b->error,b->user,b->rate}; return 0;
}
void AAudioStreamBuilder_delete(AAudioStreamBuilder* b) { delete b; }
int AAudioStream_getFormat(AAudioStream*) { return AAUDIO_FORMAT_PCM_I16; }
int AAudioStream_getChannelCount(AAudioStream*) { return 2; }
int AAudioStream_getSampleRate(AAudioStream* s) { return s->rate; }
int AAudioStream_getFramesPerBurst(AAudioStream*) { return 8; }
int AAudioStream_setBufferSizeInFrames(AAudioStream*, int n) { assert(n == 16); return n; }
aaudio_result_t AAudioStream_requestStart(AAudioStream* s) { assert(s->data && s->error); return 0; }
aaudio_result_t AAudioStream_requestStop(AAudioStream*) { return 0; }
aaudio_result_t AAudioStream_close(AAudioStream* s) { assert(!in_callback); delete s; device = nullptr; ++closes; return 0; }
int AAudioStream_getXRunCount(AAudioStream*) { return 3; }
static void render(int16_t* out, int frames) {
    assert(device->data(device, device->user, out, frames) == AAUDIO_CALLBACK_RESULT_CONTINUE);
}
int main() {
    WiiuAudio audio;
    std::string error;
    assert(audio.start(48000, &error));
    std::array<int16_t, 128> pcm;
    for (int i = 0; i < 64; ++i) { pcm[i*2] = i+1; pcm[i*2+1] = -(i+1); }
    std::array<int16_t, 160> out;
    assert(audio.write(pcm.data(), 64, &error) == 64);
    render(out.data(), 16);
    assert(out[0] == 1 && out[31] == -16);
    render(out.data(), 80);
    assert(out[0] == 17 && out[95] == -64 && out[96] == 0 && out[159] == 0);
    assert(WiiuMetrics::audio_played.load() == 64);
    assert(WiiuMetrics::audio_underflow.load() == 32);
    assert(WiiuMetrics::audio_xruns.load() == 3);
    assert(audio.write(pcm.data(), 16, &error) == 16);
    render(out.data(), 16);
    assert(out[0] == 0 && WiiuMetrics::audio_played.load() == 64); // rebuffer, don't chirp
    assert(WiiuMetrics::audio_underflow.load() == 48); // include the full active rebuffer gap
    assert(audio.write(pcm.data()+32, 16, &error) == 16);
    render(out.data(), 16);
    assert(out[0] == 1 && out[31] == -16);
    WiiuMetrics::audio_paused.store(true);
    render(out.data(), 16);
    assert(out[0] == 0 && WiiuMetrics::audio_buffered.load() == 0);
    WiiuMetrics::audio_paused.store(false);
    render(out.data(), 16);
    assert(out[0] == 0); // no stale audio replay on resume
    for (int i = 0; i < 100; ++i) {
        std::thread callback([] {
            in_callback = true;
            device->error(device, device->user, AAUDIO_ERROR_DISCONNECTED);
            in_callback = false;
        });
        callback.join();
        const auto before = audio.submittedFrames();
        assert(audio.write(pcm.data(), 1, &error) == 0 && error.empty());
        assert(closes == i); // callback and producer never close a stream
        audio.service();
        assert(closes == i + 1 && device);
        assert(audio.submittedFrames() == before); // session counters survive reopen
        assert(audio.write(pcm.data(), 64, &error) == 64);
        render(out.data(), 16);
        assert(out[0] == 1 && out[31] == -16);
    }
    assert(WiiuMetrics::audio_recoveries.load() == 100);
    assert(WiiuMetrics::audio_error.load() == -899); // retain historical fault
    device->error(device, device->user, AAUDIO_ERROR_DISCONNECTED);
    fail_open = true;
    audio.service();
    assert(!device && closes == 101);
    const auto attempts = WiiuMetrics::audio_recovery_attempts.load();
    for (int i = 0; i < 100; ++i) audio.service();
    assert(WiiuMetrics::audio_recovery_attempts.load() == attempts); // bounded retry
    audio.stop();
    audio.service(); // stop cannot resurrect a disconnected session
    assert(!device);
    fail_open = false;
    assert(audio.start(44100, &error));
    assert(WiiuMetrics::audio_rate.load() == 44100 && audio.submittedFrames() == 0);
}
