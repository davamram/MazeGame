package com.example.mazegame

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.mazegame.ui.theme.MazeGameTheme
import kotlin.random.Random
import kotlinx.coroutines.delay

data class Player(var x: Int, var y: Int)

data class Cell(var isWall: Boolean = false, var isExit: Boolean = false)

class Maze(val width: Int, val height: Int) {
    val cells: Array<Array<Cell>> = Array(width) { Array(height) { Cell() } }
    val entranceX: Int
    val entranceY: Int
    val exitX: Int
    val exitY: Int
    val pairExit: Pair<Int, Int>
    val destroyedWalls: MutableList<Pair<Int, Int>> = mutableListOf() // Liste des murs détruits

    init {
        entranceX = 1
        entranceY = 1
        pairExit = generateExit()
        exitX = pairExit.first
        exitY = pairExit.second

        generate()
    }

    fun generate() {
        // Initialize the maze with walls
        destroyedWalls.clear()
        for (x in 0 until width) {
            for (y in 0 until height) {
                cells[x][y].isWall = true
            }
        }

        // Carve paths inside the maze
        carveMaze()

        // Set entrance and exit cells
        cells[entranceX][entranceY].isWall = false
        cells[exitX][exitY].isWall = false
        cells[exitX][exitY].isExit = true
    }

    private fun carveMaze() {
        val stack = mutableListOf<Pair<Int, Int>>()
        val visited = Array(width) { BooleanArray(height) { false } }

        // Start carving from an initial position
        val startX = width/2 - 1
        val startY = height/2 - 1
        stack.add(Pair(startX, startY))
        visited[startX][startY] = true
        cells[startX][startY].isWall = false
        destroyedWalls.add(Pair(startX, startY))

        while (stack.isNotEmpty()) {
            val (x, y) = stack.last()
            val unvisitedNeighbors = listOf(
                Pair(x - 2, y),
                Pair(x + 2, y),
                Pair(x, y - 2),
                Pair(x, y + 2)
            ).filter { (nx, ny) ->
                nx in 1 until width - 1 && ny in 1 until height - 1 && !visited[nx][ny]
            }

            if (unvisitedNeighbors.isNotEmpty()) {
                val (nextX, nextY) = unvisitedNeighbors.random()
                val wallX = (x + nextX) / 2
                val wallY = (y + nextY) / 2
                cells[wallX][wallY].isWall = false
                destroyedWalls.add(Pair(wallX, wallY)) // Ajouter le mur détruit à la liste
                cells[nextX][nextY].isWall = false
                destroyedWalls.add(Pair(nextX, nextY))
                visited[nextX][nextY] = true
                stack.add(Pair(nextX, nextY))
            } else {
                stack.removeAt(stack.size - 1)
            }
        }

        // Remove walls on the external boundaries
        for (y in 0 until height) {
            cells[width - 1][y].isWall = false // Right boundary
        }
        for (x in 0 until width) {
            cells[x][height - 1].isWall = false // Bottom boundary
        }
    }

    private fun generateExit(): Pair<Int, Int> {
            return when (Random.nextInt(4)) {
                0 -> Pair(0, Random.nextInt(1, height - 2)) // Exit on the left
                1 -> Pair(width-2, Random.nextInt(1, height - 2)) // Exit on the right
                2 -> Pair(Random.nextInt(1, width - 2), 0) // Exit on the top
                else -> Pair(Random.nextInt(1, width - 2), height - 2) // Exit on the bottom
            }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MazeGameTheme {
                GameScreen()
            }
        }
    }
}

@Composable
fun GameScreen() {
    var maze by remember { mutableStateOf(Maze(20, 20)) }
    val player = remember { mutableStateOf(Player(maze.entranceX, maze.entranceY)) }
    var message by remember { mutableStateOf("") }
    var isMazeVisible by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Maze Game",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(8.dp)
        )

        if (isMazeVisible) {
            MazeView(maze = maze, player = player.value)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            maze = Maze(20, 20).apply {
                generate()
            }
            player.value = Player(maze.entranceX, maze.entranceY)
            message = "Maze regenerated"
            isMazeVisible = true
        }) {
            Text("Regenerate Maze")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            isMazeVisible = !isMazeVisible
            message = if (isMazeVisible) "Maze is visible" else "Maze is hidden"
        }) {
            Text(if (isMazeVisible) "Hide Maze" else "Show Maze")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Player Position: (${player.value.x}, ${player.value.y})",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Blue
        )

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Green
        )

        Spacer(modifier = Modifier.height(16.dp))

        ControlButtons(maze, player) { direction ->
            if (player.value.x == maze.exitX && player.value.y == maze.exitY) {
                message = "You've reached the exit!"
            } else {
                message = "Moved $direction"
            }
        }
    }
}

@Composable
fun MazeView(maze: Maze, player: Player) {
    var visibleWalls by remember { mutableStateOf<Set<Pair<Int, Int>>>(maze.cells.indices.flatMap { x ->
        maze.cells[0].indices.map { y -> Pair(x, y) }
    }.toSet()) }

    // Lancer une animation pour afficher la destruction des murs
    LaunchedEffect(maze.destroyedWalls) {
        visibleWalls = maze.cells.indices.flatMap { x ->
            maze.cells[0].indices.map { y -> Pair(x, y) }
        }.filter { (x, y) ->
            x < maze.width - 1 && y < maze.height - 1 // Exclure la dernière colonne et ligne
        }.toSet() // Initialiser tous les murs comme visibles

        for (wall in maze.destroyedWalls) {
            delay(50) // Délai de 0.05 seconde entre chaque mur
            visibleWalls = visibleWalls - wall // Retirer le mur de la liste des murs visibles
        }
    }

    BoxWithConstraints {
        val cellSize = minOf(maxWidth / maze.width, maxHeight / maze.height)
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            for (y in 0 until maze.height) {
                Row {
                    for (x in 0 until maze.width) {
                        Box(
                            modifier = Modifier
                                .size(cellSize)
                                .background(
                                    color = when {
                                        player.x == x && player.y == y -> Color.Red
                                        maze.cells[x][y].isExit -> Color.Green
                                        Pair(x, y) !in visibleWalls -> Color.White // Murs visibles en blanc
                                        else -> Color.Black // Autres murs en noir
                                    }
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ControlButtons(
    maze: Maze,
    player: MutableState<Player>,
    onDirectionChange: (String) -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Button(onClick = {
            if (player.value.y > 0 && !maze.cells[player.value.x][player.value.y - 1].isWall) {
                player.value = player.value.copy(y = player.value.y - 1)
                onDirectionChange("Up")
            }
        }) {
            Text("Up")
        }
        Row {
            Button(onClick = {
                if (player.value.x > 0 && !maze.cells[player.value.x - 1][player.value.y].isWall) {
                    player.value = player.value.copy(x = player.value.x - 1)
                    onDirectionChange("Left")
                }
            }) {
                Text("Left")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = {
                if (player.value.x < maze.width - 1 && !maze.cells[player.value.x + 1][player.value.y].isWall) {
                    player.value = player.value.copy(x = player.value.x + 1)
                    onDirectionChange("Right")
                }
            }) {
                Text("Right")
            }
        }
        Button(onClick = {
            if (player.value.y < maze.height - 1 && !maze.cells[player.value.x][player.value.y + 1].isWall) {
                player.value = player.value.copy(y = player.value.y + 1)
                onDirectionChange("Down")
            }
        }) {
            Text("Down")
        }
    }
}
