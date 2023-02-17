package io.github.pelmenstar1.digiDict.ui.addRemoteDictProvider

import android.content.Context
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.common.android.EnumResourcesStringFormatter

enum class AddRemoteDictionaryProviderMessage {
    EMPTY_TEXT,
    PROVIDER_NAME_EXISTS,
    PROVIDER_SCHEMA_EXISTS,
    PROVIDER_SCHEMA_INVALID_URL,
    PROVIDER_SCHEMA_NO_QUERY_PLACEHOLDER
}

class ResourcesAddRemoteDictionaryProviderMessageStringFormatter(
    context: Context
) : EnumResourcesStringFormatter<AddRemoteDictionaryProviderMessage>(
    context,
    AddRemoteDictionaryProviderMessage::class.java
) {
    override fun getResourceId(value: AddRemoteDictionaryProviderMessage): Int = when (value) {
        AddRemoteDictionaryProviderMessage.EMPTY_TEXT -> R.string.emptyTextError
        AddRemoteDictionaryProviderMessage.PROVIDER_NAME_EXISTS -> R.string.addRemoteDictProvider_providerNameExists
        AddRemoteDictionaryProviderMessage.PROVIDER_SCHEMA_EXISTS -> R.string.addRemoteDictProvider_providerSchemaExists
        AddRemoteDictionaryProviderMessage.PROVIDER_SCHEMA_INVALID_URL -> R.string.addRemoteDictProvider_providerSchemaInvalidUrl
        AddRemoteDictionaryProviderMessage.PROVIDER_SCHEMA_NO_QUERY_PLACEHOLDER -> R.string.addRemoteDictProvider_providerNoQueryPlaceholder
    }
}