package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
  entities = [
    UserProfileEntity::class,
    TripEntity::class,
    TripMemberEntity::class,
    ContributionEntity::class,
    ExpenseEntity::class,
    ExpensePayerEntity::class,
    ExpenseParticipantEntity::class,
    SettlementEntity::class,
    AuditLogEntity::class,
    NotificationEntity::class
  ],
  version = 1,
  exportSchema = false
)
abstract class HisabTripDatabase : RoomDatabase() {
  abstract fun hisabTripDao(): HisabTripDao

  companion object {
    @Volatile
    private var INSTANCE: HisabTripDatabase? = null

    fun getDatabase(context: Context): HisabTripDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          HisabTripDatabase::class.java,
          "hisabtrip_ledger.db"
        )
          .fallbackToDestructiveMigration()
          .build()
        INSTANCE = instance
        instance
      }
    }
  }
}
