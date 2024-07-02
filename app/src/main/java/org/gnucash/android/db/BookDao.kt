package org.gnucash.android.db

import androidx.room.Dao
import androidx.room.Query
import org.gnucash.android.model.Book

@Dao
interface BookDao {
    @Query("SELECT * FROM book")
    fun getAll(): List<Book>
}