package com.example.mymemory

import android.animation.ArgbEvaluator
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mymemory.models.BoardSize
import com.example.mymemory.models.MemoryCard
import com.example.mymemory.models.MemoryGame
import com.example.mymemory.utils.DEFAULT_ICONS
import com.example.mymemory.utils.EXTRA_BOARD_SIZE
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {
    companion object{
        private const val TAG = "MainActivity"
        private const val CREATE_REQUEST_CODE = 248 //some integer
    }


    private lateinit var adapter: MemoryBoardAdapter
    private  lateinit var rvBoard: RecyclerView //lateinit is set in oncreate
    private lateinit var tvNumMoves: TextView
    private lateinit var  tvNumPairs: TextView
    private lateinit var clRoot: ConstraintLayout
    private lateinit var memoryGame: MemoryGame
    private var boardSize: BoardSize = BoardSize.EASY
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main) //resource file
        //reference widget here
        clRoot = findViewById(R.id.clRoot)
        rvBoard = findViewById(R.id.rvBoard)
        tvNumMoves = findViewById(R.id.tvNumMoves)
        tvNumPairs = findViewById(R.id.tvNumPairs)
        setUpBoard()

    }

    private fun setUpBoard() {
        when(boardSize){
            BoardSize.EASY->{
                tvNumMoves.text = "Easy: 4 x 2"
                tvNumPairs.text = "Pairs: 0 / 4"
            }
            BoardSize.MEDIUM->{
                tvNumMoves.text = "Medium: 6 x 3"
                tvNumPairs.text = "Pairs: 0 / 9"
            }
            BoardSize.HARD->{
                tvNumMoves.text = "Hard: 6 x 4"
                tvNumPairs.text = "Pairs: 0 / 12"
            }
        }
        tvNumPairs.setTextColor(ContextCompat.getColor(this, R.color.color_progress_none))
        memoryGame = MemoryGame(boardSize)


        //recyclerview: measures and positions the items views
        adapter =  MemoryBoardAdapter(this, boardSize, memoryGame.cards, object : MemoryBoardAdapter.CardClickListener{
            //toggling is face up
            override fun onCardClicked(position: Int) {
//                Log.i(TAG, "Card clicked $position")
                updateGameWithFlip(position)
            }

        })
        rvBoard.adapter = adapter
        rvBoard.setHasFixedSize(true);
        rvBoard.layoutManager = GridLayoutManager(this, boardSize.getWidth())

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.miRefresh -> {
                //setup new game
                //notify the user
                if(memoryGame.getNumMoves() > 0 && !memoryGame.haveWonGame()){
                    showAlertDialog("Quit the Current Game ?", null, View.OnClickListener {
                        setUpBoard()
                    })
                }
                else{
                    setUpBoard()
                }
                return true
            }
            R.id.mi_new_size -> {
                showNewSizeDialog()
                return true
            }
            R.id.mi_custom -> {
                showCreationDialog()
                return true
            }

        }
        return super.onOptionsItemSelected(item)
    }

    private fun showCreationDialog() {
        //show new size
        val boardSizeView =  LayoutInflater.from(this).inflate(R.layout.dialog_board_size, null)
        val radioGroupSize = boardSizeView.findViewById<RadioGroup>(R.id.radioGroup)
        showAlertDialog("Create your own memory board", boardSizeView, View.OnClickListener {
          val desiredBoardSize  = when(radioGroupSize.checkedRadioButtonId)  {
                R.id.rbEasy -> BoardSize.EASY
                R.id.rbMedium -> BoardSize.MEDIUM
                else -> BoardSize.HARD
            }
            //Navigate to a new activity choose game
            val intent  = Intent(this, CreateActivity::class.java )
            intent.putExtra(EXTRA_BOARD_SIZE, desiredBoardSize)
            startActivityForResult(intent, CREATE_REQUEST_CODE) //get a signal back
            //pass additional data
        })
    }

    private fun showNewSizeDialog() {
        val boardSizeView =  LayoutInflater.from(this).inflate(R.layout.dialog_board_size, null)
        val radioGroupSize = boardSizeView.findViewById<RadioGroup>(R.id.radioGroup)
        when(boardSize){
            BoardSize.EASY -> radioGroupSize.check(R.id.rbEasy)
            BoardSize.MEDIUM -> radioGroupSize.check(R.id.rbMedium)
            BoardSize.HARD -> radioGroupSize.check(R.id.rbHard)
        }
        showAlertDialog("Choose new size", boardSizeView, View.OnClickListener {
              boardSize = when(radioGroupSize.checkedRadioButtonId)  {
                 R.id.rbEasy -> BoardSize.EASY
                  R.id.rbMedium -> BoardSize.MEDIUM
                  else -> BoardSize.HARD
              }
            setUpBoard()
        })
    }

    private fun showAlertDialog(title: String, view: View?, positiveClickListener: View.OnClickListener) {
        AlertDialog.Builder(this)
                .setTitle(title)
                .setView(view)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Yes"){
                    _, _  ->
                    positiveClickListener.onClick(null)
                }.show()

    }

    private fun updateGameWithFlip(position: Int) {
        //attempting to flip
        //Error checking
        if(memoryGame.haveWonGame()){
            //alert the user for invalid move
            Snackbar.make(clRoot, "You already won! ✨", Snackbar.LENGTH_LONG).show()
            return
        }
        if(memoryGame.isCardFaceUp(position)){
            //alert for invalid move of the user
            Snackbar.make(clRoot, "This is an invalid move! ", Snackbar.LENGTH_LONG).show()
            return
        }
    //memeory game handles the state of game
      if(memoryGame.flipCard(position)){
          Log.i(TAG, "Found a match ${memoryGame.numPairsFound}")
          //color is a result is a product of interpolation
          val color = ArgbEvaluator().evaluate(
                  memoryGame.numPairsFound.toFloat() / boardSize.getNumPairs(),
                  ContextCompat.getColor(this, R.color.color_progress_none),
                  ContextCompat.getColor(this, R.color.color_progress_full)

          ) as Int
          tvNumPairs.setTextColor(color)
          tvNumPairs.text = "Pairs: ${memoryGame.numPairsFound} / ${boardSize.getNumPairs()}"
          if(memoryGame.haveWonGame()){
              Snackbar.make(clRoot, "You won! Congrats. ✨", Snackbar.LENGTH_LONG ).show()
          }
      }

        tvNumMoves.text = "Moves: ${memoryGame.getNumMoves()}"
        adapter.notifyDataSetChanged()

    }
}