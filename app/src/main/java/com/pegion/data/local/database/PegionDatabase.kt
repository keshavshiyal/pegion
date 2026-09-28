package com.pegion.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.pegion.data.local.dao.DownloadDao
import com.pegion.data.local.dao.DownloadSegmentDao
import com.pegion.data.local.entity.DownloadEntity
import com.pegion.data.local.entity.DownloadSegmentEntity
import com.pegion.download.model.DownloadPriority
import com.pegion.download.model.DownloadStatus

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
    entities = [DownloadEntity::class, DownloadSegmentEntity::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(DownloadConverters::class)
abstract class PegionDatabase : RoomDatabase() {

    abstract fun downloadDao(): DownloadDao
    abstract fun downloadSegmentDao(): DownloadSegmentDao

    companion object {
        @Volatile
        private var INSTANCE: PegionDatabase? = null

        fun getInstance(context: Context): PegionDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    PegionDatabase::class.java,
                    "pegion_downloads.db"
                ).fallbackToDestructiveMigration(dropAllTables = true).build().also { INSTANCE = it }
            }
        }
    }
}
