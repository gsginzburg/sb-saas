package org.gsginzburg.cluster.sample.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateTestRecordRequest {
    @NotBlank
    private String name;
    private String description;
    private Integer value;
}
