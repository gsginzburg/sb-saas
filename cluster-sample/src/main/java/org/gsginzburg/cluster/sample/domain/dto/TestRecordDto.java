package org.gsginzburg.cluster.sample.domain.dto;

import java.time.OffsetDateTime;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class TestRecordDto {
    private String id;
    private String name;
    private String description;
    private Integer value;
    private OffsetDateTime createdAt;
}
