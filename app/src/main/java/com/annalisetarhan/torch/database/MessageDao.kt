package com.annalisetarhan.torch.database

import androidx.room.Dao
import androidx.room.Insert

@Dao
interface MessageDao {
    @Insert
    fun insert(message: DatabaseMessage)
}