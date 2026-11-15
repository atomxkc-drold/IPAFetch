package com.omnidex.app

import com.omnidex.app.data.VaultRepository

/**
 * In-memory session: the derived master key and the repository bound to it.
 * Cleared on lock, on auto-lock timeout and when the process dies.
 */
object Session {

    @Volatile
    var masterKey: ByteArray? = null
        private set

    @Volatile
    var repository: VaultRepository? = null
        private set

    val isUnlocked: Boolean get() = repository != null

    fun set(masterKey: ByteArray, repository: VaultRepository) {
        this.masterKey = masterKey
        this.repository = repository
    }

    fun clear() {
        masterKey?.fill(0)
        masterKey = null
        repository = null
    }
}
