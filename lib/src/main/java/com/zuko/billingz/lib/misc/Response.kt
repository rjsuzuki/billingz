package com.zuko.billingz.lib.misc

data class Response(

    var id: Int?,
    var status: Status
) {
    enum class Status
}
