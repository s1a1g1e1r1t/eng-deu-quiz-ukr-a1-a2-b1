package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.InitialSentences
import com.example.data.InitialWords
import com.example.data.dao.SentenceDao
import com.example.data.dao.UserProgressDao
import com.example.data.dao.WordDao
import com.example.data.model.Sentence
import com.example.data.model.UserProgress
import com.example.data.model.Word
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [Word::class, UserProgress::class, Sentence::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun wordDao(): WordDao
    abstract fun userProgressDao(): UserProgressDao
    abstract fun sentenceDao(): SentenceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE words ADD COLUMN language TEXT NOT NULL DEFAULT 'EN'")
                db.execSQL("ALTER TABLE sentences ADD COLUMN language TEXT NOT NULL DEFAULT 'EN'")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "vocab_a1_database"
                )
                    .addMigrations(MIGRATION_3_4)
                    .fallbackToDestructiveMigration()
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                INSTANCE?.let { database ->
                                    database.wordDao().insertMissing(InitialWords.getInitialWords())
                                    database.sentenceDao().insertMissing(InitialSentences.getInitialSentences())
                                    val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                                        .format(java.util.Date())
                                    database.userProgressDao().insertIfMissing(
                                        UserProgress(
                                            id = 1,
                                            totalXp = 0,
                                            quizzesCompleted = 0,
                                            currentStreak = 1,
                                            bestStreak = 1,
                                            lastActiveDate = today
                                        )
                                    )
                                }
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
