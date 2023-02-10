package io.github.pelmenstar1.digiDict.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.pelmenstar1.digiDict.common.MessageMapper
import io.github.pelmenstar1.digiDict.common.android.LocaleProvider
import io.github.pelmenstar1.digiDict.common.time.CurrentEpochSecondsProvider
import io.github.pelmenstar1.digiDict.common.time.SystemEpochSecondsProvider
import io.github.pelmenstar1.digiDict.data.*
import io.github.pelmenstar1.digiDict.formatters.RecordSearchPropertySetFormatter
import io.github.pelmenstar1.digiDict.formatters.ResourcesRecordSearchPropertySetFormatter
import io.github.pelmenstar1.digiDict.prefs.DataStoreDigiDictAppPreferences
import io.github.pelmenstar1.digiDict.prefs.DigiDictAppPreferences
import io.github.pelmenstar1.digiDict.prefs.dataStorePreferences
import io.github.pelmenstar1.digiDict.search.RecordDeepSearchCore
import io.github.pelmenstar1.digiDict.search.RecordSearchCore
import io.github.pelmenstar1.digiDict.stats.CommonStatsProvider
import io.github.pelmenstar1.digiDict.stats.DbCommonStatsProvider
import io.github.pelmenstar1.digiDict.ui.addEditBadge.AddEditBadgeFragmentMessage
import io.github.pelmenstar1.digiDict.ui.addEditRecord.AddEditRecordMessage
import io.github.pelmenstar1.digiDict.ui.addRemoteDictProvider.AddRemoteDictionaryProviderMessage
import io.github.pelmenstar1.digiDict.ui.misc.ResourcesRecordSortTypeMessageMapper
import io.github.pelmenstar1.digiDict.ui.startEditEvent.StartEditEventError
import io.github.pelmenstar1.digiDict.widgets.AppWidgetUpdater
import io.github.pelmenstar1.digiDict.widgets.ListAppWidget
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class AppModule {
    @Provides
    fun provideLocaleProvider(@ApplicationContext context: Context): LocaleProvider {
        return LocaleProvider.fromContext(context)
    }

    @Provides
    @Singleton
    fun provideAppPreferences(@ApplicationContext context: Context): DigiDictAppPreferences {
        return DataStoreDigiDictAppPreferences(context.dataStorePreferences)
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
    fun provideRecordDao(appDatabase: AppDatabase): RecordDao {
        return appDatabase.recordDao()
    }

    @Provides
    fun provideRemoteDictProviderDao(appDatabase: AppDatabase): RemoteDictionaryProviderDao {
        return appDatabase.remoteDictionaryProviderDao()
    }

    @Provides
    fun provideRemoteDictProviderStatsDao(appDatabase: AppDatabase): RemoteDictionaryProviderStatsDao {
        return appDatabase.remoteDictionaryProviderStatsDao()
    }

    @Provides
    fun provideRecordBadgeDao(appDatabase: AppDatabase): RecordBadgeDao {
        return appDatabase.recordBadgeDao()
    }

    @Provides
    fun provideRecordToBadgeRelationDao(appDatabase: AppDatabase): RecordToBadgeRelationDao {
        return appDatabase.recordToBadgeRelationDao()
    }

    @Provides
    fun provideEventDao(appDatabase: AppDatabase): EventDao {
        return appDatabase.eventDao()
    }

    @Provides
    fun provideCommonStatsProvider(
        appDatabase: AppDatabase
    ): CommonStatsProvider {
        return DbCommonStatsProvider(appDatabase)
    }

    @Provides
    fun provideCurrentEpochSecondsProvider(): CurrentEpochSecondsProvider {
        return SystemEpochSecondsProvider
    }

    @Provides
    @Singleton
    fun providesAddExpressionMessageMapper(@ApplicationContext context: Context): MessageMapper<AddEditRecordMessage> {
        return AddEditRecordMessage.defaultMapper(context)
    }

    @Provides
    @Singleton
    fun provideAddRemoteDictProviderMessageMapper(@ApplicationContext context: Context): MessageMapper<AddRemoteDictionaryProviderMessage> {
        return AddRemoteDictionaryProviderMessage.defaultMapper(context)
    }

    @Provides
    @Singleton
    fun provideBadgeSelectorInputMessageMapper(@ApplicationContext context: Context): MessageMapper<AddEditBadgeFragmentMessage> {
        return AddEditBadgeFragmentMessage.defaultMapper(context)
    }

    @Provides
    @Singleton
    fun provideRecordSortTypeMessageMapper(@ApplicationContext context: Context): MessageMapper<RecordSortType> {
        return ResourcesRecordSortTypeMessageMapper(context)
    }

    @Provides
    @Singleton
    fun provideRecordSearchPropertySetFormatter(@ApplicationContext context: Context): RecordSearchPropertySetFormatter {
        return ResourcesRecordSearchPropertySetFormatter(context.resources)
    }

    @Provides
    fun provideStartEditEventErrorMessageMapper(@ApplicationContext context: Context): MessageMapper<StartEditEventError> {
        return StartEditEventError.resourcesMapper(context)
    }

    @Provides
    fun provideRecordSearchCore(): RecordSearchCore {
        return RecordDeepSearchCore
    }
}