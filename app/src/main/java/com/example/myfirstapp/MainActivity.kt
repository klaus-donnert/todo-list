package com.example.myfirstapp // Make sure this matches your folder structure!

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Check
import kotlinx.coroutines.delay
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
            var showAddTaskInput by remember { mutableStateOf(false) }
            var editingTaskId by remember { mutableStateOf<Int?>(null) }

            // Animation state: tracks tasks that are animating out (being checked/unchecked)
            // Maps task ID to the new isCompleted state it's transitioning to
            val animatingTasks = remember { mutableStateListOf<Pair<Int, Boolean>>() }

            // Helper function to handle checkbox change with animation
            fun handleCheckboxChange(task: Task, isChecked: Boolean) {
                // Start the animation
                animatingTasks.add(Pair(task.id, isChecked))
            }

            // Process animations - when animation completes, update the task list
            animatingTasks.toList().forEach { (taskId, newIsCompleted) ->
                LaunchedEffect(taskId) {
                    delay(300) // Wait for animation to complete
                    val currentIndex = taskList.indexOfFirst { it.id == taskId }
                    if (currentIndex != -1) {
                        val task = taskList[currentIndex]
                        val updatedTask = task.copy(isCompleted = newIsCompleted)
                        taskList.removeAt(currentIndex)
                        if (newIsCompleted) {
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
                    animatingTasks.removeAll { it.first == taskId }
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "Task List",
                        color = Color.White,
                        fontSize = 32.sp,
                        fontFamily = FontFamily.SansSerif,
                        modifier = Modifier.padding(top = 40.dp)
                    )
                    Spacer(modifier = Modifier.height(15.dp))

                    if (showAddTaskInput) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = newTask,
                                onValueChange = { newTask = it },
                                label = { Text(text = "Enter a task", color = Color.Gray) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.Gray,
                                    cursorColor = Color.White,
                                    focusedBorderColor = Color.White,
                                    unfocusedBorderColor = Color.Gray
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(onClick = { showAddTaskInput = false }) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Close input",
                                    tint = Color.White
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                if (newTask.isNotEmpty()) {
                                    taskList.add(Task(nextId, newTask, false))
                                    nextId++
                                    newTask = ""
                                    saveTasks(taskList, nextId)
                                    showAddTaskInput = false // Hide input after adding task
                                }
                            },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            Text(text = "Add Task")
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                    }


                    // Display the list of tasks
                    val incompleteTasks = taskList.filter { !it.isCompleted }
                    val completedTasks = taskList.filter { it.isCompleted }

                    // Drag state
                    var draggedTaskId by remember { mutableStateOf<Int?>(null) }
                    var dragOffsetY by remember { mutableFloatStateOf(0f) }
                    var dragStartIndex by remember { mutableIntStateOf(-1) }
                    var currentHoverIndex by remember { mutableIntStateOf(-1) }
                    val itemHeight = 56.dp
                    val itemHeightPx = with(LocalDensity.current) { itemHeight.toPx() }

                    // Create a version counter that changes when the list is reordered
                    // This forces pointerInput to be recreated with fresh state
                    val listVersion = remember(incompleteTasks.map { it.id }.hashCode()) { System.currentTimeMillis() }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        // Incomplete tasks (above the line) with drag-and-drop
                        itemsIndexed(incompleteTasks, key = { _, task -> task.id }) { index, task ->
                            val isDragging = draggedTaskId == task.id
                            val isAnimatingOut = animatingTasks.any { it.first == task.id }

                            // Capture current values for use in pointerInput
                            val currentIncompleteTasksSize = incompleteTasks.size

                            // Calculate visual offset for non-dragged items using animateFloatAsState for smooth transitions
                            val targetOffset = if (!isDragging && draggedTaskId != null && dragStartIndex >= 0 && currentHoverIndex >= 0) {
                                when {
                                    // Dragging down: items between start and hover move up
                                    dragStartIndex < currentHoverIndex && index > dragStartIndex && index <= currentHoverIndex -> -itemHeightPx
                                    // Dragging up: items between hover and start move down
                                    dragStartIndex > currentHoverIndex && index >= currentHoverIndex && index < dragStartIndex -> itemHeightPx
                                    else -> 0f
                                }
                            } else 0f

                            val animatedOffset by animateFloatAsState(
                                targetValue = targetOffset,
                                animationSpec = tween(150),
                                label = "offsetAnimation"
                            )

                            // Only use AnimatedVisibility for checkbox animations, not during drag
                            if (isAnimatingOut) {
                                androidx.compose.animation.AnimatedVisibility(
                                    visible = false,
                                    exit = slideOutVertically(
                                        animationSpec = tween(300),
                                        targetOffsetY = { fullHeight -> fullHeight }
                                    ) + fadeOut(animationSpec = tween(300))
                                ) {
                                    Box(modifier = Modifier.fillMaxWidth().height(itemHeight))
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .zIndex(if (isDragging) 100f else 0f)
                                        .fillMaxWidth()
                                        .height(itemHeight)
                                        .graphicsLayer {
                                            if (isDragging) {
                                                translationY = dragOffsetY
                                                shadowElevation = 16f
                                            } else {
                                                translationY = animatedOffset
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
                                                handleCheckboxChange(task, isChecked)
                                            }
                                        )

                                        // Drag handle
                                        Box(
                                            modifier = Modifier
                                                .padding(horizontal = 8.dp)
                                                .pointerInput(task.id, listVersion) {
                                                    detectDragGesturesAfterLongPress(
                                                        onDragStart = {
                                                            // Use the index parameter directly - it's current at drag start
                                                            draggedTaskId = task.id
                                                            dragStartIndex = index
                                                            currentHoverIndex = index
                                                            dragOffsetY = 0f
                                                        },
                                                        onDrag = { change, dragAmount ->
                                                            change.consume()
                                                            dragOffsetY += dragAmount.y
                                                            val rawNewIndex = dragStartIndex + (dragOffsetY / itemHeightPx).toInt()
                                                            currentHoverIndex = rawNewIndex.coerceIn(0, currentIncompleteTasksSize - 1)
                                                        },
                                                        onDragEnd = {
                                                            val draggedId = draggedTaskId
                                                            if (draggedId != null && dragStartIndex >= 0 && currentHoverIndex >= 0 && currentHoverIndex != dragStartIndex) {
                                                                // Get current incomplete tasks snapshot
                                                                val currentIncompleteTasks = taskList.filter { !it.isCompleted }

                                                                // Find the task being dragged
                                                                val taskToMove = taskList.firstOrNull { it.id == draggedId }
                                                                if (taskToMove != null && currentHoverIndex < currentIncompleteTasks.size) {
                                                                    // Remove from current position
                                                                    val fromIdx = taskList.indexOfFirst { it.id == draggedId }
                                                                    if (fromIdx >= 0) {
                                                                        taskList.removeAt(fromIdx)

                                                                        // Calculate new position in taskList
                                                                        // Get the task that will be at the target position after removal
                                                                        val remainingIncompleteTasks = taskList.filter { !it.isCompleted }
                                                                        val targetIdx = if (currentHoverIndex >= remainingIncompleteTasks.size) {
                                                                            // Insert at end of incomplete tasks
                                                                            val lastIncomplete = remainingIncompleteTasks.lastOrNull()
                                                                            if (lastIncomplete != null) {
                                                                                taskList.indexOfFirst { it.id == lastIncomplete.id } + 1
                                                                            } else {
                                                                                0
                                                                            }
                                                                        } else if (currentHoverIndex == 0) {
                                                                            // Insert at beginning
                                                                            0
                                                                        } else {
                                                                            // Insert at the position of the task currently at hover index
                                                                            val taskAtHover = remainingIncompleteTasks[currentHoverIndex]
                                                                            taskList.indexOfFirst { it.id == taskAtHover.id }
                                                                        }

                                                                        taskList.add(targetIdx.coerceIn(0, taskList.size), taskToMove)
                                                                        saveTasks(taskList, nextId)
                                                                    }
                                                                }
                                                            }
                                                            draggedTaskId = null
                                                            dragOffsetY = 0f
                                                            dragStartIndex = -1
                                                            currentHoverIndex = -1
                                                        },
                                                        onDragCancel = {
                                                            draggedTaskId = null
                                                            dragOffsetY = 0f
                                                            dragStartIndex = -1
                                                            currentHoverIndex = -1
                                                        }
                                                    )
                                                }
                                        ) {
                                            DragHandleIcon(color = Color.Gray)
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        if (editingTaskId == task.id) {
                                            var editedText by remember { mutableStateOf(task.text) }
                                            OutlinedTextField(
                                                value = editedText,
                                                onValueChange = { editedText = it },
                                                modifier = Modifier.weight(1f),
                                                singleLine = true,
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedTextColor = Color.White,
                                                    unfocusedTextColor = Color.Gray,
                                                    cursorColor = Color.White,
                                                    focusedBorderColor = Color.White,
                                                    unfocusedBorderColor = Color.Gray
                                                )
                                            )
                                            IconButton(onClick = {
                                                val idx = taskList.indexOfFirst { it.id == task.id }
                                                if (idx != -1) {
                                                    taskList[idx] = taskList[idx].copy(text = editedText)
                                                    saveTasks(taskList, nextId)
                                                    editingTaskId = null
                                                }
                                            }) {
                                                Icon(imageVector = Icons.Filled.Check, contentDescription = "Save edit", tint = Color.White)
                                            }
                                        } else {
                                            Text(
                                                text = task.text,
                                                color = Color.White,
                                                fontSize = 18.sp,
                                                modifier = Modifier.weight(1f)
                                            )

                                            IconButton(onClick = { editingTaskId = task.id }) {
                                                Icon(
                                                    imageVector = Icons.Filled.Edit,
                                                    contentDescription = "Edit task",
                                                    tint = Color.White
                                                )
                                            }
                                        }

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
                            } // end else block
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
                            val isAnimatingOut = animatingTasks.any { it.first == task.id }

                            AnimatedVisibility(
                                visible = !isAnimatingOut,
                                exit = slideOutVertically(
                                    animationSpec = tween(300),
                                    targetOffsetY = { fullHeight -> -fullHeight } // Slide up when unchecking
                                ) + fadeOut(animationSpec = tween(300))
                            ) {
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
                                                handleCheckboxChange(task, isChecked)
                                            }
                                        )

                                        // Spacer to align with incomplete tasks (no drag handle for completed)
                                        Spacer(modifier = Modifier.width(40.dp))

                                        if (editingTaskId == task.id) {
                                            var editedText by remember { mutableStateOf(task.text) }
                                            OutlinedTextField(
                                                value = editedText,
                                                onValueChange = { editedText = it },
                                                modifier = Modifier.weight(1f),
                                                singleLine = true,
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedTextColor = Color.White,
                                                    unfocusedTextColor = Color.Gray,
                                                    cursorColor = Color.White,
                                                    focusedBorderColor = Color.White,
                                                    unfocusedBorderColor = Color.Gray
                                                )
                                            )
                                            IconButton(onClick = {
                                                val index = taskList.indexOfFirst { it.id == task.id }
                                                if (index != -1) {
                                                    taskList[index] = taskList[index].copy(text = editedText)
                                                    saveTasks(taskList, nextId)
                                                    editingTaskId = null
                                                }
                                            }) {
                                                Icon(imageVector = Icons.Filled.Check, contentDescription = "Save edit", tint = Color.White)
                                            }
                                        } else {
                                            Text(
                                                text = task.text,
                                                color = Color.White,
                                                fontSize = 18.sp,
                                                textDecoration = TextDecoration.LineThrough,
                                                modifier = Modifier.weight(1f)
                                            )

                                            IconButton(onClick = { editingTaskId = task.id }) {
                                                Icon(
                                                    imageVector = Icons.Filled.Edit,
                                                    contentDescription = "Edit task",
                                                    tint = Color.White
                                                )
                                            }
                                        }

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

                // Plus button at TopEnd
                IconButton(
                    onClick = { showAddTaskInput = true },
                    modifier = Modifier.align(Alignment.TopEnd).padding(top = 55.dp, end = 60.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Add new task",
                        tint = Color.White
                    )
                }
            }
        }
    }
}
