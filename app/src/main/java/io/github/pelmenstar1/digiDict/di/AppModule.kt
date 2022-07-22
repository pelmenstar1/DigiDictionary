package io.github.pelmenstar1.digiDict.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.pelmenstar1.digiDict.MessageMapper
import io.github.pelmenstar1.digiDict.data.AppDatabase
import io.github.pelmenstar1.digiDict.preferences
import io.github.pelmenstar1.digiDict.ui.addEditRecord.AddEditExpressionMessageMapper
import io.github.pelmenstar1.digiDict.ui.addEditRecord.AddEditRecordMessage
import io.github.pelmenstar1.digiDict.ui.addRemoteDictProvider.AddRemoteDictionaryProviderMessage
import io.github.pelmenstar1.digiDict.ui.addRemoteDictProvider.AddRemoteDictionaryProviderMessageMapper
import io.github.pelmenstar1.digiDict.ui.settings.SettingsMessage
import io.github.pelmenstar1.digiDict.ui.settings.SettingsMessageMapper
import io.github.pelmenstar1.digiDict.ui.viewRecord.ViewRecordMessage
import io.github.pelmenstar1.digiDict.ui.viewRecord.ViewRecordMessageMapper
import io.github.pelmenstar1.digiDict.widgets.AppWidgetUpdater
import io.github.pelmenstar1.digiDict.widgets.ListAppWidget

@Module
@InstallIn(SingletonComponent::class)
class AppModule {
    @Provides
    fun provideAppDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return context.preferences
    }

    @Provides
    fun provideListWidgetUpdater(@ApplicationContext context: Context): AppWidgetUpdater {
        return ListAppWidget.updater(context)
    }

    @Provides
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getOrCreate(context)
    }

    @Provides
    fun providesAddExpressionMessageMapper(@ApplicationContext context: Context): MessageMapper<AddEditRecordMessage> {
        return AddEditExpressionMessageMapper(context)
    }

    @Provides
    fun provideViewRecordMessageMapper(@ApplicationContext context: Context): MessageMapper<ViewRecordMessage> {
        return ViewRecordMessageMapper(context)
    }

    @Provides
    fun provideSettingsMessageMapper(@ApplicationContext context: Context): MessageMapper<SettingsMessage> {
        return SettingsMessageMapper(context)
    }

    @Provides
    fun provideAddRemoteDictProviderMessageMapper(@ApplicationContext context: Context): MessageMapper<AddRemoteDictionaryProviderMessage> {
        return AddRemoteDictionaryProviderMessageMapper(context)
    }
}