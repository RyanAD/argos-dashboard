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

import com.google.common.collect.Sets;
import org.junit.Test;

import java.util.Collection;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ConfigurationClusterDiscoveryTest {

    @Test
    public void testGetClusters() {
        ConfigurationClusterDiscovery discovery = new ConfigurationClusterDiscovery(Sets.newHashSet(
                "cluster1|http://www.cluster1.com/turbine.stream",
                "cluster1|http://www.cluster1-dupe.com/turbine.stream", // TODO make sure same cluster name is aggregated properly
                "cluster2|http://www.cluster2.com/turbine.stream"
        ));
        Collection<Cluster> clusters = discovery.getCurrentClusters();
        assertNotNull(clusters);
        assertEquals(3, clusters.size());
    }

    @Test
    public void testGetClusters_invalidEntry() {
        ConfigurationClusterDiscovery discovery = new ConfigurationClusterDiscovery(Sets.newHashSet(
                "cluster1|http://www.cluster1.com/turbine.stream",
                "cluster2;http://www.cluster2.com/turbine.stream",
                "cluster3|http://www.cluster3.com/turbine.stream|cluster3"
        ));
        Collection<Cluster> clusters = discovery.getCurrentClusters();
        assertNotNull(clusters);
        assertEquals(1, clusters.size());
    }

    @Test
    public void testGetClusters_emptyList() {
        ConfigurationClusterDiscovery discovery = new ConfigurationClusterDiscovery(Collections.emptySet());
        Collection<Cluster> clusters = discovery.getCurrentClusters();
        assertNotNull(clusters);
        assertEquals(0, clusters.size());
    }
}