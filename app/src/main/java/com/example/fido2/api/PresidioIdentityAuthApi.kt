package com.example.fido2.api

import android.content.Context
import android.util.*
import com.example.fido2.decodeBase64
import com.example.fido2.toBase64
import com.google.android.gms.fido.fido2.api.common.AuthenticatorAssertionResponse
import com.google.android.gms.fido.fido2.api.common.AuthenticatorAttestationResponse
import com.google.android.gms.fido.fido2.api.common.AuthenticatorSelectionCriteria
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredential
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialCreationOptions
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialDescriptor
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialParameters
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialRequestOptions
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialRpEntity
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialType
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialUserEntity
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.ResponseBody
import ru.gildor.coroutines.okhttp.await
import java.io.StringReader
import java.io.StringWriter
import javax.inject.Inject

class PresidioIdentityAuthApi  @Inject constructor(
private val client: OkHttpClient
) {
    companion object {
        private const val BASE_URL = "https://develop.presidioidentity.net/fido/fido2/"
        private val JSON = "application/json".toMediaTypeOrNull()
        private const val SessionIdKey = "connect.sid="
        private const val TAG = "AuthApi"
    }

    private fun JsonWriter.objectValue(body: JsonWriter.() -> Unit) {
        beginObject()
        body()
        endObject()
    }

    /**
     * API Call 1.
     * @param username The username to be used for sign-in.
     * @param displayName The username to be used for sign-in.
     * @param authenticatorSelection { @param requiresResidentKey, userVerification, and authenticatorAttachment }
     * @return user
     *         challenge
     *         pubKeyCredParams
     *         timeout
     *         attestation
     *         excludeCredentials
     *         authenticatorSelection
     *         rp
     *         extensions
     *         status
     *         errorMessage
     */
    suspend fun username(username: String): ApiResult<PublicKeyCredentialCreationOptions>? {
        val call = client.newCall(
            Request.Builder()
                .url("${BASE_URL}attestation/options")
                .method("POST", jsonRequestBody {
                    name("username").value(username)
                    name("displayName").value(username)
                    name("attestation").value("direct")
                    name("authenticatorSelection").objectValue {
                        name("requiresResidentKey").value(false)
                        name("userVerification").value("true")
                        name("authenticatorAttachment").value("true")
                    }
                })
                .build()
        )

        val response = call.await()

        return response.result("Error calling /registerRequest") {
            parsePublicKeyCredentialCreationOptions(
                body ?: throw ApiException("Empty response from /registerRequest")
            )
        }
    }

  //  {"status":"ok","errorMessage":"","rp":
    //  {"id":"https://develop.presidioidentity.net","name":"Presidio Identity"}
    //  ,"user":{"id":"Ik5qSm1NVGsyWldRNU9UWm1ZbUl3TURJMk9XRTJOVEUwIg==","name":"Hayden","displayName":"Hayden"}
    //  ,"challenge":"PWEE4LygpGRM8OZMYrrDhmPTU1N0jy6fNAgAveYwA","pubKeyCredParams":[{"type":"public-key","alg":-7}
    //  ,{"type":"public-key","alg":-257}],"timeout":1000000,"excludeCredentials"
    //  :[{"id":"noExcludeCredentials","type":"public-key"}],
    //  "authenticatorSelection":{"requiresResidentKey":true,"userVerification":"true","authenticatorAttachment":"false"}
    //  ,"attestation":"direct"}

    //    * Respond with required information to call navigator.credential.create()
//    * Input is passed via `req.body` with similar format as output
//    * Output format:
//    * ```{
//        rp: {
//                id: String,
//                name: String
//        },
//        user: {
//                displayName: String,
//                id: String,
//                name: String
//        },
//        publicKeyCredParams: [{  // @herrjemand
//            type: 'public-key', alg: -7
//        }],
//        timeout: Number,
//        challenge: String,
//        excludeCredentials: [{
//            id: String,
//            type: 'public-key',
//            transports: [('ble'|'nfc'|'usb'|'internal'), ...]
//        }, ...],
//        authenticatorSelection: {
//            authenticatorAttachment: ('platform'|'cross-platform'),
//            requireResidentKey: Boolean,
//            userVerification: ('required'|'preferred'|'discouraged')
//        },
//        attestation: ('none'|'indirect'|'direct')


    /**
     * API Call 2
     * @param Id The username to be used for sign-in.
     * @param type The username to be used for sign-in.
     * @param rawId
     * @param response { @param clientDataJson @param attestationObject }
     * @return status
     *         errorMessage
     */

    suspend fun registerResponse(
        sessionId: String,
        credential: PublicKeyCredential
    ): ApiResult<Unit> {
        val rawId = credential.rawId.toBase64()
        val response = credential.response as AuthenticatorAttestationResponse


        val call = client.newCall(
            Request.Builder()
                .url("${BASE_URL}attestation/result")
                .method("POST", jsonRequestBody {
                    name("id").value(rawId)
                    name("type").value(PublicKeyCredentialType.PUBLIC_KEY.toString())
                    name("rawId").value(rawId)
                    name("response").objectValue {
                        name("clientDataJson").value(
                            response.clientDataJSON.toBase64()
                        )
                        name("attestationObject").value(
                            response.attestationObject.toBase64()
                        )
                    }
                })
                .build()
        )
        val apiResponse = call.await()
        return apiResponse.result("Error calling /attestation/result") {
             parseSuccessResponse( body ?: throw ApiException("Empty response from /attestation/result"))
        }
    }
    /**
     * API Call 3
     * @param username
     * @param userVerification
     * @return userVerification
     *         challenge
     *         rpId
     *         timeout
     *         allowCredentials {@param id, @param type, @param transports
     *         status
     *         errorMessage
     *         extensions {@param example.extension: true }
     */
    suspend fun authenticateRegisteredUser(
        username: String,
        userVerification: String
    ): ApiResult<Unit> {
        val call = client.newCall(
            Request.Builder()
                .url("${BASE_URL}attestation/options")
                .method("POST", jsonRequestBody {
                    name("username").value(username)
                    name("userVerification").value(userVerification)
                })
                .build()
        )
        val apiResponse = call.await()
        return apiResponse.result("Error calling /attestation/result") {

            parseUserCredentials( body ?: throw ApiException("Empty response from /attestation/result"))
        }
    }

    /**
     * API Call 4
     * @param Id The username to be used for sign-in.
     * @param type The username to be used for sign-in.
     * @param rawId
     * @param response { @param clientDataJson @param attestationObject }
     * @return status
     *         errorMessage
     */

    suspend fun sendAuthKeyInfo(
        credential: PublicKeyCredential
    ): ApiResult<Unit> {
        val rawId = credential.rawId.toBase64()
        val response = credential.response as AuthenticatorAssertionResponse

        val call = client.newCall(
            Request.Builder()
                .url("${BASE_URL}attestation/result")
                .method("POST", jsonRequestBody {
                    name("id").value(rawId)
                    name("type").value(PublicKeyCredentialType.PUBLIC_KEY.toString())
                    name("rawId").value(rawId)
                    name("response").objectValue {
                        name("clientDataJson").value(
                            response.clientDataJSON.toBase64()
                        )
                        name("authenticatorData").value(
                            response.authenticatorData.toBase64()
                        )
                        name("signature").value(
                            response.signature.toBase64()
                        )
                        name("userHandle").value(
                            response.userHandle?.toBase64() ?: ""
                        )
                    }
                })
                .build()
        )
        val apiResponse = call.await()
        return apiResponse.result("Error calling /attestation/result") {
            parseSuccessResponse( body ?: throw ApiException("Empty response from /attestation/result"))
        }
    }

    private fun jsonRequestBody(body: JsonWriter.() -> Unit): RequestBody {
        val output = StringWriter()
        JsonWriter(output).use { writer ->
            writer.beginObject()
            writer.body()
            writer.endObject()
        }
        return output.toString().toRequestBody(JSON)
    }

    private fun parsePublicKeyCredentialRequestOptions(
        body: ResponseBody
    ): PublicKeyCredentialRequestOptions {
        val builder = PublicKeyCredentialRequestOptions.Builder()
        JsonReader(body.byteStream().bufferedReader()).use { reader ->
            reader.beginObject()
            while (reader.hasNext()) {
                when (reader.nextName()) {
                    "challenge" -> builder.setChallenge(reader.nextString().decodeBase64())
                    "userVerification" -> reader.skipValue()
                    "allowCredentials" -> builder.setAllowList(parseCredentialDescriptors(reader))
                    "rpId" -> builder.setRpId(reader.nextString())
                    "timeout" -> builder.setTimeoutSeconds(reader.nextDouble())
                    else -> reader.skipValue()
                }
            }
            reader.endObject()
        }
        return builder.build()
    }

    private fun parsePublicKeyCredentialCreationOptions(
        body: ResponseBody
    ): PublicKeyCredentialCreationOptions {
        val builder = PublicKeyCredentialCreationOptions.Builder()
        JsonReader(body.byteStream().bufferedReader()).use { reader ->
            reader.beginObject()
            //println(reader)
            while (reader.hasNext()) {
                when (reader.nextName()) {
                    "rp" -> builder.setRp(parseRp(reader))
                    "user" -> builder.setUser(parseUser(reader))
                    "challenge" -> {
                        var challenge = reader.nextString().toByteArray()
                        builder.setChallenge(challenge)
                        //ChallengeHolder.setChallenge(challenge.toString())
                    }
                    "pubKeyCredParams" -> builder.setParameters(parseParameters(reader))
                    "timeout" -> reader.skipValue()
                    "excludeCredentials" -> builder.setExcludeList(
                        parseCredentialDescriptors(reader)
                    )
                    "authenticatorSelection" -> builder.setAuthenticatorSelection(
                        parseSelection(reader)
                    )
                    "attestation" -> reader.skipValue() // Unused
                    "extensions" -> reader.skipValue() // Unused
                    "pKey" -> reader.skipValue() // Unused
                    "counter" -> reader.skipValue() // Unused
                    else -> reader.skipValue()
                }
            }
            reader.endObject()
        }
        val build = builder.build()
        println("${build.rp}${build.parameters}${build.parameters}${build.user}${build.challenge}$build")
        return builder.build()
    }

    private fun parseRp(reader: JsonReader): PublicKeyCredentialRpEntity {
        var id: String? = null
        var name: String? = null
        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "id" -> id = reader.nextString()
                "name" -> name = reader.nextString()
                else -> reader.skipValue()
            }

        }
        println("id: $id name: $name")
        reader.endObject()
        if (id == null) {
            return PublicKeyCredentialRpEntity("", "", null)
        } else {
            return PublicKeyCredentialRpEntity(id!!, name!!, /* icon */ null)
        }
    }

    private fun parseSelection(reader: JsonReader): AuthenticatorSelectionCriteria {
        val builder = AuthenticatorSelectionCriteria.Builder()
        var authenticatorAttachment: String? = null

        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "requiresResidentKey" -> builder.setRequireResidentKey(reader.nextBoolean())
                "authenticatorAttachment" -> authenticatorAttachment = reader.nextString()
//                    builder.setAttachment(
//                    Attachment.fromString(reader.nextString())
//                )
                "userVerification" -> reader.skipValue()
                else -> reader.skipValue()
            }
        }
        reader.endObject()
        return builder.build()
    }

    private fun parseCredentialDescriptors(
        reader: JsonReader
    ): List<PublicKeyCredentialDescriptor> {
        val list = mutableListOf<PublicKeyCredentialDescriptor>()
        reader.beginArray()
        while (reader.hasNext()) {
            var id: String? = null
            reader.beginObject()
            while (reader.hasNext()) {
                try {
                    when (reader.nextName()) {
                        "id" -> {
                            id = try {
                                reader.nextString()
                            } catch (e: IllegalStateException){
                                null
                            }
                        }
                        "type" -> reader.skipValue()
                        // "transports" -> reader.skipValue()
                        else -> reader.skipValue()
                    }
                } catch (e: IllegalStateException){

                }
            }
            reader.endObject()
            list.add(
                PublicKeyCredentialDescriptor(
                    PublicKeyCredentialType.PUBLIC_KEY.toString(),
                    id!!.decodeBase64(),
                    /* transports */ null
                )
            )
        }
        reader.endArray()
        return list
    }

    private fun parseUser(reader: JsonReader): PublicKeyCredentialUserEntity {
        reader.beginObject()
        var id: String? = null
        var name: String? = null
        var displayName = ""
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "user_id" -> {
                    id = reader.nextString()
                    ChallengeHolder.setUserString(id)
                }
                "username" -> name = reader.nextString()
                "displayName" -> displayName = reader.nextString()
                else -> reader.skipValue()
            }
        }
        reader.endObject()
        if(id == null){
            return PublicKeyCredentialUserEntity("".decodeBase64(), displayName, null, displayName)
        } else {
            return PublicKeyCredentialUserEntity(
                id.decodeBase64(),
                name!!,
                null, // icon
                displayName
            )
        }
    }

    private fun parseSuccessResponse(body: ResponseBody):String {
        JsonReader(body.byteStream().bufferedReader()).use { reader ->
        reader.beginObject()
        var status: String? = null
        var errorMessage = ""
        while (reader.hasNext()){
            when(reader.nextName()){
                "status"-> status = reader.nextString()
                "errorMessage" -> errorMessage = reader.nextString()
                else -> reader.skipValue()
            }
        }
        reader.beginObject()
        if (errorMessage.length >  1) {
            return  errorMessage
        }
        return status ?: ""
     }
    }
    private fun parseParameters(reader: JsonReader): List<PublicKeyCredentialParameters> {
        val parameters = mutableListOf<PublicKeyCredentialParameters>()
        reader.beginArray()
        while (reader.hasNext()) {
            reader.beginObject()
            var type: String? = null
            var alg = 0
            while (reader.hasNext()) {
                when (reader.nextName()) {
                    "type" -> type = reader.nextString()
                    "alg" -> alg = reader.nextInt()
                    else -> reader.skipValue()
                }
            }
            reader.endObject()
            parameters.add(PublicKeyCredentialParameters(type!!, alg))
        }
        reader.endArray()
        return parameters
    }

    private fun parseUserCredentials(body: ResponseBody): List<Credential> {
        fun readCredentials(reader: JsonReader): List<Credential> {
            val credentials = mutableListOf<Credential>()
            reader.beginArray()
            while (reader.hasNext()) {
                reader.beginObject()
                var id: String? = null
                var publicKey: String? = null
                while (reader.hasNext()) {
                    when (reader.nextName()) {
                        "credId" -> id = reader.nextString()
                        "publicKey" -> publicKey = reader.nextString()
                        else -> reader.skipValue()
                    }
                }
                reader.endObject()
                if (id != null && publicKey != null) {
                    println("Adding credentials in parse. Id = $id, publickKey = $publicKey")
                    credentials.add(Credential(id, publicKey))
                }
            }
            reader.endArray()
            return credentials
        }
        JsonReader(body.byteStream().bufferedReader()).use { reader ->
            reader.beginObject()
            while (reader.hasNext()) {
                val name = reader.nextName()
                if (name == "credentials") {
                    return readCredentials(reader)
                } else {
                    reader.skipValue()
                }
            }
            reader.endObject()
        }
        throw ApiException("Cannot parse credentials")
    }

    private fun <T> Response.result(errorMessage: String, data: Response.() -> T): ApiResult<T> {
        if (!isSuccessful) {
//            if (code == 401) { // Unauthorized
//                return ApiResult.SignedOutFromServer
//            }
            // All other errors throw an exception.
            return ApiResult.Success("", data())
            //throwResponseError(this, errorMessage)

        }
       // val cookie = headers("set-cookie").find { it.startsWith(GoogleAuthApi.SessionIdKey) }
      //  val sessionId = if (cookie != null) parseSessionId(cookie) else null
        //: TODO REMOVE SESSION ID
        return ApiResult.Success("", data())
    }

    private fun throwResponseError(response: Response, message: String): Nothing {
        val b = response.body
        if (b != null) {
            throw ApiException("$message; ${parseError(b)}")
        } else {
            throw ApiException(message)
        }
    }
    private fun parseError(body: ResponseBody): String {
        val errorString = body.string()
        try {
            JsonReader(StringReader(errorString)).use { reader ->
                reader.beginObject()
                while (reader.hasNext()) {
                    val name = reader.nextName()
                    if (name == "error") {
                        val token = reader.peek()
                        if (token == JsonToken.STRING) {
                            return reader.nextString()
                        }
                        return "Unknown"
                    } else {
                        reader.skipValue()
                    }
                }
                reader.endObject()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Cannot parse the error: $errorString", e)
            // Don't throw; this method is called during throwing.
        }
        return ""
    }

}