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

package org.apache.aries.component.dsl.test;

import org.apache.aries.component.dsl.OSGi;
import org.apache.aries.component.dsl.OSGiResult;
import org.apache.aries.component.dsl.Publisher;
import org.apache.aries.component.dsl.internal.ProbeImpl;
import org.junit.Ignore;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.apache.aries.component.dsl.OSGi.combine;
import static org.apache.aries.component.dsl.OSGi.configurations;
import static org.apache.aries.component.dsl.OSGi.services;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Carlos Sierra Andrés
 */
@Ignore
public class AsynchronousTest {

    static BundleContext bundleContext = FrameworkUtil.getBundle(
        AsynchronousTest.class).getBundleContext();

    @Test
    public void testApplicative() throws InterruptedException {
        int RUNS = 40;
        AtomicBoolean[][][] started = new AtomicBoolean[RUNS][RUNS][RUNS];
        AtomicBoolean[][][] closed = new AtomicBoolean[RUNS][RUNS][RUNS];

        for (int i = 0; i < RUNS; i++) {
            for (int j = 0; j < RUNS; j++) {
                for (int k = 0; k < RUNS; k++) {
                    started[i][j][k] = new AtomicBoolean();
                    closed[i][j][k] = new AtomicBoolean();
                }
            }
        }

        OSGi<Integer> as = services(Service.class, "(property=a)").map(Service::getI);
        OSGi<Integer> bs = services(Service.class, "(property=b)").map(Service::getI);
        OSGi<Integer> cs = services(Service.class, "(property=c)").map(Service::getI);

        OSGi<int[]> combined = combine((x, y, z) -> new int[] {x, y, z}, as, bs, cs);

        OSGi<?> program = combined.effects(
            i -> started[i[0]][i[1]][i[2]].set(true),
            i -> closed[i[0]][i[1]][i[2]].set(true));

        OSGiResult result = program.run(bundleContext);

        ExecutorService executor = Executors.newFixedThreadPool(RUNS);

        Random random = new Random(System.currentTimeMillis());

        for (int i = 0; i < RUNS; i++) {
            final int ii = i;
            for (int j = 0; j < RUNS; j++) {
                final int jj = j;
                for (int k = 0; k < RUNS; k++) {
                    final int kk = k;
                    executor.execute(() -> {
                        ignoreException(() -> Thread.sleep(random.nextInt(10)));

                        ServiceRegistration<Service> sr =
                            bundleContext.registerService(
                                Service.class,
                                new Service(ii),
                                HashTable.of("property", "a"));

                        ignoreException(() -> Thread.sleep(random.nextInt(2)));

                        sr.unregister();
                    });
                    executor.execute(() -> {
                        ignoreException(() -> Thread.sleep(random.nextInt(5)));

                        ServiceRegistration<Service> sr =
                            bundleContext.registerService(
                                Service.class,
                                new Service(jj),
                                HashTable.of("property", "b"));

                        ignoreException(() -> Thread.sleep(random.nextInt(2)));

                        sr.unregister();
                    });
                    executor.execute(() -> {
                        ignoreException(() -> Thread.sleep(random.nextInt(2)));

                        ServiceRegistration<Service> sr =
                            bundleContext.registerService(
                                Service.class,
                                new Service(kk),
                                HashTable.of("property", "c"));

                        ignoreException(() -> Thread.sleep(random.nextInt(2)));

                        sr.unregister();
                    });
                }
            }
        }

        executor.shutdown();

        boolean finished = executor.awaitTermination(1, TimeUnit.MINUTES);

        result.close();

        System.out.println("******** FINISHED: " + finished);

        int executedCount = 0;
        int totalCount = 0;
        int errors = 0;

        for (int i = 0; i < RUNS; i++) {
            for (int j = 0; j < RUNS; j++) {
                for (int k = 0; k < RUNS; k++) {
                    if (started[i][j][k].get()) {
                        executedCount ++;
                    }

                    if (!(started[i][j][k].get() == closed[i][j][k].get())) {
                        errors ++;
                    }

                    totalCount ++;

                }
            }
        }

        System.out.println("******* TOTAL: " + totalCount);
        System.out.println("******* EXECUTED: " + executedCount);
        System.out.println("******* ERRORS: " + errors);

        assertTrue(executedCount < totalCount);
        assertEquals(0, errors);
    }

