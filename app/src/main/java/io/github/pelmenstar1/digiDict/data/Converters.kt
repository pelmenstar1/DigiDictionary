package io.github.pelmenstar1.digiDict.data

import androidx.room.TypeConverter

object Converters {
    @TypeConverter
    fun convertRdpReplacementRulesToString(value: RemoteDictionaryProviderInfo.UrlEncodingRules): String {
        return value.raw
    }

    @TypeConverter
    fun convertStringToRdpReplacementRules(value: String): RemoteDictionaryProviderInfo.UrlEncodingRules {
        return RemoteDictionaryProviderInfo.UrlEncodingRules(raw = value)
    }
}