package org.ceid_uni.controllers;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.Cookie;
import org.ceid_uni.exception.ApplicationException;
import org.ceid_uni.helpers.Constants;
import org.ceid_uni.repository.UserRepository;
import org.ceid_uni.security.auth.PromoxUtils;
import org.ceid_uni.security.services.UserDetailsImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Arrays;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/node")
public class NodeController {

    private PromoxUtils promoxUtils;

    private UserRepository userRepository;

    private static final Logger logger = LoggerFactory.getLogger(NodeController.class);

    @GetMapping("/info")
    @Operation(summary = "Node Info",
            description = "Retrieve CPU, memory, and other crucial metrics for either a specific node or for all nodes if the request parameter is omitted.")
    public ResponseEntity<?> getNodeInfo(@AuthenticationPrincipal UserDetailsImpl userDetails,
                                         @CookieValue(value = Constants.COOKIE_NAME) String cookieValue,
                                         @RequestParam(name = "nodeId", defaultValue = "all") String nodeId) {
        userRepository.findByUsername(userDetails.getUsername()).stream().findFirst().ifPresent(user ->
                logger.info("Node info request from high privileged user {}", user.getUsername()));
        return ResponseEntity.ok(promoxUtils.getPromoxNodeDetails(new Cookie(Constants.COOKIE_NAME, cookieValue),
                (nodeId != null && !nodeId.equals("all") ? (nodeId + "/status") : "")));
    }

    @GetMapping("/resources/{nodeId}/{resource}")
    @Operation(summary = "Vm & lxc Info",
            description = "Get info for a VMs and lxc resources for a specific node")
    public ResponseEntity<?> getVmInfo(@AuthenticationPrincipal UserDetailsImpl userDetails,
                                       @CookieValue(value = Constants.COOKIE_NAME, required = false) String cookieValue,
                                       @PathVariable(name = "nodeId") String nodeId,
                                       @PathVariable(name = "resource") String resource) {
        userRepository.findByUsername(userDetails.getUsername()).stream().findFirst().ifPresent(user ->
                logger.info("VMs and lxc resources request from high privileged user {}", user.getUsername()));
        if (resource.equals("vm")) {
            return ResponseEntity.ok(promoxUtils.getPromoxNodeDetails(new Cookie(Constants.COOKIE_NAME, cookieValue),
                    nodeId + "/qemu"));
        } else if (resource.equals("lxc")) {
            return ResponseEntity.ok(promoxUtils.getPromoxNodeDetails(new Cookie(Constants.COOKIE_NAME, cookieValue),
                    nodeId + "/lxc"));
        }

        throw new ApplicationException("Request must be in the following format /resources/{nodeId}/[vm|lxc]!");

    }

    @GetMapping("/resources/{nodeId}/metrics")
    @Operation(summary = "Node Metrics",
            description = "Retrieve hourly, daily, weekly, and yearly CPU, memory, and other critical metrics for a node. These data sets can be utilized for machine learning tasks.")
    public ResponseEntity<?> getNoneMetrics(@AuthenticationPrincipal UserDetailsImpl userDetails,
                                            @CookieValue(value = Constants.COOKIE_NAME) String cookieValue,
                                            @PathVariable(name = "nodeId") String nodeId,
                                            @RequestParam(name = "timeframe", defaultValue = "hour") String timeframe) {

        userRepository.findByUsername(userDetails.getUsername()).stream().findFirst().ifPresent(user ->
                logger.info("Node time series request request from high privileged user {}", user.getUsername()));
        if (!Arrays.asList("hour", "day", "week", "month", "year").contains(timeframe)){
            throw new ApplicationException("timeframe must contain one of the  following values [hour | day | week | month | year]!");
        }
        return ResponseEntity.ok(promoxUtils.getPromoxNodeDetails(new Cookie(Constants.COOKIE_NAME, cookieValue),
                        nodeId + "/rrddata?timeframe=" + timeframe).stream().peek(
                        map -> {
                            if (map.containsKey("time")) {
                                map.put("time", Instant.ofEpochSecond((int) map.get("time")));
                            }
                            if (map.containsKey("cpu")) {
                                map.put("cpu%", String.format("%.2f", Math.round((Double) map.get("cpu") * 100.0) / 100.0) + "%");
                            }
                            if (map.containsKey("memused")) {
                                if (map.get("memused") instanceof Double) {
                                    map.put("memusedGB", String.format("%.2f", (Double) map.get("memused") / (1024 * 1024 * 1024)) + "GB");
                                } else if (map.get("memused") instanceof Long) {
                                    map.put("memusedGB", (Long) map.get("memused") / (1024 * 1024 * 1024) + "GB");
                                }
                            }
                            if (map.containsKey("iowait")) {
                                map.put("iowait%", String.format("%.2f", Math.round((Double) map.get("iowait") * 100.0) / 100.0) + "%");
                            }
                            if (map.containsKey("roottotal")) {
                                if (map.get("roottotal") instanceof Double) {
                                    map.put("roottotalGB", String.format("%.2f", (Double) map.get("roottotal") / (1024 * 1024 * 1024)) + "GB");
                                } else if (map.get("roottotal") instanceof Long) {
                                    map.put("roottotalGB", (Long) map.get("roottotal") / (1024 * 1024 * 1024) + "GB");
                                }
                            }
                            if (map.containsKey("rootused")) {
                                if (map.get("rootused") instanceof Double) {
                                    map.put("rootusedGB", String.format("%.2f", (Double) map.get("rootused") / (1024 * 1024 * 1024)) + "GB");
                                } else if (map.get("rootused") instanceof Long) {
                                    map.put("rootusedGB", (Long) map.get("rootused") / (1024 * 1024 * 1024) + "GB");
                                }
                            }
                            if (map.containsKey("memtotal")) {
                                if (map.get("memtotal") instanceof Double) {
                                    map.put("memtotalGB", String.format("%.2f", (Double) map.get("memtotal") / (1024 * 1024 * 1024)) + "GB");
                                } else if (map.get("memtotal") instanceof Long) {
                                    map.put("memtotalGB", (Long) map.get("memtotal") / (1024 * 1024 * 1024) + "GB");
                                }
                            }

                        })
                .sorted((map1, map2) -> {
                    Instant time1 = (Instant) map1.get("time");
                    Instant time2 = (Instant) map2.get("time");
                    return time1.compareTo(time2);
                })
                .collect(Collectors.toList()));
    }

    @Autowired
    public void setPromoxUtils(PromoxUtils promoxUtils) {
        this.promoxUtils = promoxUtils;
    }

    @Autowired
    public void setUserRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
}
