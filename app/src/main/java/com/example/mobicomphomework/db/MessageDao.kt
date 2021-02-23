package com.example.mobicomphomework.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface MessageDao {
    @Transaction
    @Insert
    fun insert(reminderMessageInfo: ReminderMessage): Long

    @Query("DELETE FROM reminderMessages WHERE msgid = :id")
    fun delete(id: Int)

    @Query("SELECT * FROM reminderMessages")
    fun getReminders(): List<ReminderMessage>

    @Query("SELECT * FROM reminderMessages WHERE msgid = :id")
    fun getReminder(id: Int): ReminderMessage

    @Query("UPDATE reminderMessages SET message = :message, reminder_time = :reminder_time WHERE msgid = :id") // TODO: update location
    fun updateReminder(id: Int, message: String, reminder_time: String)
}

/*
@PrimaryKey(autoGenerate = true) var msgid: Int?,
        @ColumnInfo(name = "message") var message: String,
        @ColumnInfo(name = "location_x") var location_x: Double,
        @ColumnInfo(name = "location_y") var location_y: Double,
        @ColumnInfo(name = "reminder_time") var reminder_time: String,
        @ColumnInfo(name = "creation_time") var creation_time: String,
        @ColumnInfo(name = "creator_id") var creator_id: Int?,
        @ColumnInfo(name = "reminder_seen") var reminder_seen: Boolean
 */