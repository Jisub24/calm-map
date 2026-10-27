package com.calmmap.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/** 지금 인터넷을 쓸 수 있는지. 와이파이에 붙어 있어도 실제로 인터넷이 안 되면 false */
fun isOnline(context: Context): Boolean {
    val manager = context.getSystemService(ConnectivityManager::class.java) ?: return false
    val caps = manager.getNetworkCapabilities(manager.activeNetwork) ?: return false
    // VALIDATED: 안드로이드가 실제로 인터넷에 닿는 것까지 확인한 상태
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
        caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}
