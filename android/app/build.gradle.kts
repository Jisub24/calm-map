import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    // DB 줄을 코틀린 클래스로 바꿀 때 쓴다 (@Serializable)
    alias(libs.plugins.kotlin.serialization)
}

// 키를 코드에 적지 않고 local.properties 에서 읽는다.
// local.properties 는 .gitignore 가 막고 있어서 저장소에 올라가지 않는다.
// 저장소가 공개라 키가 한 번 올라가면 기록에 남는다 (CLAUDE.md 원칙 7).
val localProps = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

android {
    namespace = "com.calmmap"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.calmmap"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // AndroidManifest.xml 의 ${naverMapClientId} 자리에 들어간다.
        // 값이 없으면 지도가 회색 화면만 나온다.
        manifestPlaceholders["naverMapClientId"] =
            localProps.getProperty("NAVER_MAP_CLIENT_ID") ?: ""

        // 코드에서 BuildConfig.SUPABASE_URL 처럼 꺼내 쓴다.
        // anon key 만 넣는다. service key 는 앱에 절대 넣지 않는다
        buildConfigField("String", "SUPABASE_URL", "\"${localProps.getProperty("SUPABASE_URL") ?: ""}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${localProps.getProperty("SUPABASE_ANON_KEY") ?: ""}\"")
        // 카카오 장소 검색(REST). 비어 있으면 검색에 카카오 결과가 안 나오고 DB 장소만 나온다
        buildConfigField("String", "KAKAO_REST_KEY", "\"${localProps.getProperty("KAKAO_REST_KEY") ?: ""}\"")
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        // 위 buildConfigField 를 쓰려면 켜야 한다
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.naver.map.sdk)
    implementation(platform(libs.supabase.bom))
    implementation(libs.supabase.auth)
    // 표 읽기 · 쓰기 (supabase.from("표").select())
    implementation(libs.supabase.postgrest)
    implementation(libs.ktor.client.okhttp)
    // YAMNet 을 폰 안에서 돌린다 (모델: src/main/assets/yamnet.tflite)
    implementation(libs.mediapipe.tasks.audio)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}