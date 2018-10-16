package org.uengine.meter.billing.kb;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;

@Service
@Data
@ConfigurationProperties(prefix = "killbill")
public class KBConfig {

    private String url;
    private String username;
    private String password;
    private String apikey;
    private String apisecret;
}
