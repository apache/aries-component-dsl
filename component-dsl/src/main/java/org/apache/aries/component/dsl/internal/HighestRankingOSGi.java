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

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * @author Carlos Sierra Andrés
 */
public class HighestRankingOSGi<T> extends OSGiImpl<T> {

    public HighestRankingOSGi(
        OSGi<T> previous, Comparator<? super T> comparator,
        Function<OSGi<T>, OSGi<T>> notHighest) {

        super((executionContext, publisher) -> {
            Comparator<Tuple<T>> comparing = Comparator.comparing(
                Tuple::getT, comparator);
            PriorityQueue<Tuple<T>> set = new PriorityQueue<>(
                comparing.reversed());
            AtomicReference<Tuple<T>> sent = new AtomicReference<>();

            Pad<T, T> notHighestPad = new Pad<>(
                executionContext, notHighest, publisher);

            OSGiResult result = previous.run(
                executionContext,
                publisher.pipe(t -> {
                    Tuple<T> tuple = new Tuple<>(t);

                    synchronized (set) {
                        set.add(tuple);

                        if (set.peek() == tuple) {
                            Tuple<T> old = sent.get();

                            if (old != null) {
                                old.osgiResult.run();
                            }

                            tuple.osgiResult = publisher.apply(t);

                            if (old != null) {
                                old.osgiResult = notHighestPad.publish(old.t);
                            }

                            sent.set(tuple);
                        } else {
                            tuple.osgiResult = notHighestPad.publish(t);
                        }
                    }

                    return new OSGiResultImpl(
                        () -> {
                            synchronized (set) {
                                Tuple<T> old = set.peek();

                                set.remove(tuple);

                                Tuple<T> current = set.peek();

                                tuple.osgiResult.run();

                                if (current != old && current != null) {
                                    current.osgiResult.run();
                                    current.osgiResult = publisher.apply(
                                        current.t);
                                    sent.set(current);
                                }
                                if (current == null) {
                                    sent.set(null);
                                }
                            }
                        },
                        us -> {
                            synchronized (set) {
                                Tuple<T> current = set.peek();

                                current.osgiResult.update(us);
                            }
                        }
                    );
                }));

            return new AggregateOSGiResult(result, notHighestPad);
        });
    }

    private static class Tuple<T> {

        Tuple(T t) {
            this.t = t;
        }

        public T getT() {
            return t;
        }
        T t;
        OSGiResult osgiResult;

    }

}
