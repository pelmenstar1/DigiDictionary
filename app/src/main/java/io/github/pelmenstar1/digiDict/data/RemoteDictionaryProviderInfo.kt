package io.github.pelmenstar1.digiDict.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "remote_dict_providers")
data class RemoteDictionaryProviderInfo(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val schema: String
) {
    fun resolvedUrl(query: String): String {
        return schema.replace("\$query$", query)
    }

    companion object {
        val PREDEFINED_PROVIDERS = arrayOf(
            RemoteDictionaryProviderInfo(
                name = "Cambridge English Dictionary",
                schema = "https://dictionary.cambridge.org/dictionary/english/\$query$"
            ),
            RemoteDictionaryProviderInfo(
                name = "Urban Dictionary",
                schema = "https://www.urbandictionary.com/define.php?term=\$query$"
            )
        )
    }
}