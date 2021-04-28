package com.annalisetarhan.torch.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface MessageDao {
    @Insert
    suspend fun insert(message: DatabaseMessage)

    @Query("SELECT * FROM message_table WHERE hashtag == :hashtag ORDER BY timeSent")
    fun getHashtagMessages(hashtag: String): LiveData<List<DatabaseMessage>>

    @Query("SELECT * FROM message_table WHERE hashtag == null")
    suspend fun getAllEncrypted(): List<DatabaseMessage>

    @Update
    fun addDecryptedInfo(message: DatabaseMessage)
}