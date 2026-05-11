package org.gsginzburg.cluster.sample.domain.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import org.gsginzburg.cluster.sample.domain.model.TestRecord;

public interface TestRecordRepository extends JpaRepository<TestRecord, UUID> {}
