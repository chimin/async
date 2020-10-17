package c4compile.async.tokenserver

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.util.*

@RestController
class TokenController {

    companion object {
        private val KEY_PAIR = KeyPairGenerator.getInstance("RSA").generateKeyPair()
        private val WEB_CLIENT = WebClient.create()
    }

    @PostMapping("/token")
    suspend fun create(@RequestBody request: TokenCreateRequest): TokenCreateResponse {
        val identity = getIdentity(request.userId)

        val token = JWT.create()
                .withSubject(request.userId)
                .withClaim("given_name", identity.firstName)
                .withClaim("family_name", identity.lastName)
                .withIssuedAt(Date())
                .sign(Algorithm.RSA256(KEY_PAIR.public as RSAPublicKey, KEY_PAIR.private as RSAPrivateKey))

        return TokenCreateResponse(token = token)
    }

    private suspend fun getIdentity(userId: String): IdentityGetResponse {
        val uri = UriComponentsBuilder.newInstance()
                .uri(URI.create("http://localhost:9000/user"))
                .queryParam("userId", userId)
                .build().toUri()

        return WEB_CLIENT.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(IdentityGetResponse::class.java)
                .awaitFirst()
    }

}