package com.ipafetch.app

import com.ipafetch.app.data.KeyRepository

/**
 * Holds the in-memory master key + repository for the app session.
 *
 * The key lives here only for the duration of the unlocked session. It is
 * wiped when the app is backgrounded long enough for the Keystore key to
 * expire (30s auth validity) or the process dies.
 */
object Session {

    @Volatile
    var masterKey: ByteArray? = null
        private set

    @Volatile
    var repository: KeyRepository? = null
        private set

    fun set(masterKey: ByteArray, repository: KeyRepository) {
        this.masterKey = masterKey
        this.repository = repository
    }

    fun clear() {
        masterKey?.fill(0)
        masterKey = null
        repository = null
    }
}
