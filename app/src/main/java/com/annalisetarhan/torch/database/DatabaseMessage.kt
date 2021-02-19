package com.annalisetarhan.torch.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity(tableName = "message_table")
data class DatabaseMessage(
    /* uid - Internal to this database */
    @PrimaryKey val uuid: String,

    /* messageId - Used to identify message to peers. SHA(encMessage) */
    @ColumnInfo val messageId : ByteArray,

    /* TTD - time to die, seconds since epoch when message should be deleted */
    @ColumnInfo val ttd: Long,

    /* encMessage -  hashkey(hashtag, timeSent, senderPublicKeyTrunc, messageString) */
    @ColumnInfo val encMessage: ByteArray,

    /* These will only be filled in if user has activated the corresponding hashtag */
    @ColumnInfo var hashtag: String? = null,
    @ColumnInfo var timeSent: Long? = null,
    @ColumnInfo var senderPublicKeyTrunc: ByteArray? = null,
    @ColumnInfo var message: String? = null
)