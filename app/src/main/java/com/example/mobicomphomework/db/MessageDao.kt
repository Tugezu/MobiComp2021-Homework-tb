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
    fun getAllReminders(): List<ReminderMessage>

    @Query("SELECT * FROM reminderMessages WHERE msgid = :id")
    fun getReminder(id: Int): ReminderMessage

    @Query("SELECT * FROM reminderMessages WHERE reminder_time <= :currentTime")
    fun getCurrentReminders(currentTime: String): List<ReminderMessage>

    @Query("UPDATE reminderMessages SET message = :message, reminder_time = :reminder_time WHERE msgid = :id")
    fun updateReminder(id: Int, message: String, reminder_time: String)
}