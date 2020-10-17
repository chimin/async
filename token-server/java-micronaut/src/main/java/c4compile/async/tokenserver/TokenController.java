package c4compile.async.tokenserver;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.client.RxHttpClient;
import io.micronaut.http.uri.UriBuilder;
import io.reactivex.Single;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import java.net.URI;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;

@Controller
@RequiredArgsConstructor
public class TokenController {

    private static final KeyPair KEY_PAIR = generateKeyPair();

    private final RxHttpClient httpClient;

    @SneakyThrows
    private static KeyPair generateKeyPair() {
        return KeyPairGenerator.getInstance("RSA").generateKeyPair();
    }

    @Post("/token")
    public Single<TokenCreateResponse> create(@Body TokenCreateRequest request) {
        return getIdentity(request.getUserId())
                .map(identity -> {
                    String token = JWT.create()
                            .withSubject(request.getUserId())
                            .withClaim("given_name", identity.getFirstName())
                            .withClaim("family_name", identity.getLastName())
                            .withIssuedAt(new Date())
                            .sign(Algorithm.RSA256((RSAPublicKey) KEY_PAIR.getPublic(), (RSAPrivateKey) KEY_PAIR.getPrivate()));

                    return TokenCreateResponse.builder()
                            .token(token)
                            .build();
                });
    }

    private Single<IdentityGetResponse> getIdentity(String userId) {
        URI uri = UriBuilder.of("http://localhost:9000/user")
                .queryParam("userId", userId)
                .build();

        return httpClient.exchange(uri.toString(), IdentityGetResponse.class)
                .map(HttpResponse::body)
                .firstOrError();
    }

}
