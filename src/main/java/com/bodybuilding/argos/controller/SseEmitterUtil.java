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

import com.google.common.base.Throwables;
import org.apache.catalina.connector.ClientAbortException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;

/**
 * Utility class to simplify using SseEmitter with Observables.
 */
public class SseEmitterUtil {
    private static final Logger LOG = LoggerFactory.getLogger(SseEmitterUtil.class);

    private SseEmitterUtil() {
    }

    /**
     * Binds the supplied observable to the SseEmitter. onNext will call the send method on SseEmitter, the Observable
     * subscription will automatically be cancelled when the emitter completes or times out.
     * @param emitter SseEmitter
     * @param observable Observable that will supply the data
     * @return Subscription to the Observable
     */
    public static <T> Subscription bindObservable(SseEmitter emitter, Observable<T> observable) {
        Subscription subscription = observable.subscribe(new Subscriber<T>() {
            @Override
            public void onCompleted() {
                emitter.complete();
            }

            @Override
            public void onError(Throwable e) {
                LOG.warn("Error from stream observable", e);
                emitter.completeWithError(e);
            }

            @Override
            public void onNext(T t) {
                emitSse(emitter, t);
            }
        });
        bindUnsubscribe(emitter, subscription);
        return subscription;
    }

    /**
     * Unsubscribes the subscription when the emitter completes or times out
     * @param emitter
     * @param subscription
     */
    public static void bindUnsubscribe(SseEmitter emitter, Subscription subscription) {
        emitter.onCompletion(subscription::unsubscribe);
        emitter.onTimeout(subscription::unsubscribe);
    }

    /**
     * Emits data via the emitter while ignoring normal exceptions that occur during client disconnection.
     * @param emitter
     * @param data
     */
    public static <T> void emitSse(SseEmitter emitter, T data) {
        internalEmit(emitter, data, null);
    }

    /**
     * Emits data via the emitter while ignoring normal exceptions that occur during client disconnection.
     * @param emitter
     * @param data
     */
    public static <T> void emitSse(SseEmitter emitter, T data, MediaType mediaType) {
        internalEmit(emitter, data, mediaType);
    }

    private static <T> void internalEmit(SseEmitter emitter, T data, MediaType mediaType) {
        try {
            if(mediaType == null) {
                emitter.send(data);
            } else {
                emitter.send(data, mediaType);
            }
        } catch (ClientAbortException ignored) {
        } catch (IllegalStateException e) {
            if(!e.getMessage().contains("ResponseBodyEmitter is already set complete")) {
                throw e;
            }
        } catch (Throwable e) {
            throw Throwables.propagate(e);
        }
    }
}
