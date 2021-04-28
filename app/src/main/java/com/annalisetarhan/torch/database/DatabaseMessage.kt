package com.annalisetarhan.torch.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.annalisetarhan.torch.DomainMessage
import java.util.*

@Entity(tableName = "message_table")
data class DatabaseMessage(
    /* uuid - Internal to this database */
    @PrimaryKey val uuid: String,

    /* messageId - Used to identify message to peers. SHA(encMessage). includes iv/receiverPublicKeyTrunc */
    @ColumnInfo val messageId : ByteArray,

    /* TTD - time to die, seconds since epoch when message should be deleted */
    @ColumnInfo val ttd: Long,

    /* messageInfo - 01 for standard, 02 for private, 03 for pkDecode */
    // TODO: decide if it's worth bitmasking the lowest two bits, since I'm only using those to distinguish among message types
    // TODO: if I decide to allow longer messages that get split up and reconstructed, a couple bits here will be for that
    @ColumnInfo val messageInfo: Int,   // Storing as int might be sloppy. I'm really thinking of this as 8 bits, with the first two used as messageType, interpreted as an int since the other bits aren't being used

    /* Standard - iv + hashkey(hashtag, timeSent, senderPublicKeyTrunc, messageString) */
    /* Private - receiverPkTrunc + receiverPublicKey(timeSent, senderPublicKeyTrunc, messageString) */
    /* There's some ambiguity about whether encMessage refers to the whole thing or just the part after the iv/receiverPkTrunc */
    @ColumnInfo val encMessage: ByteArray,

    /* These will only be filled in when user has activated the corresponding hashtag */
    @ColumnInfo var hashtag: String? = null,        // Standard messages only
    @ColumnInfo var receiverPublicKeyTrunc: Long? = null,  // Private messages only. Visible to all, not just co-hashtaggers
    @ColumnInfo var timeSent: Long? = null,
    @ColumnInfo var senderPublicKeyTrunc: Long? = null,
    @ColumnInfo var message: String? = null
)