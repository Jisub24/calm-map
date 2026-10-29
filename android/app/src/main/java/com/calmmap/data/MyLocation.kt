package com.calmmap.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.naver.maps.geometry.LatLng

/**
 * 장소 검색의 기준 위치. 가까운 가게부터 보여주는 데 쓴다.
 *
 * 폰이 마지막으로 안 위치를 쓴다. 새로 위치를 잡으면 몇 초씩 걸려서 검색이 늦어진다.
 * 위치 권한이 없거나, 위치를 모르거나, 서울 밖이면 홍대를 기준으로 한다.
 * 서울시 혼잡 정보가 서울만 있어서 서울 밖 가게는 넣어도 정보가 거의 없다. 에뮬레이터는 미국 위치가 나온다.
 */
object MyLocation {

    val HONGDAE = LatLng(37.5566, 126.9237)

    fun searchOrigin(context: Context): LatLng = lastKnown(context)?.takeIf { inSeoul(it) } ?: HONGDAE

    private fun lastKnown(context: Context): LatLng? {
        val granted = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            .any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }
        if (!granted) return null
        val manager = context.getSystemService(LocationManager::class.java) ?: return null
        // GPS · 네트워크 중 가장 최근 것
        val latest = manager.getProviders(true)
            .mapNotNull { runCatching { manager.getLastKnownLocation(it) }.getOrNull() }
            .maxByOrNull { it.time }
            ?: return null
        return LatLng(latest.latitude, latest.longitude)
    }

    private fun inSeoul(p: LatLng): Boolean = p.latitude in 37.41..37.72 && p.longitude in 126.76..127.19
}
