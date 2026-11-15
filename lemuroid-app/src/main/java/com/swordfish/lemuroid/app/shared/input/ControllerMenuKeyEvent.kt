package com.swordfish.lemuroid.app.shared.input

import android.view.KeyEvent

fun ControllerMenuEdge.handle(event: KeyEvent): Boolean =
    if (event.action == KeyEvent.ACTION_DOWN || event.action == KeyEvent.ACTION_UP) {
        handle(
            event.deviceId,
            event.keyCode,
            event.action == KeyEvent.ACTION_DOWN,
            event.repeatCount,
            event.isCanceled,
            event.downTime,
        )
    } else {
        false
    }

/** Activity-local hotplug cleanup, including navigation-only remotes. Never handles input events. */
fun androidx.lifecycle.LifecycleOwner.observeControllerMenuEdges(
    context: android.content.Context,
    vararg edges: ControllerMenuEdge,
) {
    val manager = context.getSystemService(android.content.Context.INPUT_SERVICE) as android.hardware.input.InputManager
    val listener =
        object : android.hardware.input.InputManager.InputDeviceListener {
            override fun onInputDeviceAdded(deviceId: Int) = Unit

            override fun onInputDeviceChanged(deviceId: Int) = Unit

            override fun onInputDeviceRemoved(deviceId: Int) {
                val ids = android.view.InputDevice.getDeviceIds().toSet()
                edges.forEach { it.retainDevices(ids) }
            }
        }
    lifecycle.addObserver(
        object : androidx.lifecycle.DefaultLifecycleObserver {
            override fun onStart(owner: androidx.lifecycle.LifecycleOwner) {
                manager.registerInputDeviceListener(listener, null)
            }

            override fun onStop(owner: androidx.lifecycle.LifecycleOwner) {
                manager.unregisterInputDeviceListener(listener)
                edges.forEach { it.clear() }
            }
        },
    )
}
