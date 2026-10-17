package com.calmmap.ui.map

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.calmmap.ui.theme.Ink
import com.calmmap.ui.theme.InkMuted
import com.calmmap.ui.theme.LineMid
import com.calmmap.ui.theme.Paper
import com.calmmap.ui.theme.SensoryLevel

/**
 * 지도에 찍을 마커 그림을 단계마다 하나씩 만든다.
 *
 * 네이버 지도 마커는 Compose 로 그릴 수 없고 비트맵(그림 파일)을 받는다.
 * 그래서 단계별로 원을 직접 그려서 넘긴다.
 *
 * 채움과 테두리 규칙은 배지 · 그래프와 같다.
 *   무던함 · 보통 · 부담 높음   채워서 그림
 *   후보                      채우지 않고 실선
 *   정보 부족                  채우지 않고 점선
 *
 * 만드는 데 비용이 있으므로 화면이 다시 그려질 때마다 만들지 않고
 * MapScreen 에서 remember 로 한 번만 만들어 둔다.
 */
fun markerBitmap(
    level: SensoryLevel,
    density: Density,
    /** 지금 고른 장소인지. 고른 것은 크고 두껍게 그려 어느 것을 눌렀는지 보이게 한다 */
    selected: Boolean = false,
): Bitmap = with(density) {
    val diameter = (if (selected) 26 else 20).dp.toPx()
    val strokeWidth = (if (selected) 3f else 1.5f).dp.toPx()
    // 테두리가 가장자리에서 잘리지 않게 선 굵기만큼 여유를 둔다
    val side = (diameter + strokeWidth * 2).toInt()

    val bitmap = Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val center = side / 2f
    val radius = diameter / 2f

    level.fill?.let { fill ->
        canvas.drawCircle(
            center, center, radius,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = fill.toArgb()
                style = Paint.Style.FILL
            },
        )
    }

    // 채우지 않는 단계는 바탕이 비치면 지도 위에서 안 보인다. 흰색을 먼저 깐다
    if (level.fill == null) {
        canvas.drawCircle(
            center, center, radius,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Paper.toArgb()
                style = Paint.Style.FILL
            },
        )
    }

    val borderColor = when {
        selected -> Ink
        level == SensoryLevel.HIGH -> Ink
        level == SensoryLevel.CANDIDATE || level == SensoryLevel.NO_DATA -> InkMuted
        else -> LineMid
    }
    canvas.drawCircle(
        center, center, radius,
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = borderColor.toArgb()
            style = Paint.Style.STROKE
            this.strokeWidth = strokeWidth
            if (level.dashed && !selected) {
                pathEffect = DashPathEffect(floatArrayOf(3.dp.toPx(), 2.5f.dp.toPx()), 0f)
            }
        },
    )

    bitmap
}
