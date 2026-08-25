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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGameBinding.inflate(layoutInflater)
        setContentView(binding.root)
        GameData.fetchGameModel()

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

        GameData.gameModel.observe(this) {
            gameModel = it
            setUI()
        }
        setupChat()
        // New
        setupFirebaseListener()
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
            binding.btn0.text = filledPos[0]
            binding.btn1.text = filledPos[1]
            binding.btn2.text = filledPos[2]
            binding.btn3.text = filledPos[3]
            binding.btn4.text = filledPos[4]
            binding.btn5.text = filledPos[5]
            binding.btn6.text = filledPos[6]
            binding.btn7.text = filledPos[7]
            binding.btn8.text = filledPos[8]

            binding.startGameBtn.visibility = View.VISIBLE

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
                    if (winner.isNotEmpty()) {
                        when (GameData.myID) {
                            winner -> "You won"
                            else -> "$winner won"
                        }
                    } else "DRAW"
                }
            }
        }
    }

    fun startGame() {
        gameModel?.apply {
            updateGameData(
                GameModel(
                    gameId = gameId,
                    gameStatus = GameStatus.INPROGRESS
                )
            )
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