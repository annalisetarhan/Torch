package com.annalisetarhan.torch.encryption

class Time {
    private var ttd = Constants.SECONDS_IN_A_DAY

    fun getMessageTtd(timeSent: Long): Long = timeSent + ttd

    fun updateTtdSetting(hours: Int) {
        ttd = hours * Constants.SECONDS_IN_AN_HOUR
    }

    companion object {
        fun currentTime(): Long = System.currentTimeMillis() / 1000
        fun currentTimeInSecs(): Long = System.currentTimeMillis()/1000
    }
}