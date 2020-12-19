package com.example.mymemory.models

enum class BoardSize(val numCards: Int) {
    EASY( 8),
    MEDIUM(18),
    HARD(24);
//WIDHTH AND HEIGHT
    fun getWidth(): Int{
    //switch statement
    return when(this){
        EASY -> 2
        MEDIUM -> 3
        HARD -> 4
    }
}
    fun getHeight(): Int {
        return numCards / getWidth()

    }
    //pairs of cards
    fun getNumPairs(): Int{
        return numCards / 2;
    }
}