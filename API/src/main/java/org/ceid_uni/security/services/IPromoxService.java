package org.ceid_uni.security.services;

import org.ceid_uni.dto.request.PromoxUserRequest;
import org.ceid_uni.models.Request;
import org.ceid_uni.models.User;
import org.ceid_uni.models.VmDetails;

import java.util.List;

public interface IPromoxService {
    List<Request> findRequestsByUser(User user);

    List<Request> findAllRequests();

    List<Request> findRequestsByCompleted(Boolean completed);


    void saveRequests(List<Request> requests);

    void createUserRequest(User user, PromoxUserRequest requestDto, String type);

    void deleteRequest(Request request);

    VmDetails findByVmId(Long id);

    List<VmDetails> findAllVMs();

}
