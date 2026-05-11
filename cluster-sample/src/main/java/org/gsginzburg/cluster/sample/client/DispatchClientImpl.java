package org.gsginzburg.cluster.sample.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import org.gsginzburg.cluster.framework.autoconfigure.ClusterProperties;
import org.gsginzburg.shared.client.DispatchClient;
import org.gsginzburg.shared.dto.ClusterInfo;
import org.gsginzburg.shared.dto.ClusterTenantInfo;
import org.gsginzburg.shared.dto.ClusterUserInfo;

@Slf4j
@Component
@RequiredArgsConstructor
public class DispatchClientImpl implements DispatchClient {

    private final ClusterProperties clusterProperties;
    private final RestClient.Builder restClientBuilder;

    @Override
    public ClusterTenantInfo getTenantInfo(String tenantId) {
        return getRestClient()
                .get()
                .uri("/api/dispatch/tenant/{id}", tenantId)
                .retrieve()
                .body(ClusterTenantInfoResponse.class)
                .data();
    }

    @Override
    public ClusterUserInfo getUserInfo(String userId) {
        return getRestClient()
                .get()
                .uri("/api/dispatch/user/{id}", userId)
                .retrieve()
                .body(ClusterUserInfoResponse.class)
                .data();
    }

    @Override
    public ClusterInfo getClusterInfo(String clusterId) {
        return getRestClient()
                .get()
                .uri("/api/dispatch/cluster/{id}", clusterId)
                .retrieve()
                .body(ClusterInfoResponse.class)
                .data();
    }

    private RestClient getRestClient() {
        // Forward the current JWT as Bearer token
        String token = getCurrentToken();
        return restClientBuilder
                .baseUrl(clusterProperties.getDispatchUrl())
                .defaultHeader("Authorization", "Bearer " + token)
                .build();
    }

    private String getCurrentToken() {
        // Token is stored in security context; retrieve from request attribute or context
        var auth = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        if (auth != null && auth.getCredentials() instanceof String token) {
            return token;
        }
        return "";
    }

    // Inner response wrapper types matching ApiResponse<T>
    record ClusterTenantInfoResponse(boolean success, ClusterTenantInfo data, String error) {}
    record ClusterUserInfoResponse(boolean success, ClusterUserInfo data, String error) {}
    record ClusterInfoResponse(boolean success, ClusterInfo data, String error) {}
}
