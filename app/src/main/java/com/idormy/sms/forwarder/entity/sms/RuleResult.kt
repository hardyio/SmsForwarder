package com.idormy.sms.forwarder.entity.sms

data class RuleResult(
    val code: Int,
    val `data`: Data,
    val message: String
)

data class Data(
    val PhoneNumber: Long,
    val Remarks: String,
    val SmsTypes: List<SmsType>
)

data class SmsType(
    val ID: Int,
    val Rules: List<String>
)