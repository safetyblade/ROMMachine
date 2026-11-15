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
