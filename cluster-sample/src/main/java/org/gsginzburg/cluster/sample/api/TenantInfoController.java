package org.gsginzburg.cluster.sample.api;

import java.util.Map;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.gsginzburg.cluster.framework.autoconfigure.ClusterProperties;
import org.gsginzburg.cluster.framework.datasource.TenantContextHolder;
import org.gsginzburg.cluster.sample.client.DispatchClientImpl;
import org.gsginzburg.shared.dto.ApiResponse;
import org.gsginzburg.shared.dto.ClusterInfo;
import org.gsginzburg.shared.dto.ClusterTenantInfo;
import org.gsginzburg.shared.dto.ClusterUserInfo;
import org.gsginzburg.shared.security.JwtClaims;

@RestController
@RequestMapping("/api/app/context")
@RequiredArgsConstructor
public class TenantInfoController {

    private final DispatchClientImpl dispatchClient;
    
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getContext(
            @AuthenticationPrincipal JwtClaims claims) {
        var ctx = TenantContextHolder.get();

        ClusterTenantInfo tenantInfo = dispatchClient.getTenantInfo(claims.getTenantId());
        ClusterUserInfo userInfo = dispatchClient.getUserInfo(claims.getSub());
        ClusterInfo clusterInfo = dispatchClient.getClusterInfo(claims.getClusterId());

        Map<String, Object> context = Map.of(
                "tenant", tenantInfo,
                "user", userInfo,
                "cluster", clusterInfo,
                "localContext", Map.of(
                        "userId", ctx != null ? ctx.getUserId() : "unknown",
                        "schemaName", ctx != null ? ctx.getTenantId() : "unknown"
                )
        );
        return ResponseEntity.ok(ApiResponse.ok(context));
    }
}
