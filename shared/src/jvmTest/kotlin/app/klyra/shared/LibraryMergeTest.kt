package app.klyra.shared

import kotlin.test.*
import kotlinx.serialization.encodeToString

class LibraryMergeTest {
    private val base = CloudRecord("playlist:one", "playlist", "first", 1)
    @Test fun localFilesSurviveCloudRefreshWithoutUpload() {
        val local = CloudRecord("local:song", "track", JsonCodec.encodeToString(Track("local:song", "Local", "Artist", source = Source.LOCAL)))
        val like = CloudRecord("like:local:song", "like", "local:song")
        val result = mergeLibrary(listOf(local, like), setOf(local.id), listOf(base))
        assertEquals(setOf(local.id, like.id, base.id), result.records.map { it.id }.toSet())
        assertTrue(result.pending.isEmpty())
    }
    @Test fun retryAfterLostAcknowledgementRecognizesServerContent() {
        val dirty = base.copy(payload = "edited")
        val result = mergeLibrary(listOf(dirty), setOf(base.id), listOf(dirty.copy(revision = 2)))
        assertTrue(result.pending.isEmpty())
        assertTrue(result.conflicts.isEmpty())
        assertEquals(2L, result.records.single().revision)
    }
    @Test fun concurrentEditsPreserveLocalVersionAndRequireDecision() {
        val local = base.copy(payload = "mine")
        val result = mergeLibrary(listOf(local), setOf(base.id), listOf(base.copy(payload = "other device", revision = 2)))
        assertEquals(local, result.records.single())
        assertEquals(setOf(base.id), result.conflicts)
        assertEquals(setOf(base.id), result.pending)
    }
    @Test fun pendingDeletionIsNotResurrectedByCloudRefresh() {
        val local = base.copy(deleted = true)
        val result = mergeLibrary(listOf(local), setOf(base.id), listOf(base))
        assertTrue(result.records.single().deleted)
        assertTrue(result.conflicts.isEmpty())
        assertEquals(setOf(base.id), result.pending)
    }
    @Test fun cleanRecordsFollowCloudAndNewPendingRecordsRemain() {
        val fresh = CloudRecord("playlist:new", "playlist", "new")
        val remote = base.copy(payload = "new remote", revision = 2)
        val result = mergeLibrary(listOf(base, fresh), setOf(fresh.id), listOf(remote))
        assertEquals(setOf(remote, fresh), result.records.toSet())
        assertTrue(result.conflicts.isEmpty())
    }
}
