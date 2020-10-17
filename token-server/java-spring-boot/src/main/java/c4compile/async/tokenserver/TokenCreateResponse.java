package c4compile.async.tokenserver;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TokenCreateResponse {
    private String token;
}
