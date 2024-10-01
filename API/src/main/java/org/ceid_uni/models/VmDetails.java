package org.ceid_uni.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity(name = "vmdetails")
public class VmDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @JsonIgnore
    private long id;

    @Column
    private Long vmId;

    @Column(nullable = false)
    private String vmName;

    @Column(nullable = false)
    private Long memoryGb;

    @Column(nullable = false)
    private Long processors;

    @Column
    private String storageName;

    @Column(nullable = false)
    private Long storageGb;

    @Column(nullable = false)
    private String template;

    public VmDetails() {
    }

    public VmDetails(String vmName, Long memoryGb, Long processors, String storageName, Long storageGb, String template) {
        this.vmName = vmName;
        this.memoryGb = memoryGb;
        this.processors = processors;
        this.storageName = storageName;
        this.storageGb = storageGb;
        this.template = template;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Long getVmId() {
        return vmId;
    }

    public void setVmId(Long vmId) {
        this.vmId = vmId;
    }

    public String getVmName() {
        return vmName;
    }

    public void setVmName(String vmName) {
        this.vmName = vmName;
    }

    public Long getMemoryGb() {
        return memoryGb;
    }

    public void setMemoryGb(Long memoryGb) {
        this.memoryGb = memoryGb;
    }

    public Long getProcessors() {
        return processors;
    }

    public void setProcessors(Long processors) {
        this.processors = processors;
    }

    public String getStorageName() {
        return storageName;
    }

    public void setStorageName(String storageName) {
        this.storageName = storageName;
    }

    public Long getStorageGb() {
        return storageGb;
    }

    public void setStorageGb(Long storageGb) {
        this.storageGb = storageGb;
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }
}
