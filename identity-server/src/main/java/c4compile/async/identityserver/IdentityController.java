package c4compile.async.identityserver;

import lombok.SneakyThrows;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class IdentityController {

    @GetMapping("/user")
    @SneakyThrows
    public IdentityGetResponse get(@RequestParam("userId") String userId) {
        Thread.sleep(5000);

        return IdentityGetResponse.builder()
                .firstName(userId)
                .lastName(userId)
                .build();
    }

}
