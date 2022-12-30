package io.github.pelmenstar1.digiDict.ui.addRemoteDictProvider

import android.content.Context
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.common.android.resourcesMessageMapper

enum class AddRemoteDictionaryProviderMessage {
    EMPTY_TEXT,
    PROVIDER_NAME_EXISTS,
    PROVIDER_SCHEMA_EXISTS,
    PROVIDER_SCHEMA_INVALID_URL,
    PROVIDER_SCHEMA_NO_QUERY_PLACEHOLDER;

    companion object {
        fun defaultMapper(context: Context) =
            resourcesMessageMapper<AddRemoteDictionaryProviderMessage>(context) {
                when (it) {
                    EMPTY_TEXT -> R.string.emptyTextError
                    PROVIDER_NAME_EXISTS -> R.string.addRemoteDictProvider_providerNameExists
                    PROVIDER_SCHEMA_EXISTS -> R.string.addRemoteDictProvider_providerSchemaExists
                    PROVIDER_SCHEMA_INVALID_URL -> R.string.addRemoteDictProvider_providerSchemaInvalidUrl
                    PROVIDER_SCHEMA_NO_QUERY_PLACEHOLDER -> R.string.addRemoteDictProvider_providerNoQueryPlaceholder
                }
            }
    }
}