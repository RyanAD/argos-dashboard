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

package com.bodybuilding.argos.controller;

import com.bodybuilding.argos.discovery.ClusterRegistry;
import com.bodybuilding.argos.discovery.HystrixClusterMonitor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import rx.Observable;
import rx.schedulers.Schedulers;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Proxies Server Sent Events for a single Hystrix cluster. This is a proxy of the turbine stream.
 */
@RestController
public class TurbineStreamController {
    private final ClusterRegistry clusterRegistry;
    private final Observable<Boolean> shutdown;

    @Autowired
    public TurbineStreamController(ClusterRegistry clusterRegistry, Observable<Boolean> shutdown) {
        this.clusterRegistry = Objects.requireNonNull(clusterRegistry);
        this.shutdown = Objects.requireNonNull(shutdown);
    }

    @RequestMapping("/turbine-stream/{cluster}")
    public ResponseEntity<SseEmitter> streamHystrix(@PathVariable("cluster") String cluster) {
        Optional<HystrixClusterMonitor> clusterMonitor = clusterRegistry.getCluster(cluster);
        if(!clusterMonitor.isPresent()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        final SseEmitter emitter = new SseEmitter(TimeUnit.DAYS.toMillis(45));

        SseEmitterUtil.bindObservable(emitter, clusterMonitor.get().observeJson().takeUntil(shutdown)
                .subscribeOn(Schedulers.io()));

        return ResponseEntity.ok(emitter);
    }
}
