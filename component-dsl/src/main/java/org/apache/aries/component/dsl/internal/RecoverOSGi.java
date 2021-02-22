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

import java.util.function.BiFunction;

/**
 * @author Carlos Sierra Andrés
 */
public class RecoverOSGi<T> extends OSGiImpl<T> {

    public RecoverOSGi(OSGi<T> program, BiFunction<T, Exception, T> error) {
        super((executionContext, op) -> program.run(
            executionContext, new RecoverOSGi.RecoverPublisher<>(op, error)));
    }

    private static class RecoverPublisher<T> implements Publisher<T> {
        private final Publisher<? super T> op;
        private final BiFunction<T, Exception, T> error;


        public RecoverPublisher(
            Publisher<? super T> op,
            BiFunction<T, Exception, T> error) {

            this.op = op;
            this.error = error;
        }

        @Override
        public OSGiResult publish(T t) {
            try {
                return op.publish(t);
            } catch (Exception e) {
                throw new PublisherRethrowException(e);
            }
        }

        @Override
        public <E extends Exception> OSGiResult error(T t, Exception e) throws E {
            return op.publish(error.apply(t, e));
        }
    }

}
