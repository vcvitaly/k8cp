package io.github.vcvitaly.k8cp.util;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DateTimeUtilTest {

    @Test
    void getChangedAtTest_withLongIso() {
        final LocalDateTime ldt = DateTimeUtil.toLocalDate("2024-03-10", "08:35");
        assertThat(ldt).isEqualTo(LocalDateTime.of(2024, 3, 10, 8, 35));
    }

    @Test
    void getChangedAtTest_withFullTime() {
        final LocalDateTime ldt = DateTimeUtil.toLocalDate("2024-03-10", "08:35:30.000000000");
        assertThat(ldt).isEqualTo(LocalDateTime.of(2024, 3, 10, 8, 35));
    }
}