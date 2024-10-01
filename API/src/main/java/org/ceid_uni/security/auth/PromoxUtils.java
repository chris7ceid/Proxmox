package org.ceid_uni.security.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.Cookie;
import org.ceid_uni.dto.request.UpdateVmConf;
import org.ceid_uni.dto.response.ProxmoxLoginResponse;
import org.ceid_uni.dto.response.ProxmoxUserAuthResponse;
import org.ceid_uni.models.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;

@Component
public class PromoxUtils {
    private static final Logger logger = LoggerFactory.getLogger(PromoxUtils.class);

    private WebClient webClient;

    public ProxmoxLoginResponse verifyPromoxTicket(String username, String password) {
        String response = webClient
                .post()
                .uri("/api2/json/access/ticket")
                .body(BodyInserters.fromFormData("username", username)
                        .with("password", password))
                .retrieve()
                .bodyToMono(String.class)
                .onErrorResume(throwable -> Mono.just(""))
                .block();

        List<ProxmoxLoginResponse> responses = Objects.requireNonNull(deserializeResponse(response, ProxmoxLoginResponse.class));
        if (!responses.isEmpty()) {
            ProxmoxLoginResponse result = responses.get(0);
            if (result.getTicket() != null) {
                return result;
            }
        }

        return null;
    }

    public ProxmoxUserAuthResponse verfyAuthenticatedPromoxUser(Cookie cookie) {
        String response = webClient
                .get()
                .uri("/api2/json/version")
                .header(HttpHeaders.COOKIE, cookie.getName() + "=" + cookie.getValue())
                .retrieve()
                .bodyToMono(String.class)
                .onErrorResume(throwable -> Mono.just(""))
                .block();

        List<ProxmoxUserAuthResponse> responses = Objects.requireNonNull(deserializeResponse(response, ProxmoxUserAuthResponse.class));
        if (!responses.isEmpty()) {
            ProxmoxUserAuthResponse resp = responses.get(0);
            if (resp.getVersion() != null) {
                return resp;
            }
        }

        return null;
    }

    public List<Map> getPromoxNodeDetails(Cookie cookie, String path) {
        String response = webClient
                .get()
                .uri("api2/json/nodes/" + path)
                .header(HttpHeaders.COOKIE, cookie.getName() + "=" + cookie.getValue())
                .retrieve()
                .bodyToMono(String.class)
                .onErrorResume(throwable -> Mono.just(""))
                .block();

        return deserializeResponse(response, Map.class);
    }

    public List<Map> getPromoxClusterDetails(Cookie cookie) {
        String response = webClient
                .get()
                .uri("api2/json/cluster/resources")
                .header(HttpHeaders.COOKIE, cookie.getName() + "=" + cookie.getValue())
                .retrieve()
                .bodyToMono(String.class)
                .onErrorResume(throwable -> Mono.just(""))
                .block();

        return deserializeResponse(response, Map.class);
    }

    public boolean createVm(Cookie cookie, String csrfToken, Request request) {
        String response = null;
        String requestBody = "vmid=" + URLEncoder.encode(request.getVmDetails().getVmId().toString(), StandardCharsets.UTF_8) +
                "&name=" + URLEncoder.encode(request.getVmDetails().getVmName(), StandardCharsets.UTF_8) +
                "&ide2=" + URLEncoder.encode("cdrom,media=cdrom", StandardCharsets.UTF_8) +
                "&ostype=l26" +
                "&scsihw=" + URLEncoder.encode("virtio-scsi-single", StandardCharsets.UTF_8) +
                "&scsi0=" + URLEncoder.encode("local-zfs:" + request.getVmDetails().getStorageGb() + ",iothread=on", StandardCharsets.UTF_8) +
                "&sockets=1" +
                "&cores=" + URLEncoder.encode(request.getVmDetails().getProcessors().toString(), StandardCharsets.UTF_8) +
                "&numa=0" +
                "&cpu=" + URLEncoder.encode("x86-64-v2-AES", StandardCharsets.UTF_8) +
                "&memory=" + URLEncoder.encode(request.getVmDetails().getMemoryGb().toString(), StandardCharsets.UTF_8) +
                "&net0=" + URLEncoder.encode("virtio,bridge=k8snet,firewall=1", StandardCharsets.UTF_8);


        try {
            response = webClient.post()
                    .uri("/api2/extjs/nodes/" + request.getNode() + "/qemu")
                    .header(HttpHeaders.COOKIE, cookie.getName() + "=" + cookie.getValue())
                    .header(HttpHeaders.ACCEPT, "*/*")
                    .header(HttpHeaders.ACCEPT_ENCODING, "gzip, deflate, br, zstd")
                    .header(HttpHeaders.ACCEPT_LANGUAGE, "en-US,en;q=0.9,el;q=0.8")
                    .header("CSRFPreventionToken", csrfToken)
                    .contentType(MediaType.valueOf(MediaType.APPLICATION_FORM_URLENCODED_VALUE))
                    .body(BodyInserters.fromValue(requestBody))
                    .retrieve()
                    .bodyToMono(String.class)
                    .onErrorResume(Mono::error)
                    .block();
        } catch (Exception ex) {
            logger.error("Error: {}", ex.getMessage(), ex);
        }
        return response != null && response.contains("success");
    }

