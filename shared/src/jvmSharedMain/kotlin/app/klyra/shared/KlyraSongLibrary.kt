package app.klyra.shared

import com.music.bitchord.data.model.Song

fun Song.klyraTrack(): Track = Track(
    id = if (localUri != null) videoId else "youtube:$videoId",
    sourceId = videoId, title = title, artist = artist, album = albumName.orEmpty(),
    artwork = thumbnailUrl, source = if (localUri != null) Source.LOCAL else Source.YOUTUBE,
    streamable = true,
)

fun KlyraAccountState.likes(song: Song): Boolean = records.any { it.kind == "like" && it.payload == song.klyraTrack().id }
fun toggleKlyraFavorite(song: Song) = KlyraAccount.saveTrack(song.klyraTrack(), !KlyraAccount.state.value.likes(song))
