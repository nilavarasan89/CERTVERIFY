package com.certverify.controller;

import com.certverify.dto.StatisticsResponse;
import com.certverify.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;

    @GetMapping("/course/{courseId}")
    public ResponseEntity<StatisticsResponse> getCourseStatistics(@PathVariable Long courseId) {
        return ResponseEntity.ok(statisticsService.getCourseStatistics(courseId));
    }

    @GetMapping
    public ResponseEntity<StatisticsResponse> getOverallStatistics() {
        return ResponseEntity.ok(statisticsService.getOverallStatistics());
    }
}
