package com.example.fido2.repository

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FIDORepository @Inject constructor() {
    var usingFido2: Boolean = false

    fun userSelectedFido2(){
        usingFido2 = true
    }

    fun userSelectedUAF(){
        usingFido2 = false
    }

    fun getFidoType():Boolean {
        return usingFido2
    }
}