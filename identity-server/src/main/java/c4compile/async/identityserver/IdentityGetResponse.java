package c4compile.async.identityserver;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class IdentityGetResponse {
    private String firstName;
    private String lastName;
}
