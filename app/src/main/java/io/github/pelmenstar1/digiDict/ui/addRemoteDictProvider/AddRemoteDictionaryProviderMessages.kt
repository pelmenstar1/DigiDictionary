package io.github.pelmenstar1.digiDict.ui.addRemoteDictProvider

import android.content.Context
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.ResourcesMessageMapper

enum class AddRemoteDictionaryProviderMessage {
    EMPTY_TEXT,
    PROVIDER_NAME_EXISTS,
    PROVIDER_SCHEMA_EXISTS,
    PROVIDER_SCHEMA_INVALID_URL,
    PROVIDER_SCHEMA_NO_QUERY_PLACEHOLDER,
    DB_ERROR
}

class AddRemoteDictionaryProviderMessageMapper(
    context: Context
) : ResourcesMessageMapper<AddRemoteDictionaryProviderMessage>(context, enumCount = 6) {
    override fun mapToStringResource(type: AddRemoteDictionaryProviderMessage) = when(type) {
        AddRemoteDictionaryProviderMessage.EMPTY_TEXT -> R.string.emptyTextError
        AddRemoteDictionaryProviderMessage.PROVIDER_NAME_EXISTS -> R.string.addRemoteDictProvider_providerNameExists
        AddRemoteDictionaryProviderMessage.PROVIDER_SCHEMA_EXISTS -> R.string.addRemoteDictProvider_providerSchemaExists
        AddRemoteDictionaryProviderMessage.PROVIDER_SCHEMA_INVALID_URL -> R.string.addRemoteDictProvider_providerSchemaInvalidUrl
        AddRemoteDictionaryProviderMessage.PROVIDER_SCHEMA_NO_QUERY_PLACEHOLDER -> R.string.addRemoteDictProvider_providerNoQueryPlaceholder
        AddRemoteDictionaryProviderMessage.DB_ERROR -> R.string.dbError
    }
}