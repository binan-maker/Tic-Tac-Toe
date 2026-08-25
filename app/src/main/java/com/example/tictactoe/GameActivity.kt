package com.example.tictactoe

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tictactoe.databinding.ActivityGameBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class GameActivity : AppCompatActivity(), View.OnClickListener {

    lateinit var binding: ActivityGameBinding
    private var gameModel: GameModel? = null
    private val chatMessages = mutableListOf<ChatMessage>()
    private lateinit var chatAdapter: ChatAdapter
    private var chatListenerStarted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGameBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btn0.setOnClickListener(this)
        binding.btn1.setOnClickListener(this)
        binding.btn2.setOnClickListener(this)
        binding.btn3.setOnClickListener(this)
        binding.btn4.setOnClickListener(this)
        binding.btn5.setOnClickListener(this)
        binding.btn6.setOnClickListener(this)
        binding.btn7.setOnClickListener(this)
        binding.btn8.setOnClickListener(this)

        binding.startGameBtn.setOnClickListener {
            startGame()
        }
        binding.rematchButton.setOnClickListener {
            rematch()
        }

        GameData.gameModel.observe(this) {
            gameModel = it
            setUI()
            if (!chatListenerStarted && it.gameId != "-1") {
                chatListenerStarted = true
                setupFirebaseListener()
            }
        }
        setupChat()
    }

    //New
    private fun setupChat() {
        chatAdapter = ChatAdapter(chatMessages)
        binding.chatRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.chatRecyclerView.adapter = chatAdapter

        binding.sendButton.setOnClickListener {
            val message = binding.messageInput.text.toString().trim()
            if (message.isNotEmpty()) {
                sendMessage(message)
                binding.messageInput.text.clear()
            }
        }
    }

    private fun sendMessage(message: String) {
        val username = if (GameData.myID == "X") "Player X" else "Player O"
        val chatMessage = ChatMessage(username = username, message = message)
        chatMessages.add(chatMessage)
        chatAdapter.notifyDataSetChanged()
        binding.chatRecyclerView.smoothScrollToPosition(chatMessages.size - 1)
        pushChatMessage(chatMessage)
    }
    private fun pushChatMessage(chatMessage: ChatMessage) {
        val gameId = gameModel?.gameId ?: return
        val chatRef = FirebaseDatabase.getInstance().getReference("tictactoe/games/$gameId/chats")
        chatRef.push().setValue(chatMessage)
    }

    private fun setupFirebaseListener() {
        val gameId = gameModel?.gameId ?: return
        val chatRef = FirebaseDatabase.getInstance().getReference("tictactoe/games/$gameId/chats")

        chatRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d("FirebaseListener", "Data changed in Firebase. Total children: ${snapshot.childrenCount}")

                chatMessages.clear()
                for (data in snapshot.children) {
                    val chatMessage = data.getValue(ChatMessage::class.java)
                    if (chatMessage != null) {
                        chatMessages.add(chatMessage)
                        Log.d("FirebaseListener", "Received chat message: ${chatMessage.message} from ${chatMessage.username}")
                    }
                }
                chatAdapter.notifyDataSetChanged()
                binding.chatRecyclerView.smoothScrollToPosition(chatMessages.size - 1)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseListener", "Failed to read chat messages", error.toException())
            }
        })
    }

    //Old

    private fun setUI() {
        gameModel?.apply {
            val boardButtons = listOf(
                binding.btn0, binding.btn1, binding.btn2,
                binding.btn3, binding.btn4, binding.btn5,
                binding.btn6, binding.btn7, binding.btn8
            )
            boardButtons.forEachIndexed { index, button ->
                button.text = filledPos[index]
                button.background = getDrawable(
                    if (isWinningCell(index)) R.drawable.bg_cell_winner else R.drawable.bg_cell
                )
                button.backgroundTintList = null
            }
            boardButtons.forEachIndexed { index, button ->
                button.setTextColor(
                    when (filledPos[index]) {
                        "X" -> getColor(R.color.x_color)
                        "O" -> getColor(R.color.o_color)
                        else -> getColor(R.color.text_subtle)
                    }
                )
            }

            binding.startGameBtn.visibility = View.VISIBLE
            binding.rematchButton.visibility = View.GONE
            val myName = if (GameData.myID == "X") playerXName else playerOName
            val opponentName = if (GameData.myID == "X") playerOName else playerXName
            binding.myPlayerName.text = if (myName.isBlank()) "You" else myName
            binding.opponentPlayerName.text = if (
                opponentName.isBlank() || opponentName == "Player O" || opponentName == "Player X"
            ) "Waiting..." else opponentName

            binding.gameStatusText.text = when (gameStatus) {
                GameStatus.CREATED -> {
                    binding.startGameBtn.visibility = View.INVISIBLE
                    "Game ID : $gameId"
                }
                GameStatus.JOINED -> {
                    "Click on start game"
                }
                GameStatus.INPROGRESS -> {
                    binding.startGameBtn.visibility = View.INVISIBLE
                    when (GameData.myID) {
                        currentPlayer -> "Your turn"
                        else -> "$currentPlayer's turn"
                    }
                }
                GameStatus.FINISHED -> {
                    binding.rematchButton.visibility = View.VISIBLE
                    if (winner.isNotEmpty()) {
                        when (GameData.myID) {
                            winner -> "You won · +25 rating"
                            else -> "$winner won"
                        }
                    } else "Draw game · play again?"
                }
            }
        }
    }

    fun startGame() {
        gameModel?.apply {
            updateGameData(
                GameModel(
                    gameId = gameId,
                    gameStatus = GameStatus.INPROGRESS,
                    playerXName = playerXName,
                    playerOName = playerOName
                )
            )
        }
    }

    private fun rematch() {
        gameModel?.apply {
            winner = ""
            filledPos = MutableList(9) { "" }
            gameStatus = GameStatus.INPROGRESS
            currentPlayer = "X"
            updateGameData(this)
        }
    }

    private fun isWinningCell(index: Int): Boolean {
        val model = gameModel ?: return false
        if (model.winner.isEmpty()) return false
        val winningLines = listOf(
            listOf(0, 1, 2), listOf(3, 4, 5), listOf(6, 7, 8),
            listOf(0, 3, 6), listOf(1, 4, 7), listOf(2, 5, 8),
            listOf(0, 4, 8), listOf(2, 4, 6)
        )
        return winningLines.any { line ->
            index in line && line.all { model.filledPos[it] == model.winner }
        }
    }

    fun updateGameData(model: GameModel) {
        GameData.saveGameModel(model)
    }

    fun checkForWinner() {
        val winningPos = arrayOf(
            intArrayOf(0, 1, 2),
            intArrayOf(3, 4, 5),
            intArrayOf(6, 7, 8),
            intArrayOf(0, 3, 6),
            intArrayOf(1, 4, 7),
            intArrayOf(2, 5, 8),
            intArrayOf(0, 4, 8),
            intArrayOf(2, 4, 6)
        )

        gameModel?.apply {
            for (i in winningPos) {
                // Check winning conditions
                if (
                    filledPos[i[0]] == filledPos[i[1]] &&
                    filledPos[i[1]] == filledPos[i[2]] &&
                    filledPos[i[0]].isNotEmpty()
                ) {
                    gameStatus = GameStatus.FINISHED
                    winner = filledPos[i[0]]
                }
            }

            if (filledPos.none { it.isEmpty() }) {
                gameStatus = GameStatus.FINISHED
            }

            updateGameData(this)
        }
    }

    override fun onClick(v: View?) {
        gameModel?.apply {
            if (gameStatus != GameStatus.INPROGRESS) {
                Toast.makeText(applicationContext, "Game not started", Toast.LENGTH_SHORT).show()
                return
            }
            // Game is in progress
            val clickedPos = (v?.tag as String).toInt()
            if (filledPos[clickedPos].isEmpty()) {
                filledPos[clickedPos] = currentPlayer
                currentPlayer = if (currentPlayer == "X") "O" else "X"
                checkForWinner()
                updateGameData(this)
            }
        }
    }
}