package ch.admin.bit.jeap.governance.web;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;

@SpringBootApplication
@Slf4j
public class GovernanceApplication {


    public static void main(String[] args) {
        Environment env = SpringApplication.run(GovernanceApplication.class, args).getEnvironment();

        log.info("""
            
            ----------------------------------------------------------
            \t\
            {} is running!\s
            \t\
            
            \tSwaggerUI: \t\t\t\thttp://localhost:{}{}/swagger-ui/index.html?urls.primaryName=Governance-Service-API
            \t\
            Profile(s): \t\t\t{}\
            
            ----------------------------------------------------------""",
                env.getProperty("spring.application.name"),
                env.getProperty("server.port"),
                env.getProperty("server.servlet.context-path"),
                env.getActiveProfiles());
    }
}
