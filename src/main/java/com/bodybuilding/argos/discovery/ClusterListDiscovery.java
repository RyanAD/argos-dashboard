/*
 * Copyright (C) 2015 Bodybuilding.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bodybuilding.argos.discovery;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.Netty4ClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * {@link ClusterDiscovery} implementation that uses ClusterListServlet
 * from the Bodybuilding.com Turbine Plugins.
 *
 * @see <a href="https://github.com/bbcom/turbine-plugins/">https://github.com/bbcom/turbine-plugins/</a>
 * @see <a href="https://github.com/bbcom/hystrix-dashboard-quickstart">https://github.com/bbcom/hystrix-dashboard-quickstart</a>
 */
@ConfigurationProperties(prefix = "turbine.clusterlist")
public class ClusterListDiscovery extends AbstractClusterDiscovery {
    private static final long UPDATE_INTERVAL = 10_000;
    private static final Logger LOG = LoggerFactory.getLogger(ClusterListDiscovery.class);
    private List<String> servers = new ArrayList<>(); // set by spring boot
    private RestTemplate restTemplate;

    public ClusterListDiscovery() {
        super(UPDATE_INTERVAL, TimeUnit.MILLISECONDS);
        Netty4ClientHttpRequestFactory requestFactory = new Netty4ClientHttpRequestFactory();
        requestFactory.setConnectTimeout(10_000);
        requestFactory.setReadTimeout(10_000);
        this.restTemplate = new RestTemplate(requestFactory);
    }

    public ClusterListDiscovery(RestTemplate restTemplate) {
        super(UPDATE_INTERVAL, TimeUnit.MILLISECONDS);
        this.restTemplate = restTemplate;
    }

    @VisibleForTesting
    ClusterListDiscovery(Collection<String> servers, RestTemplate restTemplate) {
        super(UPDATE_INTERVAL, TimeUnit.MILLISECONDS);
        Objects.requireNonNull(servers);
        LOG.debug("Configured with {}", servers);
        this.servers = new ArrayList<>(servers);
        this.restTemplate = restTemplate;
    }

    @Override
    public Collection<Cluster> getCurrentClusters() {
        if(servers.isEmpty()) {
            LOG.warn("No URLs Configured, is 'turbine.clusterlist.servers' property set?");
        }
        return servers.stream()
                .flatMap(u -> getClustersFromURL(u).stream())
                .collect(Collectors.toSet());
    }

    private Collection<Cluster> getClustersFromURL(String url) {
        Set<Cluster> clusters = new HashSet<>();

        try {
            ResponseEntity<List<ClusterInfo>> response =
                    restTemplate.exchange(url,
                            HttpMethod.GET, null, new ParameterizedTypeReference<List<ClusterInfo>>() {
                            });

            if (response.getStatusCode().value() != 200) {
                throw new RuntimeException("Failed to request clusters from " + url + ", return code: " +
                        response.getStatusCode().value());
            }

            response.getBody().stream()
                    .filter(c -> !Strings.isNullOrEmpty(c.getName()) && !Strings.isNullOrEmpty(c.getTurbineStream()))
                    .map(c -> new Cluster(c.getName(), c.getTurbineStream()))
                    .forEach(clusters::add);

        } catch (Exception e) {
            LOG.warn("Failed getting clusters from {}", url, e);
        }

        return clusters;
    }

    /* for spring property injection */
    public List<String> getServers() {
        return servers;
    }

    public void setServers(List<String> servers) {
        Objects.requireNonNull(servers);
        this.servers = servers;
        LOG.debug("Configured with {}", servers);
    }

    private static class ClusterInfo {
        private String name;
        private String turbineStream;
        private String dashboardUrl;

        public String getName() {
            return name;
        }

        public String getTurbineStream() {
            return turbineStream;
        }

        public String getDashboardUrl() {
            return dashboardUrl;
        }
    }
}
