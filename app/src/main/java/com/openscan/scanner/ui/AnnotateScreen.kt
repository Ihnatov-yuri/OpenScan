package com.openscan.scanner.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.openscan.scanner.data.AnnotationRenderer
import com.openscan.scanner.ui.components.editorialBarColors
import java.io.File

private class PenStroke(val color: Color, val widthPx: Float) {
    val points = mutableStateListOf<Offset>()
}

private val PEN_COLORS = listOf(Color.Black, Color(0xFF1E6FF0), Color(0xFFE53935))
private val PEN_WIDTHS = listOf(6f, 12f, 22f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnnotateScreen(
    page: File,
    pageNumber: Int,
    onSave: (List<AnnotationRenderer.Stroke>) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val aspect = remember(page.path, page.lastModified()) {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(page.path, opts)
        if (opts.outWidth > 0 && opts.outHeight > 0) opts.outWidth.toFloat() / opts.outHeight else 0.72f
    }

    val strokes = remember { mutableStateListOf<PenStroke>() }
    var activeColor by remember { mutableStateOf(PEN_COLORS.first()) }
    var activeWidth by remember { mutableStateOf(PEN_WIDTHS.first()) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var current by remember { mutableStateOf<PenStroke?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = editorialBarColors(),
                title = { Text("ANNOTATE · PAGE $pageNumber", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { if (strokes.isNotEmpty()) strokes.removeAt(strokes.lastIndex) }) {
                        Icon(Icons.Default.Undo, contentDescription = "Undo")
                    }
                    IconButton(onClick = { strokes.clear() }) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear")
                    }
                    IconButton(onClick = { onSave(strokes.toRenderStrokes(canvasSize)) }) {
                        Icon(Icons.Default.Check, contentDescription = "Save")
                    }
                }
            )
        },
        bottomBar = {
            ToolBar(
                colors = PEN_COLORS,
                widths = PEN_WIDTHS,
                activeColor = activeColor,
                activeWidth = activeWidth,
                onColor = { activeColor = it },
                onWidth = { activeWidth = it }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(aspect)
                    .onSizeChanged { canvasSize = it }
                    .pointerInput(activeColor, activeWidth) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                val s = PenStroke(activeColor, activeWidth)
                                s.points.add(offset)
                                strokes.add(s)
                                current = s
                            },
                            onDrag = { change, _ ->
                                current?.points?.add(change.position)
                                change.consume()
                            },
                            onDragEnd = { current = null },
                            onDragCancel = { current = null }
                        )
                    }
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(page)
                        .memoryCacheKey(page.path + page.lastModified())
                        .build(),
                    contentDescription = "Page $pageNumber",
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.fillMaxSize()
                )
                Canvas(modifier = Modifier.fillMaxSize()) {
                    strokes.forEach { stroke ->
                        if (stroke.points.size == 1) {
                            drawCircle(stroke.color, stroke.widthPx / 2f, stroke.points.first())
                        } else if (stroke.points.size >= 2) {
                            val path = Path().apply {
                                moveTo(stroke.points[0].x, stroke.points[0].y)
                                for (i in 1 until stroke.points.size) {
                                    lineTo(stroke.points[i].x, stroke.points[i].y)
                                }
                            }
                            drawPath(
                                path = path,
                                color = stroke.color,
                                style = Stroke(width = stroke.widthPx, cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolBar(
    colors: List<Color>,
    widths: List<Float>,
    activeColor: Color,
    activeWidth: Float,
    onColor: (Color) -> Unit,
    onWidth: (Float) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        colors.forEach { color ->
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(color, CircleShape)
                    .border(
                        width = if (color == activeColor) 3.dp else 1.dp,
                        color = if (color == activeColor) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline,
                        shape = CircleShape
                    )
                    .clickable { onColor(color) }
            )
        }
        Box(modifier = Modifier.weight(1f))
        widths.forEachIndexed { index, width ->
            val dotSize = (10 + index * 8).dp
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .border(
                        width = if (width == activeWidth) 2.dp else 0.dp,
                        color = if (width == activeWidth) MaterialTheme.colorScheme.primary else Color.Transparent,
                        shape = CircleShape
                    )
                    .clickable { onWidth(width) },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(dotSize)
                        .background(MaterialTheme.colorScheme.onSurface, CircleShape)
                )
            }
        }
    }
}

private fun List<PenStroke>.toRenderStrokes(canvasSize: IntSize): List<AnnotationRenderer.Stroke> {
    val w = canvasSize.width.toFloat().coerceAtLeast(1f)
    val h = canvasSize.height.toFloat().coerceAtLeast(1f)
    return mapNotNull { s ->
        if (s.points.isEmpty()) return@mapNotNull null
        AnnotationRenderer.Stroke(
            xs = FloatArray(s.points.size) { s.points[it].x / w },
            ys = FloatArray(s.points.size) { s.points[it].y / h },
            color = s.color.toArgb(),
            widthFrac = s.widthPx / w
        )
    }
}
