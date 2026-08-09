package com.wayside.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wayside.ui.theme.LocalWaysideColors
import com.wayside.ui.theme.WaysideMotion
import kotlinx.coroutines.launch

enum class SheetHeight { Peek, Half, Expanded }

/**
 * Bottom sheet that snaps to three heights and settles with a little bounce.
 *
 * Material 3's `BottomSheetScaffold` only offers partially-expanded and expanded, so this
 * is hand-rolled: drag the handle (or the sheet body) and it animates to the nearest of
 * the three anchors.
 */
@Composable
fun WaysideBottomSheet(
    sheetHeight: SheetHeight,
    onSheetHeightChange: (SheetHeight) -> Unit,
    modifier: Modifier = Modifier,
    peek: Dp = 132.dp,
    half: Dp = 330.dp,
    expanded: Dp = 560.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val wayside = LocalWaysideColors.current
    val scheme = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    val peekPx = with(density) { peek.toPx() }
    val halfPx = with(density) { half.toPx() }
    val expandedPx = with(density) { expanded.toPx() }

    fun anchorPx(target: SheetHeight) = when (target) {
        SheetHeight.Peek -> peekPx
        SheetHeight.Half -> halfPx
        SheetHeight.Expanded -> expandedPx
    }

    val heightPx = remember { Animatable(anchorPx(sheetHeight)) }

    LaunchedEffect(sheetHeight, peekPx, halfPx, expandedPx) {
        heightPx.animateTo(anchorPx(sheetHeight), WaysideMotion.sheetSpring())
    }

    fun nearestAnchor(current: Float): SheetHeight = listOf(
        SheetHeight.Peek to peekPx,
        SheetHeight.Half to halfPx,
        SheetHeight.Expanded to expandedPx,
    ).minByOrNull { kotlin.math.abs(it.second - current) }!!.first

    val dragState = rememberDraggableState { delta ->
        scope.launch {
            // Dragging up (negative delta) makes the sheet taller.
            heightPx.snapTo((heightPx.value - delta).coerceIn(peekPx, expandedPx))
        }
    }

    val shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(with(density) { heightPx.value.toDp() })
            .shadow(18.dp, shape, clip = false)
            .clip(shape)
            .background(scheme.surface)
            .draggable(
                state = dragState,
                orientation = Orientation.Vertical,
                onDragStopped = {
                    val target = nearestAnchor(heightPx.value)
                    scope.launch { heightPx.animateTo(anchorPx(target), WaysideMotion.sheetSpring()) }
                    onSheetHeightChange(target)
                },
            ),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(wayside.textMuted.copy(alpha = 0.45f)),
            )
        }
        content()
    }
}
