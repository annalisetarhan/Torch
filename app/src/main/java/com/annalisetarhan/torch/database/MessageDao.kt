package com.annalisetarhan.torch.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface MessageDao {
    @Insert
    fun insert(message: DatabaseMessage)

    @Query("SELECT * FROM message_table WHERE hashtag == null")
    fun getAllEncrypted(): List<DatabaseMessage>

    @Update
    fun addDecryptedInfo(message: DatabaseMessage)
}