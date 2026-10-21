package com.codereview.server.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 应用时间配置。
 */
@Configuration
public class ApplicationClockConfiguration {

    /**
     * 提供统一的系统时钟。
     *
     * @return 系统默认时区时钟
     */
    @Bean
    public Clock applicationClock() {
        return Clock.systemDefaultZone();
    }
}
