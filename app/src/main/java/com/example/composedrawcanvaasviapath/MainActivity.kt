package com.example.composedrawcanvaasviapath

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.composedrawcanvaasviapath.ui.theme.ComposeDrawCanvaasviaPathTheme

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ComposeDrawCanvaasviaPathTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    DrawingCanvas()
                }
            }
        }
    }
}

data class Draw(
    val path: Path,
    val color: Color,
    val stroke: Stroke
)

@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
@SuppressLint("ReturnFromAwaitPointerEventScope")
@Composable
fun DrawingCanvas() {
    val paths = remember { mutableStateListOf<Draw>() }
    val oldPaths = remember { mutableStateListOf<Draw>() }
    var currentPath by remember { mutableStateOf(Path()) }
    var isDrawing by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    var showStrokePicker by remember { mutableStateOf(false) }
    var selectedColor by remember { mutableStateOf(Color.Black) }
    var strokeWidth by remember { mutableStateOf(10f) }

    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 20.dp)
                .padding(top = 50.dp, bottom = 20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
        ) {
            Canvas(
                modifier = Modifier
                    .padding(horizontal = 10.dp)
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        var isDrawingGesture = false

                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                val pointers = event.changes

                                if (pointers.size >= 2) {
                                    val pan = pointers[0].position - pointers[0].previousPosition
                                    val zoom =
                                        (pointers[0].position - pointers[1].position).getDistance() /
                                                (pointers[0].previousPosition - pointers[1].previousPosition).getDistance()

                                    val gestureCenter =
                                        (pointers[0].position + pointers[1].position) / 2f

                                    val focalBefore = (gestureCenter - offset) / scale

                                    scale *= zoom
                                    scale = scale.coerceIn(0.5f, 5f)

                                    val focalAfter = (gestureCenter - offset) / scale

                                    offset += (focalAfter - focalBefore) * scale
                                    offset += pan * 1.5f

                                    pointers.forEach { it.consume() }
                                    isDrawingGesture = false
                                } else if (pointers.size == 1) {
                                    val change = pointers[0]
                                    if (change.pressed) {
                                        val x = (change.position.x - offset.x) / scale
                                        val y = (change.position.y - offset.y) / scale
                                        if (!isDrawingGesture) {
                                            currentPath = Path().apply { moveTo(x, y) }
                                            isDrawing = true
                                            isDrawingGesture = true
                                        } else {
                                            currentPath = Path().apply {
                                                addPath(currentPath)
                                                lineTo(x, y)
                                            }
                                            isDrawing = true
                                        }
                                        change.consume()
                                    } else {
                                        if (!currentPath.isEmpty) {
                                            paths.add(
                                                Draw(
                                                    path = Path().apply { addPath(currentPath) },
                                                    color = selectedColor,
                                                    stroke = Stroke(
                                                        width = strokeWidth,
                                                        cap = StrokeCap.Round,
                                                        join = StrokeJoin.Round
                                                    )
                                                )
                                            )
                                        }
                                        currentPath = Path()
                                        isDrawing = false
                                        isDrawingGesture = false
                                    }
                                }
                            }
                        }
                    }
            ) {
                scale(scale, scale, pivot = Offset.Zero) {
                    translate(offset.x / scale, offset.y / scale) {
                        paths.forEach { draw ->
                            drawPath(
                                path = draw.path,
                                color = draw.color,
                                style = draw.stroke
                            )
                        }
                        if (isDrawing && !currentPath.isEmpty) {
                            drawPath(
                                path = currentPath,
                                color = selectedColor,
                                style = Stroke(
                                    width = strokeWidth,
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }
                    }
                }
            }
        }

        AnimatedVisibility(showStrokePicker) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
                    .padding(horizontal = 20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
            ) {
                Slider(
                    strokeWidth,
                    onValueChange = { strokeWidth = it },
                    modifier = Modifier
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                        .fillMaxWidth(),
                    valueRange = 5f..40f
                )
            }
        }
        AnimatedVisibility(showColorPicker) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
                    .padding(horizontal = 20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
            ) {
                val colors = listOf(
                    Color.Black,
                    Color.Red,
                    Color(0xffFF7C00), Color(0xffBF64FF)
                )
                LazyRow {
                    items(colors) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .padding(10.dp)
                                .clip(CircleShape)
                                .background(it)
                                .border(
                                    4.dp,
                                    if (it == selectedColor) Color.White else Color.Transparent,
                                    CircleShape
                                )
                                .clickable {
                                    selectedColor = it
                                }
                        ) {
                            if (it == selectedColor)
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "",
                                    modifier = Modifier.align(
                                        Alignment.Center
                                    ), tint = Color.White
                                )
                        }
                    }
                }
            }
        }
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 40.dp)
                .padding(horizontal = 20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = {
                    if (paths.isNotEmpty()) {
                        oldPaths.add(paths.last())
                        paths.removeLast()
                    }
                }) {
                    Icon(
                        painter = painterResource(R.drawable.undo),
                        contentDescription = "",
                        tint = if (paths.isNotEmpty()) Color.Black else Color.Gray
                    )
                }
                IconButton(onClick = {
                    if (oldPaths.isNotEmpty()) {
                        paths.add(oldPaths.last())
                        oldPaths.removeLast()
                    }
                }) {
                    Icon(
                        painter = painterResource(R.drawable.redo),
                        contentDescription = "",
                        tint = if (oldPaths.isNotEmpty()) Color.Black else Color.Gray
                    )
                }
                Spacer(Modifier.width(20.dp))
                IconButton(onClick = {
                    showColorPicker = !showColorPicker
                }) {
                    Icon(
                        painter = painterResource(R.drawable.color),
                        contentDescription = "",
                        tint = selectedColor
                    )
                }
                IconButton(onClick = {
                    showStrokePicker = !showStrokePicker
                }) {
                    Icon(painter = painterResource(R.drawable.draw), contentDescription = "")
                }
                IconButton(onClick = {
                    paths.clear()
                }) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "",
                        tint = Color.Red
                    )
                }
            }
        }
    }
}