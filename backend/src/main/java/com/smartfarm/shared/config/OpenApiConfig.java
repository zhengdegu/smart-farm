package com.smartfarm.shared.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI smartFarmOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("智慧农业平台 API")
                        .description("物联网智慧农业平台接口文档 — 设备管理、遥测数据、智能灌溉、告警、报表、作物管理")
                        .version("1.0.0")
                        .contact(new Contact().name("SmartFarm Team"))
                        .license(new License().name("MIT")))
                .addSecurityItem(new SecurityRequirement().addList("Bearer"))
                .schemaRequirement("Bearer", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("JWT 认证，格式: Bearer {token}"));
    }
}
