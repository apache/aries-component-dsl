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

package org.apache.aries.component.dsl.function;

import java.util.function.Function;

/**
* @generated
*/
@FunctionalInterface
public interface Function6<A,B,C,D,E,F,RESULT> {
    
    public RESULT apply(A a,B b,C c,D d,E e,F f);
    
    default public Function<A,Function<B,Function<C,Function<D,Function<E,Function<F,RESULT>>>>>> curried() {
        return a -> b -> c -> d -> e -> f -> apply(a,b,c,d,e,f);
    }
}
