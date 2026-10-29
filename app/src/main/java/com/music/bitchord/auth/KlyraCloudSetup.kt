package com.music.bitchord.auth

import android.content.Context
import app.klyra.shared.KlyraAccount
import app.klyra.shared.SecretStore
import com.music.bitchord.BuildConfig
import java.io.File

fun installKlyraCloud(context: Context) {
    val prefs = EncryptedPrefs.open(context, "klyra_cloud", "klyra_cloud_legacy")
    KlyraAccount.install(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_ANON_KEY, object : SecretStore {
        override fun read(key: String): String? = prefs.getString(key, null)
        override fun write(key: String, value: String?) { check(prefs.edit().putString(key, value).commit()) { "Could not save account session" } }
    }, File(context.filesDir, "klyra-library"))
}
