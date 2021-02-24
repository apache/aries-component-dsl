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

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.function.Function;

/**
 * @author Carlos Sierra Andrés
 */
public class DistributeOSGiImpl<T, S> extends BaseOSGiImpl<S> {

    @SafeVarargs
    public DistributeOSGiImpl(OSGi<T> operation, Function<OSGi<T>, OSGi<S>>... funs) {

        super((executionContext, publisher) -> {
            Pad<T, S>[] pads = new Pad[funs.length];

            for (int i = 0; i < funs.length; i++) {
                pads[i] = new Pad<>(executionContext, funs[i], publisher);
            }

            OSGiResult result = operation.run(
                executionContext,
                publisher.pipe(t -> {
                    List<Runnable> terminators = new ArrayList<>(funs.length);

                    int i = 0;

                    try {
                        for (; i < funs.length; i++) {
                            terminators.add(pads[i].publish(t));
                        }
                    }
                    catch (Exception e) {
                        cleanUp(terminators);

                        throw e;
                    }

                    return () -> cleanUp(terminators);
                }));

            return () -> {
                result.close();

                for (Pad<T, S> pad : pads) {
                    try {
                        pad.close();
                    }
                    catch (Exception e) {
                    }
                }
            };
        });
    }

    private static void cleanUp(List<Runnable> terminators) {
        ListIterator<Runnable> iterator =
            terminators.listIterator(terminators.size());

        while (iterator.hasPrevious()) {
            try {
                iterator.previous().run();
            }
            catch (Exception e) {
            }
        }
    }

}
