package app.klyra.shared

import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import kotlin.test.*

class CloudAuthTest {
    private class MemorySecrets : SecretStore {
        val values = mutableMapOf<String, String>()
        override fun read(key: String) = values[key]
        override fun write(key: String, value: String?) { if (value == null) values.remove(key) else values[key] = value }
    }
    private val token = """{"access_token":"test-access","refresh_token":"test-refresh","expires_in":3600,"user":{"id":"test-user","email":"listener@example.test"}}"""
    private fun api(reply: (okhttp3.Request) -> Pair<Int, String>) = HttpApi(OkHttpClient.Builder().addInterceptor(Interceptor { chain ->
        val (code, body) = reply(chain.request())
        Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(code).message("test")
            .body(body.toResponseBody("application/json".toMediaType())).build()
    }).build())
    @Test fun confirmationSignupDoesNotPretendToHaveSession() = runBlocking {
        val secrets = MemorySecrets()
        val client = SupabaseClient(api { 200 to """{"id":"test-user","email":"listener@example.test"}""" }, "https://example.test", "public-test-key", secrets)
        assertFalse(client.signUp("listener@example.test", "password-test"))
        assertNull(client.session)
        assertNull(secrets.read("session"))
    }
    @Test fun immediateSignupPersistsSessionAndCanReadLibrary() = runBlocking {
        val secrets = MemorySecrets()
        val client = SupabaseClient(api { request ->
            if (request.url.encodedPath.endsWith("signup")) 200 to token else {
                assertEquals("Bearer test-access", request.header("Authorization"))
                200 to "[]"
            }
        }, "https://example.test", "public-test-key", secrets)
        assertTrue(client.signUp("listener@example.test", "password-test"))
        assertEquals("listener@example.test", client.session?.email)
        assertNotNull(secrets.read("session"))
        assertTrue(client.pull().isEmpty())
    }
    @Test fun confirmationFailureKeepsAccountSignedOutAndShowsServerError() = runBlocking {
        val client = SupabaseClient(api { 400 to """{"msg":"Email not confirmed"}""" }, "https://example.test", "public-test-key", MemorySecrets())
        val error = assertFailsWith<java.io.IOException> { client.signIn("listener@example.test", "password-test") }
        assertEquals("Email not confirmed", error.message)
        assertNull(client.session)
    }
    @Test fun confirmedSignInCanReadAndRevisionCheckWrites() = runBlocking {
        val client = SupabaseClient(api { request ->
            when {
                request.url.encodedPath.endsWith("token") -> 200 to token
                request.url.encodedPath.endsWith("apply_library_record") -> {
                    assertEquals("Bearer test-access", request.header("Authorization"))
                    200 to "true"
                }
                else -> 200 to "[]"
            }
        }, "https://example.test", "public-test-key", MemorySecrets())
        client.signIn("listener@example.test", "password-test")
        assertTrue(client.push(CloudRecord("playlist:test", "playlist", "{}", 0)))
        assertTrue(client.pull().isEmpty())
    }
}
