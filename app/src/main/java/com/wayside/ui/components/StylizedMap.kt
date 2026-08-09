package com.wayside.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wayside.ui.theme.LocalWaysideColors
import com.wayside.ui.theme.PlusJakartaSans

/** Normalised polyline of the Alfama → Sintra drive, bottom-left to top-right. */
private val ROUTE_POINTS = listOf(
    Offset(0.10f, 0.86f),
    Offset(0.24f, 0.72f),
    Offset(0.34f, 0.633f),
    Offset(0.32f, 0.573f),
    Offset(0.48f, 0.540f),
    Offset(0.60f, 0.480f),
    Offset(0.74f, 0.433f),
    Offset(0.84f, 0.347f),
    Offset(0.92f, 0.25f),
)

private data class Block(val x: Float, val y: Float, val w: Float, val h: Float, val rotation: Float)

private val BLOCKS = listOf(
    Block(0.04f, 0.72f, 0.14f, 0.09f, -12f),
    Block(0.20f, 0.86f, 0.11f, 0.07f, 8f),
    Block(0.40f, 0.80f, 0.16f, 0.10f, -6f),
    Block(0.62f, 0.86f, 0.13f, 0.08f, 14f),
    Block(0.80f, 0.72f, 0.15f, 0.11f, -9f),
    Block(0.06f, 0.44f, 0.12f, 0.08f, 16f),
    Block(0.44f, 0.62f, 0.10f, 0.07f, -18f),
    Block(0.66f, 0.52f, 0.14f, 0.09f, 7f),
    Block(0.24f, 0.28f, 0.13f, 0.08f, -14f),
    Block(0.52f, 0.20f, 0.11f, 0.07f, 11f),
    Block(0.76f, 0.44f, 0.10f, 0.06f, -5f),
    Block(0.14f, 0.12f, 0.12f, 0.08f, 6f),
)

private data class Park(val cx: Float, val cy: Float, val rx: Float, val ry: Float)

private val PARKS = listOf(
    Park(0.24f, 0.56f, 0.13f, 0.09f),
    Park(0.70f, 0.66f, 0.10f, 0.07f),
    Park(0.86f, 0.30f, 0.12f, 0.10f),
    Park(0.44f, 0.34f, 0.09f, 0.06f),
)

/** Deterministic dot clusters inside the parks — trees, roughly. */
private val PARK_DOTS = listOf(
    Offset(-0.5f, -0.3f), Offset(0.1f, 0.2f), Offset(0.5f, -0.4f),
    Offset(-0.2f, 0.5f), Offset(0.6f, 0.4f), Offset(-0.6f, 0.2f),
)

private val ROADS = listOf(
    // x, y pairs as a flat polyline, plus a stroke width in dp
    Triple(
        listOf(Offset(0f, 0.66f), Offset(0.3f, 0.62f), Offset(0.6f, 0.72f), Offset(1f, 0.64f)),
        9f,
        0.9f,
    ),
    Triple(
        listOf(Offset(0.16f, 1f), Offset(0.22f, 0.68f), Offset(0.18f, 0.36f), Offset(0.28f, 0f)),
        7f,
        0.85f,
    ),
    Triple(
        listOf(Offset(0f, 0.24f), Offset(0.34f, 0.30f), Offset(0.66f, 0.20f), Offset(1f, 0.26f)),
        6f,
        0.8f,
    ),
    Triple(
        listOf(Offset(0.62f, 1f), Offset(0.58f, 0.70f), Offset(0.70f, 0.40f), Offset(0.66f, 0f)),
        6f,
        0.8f,
    ),
    Triple(
        listOf(Offset(0f, 0.86f), Offset(0.40f, 0.90f), Offset(0.72f, 0.82f), Offset(1f, 0.88f)),
        4f,
        0.7f,
    ),
    Triple(
        listOf(Offset(0.86f, 1f), Offset(0.90f, 0.62f), Offset(0.82f, 0.30f), Offset(0.88f, 0f)),
        4f,
        0.7f,
    ),
    Triple(
        listOf(Offset(0f, 0.46f), Offset(0.30f, 0.44f), Offset(0.58f, 0.52f), Offset(1f, 0.46f)),
        3f,
        0.6f,
    ),
)

private val LABELS = listOf(
    Triple("BENFICA", 0.10f, 0.62f),
    Triple("QUELUZ", 0.40f, 0.72f),
    Triple("CACÉM", 0.56f, 0.44f),
    Triple("SINTRA", 0.78f, 0.14f),
)

/** Smooth a normalised polyline into a Path in pixel space. */
private fun smoothPath(points: List<Offset>, size: Size): Path = Path().apply {
    if (points.isEmpty()) return@apply
    fun px(p: Offset) = Offset(p.x * size.width, p.y * size.height)
    val first = px(points.first())
    moveTo(first.x, first.y)
    for (i in 0 until points.size - 1) {
        val current = px(points[i])
        val next = px(points[i + 1])
        val mid = Offset((current.x + next.x) / 2f, (current.y + next.y) / 2f)
        quadraticTo(current.x, current.y, mid.x, mid.y)
    }
    val last = px(points.last())
    lineTo(last.x, last.y)
}

fun routePath(size: Size): Path = smoothPath(ROUTE_POINTS, size)

/**
 * The stylised map every map-bearing screen sits on. Nothing here talks to a map SDK —
 * it is a Canvas drawing in the Wayside map palette.
 *
 * @param corridorWidth translucent "discovery corridor" under the route; animate it to
 *   show a wider detour budget opening up more of the map.
 */
