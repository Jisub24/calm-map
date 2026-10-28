package com.calmmap.ui.map

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.calmmap.ui.component.ButtonSize
import com.calmmap.ui.component.FilterChip
import com.calmmap.ui.component.FullSegmented
import com.calmmap.ui.component.PrimaryButton
import com.calmmap.ui.component.calmShadow
import com.calmmap.ui.model.TimeSlot
import com.calmmap.ui.theme.BgSurface
import com.calmmap.ui.theme.Radius
import com.calmmap.ui.theme.Shadows
import com.calmmap.ui.theme.TextPrimary
import com.calmmap.ui.theme.TextTertiary
import com.calmmap.ui.theme.caption

/** 고를 수 있는 시각. 그래프와 같은 8~22시 */
private val SLOT_HOURS = (8..22).toList()

/**
 * 지도 '시간 선택' 칩을 누르면 아래 시트 자리에 올라오는 시트.
 * 피그마에 따로 시안이 없어서 지도 아래 시트(06)와 같은 모양에 기존 Segmented · Chip 을 쓴다.
 *
 *   평일 / 주말   점수 칸이 장소 × 평일/주말 × 1시간이라 날짜 대신 이 둘만 고른다
 *   시각         8~22시를 옆으로 넘겨 고른다
 *
 * 고르는 즉시 [onChange] 로 알린다. 시트를 닫지 않아도 위쪽 지도 마커가 바로 바뀐다.
 */
@Composable
internal fun TimeSlotSheet(
    initial: TimeSlot,
    /** 오늘이 주말인지. 오늘과 다른 쪽을 고르면 다가오는 날 기준이라고 알린다 */
    todayWeekend: Boolean,
    onChange: (TimeSlot) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var weekend by remember { mutableStateOf(initial.weekend) }
    var hour by remember { mutableIntStateOf(initial.hour) }
    fun pick(newWeekend: Boolean = weekend, newHour: Int = hour) {
        weekend = newWeekend
        hour = newHour
        onChange(TimeSlot(newWeekend, newHour))
    }
    // 고른 시각이 왼쪽 끝에 붙지 않게 두 칸 앞부터 보여준다
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = (SLOT_HOURS.indexOf(initial.hour) - 2).coerceAtLeast(0),
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .calmShadow(Shadows.sheet, Radius.sheet)
            .clip(Radius.sheet)
            .background(BgSurface)
            // 아래로 끌면 닫힌다. 빈 곳을 눌러도 뒤의 지도로 넘어가지 않게 터치를 여기서 끝낸다
            .sheetDrag(open = true, onOpen = {}, onClose = onClose)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
            .padding(top = 10.dp, bottom = 14.dp),
    ) {
        SheetHandle(Modifier.align(Alignment.CenterHorizontally))
        Column(
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(text = "시간 선택", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
            Text(text = "고른 시간 기준으로 지도에 보여드려요", style = MaterialTheme.typography.caption, color = TextTertiary)
        }

        FullSegmented(
            options = listOf("평일", "주말"),
            selectedIndex = if (weekend) 1 else 0,
            onSelect = { pick(newWeekend = it == 1) },
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp),
        )
        // 오늘이 아닌 쪽은 batch 가 계산해 둔 가장 가까운 날 값이다 (영업시간이 그 요일 기준)
        Text(
            text = if (weekend == todayWeekend) "오늘 기준이에요" else "다가오는 ${if (weekend) "토요일" else "월요일"} 기준이에요",
            style = MaterialTheme.typography.caption,
            color = TextTertiary,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 6.dp),
        )

        // 시트 양옆 여백 안에서 잘리지 않고 화면 끝까지 넘어가도록 여백은 목록 안쪽에 준다
        LazyRow(
            state = listState,
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 12.dp),
        ) {
            items(SLOT_HOURS) { h ->
                FilterChip(label = "${h}시", selected = h == hour, onClick = { pick(newHour = h) })
            }
        }

        PrimaryButton(
            text = "확인",
            onClick = {
                // 아무것도 안 바꾸고 눌러도 보이는 시간으로 정한다
                onChange(TimeSlot(weekend, hour))
                onClose()
            },
            size = ButtonSize.M,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 16.dp),
        )
    }
}
