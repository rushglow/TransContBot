package org.batukhtin.transcontbot.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "mail")
@Getter
@Setter
public class MailProperties {
    private String host;
    private String username;
    private String password;
    private String folder;
    private int intervalSeconds;
}
