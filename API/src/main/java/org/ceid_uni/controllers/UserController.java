package org.ceid_uni.controllers;


import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.ceid_uni.dto.request.PromoxUserRequest;
import org.ceid_uni.dto.response.MessageResponse;
import jakarta.validation.constraints.Pattern;
import org.ceid_uni.exception.ApplicationException;
import org.ceid_uni.helpers.Utils;
import org.ceid_uni.models.Request;
import org.ceid_uni.models.User;
import org.ceid_uni.repository.UserRepository;
import org.ceid_uni.security.services.IPromoxService;
import org.ceid_uni.security.services.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/user")
public class UserController {

    private UserRepository userRepository;

    private IPromoxService promoxService;

    @PostMapping("/vm/create")
    @Operation(summary = "Create a VM",
            description = "Initiate a request and specify the resources within the body of the request. Subsequently, the admin will deploy it accordingly.")
    public ResponseEntity<?> createVmRequest(@AuthenticationPrincipal UserDetailsImpl userDetails,
                                             @Valid  @RequestBody PromoxUserRequest promoxUserRequest) {
        User user = userRepository.findByUsername(userDetails.getUsername()).stream().findFirst().orElse(null);
        if (user != null) {
            promoxService.createUserRequest(user, promoxUserRequest, "POST");
            return ResponseEntity.ok(new MessageResponse("Request added successfully!"));
        }
        throw new ApplicationException("User was not found");
    }

    @GetMapping("/vms")
    @Operation(summary = "Request list",
            description = "Retrieve all requests made from the database.")
    public ResponseEntity<?> getVmsByUser(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        User user = userRepository.findByUsername(userDetails.getUsername()).stream().findFirst().orElse(null);
        if (user != null) {
            return ResponseEntity.ok(promoxService.findRequestsByUser(user));
        }
        throw new ApplicationException("User was not found");
    }

    @DeleteMapping("/vm/delete")
    @Operation(summary = "Delete Requests",
            description = "Delete requests by specifying a comma-separated list of their IDs.")
    public ResponseEntity<?> deleteVMById(@AuthenticationPrincipal UserDetailsImpl userDetails,
                                          @RequestParam("ids") @Pattern(regexp = "(\\d+,)*\\d+") String ids) {

        User user = userRepository.findByUsername(userDetails.getUsername()).stream().findFirst().orElse(null);

        if (user != null && Utils.isValidIds(ids)) {
            List<Long> idList = Arrays.stream(ids.split(","))
                    .map(Long::parseLong)
                    .toList();

            if (!idList.isEmpty()) {
                List<Request> requests = promoxService.findRequestsByUser(user).stream()
                        .filter(obj -> idList.contains(obj.getId()))
                        .peek(request -> request.setToBeRemoved(Boolean.TRUE))
                        .toList();

                if (!requests.isEmpty()) {
                    promoxService.saveRequests(requests);
                    return ResponseEntity.ok(new MessageResponse(requests.size() + " requests marked as deleted successfully!"));
                }
            }
        }
        throw new ApplicationException("Requests were not found");
    }

    @Autowired
    public void setPromoxService(IPromoxService promoxService) {
        this.promoxService = promoxService;
    }

    @Autowired
    public void setUserRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
}
