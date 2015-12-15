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
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.Collection;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

public class ClusterListDiscoveryTest {

    @Test
    public void testGetClusters() {
        String mockBody = "" +
                "[\n" +
                "{\n" +
                "\"name\": \"test1\",\n" +
                "\"link\": \"http://meh.com\",\n" +
                "\"turbineStream\": \"http://meh.com/turbine/turbine.stream?cluster=test1\"\n" +
                "},\n" +
                "{\n" +
                "\"name\": \"test2\",\n" +
                "\"link\": \"http://meh2.com\",\n" +
                "\"turbineStream\": \"http://meh2.com:8080/turbine/turbine.stream?cluster=test2\"\n" +
                "}" +
                "]";


        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer mockServer = MockRestServiceServer.createServer(restTemplate);
        mockServer.expect(requestTo("http://127.0.0.1")).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(mockBody, MediaType.APPLICATION_JSON));
        ClusterListDiscovery discovery = new ClusterListDiscovery(Sets.newHashSet("http://127.0.0.1"), restTemplate);
        Collection<Cluster> clusters = discovery.getCurrentClusters();
        mockServer.verify();
        assertNotNull(clusters);
        assertEquals(2, clusters.size());
    }

    @Test
    public void testGetClusters_withHttpError() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer mockServer = MockRestServiceServer.createServer(restTemplate);
        mockServer.expect(requestTo("http://127.0.0.1")).andExpect(method(HttpMethod.GET))
                .andRespond(withServerError());
        ClusterListDiscovery discovery = new ClusterListDiscovery(Sets.newHashSet("http://127.0.0.1"), restTemplate);
        Collection<Cluster> clusters = discovery.getCurrentClusters();
        mockServer.verify();
        assertNotNull(clusters);
        assertEquals(0, clusters.size());
    }

    @Test
    public void testGetClusters_withMultipleUrls() {
        String mockBody = "" +
                "[\n" +
                "{\n" +
                "\"name\": \"test1\",\n" +
                "\"link\": \"http://meh.com\",\n" +
                "\"turbineStream\": \"http://meh.com/turbine/turbine.stream?cluster=test1\"\n" +
                "},\n" +
                "{\n" +
                "\"name\": \"test2\",\n" +
                "\"link\": \"http://meh2.com\",\n" +
                "\"turbineStream\": \"http://meh2.com:8080/turbine/turbine.stream?cluster=test2\"\n" +
                "}" +
                "]";

        String mockBody2 = "" +
                "[\n" +
                "{\n" +
                "\"name\": \"test3\",\n" +
                "\"link\": \"http://meh.com\",\n" +
                "\"turbineStream\": \"http://meh.com/turbine/turbine.stream?cluster=test1\"\n" +
                "},\n" +
                "{\n" +
                "\"name\": \"test4\",\n" +
                "\"link\": \"http://meh2.com\",\n" +
                "\"turbineStream\": \"http://meh2.com:8080/turbine/turbine.stream?cluster=test2\"\n" +
                "}" +
                "]";


        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer mockServer = MockRestServiceServer.createServer(restTemplate);
        mockServer.expect(requestTo("http://127.0.0.1")).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(mockBody, MediaType.APPLICATION_JSON));
        mockServer.expect(requestTo("http://127.0.0.2")).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(mockBody2, MediaType.APPLICATION_JSON));

        Set<String> urls = Sets.newLinkedHashSet();
        urls.add("http://127.0.0.1");
        urls.add("http://127.0.0.2");
        ClusterListDiscovery discovery = new ClusterListDiscovery(urls, restTemplate);
        Collection<Cluster> clusters = discovery.getCurrentClusters();
        mockServer.verify();
        assertNotNull(clusters);
        assertEquals(4, clusters.size());
    }
}