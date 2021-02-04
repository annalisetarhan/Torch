package com.annalisetarhan.torch.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity
data class DatabaseMessage(
    /* uid - Internal to this database */
    @PrimaryKey val uuid: UUID,

    /* messageId - Used to identify message to peers. SHA(message) */
    @ColumnInfo val messageId : String,

    /* TTD - time to die, seconds since epoch when message should be deleted */
    @ColumnInfo val TTD: Long,

    /* encMessage -  hashkey(hashtag, messageString, senderPublicKey) */
    @ColumnInfo val encMessage: String,

    /* These will only be filled in if user has entered the corresponding hashtag */
    @ColumnInfo var hashtag: String? = null,
    @ColumnInfo var hashkey: ByteArray? = null,
    @ColumnInfo var senderPublicKey: String? = null,
    @ColumnInfo var messageString: String? = null
)