package ch.admin.bit.jeap.governance.web.config;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.context.SecurityContextHolder;

@Configuration
public class SecurityConfig {

    @PostConstruct
    public void setSecurityContextStrategy() {
        // This makes SecurityContext inheritable by child threads
        SecurityContextHolder.setStrategyName(
                SecurityContextHolder.MODE_INHERITABLETHREADLOCAL
        );
    }
}
