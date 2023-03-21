package io.github.pelmenstar1.digiDict.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import io.github.pelmenstar1.digiDict.common.equalsPattern

@Entity(tableName = "word_queue")
data class WordQueueEntry(@PrimaryKey override val id: Int, val word: String) : EntityWithPrimaryKeyId {
    override fun equalsNoId(other: Any?) = equalsPattern(other) { o ->
        word == o.word
    }
}