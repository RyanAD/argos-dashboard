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

import org.apache.catalina.connector.ClientAbortException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import rx.Observable;

import java.io.IOException;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SseEmitterUtilTest {
    @Mock
    SseEmitter emitter;

    @Test
    public void testBindObservable() throws IOException {
        Observable<Integer> obs = Observable.range(1, 100);
        SseEmitterUtil.bindObservable(emitter, obs);
        verify(emitter, times(100)).send(anyInt());
        verify(emitter).complete();
    }

    @Test
    public void testBindObservable_exceptions() throws IOException {
        Observable<Integer> obs = Observable.range(1, 100);
        doThrow(ClientAbortException.class).when(emitter).send(anyInt());
        SseEmitterUtil.bindObservable(emitter, obs);
        verify(emitter, times(100)).send(anyInt());

        reset(emitter);

        obs = Observable.range(1, 100);
        doThrow(new IllegalStateException("ResponseBodyEmitter is already set complete")).when(emitter).send(anyInt());
        SseEmitterUtil.bindObservable(emitter, obs);
        verify(emitter, times(100)).send(anyInt());

        reset(emitter);

        obs = Observable.range(1, 100);
        doThrow(new IllegalStateException("Unit Test")).when(emitter).send(anyInt());
        SseEmitterUtil.bindObservable(emitter, obs);
        verify(emitter, times(1)).send(anyInt());
        verify(emitter).completeWithError(any());

        reset(emitter);

        obs = Observable.range(1, 100);
        doThrow(new RuntimeException("Unit Test")).when(emitter).send(anyInt());
        SseEmitterUtil.bindObservable(emitter, obs);
        verify(emitter, times(1)).send(anyInt());
        verify(emitter).completeWithError(any());
    }

    @Test
    public void testBindObservable_unsubscribe() throws IOException {
        Observable<Integer> obs = Observable.never();

        SseEmitterUtil.bindObservable(emitter, obs);
        verify(emitter).onCompletion(any(Runnable.class));
        verify(emitter).onTimeout(any(Runnable.class));
    }

    @Test
    public void testBindObservable_mediaType() throws IOException {
        SseEmitterUtil.emitSse(emitter, "test", MediaType.APPLICATION_ATOM_XML);
        verify(emitter).send(eq("test"), eq(MediaType.APPLICATION_ATOM_XML));
    }
}