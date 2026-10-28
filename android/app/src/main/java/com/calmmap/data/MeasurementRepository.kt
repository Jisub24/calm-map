package com.calmmap.data

import android.os.Build
import com.calmmap.ui.model.MeasureResult
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import java.time.ZoneId
import kotlinx.serialization.Serializable

// 측정 결과(measurements · measurement_labels)를 쓴다. 자기 계정으로만 넣을 수 있다 (RLS)
object MeasurementRepository {

    /** measurements 한 줄. 칸 이름은 DB 와 같게 둔다. 원본 소리를 담는 칸은 없다 */
    @Serializable
    private data class MeasurementRow(
        val place_id: Long,
        val user_id: String,
        /** 기종. 보정값(settings.device_offsets)은 이 앞부분으로 찾는다. 갤럭시는 "SM-F766N" 처럼 나온다 */
        val device_model: String,
        /** 측정 시작 시각. 서울 시간대를 붙여 보낸다 */
        val measured_at: String,
        val duration_sec: Int,
        /** 소리 크기 Leq */
        val avg_db: Double,
        val max_db: Double,
        /** 배경 소음 LA90 */
        val background_db: Double,
        val avg_lux: Double?,
        val lux_status: String,
        val flicker_status: String,
        val sudden_count: Int,
        val sudden_strong_count: Int,
        val sudden_max_rise: Double?,
        val alert_count: Int,
        val announce_count: Int?,
        val announce_level: Int?,
        val crowd_level: Int?,
    )

    @Serializable
    private data class LabelRow(
        val measurement_id: Long,
        val yamnet_label: String,
        val ratio: Float,
        val avg_confidence: Float,
    )

    @Serializable
    private data class IdRow(val id: Long)

    /**
     * 측정 한 건을 저장하고 measurements id 를 돌려준다.
     * 로그인이 안 돼 있거나 1분 미만이면 예외. 라벨은 측정 줄이 들어간 뒤에 넣는다
     */
    suspend fun save(result: MeasureResult): Long {
        val userId = AuthRepository.userId() ?: error("로그인하지 않은 상태에서 저장하려고 했어요")
        require(result.canSave) { "1분이 안 되는 측정은 저장할 수 없어요" }

        val row = MeasurementRow(
            place_id = result.place.id,
            user_id = userId,
            device_model = Build.MODEL,
            measured_at = result.startedAt.atZone(ZoneId.of("Asia/Seoul")).toOffsetDateTime().toString(),
            duration_sec = result.seconds,
            avg_db = result.leq,
            // DB 가 max >= avg 를 요구한다. 반올림 때문에 어긋나지 않게 맞춘다
            max_db = maxOf(result.maxDb.toDouble(), result.leq),
            background_db = result.backgroundDb,
            avg_lux = result.lux,
            lux_status = result.luxStatus,
            // 깜빡임은 폰으로 잴 수 없고 점수에도 안 들어가서 묻지 않는다
            flicker_status = "미측정",
            sudden_count = result.suddenCount,
            sudden_strong_count = result.suddenStrongCount,
            sudden_max_rise = result.suddenMaxRise,
            alert_count = result.alertCount,
            // 횟수를 직접 센 값은 없다 (측정 중 버튼을 뺌). 끝나고 고른 구간만 보낸다
            announce_count = null,
            announce_level = result.announceLevel?.dbValue,
            crowd_level = result.crowd?.dbValue,
        )
        val id = withFreshSession {
            supabase.from("measurements")
                .insert(row) { select(Columns.list("id")) }
                .decodeSingle<IdRow>()
                .id
        }

        // 0 이면 "한 번도 안 들렸다"는 뜻이라 줄을 보내지 않는다 (007 규칙)
        val labels = result.labelStats
            .filter { it.value.ratio > 0f && it.value.avgConfidence > 0f }
            .map { (label, stat) ->
                LabelRow(
                    measurement_id = id,
                    yamnet_label = label,
                    ratio = stat.ratio.coerceAtMost(1f),
                    avg_confidence = stat.avgConfidence.coerceAtMost(1f),
                )
            }
        if (labels.isNotEmpty()) withFreshSession { supabase.from("measurement_labels").insert(labels) }
        return id
    }
}
