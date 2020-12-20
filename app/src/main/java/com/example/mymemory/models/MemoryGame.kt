package com.example.mymemory.models

import com.example.mymemory.utils.DEFAULT_ICONS

//maintain state of the game
class MemoryGame(private val boardSize: BoardSize){
    val cards: List <MemoryCard>
    var numPairsFound = 0
    private var numCardFlips = 0
    private var indexOfSingleSelectedCard: Int? = null
    init{
        val chosenImages = DEFAULT_ICONS.shuffled().take(boardSize.getNumPairs())
        val randomizedImages = (chosenImages + chosenImages ).shuffled()
        cards =  randomizedImages.map {MemoryCard(it)}
    }


    /////

    fun flipCard(position: Int): Boolean {
        numCardFlips++
        val card: MemoryCard = cards[position]
        //three cases
        //0 cards previously flipped over => flip over and restore cards
        //1 card previously flipped over => flip over and check for match
        //2cards previously flipped => restore cards, flip over the selected card
        var foundMatch = false
        if(indexOfSingleSelectedCard == null){
            //restore the card
            restoreCards()
            indexOfSingleSelectedCard = position
        }else{
            //exactly 1 flipped over, not null !!
           foundMatch =  checkForMatch(indexOfSingleSelectedCard!!, position)
            indexOfSingleSelectedCard = null
        }
        card.isFaceUp = !card.isFaceUp
        return  foundMatch
    }


    ////
    private fun checkForMatch(position1: Int, position2: Int): Boolean {
      if(cards[position1].identifier != cards[position2].identifier){
          return false
      }
        cards[position1].isMatched = true
        cards[position2].isMatched = true
        numPairsFound++
        return true
    }

   ////
    private fun restoreCards() {
        for (card : MemoryCard in cards){
            if(!card.isMatched){
                card.isFaceUp = false
            }
        }
    }

    fun haveWonGame(): Boolean {
        return numPairsFound == boardSize.getNumPairs()
    }

    fun isCardFaceUp(position: Int): Boolean {
        return  cards[position].isFaceUp
    }

    fun getNumMoves(): Int {
        //as soon as i flip, my turn is over only if we flip two cards
        return  numCardFlips / 2
    }
    ////
}