    @Test
    public void testApplicativeConfiguration() throws InterruptedException {
        int RUNS = 40;
        AtomicBoolean[][][] started = new AtomicBoolean[RUNS][RUNS][RUNS];
        AtomicBoolean[][][] closed = new AtomicBoolean[RUNS][RUNS][RUNS];

        for (int i = 0; i < RUNS; i++) {
            for (int j = 0; j < RUNS; j++) {
                for (int k = 0; k < RUNS; k++) {
                    started[i][j][k] = new AtomicBoolean();
                    closed[i][j][k] = new AtomicBoolean();
                }
            }
        }

        OSGi<Integer> as = services(Service.class, "(property=a)").map(Service::getI);
        OSGi<Integer> bs = services(Service.class, "(property=b)").map(Service::getI);
        OSGi<Integer> cs = configurations("configurationc").map(d -> (Integer)d.get("property"));

        OSGi<int[]> combined = combine((x, y, z) -> new int[] {x, y, z}, as, bs, cs);

        OSGi<?> program = combined.effects(
            i -> started[i[0]][i[1]][i[2]].set(true),
            i -> closed[i[0]][i[1]][i[2]].set(true));

        OSGiResult result = program.run(bundleContext);

        ServiceReference<ConfigurationAdmin> configAdmin =
            bundleContext.getServiceReference(ConfigurationAdmin.class);

        ConfigurationAdmin configurationAdmin = bundleContext.getService(
            configAdmin);

        ExecutorService executor = Executors.newFixedThreadPool(RUNS);
        ExecutorService executor2 = Executors.newFixedThreadPool(1);

        Random random = new Random(System.currentTimeMillis());

        for (int i = 0; i < RUNS; i++) {
            final int ii = i;
            for (int j = 0; j < RUNS; j++) {
                final int jj = j;
                for (int k = 0; k < RUNS; k++) {
                    final int kk = k;
                    executor.execute(() -> {
                        ignoreException(() -> Thread.sleep(random.nextInt(10)));

                        ServiceRegistration<Service> sr =
                            bundleContext.registerService(
                                Service.class,
                                new Service(ii),
                                HashTable.of("property", "a"));

                        ignoreException(() -> Thread.sleep(random.nextInt(2)));

                        sr.unregister();
                    });
                    executor.execute(() -> {
                        ignoreException(() -> Thread.sleep(random.nextInt(5)));

                        ServiceRegistration<Service> sr =
                            bundleContext.registerService(
                                Service.class,
                                new Service(jj),
                                HashTable.of("property", "b"));

                        ignoreException(() -> Thread.sleep(random.nextInt(2)));

                        sr.unregister();
                    });
                    executor2.execute(() ->
                        ignoreException(() -> {
                            Thread.sleep(random.nextInt(2));

                            Configuration configurationc =
                                configurationAdmin.createFactoryConfiguration(
                                    "configurationc");

                            configurationc.update(
                                HashTable.of("property", kk));

                            configurationc.delete();
                        }));
                }
            }
        }

        executor.shutdown();

        boolean finished = executor.awaitTermination(1, TimeUnit.MINUTES);

        result.close();

        System.out.println("******** FINISHED: " + finished);

        int executedCount = 0;
        int totalCount = 0;
        int errors = 0;

        for (int i = 0; i < RUNS; i++) {
            for (int j = 0; j < RUNS; j++) {
                for (int k = 0; k < RUNS; k++) {
                    if (started[i][j][k].get()) {
                        executedCount ++;
                    }

                    if (!(started[i][j][k].get() == closed[i][j][k].get())) {
                        errors ++;
                    }

                    totalCount ++;

                }
            }
        }

        System.out.println("******* TOTAL: " + totalCount);
        System.out.println("******* EXECUTED: " + executedCount);
        System.out.println("******* ERRORS: " + errors);

        assertTrue(executedCount < totalCount);
        assertEquals(0, errors);
    }


