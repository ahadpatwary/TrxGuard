package com.txnrelay.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [PendingEventEntity::class],
    version = 1,
    exportSchema = false, // set a schemaLocation and export schemas once you start writing migrations
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun pendingEventDao(): PendingEventDao

    companion object {
        // NOTE: deliberately no fallbackToDestructiveMigration() – queued events must never be wiped.
        // When you change the schema, bump `version` and add a Migration here.
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "txnrelay.db")
                .build()
    }
}
