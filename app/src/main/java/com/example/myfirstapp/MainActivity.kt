package com.example.myfirstapp // Make sure this matches your folder structure!

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

@Serializable
data class Task(val id: Int, val text: String, val isCompleted: Boolean)

// Custom six-dot drag handle icon
@Composable
fun DragHandleIcon(
    modifier: Modifier = Modifier,
    color: Color = Color.Gray
) {
    Canvas(modifier = modifier.size(width = 16.dp, height = 24.dp)) {
        val dotRadius = 3.dp.toPx()
        val horizontalSpacing = 8.dp.toPx()
        val verticalSpacing = 7.dp.toPx()

        // Calculate starting positions to center the dots
        val startX = (size.width - horizontalSpacing) / 2
        val startY = (size.height - 2 * verticalSpacing) / 2

        // Draw 6 dots in 2 columns x 3 rows
        for (row in 0..2) {
            for (col in 0..1) {
                drawCircle(
                    color = color,
                    radius = dotRadius,
                    center = Offset(
                        x = startX + col * horizontalSpacing,
                        y = startY + row * verticalSpacing
                    )
                )
            }
        }
    }
}

class MainActivity : ComponentActivity() {
    private fun saveTasks(tasks: List<Task>, nextId: Int) {
        val sharedPref = getSharedPreferences("tasks", MODE_PRIVATE)
        val json = Json.encodeToString(tasks)
        sharedPref.edit().apply {
            putString("taskList", json)
            putInt("nextId", nextId)
            apply()
        }
    }

