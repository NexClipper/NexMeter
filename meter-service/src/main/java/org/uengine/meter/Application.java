package org.uengine.meter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.spring.data.rest.configuration.SpringDataRestConfiguration;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@SpringBootApplication
@EnableAutoConfiguration
@RestController
@ComponentScan
@Configuration
@EnableScheduling
@EnableRetry
@EnableAsync(proxyTargetClass = true)
@EnableCaching
public class Application {

    private final Log logger = LogFactory.getLog(getClass());

    @RequestMapping("/health")
    public String health() {
        return "";
    }

    public static ConfigurableApplicationContext applicationContext;

    public static void main(String[] args) {
        System.setProperty("org.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH", "true");
        applicationContext = new SpringApplicationBuilder(Application.class).web(true).run(args);
    }

    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }
}
