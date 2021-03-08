/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.aries.component.dsl.internal;

import org.apache.aries.component.dsl.OSGi;
import org.apache.aries.component.dsl.OSGiResult;
import org.apache.aries.component.dsl.Publisher;
import org.apache.aries.component.dsl.Transformer;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class OnceTransformer<T> implements Transformer<T, T> {

    @Override
    public Publisher<T> transform(Publisher<? super T> op) {
        AtomicInteger count = new AtomicInteger();

        AtomicReference<OSGiResult> terminator = new AtomicReference<>();

        return t -> {
            if (count.getAndIncrement() == 0) {
                UpdateSupport.deferPublication(
                    () -> terminator.set(op.apply(t)));
            }

            return new OSGiResultImpl(() -> {
                if (count.decrementAndGet() == 0) {
                    UpdateSupport.deferTermination(() -> {
                        Runnable runnable = terminator.getAndSet(OSGi.NOOP);

                        runnable.run();
                    });
                }},
                () -> terminator.get().update()
            );
        };
    }

}
