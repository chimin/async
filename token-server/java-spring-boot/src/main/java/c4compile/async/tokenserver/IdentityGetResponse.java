package c4compile.async.tokenserver;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IdentityGetResponse {
    private String firstName;
    private String lastName;
}
