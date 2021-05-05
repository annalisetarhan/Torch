package com.annalisetarhan.torch.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "message_table")
data class DatabaseMessage(
    /* uuid - Internal to this database */
    @PrimaryKey val uuid: String,

    /* messageId - Used to identify message to peers. SHA(encMessage). includes iv/receiverPublicKeyTrunc */
    @ColumnInfo val messageId : ByteArray,

    /* TTD - time to die, seconds since epoch when message should be deleted */
    @ColumnInfo val ttd: Long,

    /* messageInfo - 01 for standard, 02 for private, 03 for pkDecode */
    @ColumnInfo val messageInfo: Int,

    /* Standard - iv + hashkey(hashtag, timeSent, senderPublicKeyTrunc, messageString) */
    /* Private - receiverPkTrunc + receiverPublicKey(timeSent, senderPublicKeyTrunc, messageString) */
    @ColumnInfo val encMessage: ByteArray,

    /* These will only be filled in when user has activated the corresponding hashtag */
    @ColumnInfo var hashtag: String? = null,        // Standard messages only
    @ColumnInfo var receiverPublicKeyTrunc: Long? = null,  // Private messages only. Visible to all, not just co-hashtaggers
    @ColumnInfo var timeSent: Long? = null,
    @ColumnInfo var senderPublicKeyTrunc: Long? = null,
    @ColumnInfo var message: String? = null
)