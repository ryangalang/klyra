package com.music.bitchord.desktop

import app.klyra.shared.KlyraAccount
import app.klyra.shared.SecretStore

internal fun installKlyraCloud() {
    val base = java.io.File(System.getenv("LOCALAPPDATA") ?: System.getProperty("user.home"), "KLYRA Preview")
    KlyraAccount.install(System.getProperty("klyra.SUPABASE_URL").orEmpty(), System.getProperty("klyra.SUPABASE_ANON_KEY").orEmpty(), object : SecretStore {
        private fun attributes(key: String) = mapOf("application" to "klyra-foundation-cloud", "key" to key)
        override fun read(key: String): String? = DesktopSecretStore.lookup(attributes(key))?.toString(Charsets.UTF_8)
        override fun write(key: String, value: String?) {
            val success = if (value == null) DesktopSecretStore.remove(attributes(key))
                else DesktopSecretStore.store("KLYRA cloud session", attributes(key), value.toByteArray())
            check(success) { "Secure credential storage is unavailable" }
        }
    }, java.io.File(base, "cloud-library"))
}
