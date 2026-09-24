package com.example.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.data.local.dao.DownloadDao
import com.example.data.local.entity.DownloadEntity
import com.example.download.model.DownloadPriority
import com.example.download.model.DownloadStatus

class DownloadConverters {
    @TypeConverter
    fun fromStatus(status: DownloadStatus): String = status.name

    @TypeConverter
    fun toStatus(value: String): DownloadStatus = try {
        DownloadStatus.valueOf(value)
    } catch (_: Exception) {
        DownloadStatus.PENDING
    }

    @TypeConverter
    fun fromPriority(priority: DownloadPriority): String = priority.name

    @TypeConverter
    fun toPriority(value: String): DownloadPriority = try {
        DownloadPriority.valueOf(value)
    } catch (_: Exception) {
        DownloadPriority.NORMAL
    }
}

@Database(
    entities = [DownloadEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(DownloadConverters::class)
abstract class PegionDatabase : RoomDatabase() {

    abstract fun downloadDao(): DownloadDao

    companion object {
        @Volatile
        private var INSTANCE: PegionDatabase? = null

        fun getInstance(context: Context): PegionDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    PegionDatabase::class.java,
                    "pegion_downloads.db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
        }
    }
}
