package io.github.pelmenstar1.digiDict.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "word_queue")
data class WordQueueEntry(@PrimaryKey val id: Int, val word: String)