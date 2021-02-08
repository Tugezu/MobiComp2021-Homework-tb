package com.example.mobicomphomework

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "reminderMessage")
data class ReminderMessage(
        @PrimaryKey(autoGenerate = true) var msgid: Int?,
        @ColumnInfo(name = "eventName") var name: String,
        @ColumnInfo(name = "date") var time: String
)