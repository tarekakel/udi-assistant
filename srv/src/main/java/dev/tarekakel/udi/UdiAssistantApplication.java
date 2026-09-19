 package dev.tarekakel.udi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class UdiAssistantApplication {

    public static void main(String[] args) {
        SpringApplication.run(UdiAssistantApplication.class, args);
    }
}
