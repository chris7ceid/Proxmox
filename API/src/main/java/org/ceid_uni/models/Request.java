package org.ceid_uni.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.Instant;

@Entity(name = "requests")
public class Request {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    @ManyToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private User user;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "vm_details_id", referencedColumnName = "id")
    private VmDetails vmDetails;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private Instant startDate;

    @Column(nullable = false)
    private Instant endDate;

    @Column
    private String node;

    @Column
    private Long repeat = 1L;

    @Column
    private Boolean completed;

    @Column
    private Boolean toBeRemoved = Boolean.FALSE;

    public Request() {
    }

    public Request(User user, VmDetails vmDetails, String type, Instant startDate, Instant endDate) {
        this.user = user;
        this.vmDetails = vmDetails;
        this.type = type;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @JsonIgnore
    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public VmDetails getVmDetails() {
        return vmDetails;
    }

    public void setVmDetails(VmDetails vmDetails) {
        this.vmDetails = vmDetails;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

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

    public Long getRepeat() {
        return repeat;
    }

    public void setRepeat(Long repeat) {
        this.repeat = repeat;
    }

    public Boolean getCompleted() {
        return completed;
    }

    public Boolean getToBeRemoved() {
        return toBeRemoved;
    }

    public void setToBeRemoved(Boolean toBeRemoved) {
        this.toBeRemoved = toBeRemoved;
    }

    public void setCompleted(Boolean completed) {
        this.completed = completed;
    }
    @PrePersist
    public void prePersist() {
        if (completed == null) {
            completed = false;
        }
    }

    public String getNode() {
        return node;
    }

    public void setNode(String node) {
        this.node = node;
    }
}
