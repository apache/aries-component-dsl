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

package org.apache.aries.component.dsl.update;

import java.util.function.Consumer;

/**
 * @author Carlos Sierra Andrés
 */
public final class UpdateQuery<T> {
    public final From<T>[] froms;

    @SafeVarargs
    public UpdateQuery(From<T>... froms) {
        this.froms = froms;
    }

    @SafeVarargs
    public static <T> UpdateQuery<T> onUpdate(From<T> ... froms) {
        return new UpdateQuery<>(froms);
    }

    public static class From<T> {
        public final UpdateSelector selector;
        public final Consumer<T> consumer;

        public From(UpdateSelector selector, Consumer<T> consumer) {
            this.selector = selector;
            this.consumer = consumer;
        }

        public static <T> From<T> from(UpdateSelector selector, Consumer<T> consumer) {
            return new From<>(selector, consumer);
        }
    }
}
