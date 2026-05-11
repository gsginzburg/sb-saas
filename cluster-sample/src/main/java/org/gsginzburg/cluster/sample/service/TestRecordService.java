package org.gsginzburg.cluster.sample.service;

import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.gsginzburg.cluster.sample.domain.dto.CreateTestRecordRequest;
import org.gsginzburg.cluster.sample.domain.dto.TestRecordDto;
import org.gsginzburg.cluster.sample.domain.model.TestRecord;
import org.gsginzburg.cluster.sample.domain.repository.TestRecordRepository;

@Service
@RequiredArgsConstructor
public class TestRecordService {

    private final TestRecordRepository testRecordRepository;

    @Transactional(readOnly = true)
    public List<TestRecordDto> getAll() {
        // Set search_path to tenant schema before query
        return testRecordRepository.findAll().stream().map(this::toDto).toList();
    }

    @Transactional
    public TestRecordDto create(CreateTestRecordRequest request) {
        TestRecord record = TestRecord.builder()
                .name(request.getName())
                .description(request.getDescription())
                .value(request.getValue())
                .build();
        return toDto(testRecordRepository.save(record));
    }

    @Transactional
    public void delete(UUID id) {
        testRecordRepository.deleteById(id);
    }

    private TestRecordDto toDto(TestRecord r) {
        return TestRecordDto.builder()
                .id(r.getId().toString())
                .name(r.getName())
                .description(r.getDescription())
                .value(r.getValue())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
