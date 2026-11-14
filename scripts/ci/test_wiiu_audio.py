"""Compile the real audio sink against a deterministic device API test double.

Exercises short writes, backpressure, disconnects and owned cleanup; does not
claim Android speaker output. The Android target separately links real AAudio.
"""
import pathlib
import subprocess
import tempfile
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[2]

HEADER = r'''
#pragma once
#include <cstdint>
using aaudio_result_t = int32_t;
struct AAudioStream {};
struct AAudioStreamBuilder {};
constexpr int AAUDIO_OK=0, AAUDIO_ERROR_ILLEGAL_ARGUMENT=-1,
 AAUDIO_ERROR_INVALID_FORMAT=-2, AAUDIO_DIRECTION_OUTPUT=0,
 AAUDIO_FORMAT_PCM_I16=1, AAUDIO_SHARING_MODE_SHARED=0,
 AAUDIO_PERFORMANCE_MODE_LOW_LATENCY=1;
extern int mode, calls, stopped, closed, deleted, rate;
inline const char* AAudio_convertResultToText(int) { return "test error"; }
inline int AAudio_createStreamBuilder(AAudioStreamBuilder** b) {
 *b=new AAudioStreamBuilder; return 0;
}
inline void AAudioStreamBuilder_setDirection(AAudioStreamBuilder*,int) {}
inline void AAudioStreamBuilder_setFormat(AAudioStreamBuilder*,int) {}
inline void AAudioStreamBuilder_setChannelCount(AAudioStreamBuilder*,int) {}
inline void AAudioStreamBuilder_setSampleRate(AAudioStreamBuilder*,int n) { rate=n; }
inline void AAudioStreamBuilder_setSharingMode(AAudioStreamBuilder*,int) {}
inline void AAudioStreamBuilder_setPerformanceMode(AAudioStreamBuilder*,int) {}
inline int AAudioStreamBuilder_openStream(AAudioStreamBuilder*,AAudioStream** s) {
 if(mode==1) return -3; *s=new AAudioStream; return 0;
}
inline int AAudioStreamBuilder_delete(AAudioStreamBuilder* b) { ++deleted; delete b; return 0; }
inline int AAudioStream_getFormat(AAudioStream*) { return mode==2 ? 9 : 1; }
inline int AAudioStream_getChannelCount(AAudioStream*) { return 2; }
inline int AAudioStream_getSampleRate(AAudioStream*) { return rate; }
inline int AAudioStream_requestStart(AAudioStream*) { return mode==3 ? -4 : 0; }
inline int AAudioStream_write(AAudioStream*,const void*,int n,int64_t timeout) {
 ++calls;
 if(timeout!=20000000 || n>1024) return -99;
 if(mode==4) return -5;
 if(mode==5) return 0;
 return n>3 ? 3 : n;
}
inline int AAudioStream_requestStop(AAudioStream*) { ++stopped; return 0; }
inline int AAudioStream_close(AAudioStream* s) { ++closed; delete s; return 0; }
'''

TEST = r'''
#include "WiiuAudio.h"
#include <cassert>
int mode=0,calls=0,stopped=0,closed=0,deleted=0,rate=0;
int main() {
 WiiuAudio sink; std::string error;
 int16_t pcm[40]{};
 assert(!sink.start(0,&error)); assert(deleted==0);
 for(mode=1;mode<=3;++mode) {
   error.clear(); assert(!sink.start(48000,&error)); assert(!error.empty());
 }
 assert(deleted==3 && closed==2);
 mode=0; error.clear(); assert(sink.start(48000,&error));
 assert(sink.write(pcm,20,&error)==20); assert(sink.submittedFrames()==20);
 assert(calls==7 && error.empty());
 mode=5; assert(sink.write(pcm,20,&error)==0); assert(error.empty());
 mode=4; assert(sink.write(pcm,20,&error)==0); assert(!error.empty());
 assert(sink.submittedFrames()==20);
 sink.stop(); int before=closed; sink.stop(); assert(closed==before);
 assert(sink.write(pcm,1,&error)==0);
 assert(stopped==closed);
}
'''


class NativeAudioBehavior(unittest.TestCase):
    def test_real_sink_failure_and_partial_write_behavior(self):
        source = ROOT / "spikes/wiiu/vulkan-host"
        with tempfile.TemporaryDirectory() as tmp:
            work = pathlib.Path(tmp)
            (work / "aaudio").mkdir()
            (work / "aaudio/AAudio.h").write_text(HEADER)
            (work / "test.cpp").write_text(TEST)
            subprocess.run([
                "c++", "-std=c++17", "-Wall", "-Wextra", "-Werror",
                "-Wno-misleading-indentation", "-I" + str(work),
                "-I" + str(source), str(source / "WiiuAudio.cpp"),
                str(work / "test.cpp"), "-o", str(work / "test"),
            ], check=True)
            subprocess.run([str(work / "test")], check=True, timeout=10)


if __name__ == "__main__":
    unittest.main()
