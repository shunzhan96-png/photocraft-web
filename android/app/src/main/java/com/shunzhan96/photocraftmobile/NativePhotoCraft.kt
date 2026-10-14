package com.shunzhan96.photocraftmobile

/**
 * Minimal bridge to the in-process PhotoCraft Rust engine.
 *
 * The native library is built by .github/workflows/android-apk.yml. This proof-of-concept
 * entry point accepts encoded image bytes and returns the edited image as PNG bytes.
 */
object NativePhotoCraft {
    init {
        System.loadLibrary("photocraft_native")
    }

    /**
     * Runs PhotoCraft's brightness/contrast command on-device.
     * Brightness is clamped to -150..150; contrast is clamped to -50..100.
     */
    external fun adjustPng(input: ByteArray, brightness: Int, contrast: Int): ByteArray
}
