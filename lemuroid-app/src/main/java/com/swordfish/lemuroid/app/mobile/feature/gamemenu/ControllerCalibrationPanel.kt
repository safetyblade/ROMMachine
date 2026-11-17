package com.swordfish.lemuroid.app.mobile.feature.gamemenu

import android.view.InputDevice
import android.view.MotionEvent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.swordfish.lemuroid.app.shared.input.ControllerHardwareProfile
import com.swordfish.lemuroid.app.shared.input.ControllerInputProbe
import com.swordfish.lemuroid.app.shared.input.ControllerProfileStore
import com.swordfish.lemuroid.app.shared.input.InputDeviceManager
import com.swordfish.lemuroid.app.shared.input.PhysicalControllerInput
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun ControllerCalibrationPanel(
    manager: InputDeviceManager,
    device: InputDevice,
    probe: ControllerInputProbe,
) {
    val scope = rememberCoroutineScope()
    var profile by remember(device.descriptor) { mutableStateOf<ControllerProfileStore.Profile?>(null) }
    var status by remember(device.descriptor) { mutableStateOf("Release sticks, then test every physical position.") }
    val axes = device.motionRanges.map { it.axis }.toSet()
    LaunchedEffect(device.descriptor) {
        val result = withContext(Dispatchers.IO) { runCatching { manager.profiles.read(device.descriptor) } }
        profile = result.getOrNull()
        if (result.isFailure) status = "Existing calibration could not be read and was retained."
    }
    val right = ControllerHardwareProfile.rightStickAxes(device)
    val sticks =
        setOf(
            MotionEvent.AXIS_X,
            MotionEvent.AXIS_Y,
            profile?.rightX ?: right.first,
            profile?.rightY ?: right.second,
        ).intersect(axes)

    fun save(edit: (ControllerProfileStore.Profile) -> ControllerProfileStore.Profile) {
        scope.launch {
            val result =
                withContext(Dispatchers.IO) {
                    runCatching {
                        val next = edit(manager.profiles.read(device.descriptor))
                        manager.profiles.save(device.descriptor, null, next)
                        next
                    }
                }
            profile = result.getOrNull()
            status =
                if (result.isSuccess) {
                    "Calibration saved for this device. No game restart is requested."
                } else {
                    "Calibration was not saved; existing profile retained."
                }
        }
    }
    Text("Calibration: $status")
    GameMenuLink("Calibrate neutral — release sticks", enabled = sticks.isNotEmpty()) {
        val neutral = sticks.mapNotNull { axis -> probe.axes[axis]?.let { axis to it.value } }.toMap()
        val released =
            neutral.all { (axis, value) ->
                val range = device.getMotionRange(axis)?.let { PhysicalControllerInput.Range(it.min, it.max, it.flat) }
                kotlin.math.abs(PhysicalControllerInput.axis(value, range, false)) <= 0.3f
            }
        if (neutral.size != sticks.size || !released) {
            status = "Move each stick, release it, then calibrate neutral."
        } else {
            save { it.copy(centres = neutral, calibrated = true) }
        }
    }
    val deadzonePercent =
        (PhysicalControllerInput.effectiveDeadzone(profile ?: ControllerProfileStore.Profile()) * 100).toInt()
    GameMenuLink(
        "Deadzone: $deadzonePercent% — adjust",
        enabled = sticks.isNotEmpty(),
    ) {
        save {
            val current = PhysicalControllerInput.effectiveDeadzone(it)
            it.copy(deadzone = if (current >= 0.2f) (if (it.calibrated) 0f else 0.05f) else current + 0.05f)
        }
    }
    val bothRight =
        setOf(
            MotionEvent.AXIS_Z,
            MotionEvent.AXIS_RZ,
            MotionEvent.AXIS_RX,
            MotionEvent.AXIS_RY,
        ).all { it in axes }
    if (bothRight) {
        GameMenuLink("Right-stick assignment: ${if (profile?.rightX == MotionEvent.AXIS_RX) "RX / RY" else "Z / RZ"}") {
            save {
                if (it.rightX == MotionEvent.AXIS_RX) {
                    it.copy(rightX = MotionEvent.AXIS_Z, rightY = MotionEvent.AXIS_RZ)
                } else {
                    it.copy(rightX = MotionEvent.AXIS_RX, rightY = MotionEvent.AXIS_RY)
                }
            }
        }
        Text("Move only the right stick and verify the axis response before saving a mapping.")
    }
    GameMenuLink("Reset calibration to automatic defaults") {
        save { it.copy(deadzone = 0f, centres = emptyMap(), rightX = null, rightY = null, calibrated = false) }
    }
}
