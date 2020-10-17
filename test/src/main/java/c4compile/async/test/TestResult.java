package c4compile.async.test;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class TestResult {
    private long startTimeMs;
    private List<Long> result;
}
