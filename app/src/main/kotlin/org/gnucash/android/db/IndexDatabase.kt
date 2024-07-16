package org.gnucash.android.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import org.gnucash.android.app.GnuCashApplication
import org.gnucash.android.db.DatabaseSchema.BOOK_DATABASE_NAME
import org.gnucash.android.db.DatabaseSchema.BOOK_DATABASE_VERSION
import org.gnucash.android.db.DatabaseSchema.BookEntry
import org.gnucash.android.db.adapter.AccountsDbAdapter
import org.gnucash.android.db.adapter.BooksDbAdapter
import org.gnucash.android.db.adapter.SplitsDbAdapter
import org.gnucash.android.db.adapter.TransactionsDbAdapter
import org.gnucash.android.model.BaseModel.Companion.generateUID
import org.gnucash.android.model.Book
import org.gnucash.android.model.Converters

@Database(
    entities = [Book::class],
    version = BOOK_DATABASE_VERSION,
    )
@TypeConverters(Converters::class)
abstract class IndexDatabase: RoomDatabase(){
    abstract fun bookDao(): BookDao
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
    }
}

private fun databaseCallback() = object : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        db.execSQL(DatabaseHelper.createUpdatedAtTrigger(BookEntry.TABLE_NAME));
        val book = Book()
        DatabaseHelper( GnuCashApplication.getAppContext(), book.uID ).use { helper ->
            val mainDb = helper.writableDatabase
            AccountsDbAdapter(
                mainDb,
                TransactionsDbAdapter(mainDb, SplitsDbAdapter(mainDb))
            ).use {
                book.rootAccountUID = it.getOrCreateGnuCashRootAccountUID()
                book.active = true
                val contentValues = ContentValues()
                contentValues.put(BookEntry.COLUMN_UID, book.uID)
                contentValues.put(BookEntry.COLUMN_ROOT_GUID, book.rootAccountUID)
                contentValues.put(BookEntry.COLUMN_TEMPLATE_GUID, generateUID())
                contentValues.put(
                    BookEntry.COLUMN_DISPLAY_NAME,
                    BooksDbAdapter(db).generateDefaultBookName()
                )
                contentValues.put(BookEntry.COLUMN_ACTIVE, if (book.active!!) 1 else 0)
                db.insert(BookEntry.TABLE_NAME, SQLiteDatabase.CONFLICT_IGNORE, contentValues)
            }
        }
    }
}

fun indexDatabaseBuilder(context: Context) = with(Room.databaseBuilder(context, IndexDatabase::class.java, BOOK_DATABASE_NAME)) {
    addMigrations(MIGRATION_1_2)
    addCallback(databaseCallback())
}
