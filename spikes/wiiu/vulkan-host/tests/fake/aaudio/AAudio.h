#pragma once
#include <cstdint>
using aaudio_result_t = int32_t;
using aaudio_data_callback_result_t = int32_t;
constexpr int AAUDIO_OK = 0, AAUDIO_DIRECTION_OUTPUT = 0, AAUDIO_FORMAT_PCM_I16 = 1;
constexpr int AAUDIO_SHARING_MODE_SHARED = 1, AAUDIO_PERFORMANCE_MODE_LOW_LATENCY = 12;
constexpr int AAUDIO_ERROR_ILLEGAL_ARGUMENT = -1, AAUDIO_ERROR_INVALID_FORMAT = -2;
constexpr int AAUDIO_CALLBACK_RESULT_CONTINUE = 0;
struct AAudioStream;
using DataCallback = aaudio_data_callback_result_t (*)(AAudioStream*, void*, void*, int32_t);
using ErrorCallback = void (*)(AAudioStream*, void*, aaudio_result_t);
struct AAudioStreamBuilder { DataCallback data{}; ErrorCallback error{}; void* user{}; int rate{}; };
struct AAudioStream { DataCallback data{}; ErrorCallback error{}; void* user{}; int rate{}; };
const char* AAudio_convertResultToText(aaudio_result_t);
aaudio_result_t AAudio_createStreamBuilder(AAudioStreamBuilder**);
void AAudioStreamBuilder_setDirection(AAudioStreamBuilder*, int);
void AAudioStreamBuilder_setFormat(AAudioStreamBuilder*, int);
void AAudioStreamBuilder_setChannelCount(AAudioStreamBuilder*, int);
void AAudioStreamBuilder_setSampleRate(AAudioStreamBuilder*, int);
void AAudioStreamBuilder_setSharingMode(AAudioStreamBuilder*, int);
void AAudioStreamBuilder_setPerformanceMode(AAudioStreamBuilder*, int);
void AAudioStreamBuilder_setDataCallback(AAudioStreamBuilder*, DataCallback, void*);
void AAudioStreamBuilder_setErrorCallback(AAudioStreamBuilder*, ErrorCallback, void*);
aaudio_result_t AAudioStreamBuilder_openStream(AAudioStreamBuilder*, AAudioStream**);
void AAudioStreamBuilder_delete(AAudioStreamBuilder*);
int AAudioStream_getFormat(AAudioStream*);
int AAudioStream_getChannelCount(AAudioStream*);
int AAudioStream_getSampleRate(AAudioStream*);
int AAudioStream_getFramesPerBurst(AAudioStream*);
int AAudioStream_setBufferSizeInFrames(AAudioStream*, int);
aaudio_result_t AAudioStream_requestStart(AAudioStream*);
aaudio_result_t AAudioStream_requestStop(AAudioStream*);
aaudio_result_t AAudioStream_close(AAudioStream*);
int AAudioStream_getXRunCount(AAudioStream*);
// No blocking AAudioStream_write API: callback-mode production must not use it.
