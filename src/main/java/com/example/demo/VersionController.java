package com.example.demo;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class VersionController {

    @GetMapping("/version")
    public ResponseEntity<VersionResponse> getVersion() {
        try {
            VersionResponse response = VersionResponse.builder()
                    .backendVersion(getBackendVersion())
                    .frontendVersion(getFrontendVersion())
                    .gitCommitHash(getGitCommitHash())
                    .buildTimestamp(getBuildTimestamp())
                    .build();
            
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.maxAge(300, java.util.concurrent.TimeUnit.SECONDS))
                    .body(response);
        } catch (Exception e) {
            // Возвращаем 500 Internal Server Error при проблемах с получением версии
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private String getBackendVersion() {
        // Временное решение для разработки
        // TODO: Использовать @Value("${project.version}") после настройки Maven resource filtering
        return "0.0.1-SNAPSHOT";
    }

    private String getFrontendVersion() {
        // Версия фронтенда из package.json
        // Временное значение, будет обновлено при интеграции с фронтендом
        return "0.1.0";
    }

    private String getGitCommitHash() {
        // Git commit hash через git-commit-id-plugin
        // TODO: Использовать @Value("${git.commit.id.abbrev}") после настройки
        return "unknown";
    }

    private String getBuildTimestamp() {
        // Timestamp сборки через Maven
        return LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
}