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

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Carlos Sierra Andrés
 */
public class UpdateSupportTest {

    @Test
    public void testDefer() {
        List<Integer> list = new ArrayList<>();

        UpdateSupport.runUpdate(() -> {
            list.add(1);

            UpdateSupport.deferPublication(() -> list.add(4));
            UpdateSupport.deferTermination(() -> list.add(3));

            list.add(2);
        });

        assertEquals(Arrays.asList(1, 2, 3, 4), list);
    }

    @Test
    public void testDeferOutsideUpdate() {
        List<Integer> list = new ArrayList<>();

        list.add(1);

        UpdateSupport.deferPublication(() -> list.add(2));
        UpdateSupport.deferTermination(() -> list.add(3));

        list.add(4);

        assertEquals(Arrays.asList(1, 2, 3, 4), list);
    }

    @Test
    public void testDeferNestedStack() {
        List<Integer> list = new ArrayList<>();

        UpdateSupport.runUpdate(() -> {
            list.add(1);

            UpdateSupport.deferTermination(() -> list.add(9));

            UpdateSupport.runUpdate(() -> {
                list.add(2);

                UpdateSupport.deferTermination(() -> {
                    list.add(3);

                    UpdateSupport.deferTermination(() -> {
                        UpdateSupport.deferPublication(() -> list.add(4));
                        UpdateSupport.deferTermination(() -> list.add(5));

                        UpdateSupport.runUpdate(() -> {
                            UpdateSupport.deferPublication(() -> list.add(7));
                            UpdateSupport.deferTermination(() -> list.add(6));
                        });
                    });

                    list.add(8);
                });
            });

            UpdateSupport.deferPublication(() -> list.add(10));
        });

        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), list);
    }

    @Test
    public void testDeferTerminationNestedStackWithUpdate() {
        List<Integer> list = new ArrayList<>();

        UpdateSupport.runUpdate(() -> {
            list.add(1);

            UpdateSupport.deferTermination(() -> list.add(9));

            UpdateSupport.runUpdate(() -> {
                list.add(2);

                UpdateSupport.deferTermination(() -> {
                    UpdateSupport.runUpdate(
                        () -> {
                            list.add(4);

                            UpdateSupport.deferTermination(
                                () -> UpdateSupport.runUpdate(
                                    () -> {
                                        list.add(6);

                                        UpdateSupport.deferTermination(
                                            () -> list.add(8));

                                        list.add(7);
                                    }));

                            list.add(5);
                    });
                });

                list.add(3);
            });
        });

        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9), list);
    }

    @Test
    public void testDeferTerminationStack() {
        List<Integer> list = new ArrayList<>();

        UpdateSupport.runUpdate(() -> {
            list.add(1);

            UpdateSupport.deferTermination(() -> list.add(6));

            UpdateSupport.runUpdate(() -> {
                list.add(2);

                UpdateSupport.runUpdate(() -> {
                    list.add(3);

                    UpdateSupport.deferTermination(() -> list.add(4));
                });

                UpdateSupport.deferTermination(() -> list.add(5));
            });
        });

        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), list);
    }

}
