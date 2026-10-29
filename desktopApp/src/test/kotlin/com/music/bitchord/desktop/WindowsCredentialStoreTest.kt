package com.music.bitchord.desktop

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WindowsCredentialStoreTest {
    @Test fun protectsReplacesIsolatesAndRemovesSecrets() {
        if (!DesktopPlatform.isWindows) return
        val directory = Files.createTempDirectory("klyra-dpapi-test")
        val store = WindowsCredentialStore(directory)
        val account = mapOf("application" to "klyra-test", "account" to "one")
        val other = account + ("account" to "two")
        val secret = "test-value-only-not-a-real-credential".toByteArray()
        try {
            assertTrue(store.available())
            assertTrue(store.store(account, secret))
            assertContentEquals(secret, store.lookup(account))
            assertFalse(Files.readAllBytes(store.pathFor(account)).toString(Charsets.UTF_8).contains(secret.toString(Charsets.UTF_8)))
            assertNull(store.lookup(other))
            assertEquals(store.pathFor(account), store.pathFor(account.entries.reversed().associate { it.toPair() }))
            assertTrue(store.store(account, "replacement".toByteArray()))
            assertContentEquals("replacement".toByteArray(), store.lookup(account))
            Files.write(store.pathFor(account), byteArrayOf(1, 2, 3))
            assertNull(store.lookup(account), "Corrupt data must never be treated as plaintext")
            assertTrue(store.remove(account))
            assertNull(store.lookup(account))
        } finally {
            store.remove(account)
            Files.deleteIfExists(directory)
        }
    }
}
