package com.example.tictactoe

data class ChatMessage(
    val username: String = "",
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis()
)