    public void updateVmConfig(Cookie cookie, String csrfToken, UpdateVmConf updateVmConf) {
        webClient.put()
                .uri("/api2/json/nodes/" + updateVmConf.getNode() + "/qemu/" +
                        updateVmConf.getVmId() + "/config")
                .header(HttpHeaders.COOKIE, cookie.getName() + "=" + cookie.getValue())
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .header("CSRFPreventionToken", csrfToken)
                .body(BodyInserters.fromValue("memory=" + updateVmConf.getMemory() +
                        "&delete=" + updateVmConf.getDelete() + "&digest=" + updateVmConf.getDigest()))
                .retrieve()
                .bodyToMono(String.class)
                .onErrorResume(throwable -> Mono.just(""))
                .block();
    }

    public String getVmConfig(Cookie cookie, UpdateVmConf updateVmConf) {
        String response = webClient
                .get()
                .uri("/api2/json/nodes/" + updateVmConf.getNode() + "/qemu/" +
                        updateVmConf.getVmId() + "/config")
                .header(HttpHeaders.COOKIE, cookie.getName() + "=" + cookie.getValue())
                .retrieve()
                .bodyToMono(String.class)
                .onErrorResume(throwable -> Mono.just(""))
                .block();

        List<Map> maps = deserializeResponse(response, Map.class);
        if (maps != null && !maps.isEmpty()) {
            return (String) maps.get(0).get("digest");
        }
        return null;
    }

    public boolean deleteVm(Cookie cookie, String csrfToken, Request request) {
        String response = null;
        try {
            response = webClient
                    .delete()
                    .uri("api2/extjs/nodes/" + request.getNode() + "/qemu/" +
                            request.getVmDetails().getVmId() + "?purge=1&destroy-unreferenced-disks=1")
                    .header(HttpHeaders.COOKIE, cookie.getName() + "=" + cookie.getValue())
                    .header("CSRFPreventionToken", csrfToken)
                    .retrieve()
                    .bodyToMono(String.class)
                    .onErrorResume(Mono::error)
                    .block();
        } catch (Exception ex) {
            logger.error("Error: {}", ex.getMessage(), ex);
        }

        return response != null && response.contains("success");
    }


    private <T> List<T> deserializeResponse(String responseString, Class<T> responseClass) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseNode = objectMapper.readTree(responseString);
            List<T> responseArray = new ArrayList<>();
            if (!responseNode.isEmpty()) {
                if (responseNode.get("data") instanceof ArrayNode) {
                    ArrayNode arrayNode = (ArrayNode) responseNode.get("data");
                    for (JsonNode element : arrayNode) {
                        ObjectNode dataNode = (ObjectNode) element;
                        responseArray.add(objectMapper.treeToValue(dataNode, responseClass));
                    }
                } else {
                    ObjectNode dataNode = (ObjectNode) responseNode.get("data");
                    if (dataNode != null) {
                        responseArray.add(objectMapper.treeToValue(dataNode, responseClass));
                    }
                }
            }
            return responseArray;
        } catch (Exception ex) {
            logger.error("Error during deserialization: " + ex.getMessage());
            return null;
        }
    }

    public int generateRandomNumber(int min, int max, Set<Integer> excludedNumbers) {
        Random random = new Random();
        int randomNumber;

        do {
            randomNumber = random.nextInt((max - min) + 1) + min;
        } while (excludedNumbers.contains(randomNumber));

        return randomNumber;
    }

    @Autowired
    public void setWebClient(WebClient webClient) {
        this.webClient = webClient;
    }

}