    private fun loadTasks(): Pair<List<Task>, Int> {
        val sharedPref = getSharedPreferences("tasks", MODE_PRIVATE)
        val json = sharedPref.getString("taskList", "[]") ?: "[]"
        val nextId = sharedPref.getInt("nextId", 0)
        return try {
            Pair(Json.decodeFromString(json), nextId)
        } catch (e: Exception) {
            Pair(emptyList(), 0)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val (savedTasks, savedNextId) = loadTasks()
            val taskList = remember { mutableStateListOf<Task>().apply { addAll(savedTasks) } }
            var newTask: String by remember { mutableStateOf("") }
            var nextId by remember { mutableStateOf(savedNextId) }


            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),

                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "ToDo List",
                    color = Color.White,
                    fontSize = 32.sp,
                    fontFamily = FontFamily.SansSerif,
                    modifier = Modifier.padding(top = 40.dp)
                )
                Spacer(modifier = Modifier.height(20.dp))
                OutlinedTextField(
                    value = newTask,
                    onValueChange = { newTask = it },
                    label = { Text(text = "Enter a task") },
                    modifier = Modifier.padding(horizontal = 16.dp)

                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        if (newTask.isNotEmpty()) {
                            taskList.add(Task(nextId, newTask, false))
                            nextId++
                            newTask = ""
                            saveTasks(taskList, nextId)
                        }
                    },
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Text(text = "Add Task")
                }
                Spacer(modifier = Modifier.height(20.dp))

                // Display the list of tasks
                val incompleteTasks = taskList.filter { !it.isCompleted }
                val completedTasks = taskList.filter { it.isCompleted }

                // Drag state
                var draggedTaskId by remember { mutableStateOf<Int?>(null) }
                var dragOffsetY by remember { mutableFloatStateOf(0f) }
                var draggedItemInitialY by remember { mutableFloatStateOf(0f) }
                val itemPositions = remember { mutableMapOf<Int, Float>() }
                val itemHeight = 56.dp
                val itemHeightPx = with(LocalDensity.current) { itemHeight.toPx() }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    // Incomplete tasks (above the line) with drag-and-drop
                    itemsIndexed(incompleteTasks, key = { _, task -> task.id }) { index, task ->
                        val isDragging = draggedTaskId == task.id

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(itemHeight)
                                .onGloballyPositioned { coordinates ->
                                    itemPositions[task.id] = coordinates.positionInParent().y
                                }
                                .zIndex(if (isDragging) 1f else 0f)
                                .graphicsLayer {
                                    if (isDragging) {
                                        translationY = dragOffsetY
                                        shadowElevation = 8f
                                    } else if (draggedTaskId != null) {
                                        // Animate other items when something is being dragged
                                        val draggedIndex = incompleteTasks.indexOfFirst { it.id == draggedTaskId }
                                        if (draggedIndex != -1) {
                                            val draggedCurrentY = draggedItemInitialY + dragOffsetY
                                            val myY = itemPositions[task.id] ?: 0f

                                            // Calculate if we should shift
                                            if (index < draggedIndex && draggedCurrentY < myY + itemHeightPx / 2) {
                                                translationY = itemHeightPx
                                            } else if (index > draggedIndex && draggedCurrentY > myY - itemHeightPx / 2) {
                                                translationY = -itemHeightPx
                                            }
                                        }
                                    }
                                }
                                .background(if (isDragging) Color.DarkGray else Color.Black),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = task.isCompleted,
                                    onCheckedChange = { isChecked ->
                                        val currentIndex = taskList.indexOfFirst { it.id == task.id }
                                        if (currentIndex != -1) {
                                            val updatedTask = task.copy(isCompleted = isChecked)
                                            taskList.removeAt(currentIndex)
                                            if (isChecked) {
                                                val firstCompletedIndex = taskList.indexOfFirst { it.isCompleted }
                                                if (firstCompletedIndex == -1) {
                                                    taskList.add(updatedTask)
                                                } else {
                                                    taskList.add(firstCompletedIndex, updatedTask)
                                                }
                                            } else {
                                                taskList.add(0, updatedTask)
                                            }
                                            saveTasks(taskList, nextId)
                                        }
                                    }
                                )

                                // Drag handle
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 8.dp)
                                        .pointerInput(task.id) {
                                            detectDragGesturesAfterLongPress(
                                                onDragStart = {
                                                    draggedTaskId = task.id
                                                    draggedItemInitialY = itemPositions[task.id] ?: 0f
                                                    dragOffsetY = 0f
                                                },
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    dragOffsetY += dragAmount.y
                                                },
                                                onDragEnd = {
                                                    // Calculate new position
                                                    val draggedIdx = incompleteTasks.indexOfFirst { it.id == draggedTaskId }
                                                    if (draggedIdx != -1) {
                                                        val draggedCurrentY = draggedItemInitialY + dragOffsetY
                                                        var newIdx = draggedIdx

                                                        for (i in incompleteTasks.indices) {
                                                            if (i == draggedIdx) continue
                                                            val itemY = itemPositions[incompleteTasks[i].id] ?: continue

                                                            if (i < draggedIdx && draggedCurrentY < itemY + itemHeightPx / 2) {
                                                                newIdx = minOf(newIdx, i)
                                                            } else if (i > draggedIdx && draggedCurrentY > itemY - itemHeightPx / 2) {
                                                                newIdx = maxOf(newIdx, i)
                                                            }
                                                        }

                                                        if (newIdx != draggedIdx) {
                                                            val taskToMove = taskList.first { it.id == draggedTaskId }
                                                            val fromIdx = taskList.indexOfFirst { it.id == draggedTaskId }
                                                            taskList.removeAt(fromIdx)

                                                            // Find the actual position in taskList
                                                            val targetTask = incompleteTasks[newIdx]
                                                            var toIdx = taskList.indexOfFirst { it.id == targetTask.id }
                                                            if (newIdx > draggedIdx) toIdx++
                                                            taskList.add(toIdx.coerceIn(0, taskList.size), taskToMove)
                                                            saveTasks(taskList, nextId)
                                                        }
                                                    }
                                                    draggedTaskId = null
                                                    dragOffsetY = 0f
                                                },
                                                onDragCancel = {
                                                    draggedTaskId = null
                                                    dragOffsetY = 0f
                                                }
                                            )
                                        }
                                ) {
                                    DragHandleIcon(color = Color.Gray)
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Text(
                                    text = task.text,
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    modifier = Modifier.weight(1f)
                                )

                                IconButton(
                                    onClick = {
                                        val currentIndex = taskList.indexOfFirst { it.id == task.id }
                                        if (currentIndex != -1) {
                                            taskList.removeAt(currentIndex)
                                            saveTasks(taskList, nextId)
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Delete,
                                        contentDescription = "Delete task",
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                    }

                    // Horizontal line between incomplete and completed tasks
                    item {
                        Divider(
                            color = Color.Gray,
                            thickness = 1.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        )
                    }

                    // Completed tasks (below the line) - no drag handle
                    items(completedTasks, key = { it.id }) { task ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(itemHeight),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = task.isCompleted,
                                    onCheckedChange = { isChecked ->
                                        val currentIndex = taskList.indexOfFirst { it.id == task.id }
                                        if (currentIndex != -1) {
                                            val updatedTask = task.copy(isCompleted = isChecked)
                                            taskList.removeAt(currentIndex)
                                            if (isChecked) {
                                                val firstCompletedIndex = taskList.indexOfFirst { it.isCompleted }
                                                if (firstCompletedIndex == -1) {
                                                    taskList.add(updatedTask)
                                                } else {
                                                    taskList.add(firstCompletedIndex, updatedTask)
                                                }
                                            } else {
                                                taskList.add(0, updatedTask)
                                            }
                                            saveTasks(taskList, nextId)
                                        }
                                    }
                                )

                                // Spacer to align with incomplete tasks (no drag handle for completed)
                                Spacer(modifier = Modifier.width(40.dp))

                                Text(
                                    text = task.text,
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    textDecoration = TextDecoration.LineThrough,
                                    modifier = Modifier.weight(1f)
                                )

                                IconButton(
                                    onClick = {
                                        val currentIndex = taskList.indexOfFirst { it.id == task.id }
                                        if (currentIndex != -1) {
                                            taskList.removeAt(currentIndex)
                                            saveTasks(taskList, nextId)
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Delete,
                                        contentDescription = "Delete task",
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

    }
}
