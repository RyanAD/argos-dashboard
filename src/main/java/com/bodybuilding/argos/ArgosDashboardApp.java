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

package com.bodybuilding.argos;

import com.bodybuilding.argos.discovery.ClusterDiscovery;
import com.bodybuilding.argos.discovery.DefaultHystrixClusterMonitorFactory;
import com.bodybuilding.argos.discovery.HystrixClusterMonitorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.web.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.WebApplicationInitializer;
import rx.Observable;
import rx.schedulers.Schedulers;
import rx.subjects.ReplaySubject;
import rx.subjects.SerializedSubject;
import rx.subjects.Subject;

import javax.annotation.PreDestroy;

@SpringBootApplication
@EnableScheduling
public class ArgosDashboardApp extends SpringBootServletInitializer implements WebApplicationInitializer {
    private static final Logger LOG = LoggerFactory.getLogger(ArgosDashboardApp.class);

    @Value("${discovery.impl}")
    private String discoveryImplClass;

    @Autowired
    private ClusterDiscovery discovery;

    private Subject<Boolean, Boolean> shutdown = new SerializedSubject<>(ReplaySubject.create());


    public static void main(String[] args) {
        SpringApplication.run(ArgosDashboardApp.class, args);
    }


    @Bean
    public ClusterDiscovery clusterDiscovery() throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        if(discovery == null) {
            LOG.info("Creating ClusterDiscovery instance from {}", discoveryImplClass);
            return (ClusterDiscovery) Class.forName(discoveryImplClass).newInstance();
        } else {
            return null;
        }
    }

    @Bean
    public HystrixClusterMonitorFactory clusterMonitorFactory() {
        return new DefaultHystrixClusterMonitorFactory();
    }

    @Bean
    public Observable<Boolean> observeShutdown() {
        return shutdown.asObservable().doOnEach(i -> LOG.info("Sending shutdown signal"));
    }

    @PreDestroy
    public void shutdown() {
        shutdown.onNext(true);
        shutdown.onCompleted();
        Schedulers.shutdown();
    }
}
