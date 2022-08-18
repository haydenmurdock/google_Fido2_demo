package com.example.fido2.api

data class ChallengeString(var info: String) {
    init {
        println("challenge init: $info")
    }
}

data class UserString(var info: String){
    init {
        println("user init: $info")
    }
}