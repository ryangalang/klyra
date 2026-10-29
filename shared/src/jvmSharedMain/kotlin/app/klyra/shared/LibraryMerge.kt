package app.klyra.shared

/** A local file's metadata must survive a cloud refresh, but never enter the outbox. */
internal fun CloudRecord.deviceOnly(): Boolean =
    id.startsWith("local:") || (kind == "like" && payload.startsWith("local:")) ||
        (kind == "track" && runCatching { JsonCodec.decodeFromString<Track>(payload).source == Source.LOCAL }.getOrDefault(false))

internal fun CloudRecord.sameContent(other: CloudRecord?): Boolean = other != null &&
    id == other.id && kind == other.kind && payload == other.payload && deleted == other.deleted

internal data class LibraryMerge(val records: List<CloudRecord>, val pending: Set<String>, val conflicts: Set<String>)

internal fun mergeLibrary(local: List<CloudRecord>, pending: Set<String>, remote: List<CloudRecord>): LibraryMerge {
    val server = remote.associateBy { it.id }
    val dirty = local.filter { it.id in pending && !it.deviceOnly() && !it.sameContent(server[it.id]) }
    val conflicts = dirty.filter { it.revision != (server[it.id]?.revision ?: 0L) }.mapTo(mutableSetOf()) { it.id }
    val preserved = local.filter { it.deviceOnly() } + dirty
    return LibraryMerge((server + preserved.associateBy { it.id }).values.toList(), dirty.mapTo(mutableSetOf()) { it.id }, conflicts)
}
