package com.example.myfirstapp // Make sure this matches your folder structure!

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Label
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

@Serializable
data class Task(val id: Int, val text: String, val isCompleted: Boolean)

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

	                LazyColumn(
	                    modifier = Modifier
	                        .fillMaxWidth()
	                        .padding(horizontal = 16.dp)
	                ) {
	                    // Incomplete tasks (above the line)
	                    items(incompleteTasks, key = { it.id }) { task ->
	                        Box(
	                            modifier = Modifier
	                                .fillMaxWidth()
	                                .padding(vertical = 8.dp),
	                            contentAlignment = Alignment.CenterStart
	                        ) {
	                            Checkbox(
	                                checked = task.isCompleted,
	                                onCheckedChange = { isChecked ->
	                                    val currentIndex = taskList.indexOfFirst { it.id == task.id }
	                                    if (currentIndex != -1) {
	                                        val updatedTask = task.copy(isCompleted = isChecked)
	                                        taskList.removeAt(currentIndex)
	                                        if (isChecked) {
	                                            // Move to top of completed section (below the line)
	                                            val firstCompletedIndex = taskList.indexOfFirst { it.isCompleted }
	                                            if (firstCompletedIndex == -1) {
	                                                taskList.add(updatedTask)
	                                            } else {
	                                                taskList.add(firstCompletedIndex, updatedTask)
	                                            }
	                                        } else {
	                                            // Move to top of incomplete section (above the line)
	                                            taskList.add(0, updatedTask)
	                                        }
	                                        saveTasks(taskList, nextId)
	                                    }
	                                },
	                                modifier = Modifier.padding(end = 8.dp)
	                            )
	                            Text(
	                                text = task.text,
	                                color = Color.White,
	                                fontSize = 18.sp,
	                                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
	                                modifier = Modifier.padding(start = 48.dp)
	                            )
	                            IconButton(
	                                onClick = {
	                                    val currentIndex = taskList.indexOfFirst { it.id == task.id }
	                                    if (currentIndex != -1) {
	                                        taskList.removeAt(currentIndex)
	                                        saveTasks(taskList, nextId)
	                                    }
	                                },
	                                modifier = Modifier.align(Alignment.CenterEnd)
	                            ) {
	                                Icon(
	                                    imageVector = Icons.Filled.Delete,
	                                    contentDescription = "Delete task",
	                                    tint = Color.White
	                                )
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

	                    // Completed tasks (below the line)
	                    items(completedTasks, key = { it.id }) { task ->
	                        Box(
	                            modifier = Modifier
	                                .fillMaxWidth()
	                                .padding(vertical = 8.dp),
	                            contentAlignment = Alignment.CenterStart
	                        ) {
	                            Checkbox(
	                                checked = task.isCompleted,
	                                onCheckedChange = { isChecked ->
	                                    val currentIndex = taskList.indexOfFirst { it.id == task.id }
	                                    if (currentIndex != -1) {
	                                        val updatedTask = task.copy(isCompleted = isChecked)
	                                        taskList.removeAt(currentIndex)
	                                        if (isChecked) {
	                                            // Move to top of completed section (below the line)
	                                            val firstCompletedIndex = taskList.indexOfFirst { it.isCompleted }
	                                            if (firstCompletedIndex == -1) {
	                                                taskList.add(updatedTask)
	                                            } else {
	                                                taskList.add(firstCompletedIndex, updatedTask)
	                                            }
	                                        } else {
	                                            // Move to top of incomplete section (above the line)
	                                            taskList.add(0, updatedTask)
	                                        }
	                                        saveTasks(taskList, nextId)
	                                    }
	                                },
	                                modifier = Modifier.padding(end = 8.dp)
	                            )
	                            Text(
	                                text = task.text,
	                                color = Color.White,
	                                fontSize = 18.sp,
	                                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
	                                modifier = Modifier.padding(start = 48.dp)
	                            )
	                            IconButton(
	                                onClick = {
	                                    val currentIndex = taskList.indexOfFirst { it.id == task.id }
	                                    if (currentIndex != -1) {
	                                        taskList.removeAt(currentIndex)
	                                        saveTasks(taskList, nextId)
	                                    }
	                                },
	                                modifier = Modifier.align(Alignment.CenterEnd)
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
