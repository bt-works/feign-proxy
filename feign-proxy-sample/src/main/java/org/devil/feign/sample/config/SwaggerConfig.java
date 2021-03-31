package org.devil.feign.sample.config;

import com.google.common.base.Predicates;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.util.Collections;

@Configuration
@EnableSwagger2
public class SwaggerConfig {

    @Bean
    public Docket docket(){
        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo())
                .enable(true)
                .pathMapping("/")
                .select()
                .apis(Predicates.and(RequestHandlerSelectors.withClassAnnotation(RestController.class)))
                .build();
    }

    private ApiInfo apiInfo(){
        return new ApiInfo("Test","Test Feign Proxy","V1.0","",new Contact("","",""),"","", Collections.emptyList())
                ;
    }

}
