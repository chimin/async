package c4compile.async.test;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Builder
public class TokenCreateRequest {
    private String userId;
}
