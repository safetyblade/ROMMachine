package com.swordfish.lemuroid.app.mobile.feature.game

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Lightweight, on-demand evidence for the four native render hosts.
 * Surface dimensions are Android layout evidence, never proof of image pixels.
 * Only Cemu currently supplies source image dimensions in its native report.
 * No GPU readbacks, polling, renderer recreation, or save access is performed.
 */
internal object NativeRendererAudit {
    data class Geometry(
        val requestedWidth: Int = 0,
        val requestedHeight: Int = 0,
        val observedWidth: Int = 0,
        val observedHeight: Int = 0,
        val nativeImageWidth: Int = 0,
        val nativeImageHeight: Int = 0,
    ) {
        val hasRequested: Boolean get() = requestedWidth > 0 && requestedHeight > 0
        val hasObserved: Boolean get() = observedWidth > 0 && observedHeight > 0
        val hasNativeImage: Boolean get() = nativeImageWidth > 0 && nativeImageHeight > 0
    }

    /** Main-thread host measurements; no rendering or persistent writes. */
    class Tracker(private val host: NativeViewportPolicy.Host) {
        private var requestedWidth = 0
        private var requestedHeight = 0
        private var observedWidth = 0
        private var observedHeight = 0
        private var nativeImageWidth = 0
        private var nativeImageHeight = 0

        fun request(width: Int, height: Int) {
            if (width > 0 && height > 0) {
                requestedWidth = width
                requestedHeight = height
            }
        }

        fun observe(width: Int, height: Int) {
            observedWidth = width.coerceAtLeast(0)
            observedHeight = height.coerceAtLeast(0)
        }

        fun nativeImage(width: Int, height: Int) {
            if (width > 0 && height > 0) {
                nativeImageWidth = width
                nativeImageHeight = height
            }
        }

        fun summary(): String =
            describe(
                host,
                Geometry(
                    requestedWidth,
                    requestedHeight,
                    observedWidth,
                    observedHeight,
                    nativeImageWidth,
                    nativeImageHeight,
                ),
            )
    }

    fun surfaceStatus(g: Geometry): String =
        when {
            !g.hasRequested -> "NO_REQUESTED_APERTURE"
            !g.hasObserved -> "NO_OBSERVED_SURFACE"
            abs(g.requestedWidth - g.observedWidth) > 1 ||
                abs(g.requestedHeight - g.observedHeight) > 1 -> "PENDING_OR_MISMATCHED_RESIZE"
            else -> "SURFACE_DIMENSIONS_MATCH"
        }

    fun describe(
        host: NativeViewportPolicy.Host,
        geometry: Geometry,
    ): String {
        val image =
            if (geometry.hasNativeImage) {
                "${geometry.nativeImageWidth}x${geometry.nativeImageHeight}"
            } else {
                "NOT_REPORTED"
            }
        val expectedFit =
            if (geometry.hasNativeImage && geometry.hasObserved) {
                val aspect = geometry.nativeImageWidth.toFloat() / geometry.nativeImageHeight
                val fit =
                    NativeViewportPolicy.fit(
                        geometry.observedWidth.toFloat(),
                        geometry.observedHeight.toFloat(),
                        aspect,
                    )
                "${fit.width.roundToInt()}x${fit.height.roundToInt()}"
            } else {
                "UNKNOWN"
            }
        return "viewport_host=${host.name}; requested=${geometry.requestedWidth}x${geometry.requestedHeight}; " +
            "surface=${geometry.observedWidth}x${geometry.observedHeight}; " +
            "surface_state=${surfaceStatus(geometry)}; native_image=$image; " +
            "expected_aspect_fit=$expectedFit; pixel_presentation=UNVERIFIED"
    }
}
