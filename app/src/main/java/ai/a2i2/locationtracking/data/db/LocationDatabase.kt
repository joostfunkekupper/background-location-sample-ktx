package ai.a2i2.locationtracking.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [LocationEntity::class], version = 1)
@TypeConverters(LocationTypeConverters::class)
abstract class LocationDatabase : RoomDatabase() {
    abstract fun locationDao(): LocationDao

    companion object {
        private val DB_NAME = "location-tracking-database"

        @Volatile private var INSTANCE: LocationDatabase? = null

        fun getInstance(context: Context): LocationDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): LocationDatabase {
            return Room.databaseBuilder(
                context,
                LocationDatabase::class.java,
                DB_NAME
            ).build()
        }
    }
}