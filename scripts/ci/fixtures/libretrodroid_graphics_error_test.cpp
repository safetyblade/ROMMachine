#include "hwcontextnegotiation.h"
#include <cassert>

int main() {
    HwContextNegotiation negotiation;
    // A valid image rejected only because Play! needs GLES 3.2 must report graphics.
    negotiation.beginLoad();
    negotiation.reject();
    assert(negotiation.wasRejected());
    // Dolphin probes 3.2 then 3.1/3.0. An accepted fallback must clear the probe failure.
    negotiation.reject();
    negotiation.accept();
    assert(!negotiation.wasRejected());
    // A later malformed image is a content error, independent of an earlier rejection.
    negotiation.reject();
    negotiation.beginLoad();
    assert(!negotiation.wasRejected());
    // No hardware request (a software core) must retain ordinary content errors.
    negotiation.beginLoad();
    assert(!negotiation.wasRejected());
}
