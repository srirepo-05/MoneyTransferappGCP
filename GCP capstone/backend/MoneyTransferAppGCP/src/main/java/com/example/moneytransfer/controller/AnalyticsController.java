package com.example.moneytransfer.controller;

import com.example.moneytransfer.domain.dto.analytics.*;
import com.example.moneytransfer.service.BigQueryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST controller that exposes BigQuery analytics queries.
 *
 * <p>All endpoints are restricted to the {@code ADMIN} role (see {@code SecurityConfig}).
 *
 * <p>When {@code bigquery.enabled=false} the {@link BigQueryService} bean does not exist
 * and every endpoint returns {@code 503 Service Unavailable} with a clear message.
 *
 * <h3>Endpoints</h3>
 * <pre>
 * GET /api/v1/analytics/summary                          → all KPIs in one call
 * GET /api/v1/analytics/daily-volume?days=30             → daily transaction volumes
 * GET /api/v1/analytics/success-rate                     → overall success / failure stats
 * GET /api/v1/analytics/top-accounts?limit=10            → most active accounts
 * GET /api/v1/analytics/peak-hours?top=5                 → busiest hours of day
 * </pre>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {

    private final Optional<BigQueryService> bigQueryService;

    public AnalyticsController(Optional<BigQueryService> bigQueryService) {
        this.bigQueryService = bigQueryService;
    }

    // -----------------------------------------------------------------------
    // Summary
    // -----------------------------------------------------------------------

    /**
     * GET /api/v1/analytics/summary
     * <p>Returns a single object containing all major KPIs — designed for a dashboard card.
     */
    @GetMapping("/summary")
    public ResponseEntity<?> getSummary() {
        if (bigQueryService.isEmpty()) {
            return bigQueryDisabledResponse();
        }
        log.debug("Analytics request: summary");
        AnalyticsSummaryDto summary = bigQueryService.get().getSummary();
        return ResponseEntity.ok(summary);
    }

    // -----------------------------------------------------------------------
    // Daily volume
    // -----------------------------------------------------------------------

    /**
     * GET /api/v1/analytics/daily-volume?days=30
     * <p>Daily transfer counts and totals for the last {@code days} days (default 30).
     *
     * @param days look-back window in calendar days (1 – 365)
     */
    @GetMapping("/daily-volume")
    public ResponseEntity<?> getDailyVolume(
            @RequestParam(name = "days", defaultValue = "30") int days) {

        if (bigQueryService.isEmpty()) {
            return bigQueryDisabledResponse();
        }
        days = clamp(days, 1, 365);
        log.debug("Analytics request: daily-volume days={}", days);
        List<DailyVolumeDto> data = bigQueryService.get().getDailyVolume(days);
        return ResponseEntity.ok(data);
    }

    // -----------------------------------------------------------------------
    // Success rate
    // -----------------------------------------------------------------------

    /**
     * GET /api/v1/analytics/success-rate
     * <p>Overall count and percentage of successful vs failed transfers.
     */
    @GetMapping("/success-rate")
    public ResponseEntity<?> getSuccessRate() {
        if (bigQueryService.isEmpty()) {
            return bigQueryDisabledResponse();
        }
        log.debug("Analytics request: success-rate");
        SuccessRateDto data = bigQueryService.get().getSuccessRate();
        return ResponseEntity.ok(data);
    }

    // -----------------------------------------------------------------------
    // Top accounts
    // -----------------------------------------------------------------------

    /**
     * GET /api/v1/analytics/top-accounts?limit=10
     * <p>Most active accounts by combined send + receive transfer count.
     *
     * @param limit maximum number of accounts to return (1 – 100)
     */
    @GetMapping("/top-accounts")
    public ResponseEntity<?> getTopAccounts(
            @RequestParam(name = "limit", defaultValue = "10") int limit) {

        if (bigQueryService.isEmpty()) {
            return bigQueryDisabledResponse();
        }
        limit = clamp(limit, 1, 100);
        log.debug("Analytics request: top-accounts limit={}", limit);
        List<TopAccountDto> data = bigQueryService.get().getTopAccounts(limit);
        return ResponseEntity.ok(data);
    }

    // -----------------------------------------------------------------------
    // Peak hours
    // -----------------------------------------------------------------------

    /**
     * GET /api/v1/analytics/peak-hours?top=5
     * <p>Busiest hours of the day ranked by transfer count.
     *
     * @param top number of peak hours to return (1 – 24)
     */
    @GetMapping("/peak-hours")
    public ResponseEntity<?> getPeakHours(
            @RequestParam(name = "top", defaultValue = "5") int top) {

        if (bigQueryService.isEmpty()) {
            return bigQueryDisabledResponse();
        }
        top = clamp(top, 1, 24);
        log.debug("Analytics request: peak-hours top={}", top);
        List<PeakHourDto> data = bigQueryService.get().getPeakHours(top);
        return ResponseEntity.ok(data);
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    /** Returns a 503 with a clear explanation when BigQuery is not enabled. */
    private ResponseEntity<Map<String, String>> bigQueryDisabledResponse() {
        return ResponseEntity.status(503).body(Map.of(
                "error",   "BIGQUERY_DISABLED",
                "message", "BigQuery integration is not enabled. " +
                           "Set bigquery.enabled=true and provide GCP credentials to activate analytics."
        ));
    }

    /** Clamps an integer to [min, max]. */
    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
