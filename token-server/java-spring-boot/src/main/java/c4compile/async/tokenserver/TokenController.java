package c4compile.async.tokenserver;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import lombok.SneakyThrows;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;

@RestController
public class TokenController {

    private static final KeyPair KEY_PAIR = generateKeyPair();

    @SneakyThrows
    private static KeyPair generateKeyPair() {
        return KeyPairGenerator.getInstance("RSA").generateKeyPair();
    }

    @PostMapping("/token")
    public TokenCreateResponse create(@RequestBody TokenCreateRequest request) {
        IdentityGetResponse identity = getIdentity(request.getUserId());

        String token = JWT.create()
                .withSubject(request.getUserId())
                .withClaim("given_name", identity.getFirstName())
                .withClaim("family_name", identity.getLastName())
                .withIssuedAt(new Date())
                .sign(Algorithm.RSA256((RSAPublicKey) KEY_PAIR.getPublic(), (RSAPrivateKey) KEY_PAIR.getPrivate()));

        return TokenCreateResponse.builder()
                .token(token)
                .build();
    }

    private IdentityGetResponse getIdentity(String userId) {
        URI uri = UriComponentsBuilder.newInstance()
                .uri(URI.create("http://localhost:9000/user"))
                .queryParam("userId", userId)
                .build().toUri();

        return new RestTemplate()
                .getForObject(uri, IdentityGetResponse.class);
    }

}
