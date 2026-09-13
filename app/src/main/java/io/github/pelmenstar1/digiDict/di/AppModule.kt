package io.github.pelmenstar1.digiDict.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.pelmenstar1.digiDict.PreferencesTextBreakAndHyphenationInfoSource
import io.github.pelmenstar1.digiDict.common.StringFormatter
import io.github.pelmenstar1.digiDict.common.android.TextBreakAndHyphenationInfoSource
import io.github.pelmenstar1.digiDict.common.time.CurrentEpochSecondsProvider
import io.github.pelmenstar1.digiDict.common.time.SystemEpochSecondsProvider
import io.github.pelmenstar1.digiDict.data.AppDatabase
import io.github.pelmenstar1.digiDict.data.EventDao
import io.github.pelmenstar1.digiDict.data.RecordBadgeDao
import io.github.pelmenstar1.digiDict.data.RecordDao
import io.github.pelmenstar1.digiDict.data.RecordSortType
import io.github.pelmenstar1.digiDict.data.RecordToBadgeRelationDao
import io.github.pelmenstar1.digiDict.data.RemoteDictionaryProviderDao
import io.github.pelmenstar1.digiDict.data.RemoteDictionaryProviderStatsDao
import io.github.pelmenstar1.digiDict.data.WordQueueDao
import io.github.pelmenstar1.digiDict.formatters.RecordSearchPropertySetFormatter
import io.github.pelmenstar1.digiDict.formatters.ResourcesRecordSearchPropertySetFormatter
import io.github.pelmenstar1.digiDict.prefs.DataStoreDigiDictAppPreferences
import io.github.pelmenstar1.digiDict.prefs.DigiDictAppPreferences
import io.github.pelmenstar1.digiDict.prefs.dataStorePreferences
import io.github.pelmenstar1.digiDict.search.RecordDeepSearchCore
import io.github.pelmenstar1.digiDict.search.RecordSearchCore
import io.github.pelmenstar1.digiDict.stats.CommonStatsProvider
import io.github.pelmenstar1.digiDict.stats.DbCommonStatsProvider
import io.github.pelmenstar1.digiDict.ui.addEditBadge.AddEditBadgeMessage
import io.github.pelmenstar1.digiDict.ui.addEditBadge.ResourcesAddEditBadgeMessageStringFormatter
import io.github.pelmenstar1.digiDict.ui.addEditRecord.AddEditRecordMessage
import io.github.pelmenstar1.digiDict.ui.addEditRecord.ResourcesAddEditRecordMessageStringFormatter
import io.github.pelmenstar1.digiDict.ui.addRemoteDictProvider.AddRemoteDictionaryProviderMessage
import io.github.pelmenstar1.digiDict.ui.addRemoteDictProvider.ResourcesAddRemoteDictionaryProviderMessageStringFormatter
import io.github.pelmenstar1.digiDict.ui.misc.ResourcesRecordSortTypeStringFormatter
import io.github.pelmenstar1.digiDict.ui.record.RecordTextPrecomputeController
import io.github.pelmenstar1.digiDict.ui.startEditEvent.ResourcesStartEditEventErrorStringFormatter
import io.github.pelmenstar1.digiDict.ui.startEditEvent.StartEditEventError
import io.github.pelmenstar1.digiDict.ui.wordQueue.addWordDialog.AddWordToQueueDialogError
import io.github.pelmenstar1.digiDict.ui.wordQueue.addWordDialog.ResourcesAddWordToQueueDialogErrorStringFormatter
import io.github.pelmenstar1.digiDict.widgets.AppWidgetUpdater
import io.github.pelmenstar1.digiDict.widgets.ListAppWidget
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class AppModule {
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
    fun provideRecordTextPrecomputeController(
        @ApplicationContext context: Context
    ): RecordTextPrecomputeController {
        return RecordTextPrecomputeController.create(context)
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.createFileDatabase(context)
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
    fun provideWordQueueDao(appDatabase: AppDatabase): WordQueueDao {
        return appDatabase.wordQueueDao()
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
    fun providesAddExpressionStringFormatter(@ApplicationContext context: Context): StringFormatter<AddEditRecordMessage> {
        return ResourcesAddEditRecordMessageStringFormatter(context)
    }

    @Provides
    @Singleton
    fun provideAddRemoteDictProviderStringFormatter(@ApplicationContext context: Context): StringFormatter<AddRemoteDictionaryProviderMessage> {
        return ResourcesAddRemoteDictionaryProviderMessageStringFormatter(context)
    }

    @Provides
    @Singleton
    fun provideBadgeSelectorInputStringFormatter(@ApplicationContext context: Context): StringFormatter<AddEditBadgeMessage> {
        return ResourcesAddEditBadgeMessageStringFormatter(context)
    }

    @Provides
    @Singleton
    fun provideRecordSortTypeStringFormatter(@ApplicationContext context: Context): StringFormatter<RecordSortType> {
        return ResourcesRecordSortTypeStringFormatter(context)
    }

    @Provides
    @Singleton
    fun provideRecordSearchPropertySetFormatter(@ApplicationContext context: Context): RecordSearchPropertySetFormatter {
        return ResourcesRecordSearchPropertySetFormatter(context.resources)
    }

    @Provides
    fun provideStartEditEventErrorStringFormatter(@ApplicationContext context: Context): StringFormatter<StartEditEventError> {
        return ResourcesStartEditEventErrorStringFormatter(context)
    }

    @Provides
    fun provideAddWordToQueueDialogErrorStringFormatter(@ApplicationContext context: Context): StringFormatter<AddWordToQueueDialogError> {
        return ResourcesAddWordToQueueDialogErrorStringFormatter(context)
    }

    @Provides
    fun provideRecordSearchCore(): RecordSearchCore {
        return RecordDeepSearchCore
    }

    @Provides
    fun provideTextBreakAndHyphenationInfoSource(prefs: DigiDictAppPreferences): TextBreakAndHyphenationInfoSource {
        return PreferencesTextBreakAndHyphenationInfoSource(prefs)
    }
}