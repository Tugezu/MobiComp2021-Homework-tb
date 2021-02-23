package com.example.mobicomphomework.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "reminderMessages")
data class ReminderMessage(
        @PrimaryKey(autoGenerate = true) var msgid: Int?,
        @ColumnInfo(name = "message") var message: String,
        @ColumnInfo(name = "location_x") var location_x: Double,
        @ColumnInfo(name = "location_y") var location_y: Double,
        @ColumnInfo(name = "reminder_time") var reminder_time: String,
        @ColumnInfo(name = "creation_time") var creation_time: String,
        @ColumnInfo(name = "creator_id") var creator_id: Int?,
        @ColumnInfo(name = "reminder_seen") var reminder_seen: Boolean
)