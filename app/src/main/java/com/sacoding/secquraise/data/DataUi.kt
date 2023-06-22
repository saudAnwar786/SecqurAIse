package com.sacoding.secquraise.data

import android.net.Uri

data class DataUi(
    val uri: String? = null,
    val date :String,
    val location:String,
    val chargingStatus:Boolean,
    val chargingPercent:Int
    )
