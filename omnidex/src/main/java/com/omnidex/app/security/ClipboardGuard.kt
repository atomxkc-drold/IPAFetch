package com.omnidex.app.security

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper

/**
 * Copies a secret to the clipboard and clears it again after a short window
 * (45s by default) so a stray paste cannot leak it into another app.
 */
object ClipboardGuard {

    private const val DEFAULT_CLEAR_MS = 45_000L

    private var timer: CountDownTimer? = null

    fun copy(
        context: Context,
        label: String,
        value: String,
        clearAfterMs: Long = DEFAULT_CLEAR_MS
    ) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText(label, value))
        AuditLogger.log(context, AuditLogger.EVENT_COPY, label)
        cancel()
        if (clearAfterMs <= 0L || clearAfterMs == Long.MAX_VALUE) return
        timer = object : CountDownTimer(clearAfterMs, 1000L) {
            override fun onTick(millisUntilFinished: Long) = Unit
            override fun onFinish() {
                Handler(Looper.getMainLooper()).post { clearIfMatches(context, value) }
            }
        }.start()
    }

    private fun clearIfMatches(context: Context, value: String) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = cm.primaryClip ?: return
        if (clip.itemCount > 0 && clip.getItemAt(0).text?.toString() == value) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                cm.clearPrimaryClip()
            } else {
                cm.setPrimaryClip(ClipData.newPlainText("", ""))
            }
        }
    }

    fun cancel() {
        timer?.cancel()
        timer = null
    }
}
