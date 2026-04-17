package com.dailydevinsight.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebResourceConfig implements WebMvcConfigurer {

    private final Path thumbnailUploadDirectory;

    /**
     * @date 2026-04-17
     * @desc 업로드 썸네일 루트 경로를 외부 설정값으로 초기화합니다.
     */
    public WebResourceConfig(
            @Value("${crawler.thumbnail-upload-dir:./uploads}") String thumbnailUploadDirectory
    ) {
        this.thumbnailUploadDirectory = Paths.get(thumbnailUploadDirectory).toAbsolutePath().normalize();
    }

    /**
     * @date 2026-04-17
     * @desc /uploads/** 요청을 로컬 파일 업로드 디렉터리로 매핑합니다.
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String resourceLocation = thumbnailUploadDirectory.toUri().toString();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(resourceLocation);
    }
}
