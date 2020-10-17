package c4compile.async.identityserver;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class IdentityController {

    @GetMapping("/user")
    @SneakyThrows
    public IdentityGetResponse get(@RequestParam("userId") String userId) {
        log.info("Get identity for {}", userId);

        Thread.sleep(5000);

        return IdentityGetResponse.builder()
                .firstName(userId)
                .lastName(userId)
                .build();
    }

}
