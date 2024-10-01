package org.ceid_uni.controllers;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.Cookie;
import jakarta.validation.constraints.Pattern;
import org.ceid_uni.dto.request.UpdateVmConf;
import org.ceid_uni.dto.response.MessageResponse;
import org.ceid_uni.exception.ApplicationException;
import org.ceid_uni.helpers.Constants;
import org.ceid_uni.helpers.Utils;
import org.ceid_uni.repository.UserRepository;
import org.ceid_uni.security.auth.PromoxUtils;
import org.ceid_uni.security.services.IPromoxService;
import org.ceid_uni.security.services.UserDetailsImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.ceid_uni.models.Request;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);
    private PromoxUtils promoxUtils;
    private IPromoxService promoxService;

    private UserRepository userRepository;

    @GetMapping("/requests/deploy")
    @Operation(summary = "Deploy requests",
            description = "Attempt deployment to Promox for all requests in the database that are not yet completed.")
    public ResponseEntity<?> deployUserRequests(@AuthenticationPrincipal UserDetailsImpl userDetails,
                                                @CookieValue(value = Constants.COOKIE_NAME) String cookieValue,
                                                @RequestHeader(value = Constants.CSRF_TOKEN_HEADER) String csrfToken) {

        userRepository.findByUsername(userDetails.getUsername()).stream().findFirst().ifPresent(user ->
                logger.info("Attempt to deploy resources from high privileged user {}", user.getUsername()));

        List<Request> requests = promoxService.findRequestsByCompleted(Boolean.FALSE).stream()
                .sorted(Comparator.comparing(request -> request.getVmDetails().getMemoryGb(),
                        Comparator.reverseOrder())).collect(Collectors.toList());

        if (!requests.isEmpty()){
            int requestCnt = 0;
            Set<Map> resources = new HashSet<>(promoxUtils.getPromoxClusterDetails(new Cookie(Constants.COOKIE_NAME, cookieValue)));
            Set<Integer> excluded = resources
                    .stream()
                    .filter(map -> map.get("type").equals("qemu"))
                    .map(map -> (Integer) map.get("vmid"))
                    .collect(Collectors.toSet());
            Set<Map> nodes = resources.stream()
                    .filter(map -> map.get("type").equals("node"))
                    .filter(map -> map.get("status").equals("online"))
                    .peek(this::setMemValues).sorted(Comparator.comparing(map -> (BigDecimal) map.get("memperc")))
                    .collect(Collectors.toCollection(LinkedHashSet::new));

            for (Request request : requests) {
                try {
                    if (!nodes.isEmpty()) {
                        Iterator<Map> iterator = nodes.iterator();
                        if (iterator.hasNext()) {
                            Map node = iterator.next();
                            iterator.remove();
                            request.setNode((String) node.get("node"));
                        }
                    } else {
                        break;
                    }
                    int randNum = promoxUtils.generateRandomNumber(100, 199, excluded);
                    excluded.add(randNum);
                    request.getVmDetails().setVmId((long) randNum);
                    request.getVmDetails().setVmName(cookieValue.split(":")[1].split("@")[0] + "-vm-" + randNum);
                    request.getVmDetails().setStorageName(cookieValue.split(":")[1].split("@")[0] + "-disk-" + randNum);
                    // do the call to proxmox and then mark as completed
                    if (promoxUtils.createVm(new Cookie(Constants.COOKIE_NAME, cookieValue), csrfToken, request)) {
                        logger.info("VM {} was mapped to node {}",  request.getVmDetails().getVmId(), request.getNode());
                        request.setCompleted(Boolean.TRUE);
                        requestCnt++;
                    } else {
                        logger.warn("VM: {} was not deployed successfully", request.getVmDetails().getVmId());
                    }
                } catch (Exception e) {
                    // Something went wrong and the completed flag will be false no need to raise exception, skip it
                    logger.error("Something went wrong: {}", e.getMessage());
                }
            }
            promoxService.saveRequests(requests);
            return ResponseEntity.ok(new MessageResponse(requestCnt + "/" + requests.size() + " requests deployed successfully!"));
        }
        throw new ApplicationException("No Valid Requests Found to deploy");
    }

    @DeleteMapping("/requests/delete")
    @Operation(summary = "Delete VMs",
            description = "Delete all virtual machines based on a comma-separated list of VM IDs or delete all VMs marked for removal from the database.")
    public ResponseEntity<?> deleteRequests(@AuthenticationPrincipal UserDetailsImpl userDetails,
                                            @CookieValue(value = Constants.COOKIE_NAME) String cookieValue,
                                            @RequestHeader(value = Constants.CSRF_TOKEN_HEADER) String csrfToken,
                                            @RequestParam(value = "ids", required = false) @Pattern(regexp = "(\\d+,)*\\d+") String ids,
                                            @RequestParam(value = "marked", required = false) Boolean marked) {

        userRepository.findByUsername(userDetails.getUsername()).stream().findFirst().ifPresent(user ->
                logger.info("Attempt to delete resources from high privileged user {}", user.getUsername()));
        // Suspend by default and delete by admin
        Set<Integer> vmIds = new HashSet<>(promoxUtils.getPromoxClusterDetails(new Cookie(Constants.COOKIE_NAME, cookieValue)))
                .stream()
                .filter(map -> map.get("type").equals("qemu"))
                .map(map -> (Integer) map.get("vmid"))
                .collect(Collectors.toSet());
        List<Request> requests = new ArrayList<>();
        if (Utils.isValidIds(ids)) {
            List<Long> idList = Arrays.stream(ids.split(","))
                    .map(Long::parseLong)
                    .toList();
            requests = promoxService.findAllRequests().stream().
                    filter(request -> idList.contains(request.getId())).
                    toList();
        } else if (marked != null) {
            requests = promoxService.findAllRequests().stream().
                    filter(request -> request.getToBeRemoved().equals(marked)).
                    toList();
        }
        if (!requests.isEmpty()) {
            for (Request req : requests) {
                // Check if request is completed or not
                if (req.getVmDetails().getVmId() != null && vmIds.contains(req.getVmDetails().getVmId().intValue())) {
                    if (promoxUtils.deleteVm(new Cookie(Constants.COOKIE_NAME, cookieValue), csrfToken, req)) {
                        promoxService.deleteRequest(req);
                        logger.info("VM: {} deleted successfully both promox and db", req.getVmDetails().getVmId());
                    }
                } else {
                    promoxService.deleteRequest(req);
                    logger.info("VM: {} deleted successfully only from db (not found in promox)", req.getVmDetails().getVmId());
                }
            }
            return ResponseEntity.ok(new MessageResponse("info: Delete operation completed successfully."));
        }

        throw new ApplicationException("No Valid Requests Found to delete");
    }

    @GetMapping("/requests/resize")
    @Operation(summary = "Resize k8s VMs",
            description = "Resize all VMs in the range of 200 to 299 based on memory usage.")
    public ResponseEntity<?> vmsConfigure(@AuthenticationPrincipal UserDetailsImpl userDetails,
                                          @CookieValue(value = Constants.COOKIE_NAME) String cookieValue,
                                          @RequestHeader(value = Constants.CSRF_TOKEN_HEADER) String csrfToken) {
        userRepository.findByUsername(userDetails.getUsername()).stream().findFirst().ifPresent(user ->
                logger.info("Attempt to resize VMs from high privileged user {}", user.getUsername()));
        Set<Map> resources = new HashSet<>(promoxUtils.getPromoxClusterDetails(new Cookie(Constants.COOKIE_NAME, cookieValue)));
        Set<Map> nodes = resources.stream()
                .filter(map -> map.get("type").equals("qemu"))
                .filter(map -> map.get("status").equals("running"))
                .filter(map -> {
                    Integer vmid = (Integer) map.get("vmid");
                    return vmid > 199 && vmid < 300;
                })
                .peek(this::setMemValues).sorted(Comparator.comparing(map -> (BigDecimal) map.get("memperc"),
                        Comparator.reverseOrder()))
                .filter(map -> ((BigDecimal) map.get("memperc")).compareTo(BigDecimal.valueOf(0.5)) < 0)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        try {
            for (Map node : nodes) {
                UpdateVmConf updateVmConf = new UpdateVmConf();
                updateVmConf.setNode((String) node.get("node"));
                Object memValue = node.get("mem");
                long mem = memValue instanceof Long ? (Long) memValue : ((Integer) memValue).longValue();
                mem = (mem / (1024 * 1024)) * 2;
                updateVmConf.setMemory(String.valueOf(mem));
                updateVmConf.setVmId((int) node.get("vmid"));
                updateVmConf.setDigest(promoxUtils.getVmConfig(new Cookie(Constants.COOKIE_NAME, cookieValue), updateVmConf));
                promoxUtils.updateVmConfig(new Cookie(Constants.COOKIE_NAME, cookieValue), csrfToken, updateVmConf);
                logger.info("Vm: {} updated successfully", updateVmConf.getVmId());
            }
        } catch (Exception ex) {
            logger.error("Something went wrong while updating Vms: {}", ex.getMessage());
        }


        return ResponseEntity.ok(new MessageResponse("Vms were resized successfully"));
    }

    private void setMemValues(Map map) {
        Object memValue = map.get("mem");
        Object maxmemValue = map.get("maxmem");
        long mem = memValue instanceof Long ? (Long) memValue : ((Integer) memValue).longValue();
        long maxmem = maxmemValue instanceof Long ? (Long) maxmemValue : ((Integer) maxmemValue).longValue();
        map.put("memperc", BigDecimal.valueOf(mem).divide(BigDecimal.valueOf(maxmem), 3, RoundingMode.HALF_UP));
    }

    @GetMapping("/cluster")
    @Operation(summary = "Cluster Info",
            description = "Retrieve information for all resources, including nodes, virtual machines (VMs), LXC containers, and others.")
    public ResponseEntity<?> getClusterInfo(@AuthenticationPrincipal UserDetailsImpl userDetails,
                                            @CookieValue(value = Constants.COOKIE_NAME, required = false) String cookieValue) {
        userRepository.findByUsername(userDetails.getUsername()).stream().findFirst().ifPresent(user ->
                logger.info("Attempt to retrieve resources information from high privileged user {}", user.getUsername()));
        return ResponseEntity.ok(promoxUtils.getPromoxClusterDetails(new Cookie(Constants.COOKIE_NAME, cookieValue)));
    }

    @Autowired
    public void setPromoxUtils(PromoxUtils promoxUtils) {
        this.promoxUtils = promoxUtils;
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
