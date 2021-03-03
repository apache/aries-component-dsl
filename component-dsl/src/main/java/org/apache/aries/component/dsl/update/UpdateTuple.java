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

import org.apache.aries.component.dsl.OSGi;

import java.util.function.BiFunction;

public final class UpdateTuple<T> {

    public final UpdateSelector updateSelector;
    public final T t;

    public UpdateTuple(UpdateSelector updateSelector, T t) {
        this.updateSelector = updateSelector;
        this.t = t;
    }

    public T getT() {
        return t;
    }

    public static <S, R> OSGi<R> flatMap(
        OSGi<UpdateTuple<S>> tuple, BiFunction<UpdateSelector, S, OSGi<R>> biFunction) {

        return tuple.flatMap(updateTuple -> biFunction.apply(updateTuple.updateSelector, updateTuple.t));
    }

    public static <T> UpdateTuple<T> fromStatic(T t) {
        return new UpdateTuple<>(UpdateSelector.STATIC, t);
    }

}
