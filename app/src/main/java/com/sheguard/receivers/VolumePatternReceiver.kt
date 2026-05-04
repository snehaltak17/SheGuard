package com.sheguard.receivers

class VolumePatternReceiver(
    private val onPatternDetected: () -> Unit
) {

    private val pattern = ArrayDeque<Int>()

    fun registerKey(keyCode: Int) {
        pattern.addLast(keyCode)
        while (pattern.size > 3) {
            pattern.removeFirst()
        }

        if (pattern.size == 3) {
            val values = pattern.toList()
            if (values[0] == android.view.KeyEvent.KEYCODE_VOLUME_UP &&
                values[1] == android.view.KeyEvent.KEYCODE_VOLUME_UP &&
                values[2] == android.view.KeyEvent.KEYCODE_VOLUME_DOWN
            ) {
                pattern.clear()
                onPatternDetected.invoke()
            }
        }
    }
}
