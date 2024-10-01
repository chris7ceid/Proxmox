package org.ceid_uni.security.services;

import org.ceid_uni.dto.request.PromoxUserRequest;
import org.ceid_uni.models.Request;
import org.ceid_uni.models.User;
import org.ceid_uni.models.VmDetails;
import org.ceid_uni.repository.RequestsRepository;
import org.ceid_uni.repository.VmDetailsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PromoxService implements IPromoxService {
    private RequestsRepository requestsRepository;

    private VmDetailsRepository vmDetailsRepository;

    @Override
    public List<Request> findRequestsByUser(User user) {
        return requestsRepository.findByUser(user);
    }

    @Override
    public List<Request> findAllRequests() {
        return requestsRepository.findAll();
    }

    @Override
    public List<Request> findRequestsByCompleted(Boolean completed) {
        return requestsRepository.findRequestsByCompleted(completed);
    }

    @Override
    @Transactional
    public void saveRequests(List<Request> requests) {
        requestsRepository.saveAll(requests);
    }

    @Override
    @Transactional
    public void createUserRequest(User user, PromoxUserRequest requestDto, String type) {
        Request request = new Request(user, new VmDetails("", requestDto.getMemory(),
                requestDto.getProcessors(), "",
                requestDto.getStorage(), requestDto.getOs()), type,
                requestDto.getStartDate(), requestDto.getEndDate());
        requestsRepository.save(request);
    }

    @Override
    @Transactional
    public void deleteRequest(Request request) {
        requestsRepository.delete(request);
    }

    @Override
    public VmDetails findByVmId(Long id) {
        return vmDetailsRepository.findByVmId(id);
    }

    @Override
    public List<VmDetails> findAllVMs() {
        return vmDetailsRepository.findAll();
    }

    @Autowired
    public void setRequestsRepository(RequestsRepository requestsRepository) {
        this.requestsRepository = requestsRepository;
    }

    @Autowired
    public void setVmDetailsRepository(VmDetailsRepository vmDetailsRepository) {
        this.vmDetailsRepository = vmDetailsRepository;
    }
}
