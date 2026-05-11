package org.gsginzburg.cluster.sample.api;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.gsginzburg.cluster.sample.domain.dto.CreateTestRecordRequest;
import org.gsginzburg.cluster.sample.domain.dto.TestRecordDto;
import org.gsginzburg.cluster.sample.service.TestRecordService;
import org.gsginzburg.shared.dto.ApiResponse;

@RestController
@RequestMapping("/api/app/test-records")
@RequiredArgsConstructor
public class SampleController {

    private final TestRecordService testRecordService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<TestRecordDto>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(testRecordService.getAll()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TestRecordDto>> create(@Valid @RequestBody CreateTestRecordRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(testRecordService.create(request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        testRecordService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
