package com.example.moneytransfer.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Google BigQuery client configuration.
 *
 * <p>The {@link BigQuery} bean is only created when {@code bigquery.enabled=true}.
 * This allows the application to start normally in local development without
 * any GCP credentials.
 *
 * <h3>Credential resolution order:</h3>
 * <ol>
 *   <li>If {@code bigquery.credentials-file} is set → load service-account JSON from that path.
 *   <li>Otherwise → use Application Default Credentials (ADC).
 *       Works automatically on GCP (Cloud Run, GKE, App Engine) and when
 *       {@code gcloud auth application-default login} has been run locally.
 * </ol>
 */
@Slf4j
@Configuration
public class BigQueryConfig {

    @Value("${bigquery.project-id}")
    private String projectId;

    @Value("${bigquery.credentials-file:}")
    private String credentialsFile;

    /**
     * Builds and exposes a fully configured {@link BigQuery} client.
     * Only activated when {@code bigquery.enabled=true}.
     */
    @Bean
    @ConditionalOnProperty(name = "bigquery.enabled", havingValue = "true")
    public BigQuery bigQuery() throws IOException {
        GoogleCredentials credentials = resolveCredentials();

        BigQuery client = BigQueryOptions.newBuilder()
                .setProjectId(projectId)
                .setCredentials(credentials)
                .build()
                .getService();

        log.info("BigQuery client initialized — project='{}', credentials='{}'",
                projectId, credentialsFile.isBlank() ? "ADC" : credentialsFile);
        return client;
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private GoogleCredentials resolveCredentials() throws IOException {
        if (credentialsFile != null && !credentialsFile.isBlank()) {
            log.debug("Loading BigQuery credentials from file: {}", credentialsFile);
            try (InputStream is = new FileInputStream(credentialsFile)) {
                return ServiceAccountCredentials.fromStream(is)
                        .createScoped("https://www.googleapis.com/auth/bigquery");
            }
        }
        // Fallback: Application Default Credentials
        log.debug("BigQuery: using Application Default Credentials (ADC)");
        return GoogleCredentials.getApplicationDefault()
                .createScoped("https://www.googleapis.com/auth/bigquery");
    }
}