@Composable
fun StylizedMap(
    modifier: Modifier = Modifier,
    showRoute: Boolean = false,
    corridorWidth: Dp = 0.dp,
    overlay: @Composable BoxScope.(width: Dp, height: Dp) -> Unit = { _, _ -> },
) {
    val wayside = LocalWaysideColors.current
    val scheme = MaterialTheme.colorScheme
    val measurer = rememberTextMeasurer()
    val labelStyle = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        letterSpacing = 2.sp,
        color = scheme.onBackground.copy(alpha = if (wayside.isDark) 0.24f else 0.20f),
    )

    BoxWithConstraints(modifier) {
        val boxWidth = maxWidth
        val boxHeight = maxHeight
        Canvas(Modifier.fillMaxSize()) {
            drawLand(wayside.mapLand)
            drawRiver(wayside.mapWater)
            drawParks(wayside.mapPark, wayside.isDark)
            drawBlocks(wayside.isDark)
            drawRoads(wayside.mapRoad)
            if (showRoute) {
                drawDiscoveryCorridor(scheme.primary, corridorWidth.toPx())
                drawRoute(wayside.mapRoad, scheme.primary)
            }
            LABELS.forEach { (text, x, y) ->
                drawText(
                    textMeasurer = measurer,
                    text = text,
                    topLeft = Offset(size.width * x, size.height * y),
                    style = labelStyle,
                )
            }
        }
        overlay(boxWidth, boxHeight)
    }
}

private fun DrawScope.drawLand(land: Color) {
    drawRect(land)
}

private fun DrawScope.drawRiver(water: Color) {
    val river = smoothPath(
        listOf(
            Offset(-0.05f, 0.70f),
            Offset(0.22f, 0.79f),
            Offset(0.48f, 0.80f),
            Offset(0.72f, 0.88f),
            Offset(1.05f, 0.86f),
        ),
        size,
    )
    drawPath(river, water, style = Stroke(width = size.height * 0.045f, cap = StrokeCap.Round))
}

private fun DrawScope.drawParks(park: Color, isDark: Boolean) {
    PARKS.forEach { p ->
        val center = Offset(size.width * p.cx, size.height * p.cy)
        val rx = size.width * p.rx
        val ry = size.height * p.ry
        drawOval(
            color = park,
            topLeft = Offset(center.x - rx, center.y - ry),
            size = Size(rx * 2, ry * 2),
        )
        val dotColor = if (isDark) {
            Color.White.copy(alpha = 0.10f)
        } else {
            Color.Black.copy(alpha = 0.07f)
        }
        PARK_DOTS.forEach { d ->
            drawCircle(
                color = dotColor,
                radius = size.minDimension * 0.011f,
                center = Offset(center.x + rx * d.x * 0.7f, center.y + ry * d.y * 0.7f),
            )
        }
    }
}

private fun DrawScope.drawBlocks(isDark: Boolean) {
    val blockColor = if (isDark) {
        Color.White.copy(alpha = 0.05f)
    } else {
        Color.Black.copy(alpha = 0.045f)
    }
    BLOCKS.forEach { b ->
        val topLeft = Offset(size.width * b.x, size.height * b.y)
        val blockSize = Size(size.width * b.w, size.height * b.h)
        val pivot = Offset(topLeft.x + blockSize.width / 2f, topLeft.y + blockSize.height / 2f)
        withTransform({ rotate(b.rotation, pivot) }) {
            drawRoundRect(
                color = blockColor,
                topLeft = topLeft,
                size = blockSize,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.minDimension * 0.02f),
            )
        }
    }
}

private fun DrawScope.drawRoads(road: Color) {
    ROADS.forEach { (points, widthDp, alpha) ->
        drawPath(
            path = smoothPath(points, size),
            color = road.copy(alpha = alpha),
            style = Stroke(width = widthDp.dp.toPx(), cap = StrokeCap.Round),
        )
    }
}

private fun DrawScope.drawDiscoveryCorridor(primary: Color, widthPx: Float) {
    if (widthPx <= 0f) return
    val path = routePath(size)
    drawPath(path, primary.copy(alpha = 0.10f), style = Stroke(width = widthPx, cap = StrokeCap.Round))
    drawPath(
        path,
        primary.copy(alpha = 0.16f),
        style = Stroke(width = widthPx * 0.55f, cap = StrokeCap.Round),
    )
}

private fun DrawScope.drawRoute(road: Color, primary: Color) {
    val path = routePath(size)
    drawPath(path, road, style = Stroke(width = 11.dp.toPx(), cap = StrokeCap.Round))
    drawPath(path, primary, style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round))
}

/** The winding road used by the onboarding illustrations: white with a dashed petrol overlay. */
fun DrawScope.drawWindingRoad(roadColor: Color, dashColor: Color, points: List<Offset>) {
    val path = smoothPath(points, size)
    drawPath(path, roadColor, style = Stroke(width = size.minDimension * 0.035f, cap = StrokeCap.Round))
    drawPath(
        path,
        dashColor,
        style = Stroke(
            width = size.minDimension * 0.009f,
            cap = StrokeCap.Round,
            pathEffect = PathEffect.dashPathEffect(
                floatArrayOf(size.minDimension * 0.035f, size.minDimension * 0.030f),
            ),
        ),
    )
}

/** Positions a composable at a normalised map coordinate, anchored at its bottom tip. */
@Composable
fun MapAnchor(
    x: Float,
    y: Float,
    mapWidth: Dp,
    mapHeight: Dp,
    anchorWidth: Dp,
    anchorHeight: Dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier.offset(
            x = mapWidth * x - anchorWidth / 2,
            y = mapHeight * y - anchorHeight,
        ),
    ) {
        content()
    }
}
