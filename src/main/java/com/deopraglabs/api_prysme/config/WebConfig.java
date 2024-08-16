package com.deopraglabs.api_prysme.config;

import jdk.jfr.ContentType;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
//         QUERY PARAM (https://localhost:8080/api/v1/yser?mediaType={TIPO ARQUIVO})
//        configurer.favorParameter(true)
//                .parameterName("mediaType").ignoreAcceptHeader(true)
//                .useRegisteredExtensionsOnly(true)
//                .defaultContentType(MediaType.APPLICATION_JSON)
//                    .mediaType("json", MediaType.APPLICATION_JSON)
//                    .mediaType("xml", MediaType.APPLICATION_XML)
//                    .mediaType("csv", MediaType.TEXT_PLAIN);

//         HEADER PARAM (https://localhost:8080/api/v1/user)
//         Quando usar assim, ao enviar a solicitação, usar o parâmetro "Accept"
        configurer.favorParameter(false).ignoreAcceptHeader(false)
                .useRegisteredExtensionsOnly(false)
                .defaultContentType(MediaType.APPLICATION_JSON)
                    .mediaType("json", MediaType.APPLICATION_JSON)
                    .mediaType("xml", MediaType.APPLICATION_XML)
                    .mediaType("csv", MediaType.TEXT_PLAIN);
    }
}
