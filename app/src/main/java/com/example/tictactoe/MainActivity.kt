package com.example.tictactoe

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.tictactoe.databinding.ActivityMainBinding
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlin.random.Random
import kotlin.random.nextInt
import android.view.inputmethod.InputMethodManager
import android.content.Context

class MainActivity : AppCompatActivity() {

    lateinit var binding : ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.playerNameInput.setText(
            getPreferences(Context.MODE_PRIVATE).getString("player_name", "")
        )

        binding.playOfflineBtn.setOnClickListener {
            createOfflineGame()
        }

        binding.createOnlineBtn.setOnClickListener{
            createOnlineGame()

        }
        binding.joinOnlineBtn.setOnClickListener {
            joinOnlineGame()
        }
    }
    fun createOfflineGame(){
        savePlayerName()
        GameData.saveGameModel(
            GameModel(
                gameStatus = GameStatus.JOINED,
                playerXName = playerName(),
                playerOName = "Player 2"
            )
        )
        startGame()
    }
    fun createOnlineGame(){
        savePlayerName()
        GameData.myID = "X"
        GameData.saveGameModel(
            GameModel(
                gameStatus = GameStatus.CREATED,
                gameId = Random.nextInt(1000..9999).toString(),
                playerXName = playerName()

            )
        )
        startGame()
    }
    fun joinOnlineGame(){
        savePlayerName()
        var gameId = binding.getIdInput.text.toString()
        if(gameId.isEmpty()){
            binding.getIdInput.error = "Enter Game Id"
            return
        }
        GameData.myID = "O"
        Firebase.firestore.collection("games")
            .document(gameId)
            .get()
            .addOnSuccessListener {
                var model = it?.toObject(GameModel::class.java)
                if(model==null){
                    binding.getIdInput.setError("Please enter a valid game id")

                }else{
                    model.gameStatus = GameStatus.JOINED
                    model.playerOName = playerName()
                    GameData.saveGameModel(model)
                    startGame()
                }
            }

    }

    fun startGame(){
        startActivity(Intent(this,GameActivity::class.java))
    }

    private fun playerName(): String {
        val value = binding.playerNameInput.text.toString().trim()
        return if (value.isEmpty()) "Guest" else value.take(18)
    }

    private fun savePlayerName() {
        getPreferences(Context.MODE_PRIVATE).edit()
            .putString("player_name", playerName())
            .apply()
        (getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager)
            ?.hideSoftInputFromWindow(binding.playerNameInput.windowToken, 0)
    }

}