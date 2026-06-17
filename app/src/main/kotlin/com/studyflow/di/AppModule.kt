package com.studyflow.di

import android.content.Context
import androidx.room.Room
import com.studyflow.data.db.StudyFlowDatabase
import com.studyflow.data.db.dao.DailyGoalDao
import com.studyflow.data.db.dao.StreakDao
import com.studyflow.data.db.dao.StudySessionDao
import com.studyflow.data.db.dao.SubjectDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): StudyFlowDatabase =
        Room.databaseBuilder(ctx, StudyFlowDatabase::class.java, StudyFlowDatabase.DB_NAME)
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideSessionDao(db: StudyFlowDatabase): StudySessionDao = db.sessionDao()
    @Provides fun provideSubjectDao(db: StudyFlowDatabase): SubjectDao = db.subjectDao()
    @Provides fun provideDailyGoalDao(db: StudyFlowDatabase): DailyGoalDao = db.dailyGoalDao()
    @Provides fun provideStreakDao(db: StudyFlowDatabase): StreakDao = db.streakDao()
}
