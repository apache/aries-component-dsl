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

import org.apache.aries.component.dsl.*;

/**
 * @author Carlos Sierra Andrés
 */
public class OSGiImpl<T> extends BaseOSGiImpl<T> {

    protected OSGiImpl(OSGiRunnable<T> operation) {
        super(new ErrorHandlerOSGiRunnable<>(operation));
    }

    @SuppressWarnings("unchecked")
    static <T extends Throwable> void rethrow(Throwable t) throws T {
        throw (T)t;
    }

    public static <T> OSGi<T> create(OSGiRunnable<T> runnable) {
        return new OSGiImpl<>(runnable);
    }

    protected static class ErrorHandlerOSGiRunnable<T>
        implements OSGiRunnable<T> {

        private final OSGiRunnable<T> operation;

        public ErrorHandlerOSGiRunnable(OSGiRunnable<T> operation) {
            this.operation = operation;
        }

        @Override
        public OSGiResult run(
            ExecutionContext ec, Publisher<? super T> op) {

            return operation.run(ec,
                op.pipe(t -> {
                    try {
                        return op.publish(t);
                    } catch (PublisherRethrowException pre) {
                        rethrow(pre.getCause());

                        return null;
                    } catch (Exception e) {
                        return op.error(t, e);
                    }
                }));
        }
    }
}


