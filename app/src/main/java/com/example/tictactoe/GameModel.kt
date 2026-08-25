package com.example.tictactoe

import kotlin.random.Random

data class GameModel (
    var gameId : String = "-1",
    var filledPos : MutableList<String> = mutableListOf("","","","","","","","",""),
    var winner : String ="",
    var playerXName: String = "Player X",
    var playerOName: String = "Player O",
    var gameStatus : GameStatus = GameStatus.CREATED,
    var currentPlayer : String = (arrayOf("X","O"))[Random.nextInt(2)]
)


enum class GameStatus{
    CREATED,
    JOINED,
    INPROGRESS,
    FINISHED
}