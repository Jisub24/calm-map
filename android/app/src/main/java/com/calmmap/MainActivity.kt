package com.calmmap

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.calmmap.ui.map.MapScreen
import com.calmmap.ui.theme.CalmMapTheme
import com.naver.maps.map.NaverMapSdk

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 지도 인증이 실패해도 네이버 SDK 는 아무 말 없이 빈 화면만 보여준다.
        // 원인(키가 틀렸는지, 패키지 이름이 안 맞는지)을 알 수 있게 이유를 남긴다.
        // 지도가 만들어지기 전에 걸어둬야 해서 setContent 앞에 둔다.
        NaverMapSdk.getInstance(this).onAuthFailedListener =
            NaverMapSdk.OnAuthFailedListener { exception ->
                Log.e("CalmMap", "네이버 지도 인증 실패: ${exception.message}", exception)
            }

        enableEdgeToEdge()
        setContent {
            CalmMapTheme {
                // 지금은 지도 하나만 띄운다.
                // 스플래시 · 온보딩이 붙은 뒤에 화면 이동(네비게이션)을 넣는다.
                MapScreen()
            }
        }
    }
}
