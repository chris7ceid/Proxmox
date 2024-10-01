package org.ceid_uni.dto.request;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public class PromoxUserRequest {
    @FutureOrPresent
    @NotNull
    private Instant startDate;
    @FutureOrPresent
    @NotNull
    private Instant endDate;
    @NotNull
    private Long processors;
    @NotNull
    private Long storage;
    @NotNull
    private Long memory;
    private String os;

    public Instant getStartDate() {
        return startDate;
    }

    public void setStartDate(Instant startDate) {
        this.startDate = startDate;
    }

    public Instant getEndDate() {
        return endDate;
    }

    public void setEndDate(Instant endDate) {
        this.endDate = endDate;
    }

    public Long getProcessors() {
        return processors;
    }

    public void setProcessors(Long processors) {
        this.processors = processors;
    }

    public Long getStorage() {
        return storage;
    }

    public void setStorage(Long storage) {
        this.storage = storage;
    }

    public Long getMemory() {
        return memory;
    }

    public void setMemory(Long memory) {
        this.memory = memory;
    }

    public String getOs() {
        return os;
    }

    public void setOs(String os) {
        this.os = os;
    }
}
