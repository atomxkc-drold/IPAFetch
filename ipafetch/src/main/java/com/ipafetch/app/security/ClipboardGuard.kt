package com.ipafetch.app.security

import android.content.ClipboardManager
import android.content.Context
import android.os.CountDownTimer
import com.ipafetch.app.security.AuditLogger

/**
 * Copies an API key to the clipboard, then auto-clears it after a short window
 * (default 45s) so a stray paste doesn't leak it into another app.
 */
object ClipboardGuard {

    private const val CLEAR_DELAY_MS = 45_000L
    private var timer: CountDownTimer? = null

    fun copy(context: Context, label: String, value: String) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(cm.newTextClip(label, value))
        AuditLogger.log(context, AuditLogger.EVENT_COPY, label)
        cancel()
        timer = object : CountDownTimer(CLEAR_DELAY_MS, 1000) {
            override fun onTick(millisUntilFinished: Long) {}
            override fun onFinish() {
                clearIfMatches(context, value)
            }
        }.start()
    }

    private fun clearIfMatches(context: Context, value: String) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = cm.primaryClip ?: return
        if (clip.itemCount > 0 && clip.getItemAt(0).text?.toString() == value) {
            cm.clearPrimaryClip()
        }
    }

    private fun cancel() {
        timer?.cancel()
        timer = null
    }
}
