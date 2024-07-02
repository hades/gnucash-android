package org.gnucash.android.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import org.gnucash.android.db.BookDao
import org.gnucash.android.db.DatabaseSchema.BOOK_DATABASE_NAME
import org.gnucash.android.db.DatabaseSchema.BOOK_DATABASE_VERSION
import org.gnucash.android.model.Book
import org.gnucash.android.model.Converters

// CREATE TABLE books (
//  _id integer primary key autoincrement,
//  uid varchar(255) not null UNIQUE,
//  name varchar(255) not null,
//  root_account_guid varchar(255) not null,
//  root_template_guid varchar(255),
//  is_active tinyint default 0,
//  uri varchar(255),
//  last_export_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
//  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
//  modified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP );
//  CREATE TRIGGER update_time_trigger   AFTER UPDATE ON books FOR EACH ROW  BEGIN UPDATE books  SET modified_at = CURRENT_TIMESTAMP  WHERE OLD.uid = NEW.uid;  END;
// CREATE TABLE IF NOT EXISTS `${TABLE_NAME}` (
// `_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
// `uid` TEXT,
// `name` TEXT NOT NULL,
// `root_account_guid` TEXT NOT NULL,
// `root_template_guid` TEXT,
// `is_active` INTEGER NOT NULL,
// `uri` TEXT,
// `last_export_time` INTEGER NOT NULL DEFAULT CURRENT_TIMESTAMP,
// `created_at` INTEGER NOT NULL DEFAULT CURRENT_TIMESTAMP,
// `modified_at` INTEGER NOT NULL DEFAULT CURRENT_TIMESTAMP)",

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
        db.execSQL("""alter table books""")
    }
}

fun indexDatabaseBuilder(context: Context) = with(Room.databaseBuilder(context, IndexDatabase::class.java, BOOK_DATABASE_NAME)) {
    addMigrations(MIGRATION_1_2)
}