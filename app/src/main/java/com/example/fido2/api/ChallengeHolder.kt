package com.example.fido2.api

object ChallengeHolder {

    private var challengeString: String? = null
    private var userString: String? = null

    fun setChallenge(challenge: String) {
        println("Set Challenge: $challenge")
        challengeString = ChallengeString(challenge).info
    }

    fun findChallengeString():String? {
        return challengeString
    }
    fun setUserString(user: String){
        userString = UserString(user).info
    }

    fun findUserString():String? {
        return userString
    }
}