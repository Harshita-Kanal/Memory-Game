package com.example.mymemory.models

data class MemoryCard(
    val identifier: Int, //image, once set value can be change
    var isFaceUp: Boolean = false,
    var isMatched: Boolean = false

)