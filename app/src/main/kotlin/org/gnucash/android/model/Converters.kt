package org.gnucash.android.model

import android.net.Uri
import androidx.room.TypeConverter
import java.sql.Timestamp

class Converters {
    @TypeConverter
    fun convertStringToUri(uri: String?) = uri?.let { Uri.parse(it) }

    @TypeConverter
    fun convertUriToString(uri: Uri?) = uri?.toString()

    @TypeConverter
    fun convertTimestampToLong(timestamp: Timestamp?) = timestamp?.time

    @TypeConverter
    fun convertLongToTimestamp(time: Long?) = time?.let { Timestamp(it) }
}