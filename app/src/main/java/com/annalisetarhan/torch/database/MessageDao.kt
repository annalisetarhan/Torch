package com.annalisetarhan.torch.database

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface MessageDao {
    @Insert
    fun insertExternal(message: DatabaseMessage)

    @Insert
    suspend fun insertInternal(message: DatabaseMessage)

    @Query("SELECT * FROM message_table WHERE hashtag == :hashtag ORDER BY timeSent")
    fun getHashtagMessages(hashtag: String): LiveData<List<DatabaseMessage>>

    @Query("SELECT * FROM message_table WHERE hashtag == null")
    suspend fun getAllEncrypted(): List<DatabaseMessage>

    @Update
    fun addDecryptedInfo(message: DatabaseMessage)

    @Query("SELECT * FROM message_table")
    fun getAllMessages(): List<DatabaseMessage>

    @Query("DELETE FROM message_table WHERE ttd < :ttd")
    fun purge(ttd: Long)
}