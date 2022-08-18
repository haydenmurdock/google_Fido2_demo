package com.example.fido2.providers

import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialCreationOptions

data class UserNameResponse(val publicKeyCredentialCreationOptions: PublicKeyCredentialCreationOptions)