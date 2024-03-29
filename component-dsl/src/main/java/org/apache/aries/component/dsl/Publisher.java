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

package org.apache.aries.component.dsl;

import java.util.function.Function;

/**
 * @author Carlos Sierra Andrés
 */
public interface Publisher<T> extends Function<T, Runnable> {

    default OSGiResult apply(T t) {
        return publish(t);
    }

    OSGiResult publish(T t);

    @SuppressWarnings("unchecked")
    default <E extends Exception> OSGiResult error(T t, Exception e) throws E {
        throw (E)e;
    }

    default <S> Publisher<S> pipe(Function<? super S, OSGiResult> next) {

        return new Publisher<S>() {
            @Override
            public OSGiResult publish(S t) {
                return next.apply(t);
            }

            @SuppressWarnings("unchecked")
            @Override
            public <E extends Exception> OSGiResult error(S s, Exception e) throws E {
                return Publisher.this.error((T)s, e);
            }
        };

    }

}