    @Test
    public void testApplicativeProbe() throws InterruptedException {
        int RUNS = 40;
        AtomicBoolean[][][] started = new AtomicBoolean[RUNS][RUNS][RUNS];
        AtomicBoolean[][][] closed = new AtomicBoolean[RUNS][RUNS][RUNS];

        for (int i = 0; i < RUNS; i++) {
            for (int j = 0; j < RUNS; j++) {
                for (int k = 0; k < RUNS; k++) {
                    started[i][j][k] = new AtomicBoolean();
                    closed[i][j][k] = new AtomicBoolean();
                }
            }
        }

        OSGi<Integer> as = new ProbeImpl<>();
        OSGi<Integer> bs = new ProbeImpl<>();
        OSGi<Integer> cs = new ProbeImpl<>();

        OSGi<int[]> combined = combine((x, y, z) -> new int[] {x, y, z}, as, bs, cs);

        OSGi<?> program = combined.effects(
            i -> started[i[0]][i[1]][i[2]].set(true),
            i -> closed[i[0]][i[1]][i[2]].set(true));

        OSGiResult result = program.run(bundleContext);

        Publisher<? super Integer> opa = ((ProbeImpl<Integer>) as).getPublisher();
        Publisher<? super Integer> opb = ((ProbeImpl<Integer>) bs).getPublisher();
        Publisher<? super Integer> opc = ((ProbeImpl<Integer>) cs).getPublisher();

        ExecutorService executor = Executors.newFixedThreadPool(8);

        Random random = new Random(System.currentTimeMillis());

        for (int i = 0; i < RUNS; i++) {
            final int ii = i;
            for (int j = 0; j < RUNS; j++) {
                final int jj = j;
                for (int k = 0; k < RUNS; k++) {
                    final int kk = k;
                    executor.execute(() -> {
                        ignoreException(() -> Thread.sleep(random.nextInt(10)));

                        Runnable sentEvent = opa.apply(ii);

                        ignoreException(() -> Thread.sleep(random.nextInt(2)));

                        sentEvent.run();
                    });
                    executor.execute(() -> {
                        ignoreException(() -> Thread.sleep(random.nextInt(5)));

                        Runnable sentEvent = opb.apply(jj);

                        ignoreException(() -> Thread.sleep(random.nextInt(2)));

                        sentEvent.run();
                    });
                    executor.execute(() -> {
                        ignoreException(() -> Thread.sleep(random.nextInt(2)));

                        Runnable sentEvent = opc.apply(kk);

                        ignoreException(() -> Thread.sleep(random.nextInt(2)));

                        sentEvent.run();
                    });
                }
            }
        }

        executor.shutdown();

        boolean finished = executor.awaitTermination(2, TimeUnit.MINUTES);

        result.close();

        System.out.println("******** FINISHED: " + finished);

        int executedCount = 0;
        int totalCount = 0;
        int errors = 0;

        for (int i = 0; i < RUNS; i++) {
            for (int j = 0; j < RUNS; j++) {
                for (int k = 0; k < RUNS; k++) {
                    if (started[i][j][k].get()) {
                        executedCount ++;
                    }

                    if (!(started[i][j][k].get() == closed[i][j][k].get())) {
                        errors ++;
                    }

                    totalCount ++;

                }
            }
        }

        System.out.println("******* TOTAL: " + totalCount);
        System.out.println("******* EXECUTED: " + executedCount);
        System.out.println("******* ERRORS: " + errors);

        assertTrue(executedCount < totalCount);
        assertEquals(0, errors);

    }


    private interface ExceptionalRunnable {
        void run() throws Exception;
    }

    private static void ignoreException(ExceptionalRunnable callable) {
        try {
            callable.run();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private class Service {
        public Service(int i) {
            this.i = i;
        }

        int i;

        public int getI() {
            return i;
        }
    }
}

