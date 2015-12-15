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
import com.google.common.base.Splitter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import rx.Observable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * {@link ClusterDiscovery} implementation that returns list of clusters from a configuration file.
 * This does not currently support updating the configuration at runtime
 */
@ConfigurationProperties(prefix="turbine")
public class ConfigurationClusterDiscovery implements ClusterDiscovery {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigurationClusterDiscovery.class);
    private static final Splitter CLUSTER_SPLITTER = Splitter.on('|').omitEmptyStrings().trimResults();
    private List<String> servers = new ArrayList<>(); // this is set by Spring Boot


    public ConfigurationClusterDiscovery() {
    }

    @VisibleForTesting
    ConfigurationClusterDiscovery(Set<String> servers) {
        this.servers = new ArrayList<>(servers);
    }

    @Override
    public Observable<Cluster> getClusters() {
        return Observable.from(getCurrentClusters()).concatWith(Observable.never());
    }

    public Collection<Cluster> getCurrentClusters() {

        LOG.debug("Configured with {}", servers);
        return servers.stream()
                .map(this::parseCluster).filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private Cluster parseCluster(String s) {
        List<String> clusterInfo = CLUSTER_SPLITTER.splitToList(s);
        if (clusterInfo.size() != 2) {
            LOG.warn("{} is not valid, should be in the form <cluster name>|<cluster url>", s);
            return null;
        } else {
            return new Cluster(clusterInfo.get(0), clusterInfo.get(1));
        }
    }


    // these are here for spring
    public List<String> getServers() {
        return servers;
    }

    public void setServers(List<String> servers) {
        Objects.requireNonNull(servers);
        this.servers = servers;
    }
}
