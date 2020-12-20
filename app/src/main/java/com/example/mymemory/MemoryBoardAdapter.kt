package com.example.mymemory
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.mymemory.models.BoardSize
import com.example.mymemory.models.MemoryCard
import kotlin.math.min

class MemoryBoardAdapter(
    private val context: Context,
    private val boardSize: BoardSize,
    private val cards: List<MemoryCard>,
    private val cardClickListener: CardClickListener
) :
        RecyclerView.Adapter<MemoryBoardAdapter.ViewHolder>() {

        //companion objects contain constants
        companion object{
            private const val MARGIN_SIZE = 10
            private const val TAG = "MemoryBoardAdapter"

        }

        interface  CardClickListener {
            fun onCardClicked(position: Int)
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        //LayoutInflater is a fundamental component in Android.
        // You must use it all the time to turn xml files
        // into view hierarchies
        val cardWidth = parent.width / boardSize.getWidth() - (2 * MARGIN_SIZE)
        val cardHeight = parent.height / boardSize.getHeight() - (2 * MARGIN_SIZE)
        val cardSideLength = min(cardWidth, cardHeight)
        val view =  LayoutInflater.from(context).inflate(R.layout.memory_card, parent, false)
        val layoutParams  =  view.findViewById<CardView>(R.id.cardView).layoutParams as ViewGroup.MarginLayoutParams
        layoutParams.width = cardSideLength
        layoutParams.height = cardSideLength
        layoutParams.setMargins(MARGIN_SIZE, MARGIN_SIZE, MARGIN_SIZE, MARGIN_SIZE)
        return ViewHolder(view)
    }

    override fun getItemCount() = boardSize.numCards //no of items in recycler view

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
       holder.bind(position)
    };
    //define our own view card
    inner  class  ViewHolder(itemView:View): RecyclerView.ViewHolder(itemView) {
        private  val imageButton =  itemView.findViewById<ImageButton>(R.id.imageButton)

        fun bind(position: Int) {
            //nothing here
            //set image on image button
            val memoryCard = cards[position]
            imageButton.setImageResource(if (cards[position].isFaceUp) cards[position].identifier else R.drawable.cover )
            imageButton.alpha = if(memoryCard.isMatched) .4f else 1.0f
            //grey out color if match
            val colorStateList =  if(memoryCard.isMatched)ContextCompat.getColorStateList(context, R.color.color_gray) else null
            //set a shading
            ViewCompat.setBackgroundTintList(imageButton, colorStateList)
            imageButton.setOnClickListener{
                //get notified by mainactivity for change to happen, define an interface for this
                cardClickListener.onCardClicked(position)
            }
        }
    }
}
