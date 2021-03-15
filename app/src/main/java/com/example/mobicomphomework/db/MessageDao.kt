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

    @Query("SELECT * FROM reminderMessages WHERE (reminder_time <= :currentTime AND location_y < -90) OR reminder_seen = 1")
    fun getReminderHistory(currentTime: String): List<ReminderMessage>

    @Query("SELECT * FROM reminderMessages WHERE reminder_seen = 0 AND location_y >= -90")
    fun getUnseenLocationEnabledReminders(): List<ReminderMessage>

    @Query("UPDATE reminderMessages SET reminder_seen = 1 WHERE msgid = :id")
    fun setReminderSeen(id: Int)

    @Query("UPDATE reminderMessages SET message = :message, location_x = :location_x, location_y = :location_y, reminder_time = :reminder_time, reminder_seen = :reminder_seen WHERE msgid = :id")
    fun updateReminder(id: Int, message: String, location_x: Double, location_y: Double, reminder_time: String, reminder_seen: Boolean)
}