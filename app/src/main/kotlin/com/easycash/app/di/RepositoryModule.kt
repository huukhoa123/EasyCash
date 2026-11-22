package com.easycash.app.di

import com.easycash.domain.DataManagementRepository
import com.easycash.data.RoomDataManagementRepository
import com.easycash.domain.AccountRepository
import com.easycash.domain.BudgetRepository
import com.easycash.domain.CategoryRepository
import com.easycash.domain.NotificationSourceRepository
import com.easycash.domain.StatsRepository
import com.easycash.domain.TransactionRepository
import com.easycash.data.RoomBudgetRepository
import com.easycash.data.local.MIGRATION_4_5
import com.easycash.data.local.MIGRATION_5_6
import com.easycash.data.RoomFinanceRepository
import com.easycash.data.local.MIGRATION_1_2
import com.easycash.data.local.DefaultCategories
import com.easycash.data.RoomCategoryRepository
import com.easycash.data.local.EasyCashDatabase
import android.content.Context
import androidx.room.Room
import dagger.hilt.android.qualifiers.ApplicationContext
import com.easycash.data.RoomNotificationSourceRepository
import com.easycash.data.notification.observeNotificationAccess
import com.easycash.data.local.MIGRATION_2_3
import com.easycash.data.local.MIGRATION_3_4
import com.easycash.data.notification.NotificationProcessor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    @Provides
    @Singleton
    fun provideDataManagement(database: EasyCashDatabase): DataManagementRepository = RoomDataManagementRepository(database)

    @Provides
    @Singleton
    fun provideNotificationProcessor(database: EasyCashDatabase) = NotificationProcessor(database)

    @Provides
    @Singleton
    fun provideFinanceRepository(database: EasyCashDatabase) = RoomFinanceRepository(database)

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): EasyCashDatabase =
        Room.databaseBuilder(context, EasyCashDatabase::class.java, "easycash.db").addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6).addCallback(DefaultCategories).build()

    @Provides
    @Singleton
    fun provideStatsRepository(
        repository: RoomFinanceRepository,
    ): StatsRepository = repository

    @Provides
    @Singleton
    fun provideAccountRepository(repository: RoomFinanceRepository): AccountRepository = repository

    @Provides
    @Singleton
    fun provideTransactionRepository(repository: RoomFinanceRepository): TransactionRepository = repository

    @Provides
    @Singleton
    fun provideCategoryRepository(database: EasyCashDatabase): CategoryRepository =
        RoomCategoryRepository(database)

    @Provides
    @Singleton
    fun provideNotificationSourceRepository(database: EasyCashDatabase, @ApplicationContext context: Context): NotificationSourceRepository =
        RoomNotificationSourceRepository(database, observeNotificationAccess(context))

    @Provides
    @Singleton
    fun provideBudgetRepository(database: EasyCashDatabase): BudgetRepository = RoomBudgetRepository(database)
}
