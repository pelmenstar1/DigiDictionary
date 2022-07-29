package io.github.pelmenstar1.digiDict.data

import android.net.Uri
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "remote_dict_providers")
data class RemoteDictionaryProviderInfo(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val schema: String,
    val urlEncodingRules: UrlEncodingRules
) {
    class UrlEncodingRules {
        val raw: String
        val spaceReplacement: Char

        constructor(raw: String) {
            this.raw = raw
            this.spaceReplacement = raw.getOrElse(0) { DEFAULT_SPACE_REPLACEMENT }
        }

        constructor(spaceReplacement: Char = DEFAULT_SPACE_REPLACEMENT) {
            this.spaceReplacement = spaceReplacement
            this.raw = spaceReplacement.toString()
        }

        fun applyTo(text: String): String {
            return text.replace(' ', spaceReplacement)
        }

        companion object {
            const val DEFAULT_SPACE_REPLACEMENT = '+'
        }
    }

    fun resolvedUrl(query: String): String {
        var encodedQuery = urlEncodingRules.applyTo(query)
        encodedQuery = Uri.encode(encodedQuery, "+")

        return schema.replace("\$query$", encodedQuery)
    }

    companion object {
        val PREDEFINED_PROVIDERS = arrayOf(
            RemoteDictionaryProviderInfo(
                name = "Cambridge English Dictionary",
                schema = "https://dictionary.cambridge.org/dictionary/english/\$query$",
                urlEncodingRules = UrlEncodingRules(spaceReplacement = '-')
            ),
            RemoteDictionaryProviderInfo(
                name = "Urban Dictionary",
                schema = "https://www.urbandictionary.com/define.php?term=\$query$",
                urlEncodingRules = UrlEncodingRules()
            )
        )
    }
}