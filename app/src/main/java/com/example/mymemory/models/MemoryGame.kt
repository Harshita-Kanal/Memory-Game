package com.example.mymemory.models

import com.example.mymemory.utils.DEFAULT_ICONS

//maintain state of the game
class MemoryGame(private val boardSize: BoardSize){
    val cards: List <MemoryCard>
    val numPairsFound = 0
    init{
        val chosenImages = DEFAULT_ICONS.shuffled().take(boardSize.getNumPairs())
        val randomizedImages = (chosenImages + chosenImages ).shuffled()
        cards =  randomizedImages.map {MemoryCard(it)}
    }
}