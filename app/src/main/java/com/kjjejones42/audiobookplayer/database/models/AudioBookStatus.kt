package com.kjjejones42.audiobookplayer.database.models

enum class AudioBookStatus(val value: Int, val displayName: String) {
    IN_PROGRESS(0, "In Progress"),
    NOT_BEGUN(1, "Not Begun"),
    FINISHED(2, "Finished"),
}