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

import org.apache.aries.component.dsl.CachingServiceReference;
import org.apache.aries.component.dsl.OSGi;
import org.apache.aries.component.dsl.OSGiResult;
import org.apache.aries.component.dsl.Publisher;
import org.apache.aries.component.dsl.Utils;
import org.apache.aries.component.dsl.configuration.Configurations;
import org.apache.aries.component.dsl.internal.ProbeImpl;
import org.apache.aries.component.dsl.services.ServiceReferences;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ManagedService;
import org.osgi.util.tracker.ServiceTracker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.apache.aries.component.dsl.OSGi.*;
import static org.apache.aries.component.dsl.Utils.accumulate;
import static org.apache.aries.component.dsl.Utils.highest;
import static org.apache.aries.component.dsl.configuration.ConfigurationHolder.fromMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class DSLTest {

    @Test
    public void testAccumulate() {
        ArrayList<List<String>> lists = new ArrayList<>();

        ArrayList<List<String>> expected = new ArrayList<>();

        ArrayList<List<String>> gone = new ArrayList<>();

        expected.add(Collections.emptyList());

        OSGi<List<String>> osgi = accumulate(
            serviceReferences(Service.class).map(
                this::getId
            )
        ).effects(lists::add, gone::add);

        OSGiResult run = osgi.run(bundleContext);

        ServiceRegistration<Service> serviceRegistrationOne =
            bundleContext.registerService(
                Service.class, new Service(),
                new Hashtable<String, Object>() {{
                }});

        expected.add(
            Collections.singletonList(getId(serviceRegistrationOne)));

        serviceRegistrationOne.unregister();

        expected.add(Collections.emptyList());

        serviceRegistrationOne =
            bundleContext.registerService(
                Service.class, new Service(),
                new Hashtable<String, Object>() {{
                }});

        expected.add(
            Collections.singletonList(getId(serviceRegistrationOne)));

        ServiceRegistration<Service> serviceRegistrationTwo =
            bundleContext.registerService(
                Service.class, new Service(),
                new Hashtable<String, Object>() {{
                }});

        expected.add(
            Arrays.asList(
                getId(serviceRegistrationOne),
                getId(serviceRegistrationTwo)));

        serviceRegistrationOne.unregister();

        expected.add(
            Collections.singletonList(
                getId(serviceRegistrationTwo)));

        serviceRegistrationTwo.unregister();

        expected.add(Collections.emptyList());

        assertEquals(expected, lists);

        run.close();

        assertEquals(lists, gone);
    }

    @Test
    public void testAccumulateAtLeastOne() {
        ArrayList<List<String>> lists = new ArrayList<>();

        ArrayList<List<String>> expected = new ArrayList<>();

        ArrayList<List<String>> gone = new ArrayList<>();

        OSGi<List<String>> osgi =
            accumulate(
            serviceReferences(Service.class).map(this::getId)
        ).filter(l -> !l.isEmpty()).effects(
            lists::add, gone::add
        );

        OSGiResult run = osgi.run(bundleContext);

        ServiceRegistration<Service> serviceRegistrationOne =
            bundleContext.registerService(
                Service.class, new Service(),
                new Hashtable<String, Object>() {{
                }});

        expected.add(
            Collections.singletonList(getId(serviceRegistrationOne)));

        serviceRegistrationOne.unregister();

        serviceRegistrationOne =
            bundleContext.registerService(
                Service.class, new Service(),
                new Hashtable<String, Object>() {{
                }});

        expected.add(
            Collections.singletonList(getId(serviceRegistrationOne)));

        ServiceRegistration<Service> serviceRegistrationTwo =
            bundleContext.registerService(
                Service.class, new Service(),
                new Hashtable<String, Object>() {{
                }});

        expected.add(
            Arrays.asList(
                getId(serviceRegistrationOne),
                getId(serviceRegistrationTwo)));

        serviceRegistrationOne.unregister();

        expected.add(
            Collections.singletonList(
                getId(serviceRegistrationTwo)));

        serviceRegistrationTwo.unregister();

        assertEquals(expected, lists);

        run.close();

        assertEquals(lists, gone);
    }

    @Test
    public void testApplicativeApplyTo() {
        AtomicInteger integer = new AtomicInteger(0);

        OSGi<Integer> program = just(5).applyTo(just((i) -> i + 5));

        program.run(bundleContext, newValue -> {
            integer.set(newValue);

            return NOOP;
        });

        assertEquals(10, integer.get());
    }

    @Test
    public void testApply() {
        AtomicInteger integer = new AtomicInteger(0);

        OSGi<Integer> program = OSGi.combine(
            (a, b, c) -> a + b + c, just(5), just(5), just(5));

        program.run(bundleContext, newValue -> {
            integer.set(newValue);

            return NOOP;
        });

        assertEquals(15, integer.get());
    }

    @Test
    public void testCombine() {
        List<Integer> list = new ArrayList<>();

        ProbeImpl<Integer> probe1 = new ProbeImpl<>();
        ProbeImpl<Integer> probe2 = new ProbeImpl<>();

        OSGi<Integer> program = combine(
            (x, y) -> x * y, probe1, probe2
        ).effects(
            list::add, list::remove
        );

        OSGiResult run = program.run(bundleContext);

        Publisher<? super Integer> publisher1 = probe1.getPublisher();
        Publisher<? super Integer> publisher2 = probe2.getPublisher();

        OSGiResult osgiResult1 = publisher1.publish(3);
        OSGiResult osgiResult2 = publisher2.publish(5);

        assertTrue(list.contains(15));

        osgiResult1.close();

        assertFalse(list.contains(15));

        publisher1.publish(3);

        assertTrue(list.contains(15));

        osgiResult2.close();

        assertFalse(list.contains(15));

        run.close();
    }

    @Test
    public void testFlatCombine() {
        List<Integer> list = new ArrayList<>();

        ProbeImpl<Integer> probe1 = new ProbeImpl<>();
        ProbeImpl<Integer> probe2 = new ProbeImpl<>();

        OSGi<Integer> program = flatCombine(
            (x, y) -> just(x * y), probe1, probe2
        ).effects(
            list::add, list::remove
        );

        OSGiResult run = program.run(bundleContext);

        Publisher<? super Integer> publisher1 = probe1.getPublisher();
        Publisher<? super Integer> publisher2 = probe2.getPublisher();

        OSGiResult osgiResult1 = publisher1.publish(3);
        OSGiResult osgiResult2 = publisher2.publish(5);

        assertTrue(list.contains(15));

        osgiResult1.close();

        assertFalse(list.contains(15));

        publisher1.publish(3);

        assertTrue(list.contains(15));

        osgiResult2.close();

        assertFalse(list.contains(15));

        run.close();
    }


    @Test
    public void testEffectsOrder() {
        ArrayList<Object> effects = new ArrayList<>();

        OSGi<Void> program = just(Arrays.asList(1, 2, 3, 4)).effects(
            effects::add,
            effects::remove
        ).
        flatMap(t -> OSGi.effects(
            () -> assertEquals(1, effects.stream().filter(t::equals).count()),
            () -> assertEquals(1, effects.stream().filter(t::equals).count())
        ).then(just(t))).
        effects(
            effects::add,
            effects::remove
        ).flatMap(t ->
        OSGi.effects(
            () ->
                assertEquals(2, effects.stream().filter(t::equals).count()),
            () ->
                assertEquals(2, effects.stream().filter(t::equals).count()))
        );

        program = OSGi.effects(
            () -> assertTrue(effects.isEmpty()),
            () -> assertTrue(effects.isEmpty())).
            then(program);

        try (OSGiResult result = program.run(bundleContext)) {}
    }

    @Test
    public void testEffectsErrorOnAddedAfter() {
        ArrayList<Object> effects = new ArrayList<>();

        OSGi<Integer> program = recoverWith(
                just(Arrays.asList(1, 2, 3, 4)).
                effects(
                    effects::add,
                    i -> {
                        if (i % 2 == 0) {
                            throw new RuntimeException();
                        }
                    },
                    __ -> {},
                    effects::remove),
                (__, e) -> nothing()
        );

        try (OSGiResult result = program.run(bundleContext)) {
            assertEquals(Arrays.asList(1, 3), effects);
        }

        assertEquals(Collections.emptyList(), effects);
    }


    @Test
    public void testCoalesce() {
        ProbeImpl<String> program1 = new ProbeImpl<>();
        ProbeImpl<String> program2 = new ProbeImpl<>();

        ArrayList<String> effects = new ArrayList<>();

        OSGiResult result = coalesce(
            program1, program2, just(Arrays.asList("fixed1", "fixed2")),
            just("never")).
            effects(effects::add, effects::add).
            run(bundleContext);

        Publisher<? super String> publisher1 = program1.getPublisher();
        Publisher<? super String> publisher2 = program2.getPublisher();

        assertEquals(Arrays.asList("fixed1", "fixed2"), effects);

        OSGiResult event1Result = publisher2.publish("event1");

        program2.onClose(event1Result);

        assertEquals(
            Arrays.asList(
                "fixed1", "fixed2", "fixed2", "fixed1", "event1"), effects);

        OSGiResult event2Result = publisher1.publish("event2");

        assertEquals(
            Arrays.asList(
                "fixed1", "fixed2", "fixed2", "fixed1", "event1", "event1", "event2"), effects);

        event2Result.close();

        assertEquals(
            Arrays.asList(
                "fixed1", "fixed2", "fixed2", "fixed1", "event1", "event1", "event2", "event2", "fixed1", "fixed2"), effects);

        event2Result = publisher1.publish("event3");

        program1.onClose(event2Result);

        assertEquals(
            Arrays.asList(
                "fixed1", "fixed2", "fixed2", "fixed1", "event1", "event1",
                "event2", "event2", "fixed1", "fixed2", "fixed2", "fixed1",
                "event3"),
            effects);

        result.close();

        assertEquals(
            Arrays.asList(
                "fixed1", "fixed2", "fixed2", "fixed1", "event1", "event1",
                "event2", "event2", "fixed1", "fixed2", "fixed2", "fixed1",
                "event3", "event3"),
            effects);
    }

    @Test
    public void testCoalesceWhenEmpty() {
        ProbeImpl<String> program1 = new ProbeImpl<>();
        ProbeImpl<String> program2 = new ProbeImpl<>();

        ArrayList<String> effects = new ArrayList<>();

        OSGiResult result = coalesce(program1, program2).
            effects(effects::add, effects::add).
            run(bundleContext);

        Publisher<? super String> publisher1 = program1.getPublisher();
        Publisher<? super String> publisher2 = program2.getPublisher();

        assertEquals(Collections.emptyList(), effects);

        OSGiResult event1Result = publisher2.publish("event1");

        program2.onClose(event1Result);

        assertEquals(
            Arrays.asList("event1"), effects);

        OSGiResult event2Result = publisher1.publish("event2");

        assertEquals(
            Arrays.asList("event1", "event1", "event2"), effects);

        event2Result.close();

        assertEquals(
            Arrays.asList("event1", "event1", "event2", "event2"), effects);

        event2Result = publisher1.publish("event3");

        program1.onClose(event2Result);

        assertEquals(
            Arrays.asList(
                "event1", "event1", "event2", "event2", "event3"),
            effects);

        result.close();

        assertEquals(
            Arrays.asList(
                "event1", "event1", "event2", "event2", "event3", "event3"),
            effects);
    }

    @Test
    public void testCoalesceWithConfigurationUpdate()
        throws IOException, InterruptedException {

        ServiceReference<ConfigurationAdmin> serviceReference =
            bundleContext.getServiceReference(ConfigurationAdmin.class);

        ConfigurationAdmin configurationAdmin = bundleContext.getService(
            serviceReference);

        Configuration configuration = configurationAdmin.getConfiguration(
            "test.configuration");

        configuration.update(new Hashtable<>());

        AtomicReference<Dictionary<?,?>> atomicReference =
            new AtomicReference<>(null);

        AtomicInteger counter = new AtomicInteger();
        AtomicInteger updateCounter = new AtomicInteger();

        CountDownLatch countDownLatch = new CountDownLatch(4);

        ServiceRegistration<ManagedService> serviceRegistration =
            bundleContext.registerService(
                ManagedService.class, __ -> countDownLatch.countDown(),
                new Hashtable<String, Object>() {{
                    put("service.pid", "test.configuration");
                }});

        AtomicReference<Runnable> effect = new AtomicReference<>();

        effect.set(countDownLatch::countDown);

        try(OSGiResult result =
                coalesce(
                    Configurations.singleton("test.configuration"),
                    just(fromMap(new HashMap<>())))
                .effects(
                        holder -> {
                            atomicReference.set(holder.getUpdatedProperties());

                            counter.incrementAndGet();

                            effect.get().run();
                        },
                        __ -> {},
                        __ -> {},
                        __ -> {},
                        holder -> {
                                atomicReference.set(holder.getUpdatedProperties());

                                updateCounter.incrementAndGet();

                                effect.get().run();
                            }
                    )
                .run(bundleContext)
        ) {
            configuration.update(
                new Hashtable<String, Object>() {{
                    put("property", "value");
                }}
            );

            countDownLatch.await(10, TimeUnit.SECONDS);

            assertEquals(1, counter.get());
            assertEquals(1, updateCounter.get());

            assertEquals("value", atomicReference.get().get("property"));

            if (serviceRegistration != null) {
                serviceRegistration.unregister();
            }

            CountDownLatch deleteLatch = new CountDownLatch(2);

            effect.set(deleteLatch::countDown);

            serviceRegistration =
                bundleContext.registerService(
                    ManagedService.class, __ -> deleteLatch.countDown(),
                    new Hashtable<String, Object>() {{
                        put("service.pid", "test.configuration");
                    }});

            configuration.delete();

            deleteLatch.await(10, TimeUnit.SECONDS);

            assertEquals(2, counter.get());
            assertEquals(1, updateCounter.get());

            assertTrue(atomicReference.get().isEmpty());
        }
        finally {
            bundleContext.ungetService(serviceReference);

            if (serviceRegistration != null) {
                serviceRegistration.unregister();
            }
        }
    }


    @Test
    public void testCoalesceWithConfigurationUpdateRefresh()
        throws IOException, InterruptedException {

        ServiceReference<ConfigurationAdmin> serviceReference =
            bundleContext.getServiceReference(ConfigurationAdmin.class);

        ConfigurationAdmin configurationAdmin = bundleContext.getService(
            serviceReference);

        Configuration configuration = configurationAdmin.getConfiguration(
            "test.configuration");

        configuration.update(new Hashtable<>());

        AtomicReference<Dictionary<?,?>> atomicReference =
            new AtomicReference<>(null);

        AtomicInteger counter = new AtomicInteger();

        CountDownLatch countDownLatch = new CountDownLatch(4);

        ServiceRegistration<ManagedService> serviceRegistration =
            bundleContext.registerService(
                ManagedService.class, __ -> countDownLatch.countDown(),
                new Hashtable<String, Object>() {{
                    put("service.pid", "test.configuration");
                }});

        AtomicReference<Runnable> effect = new AtomicReference<>();

        effect.set(countDownLatch::countDown);

        try(OSGiResult result =
            coalesce(
                configuration("test.configuration"),
                just(Hashtable::new)
            ).run(
                bundleContext,
                x -> {
                    atomicReference.set(x);

                    counter.incrementAndGet();

                    effect.get().run();

                    return NOOP;
                }))
        {
            configuration.update(
                new Hashtable<String, Object>() {{
                    put("property", "value");
                }}
            );

            countDownLatch.await(10, TimeUnit.SECONDS);

            assertEquals(2, counter.get());

            assertEquals("value", atomicReference.get().get("property"));

            if (serviceRegistration != null) {
                serviceRegistration.unregister();
            }

            CountDownLatch deleteLatch = new CountDownLatch(2);

            effect.set(deleteLatch::countDown);

            serviceRegistration =
                bundleContext.registerService(
                    ManagedService.class, __ -> deleteLatch.countDown(),
                    new Hashtable<String, Object>() {{
                        put("service.pid", "test.configuration");
                    }});

            configuration.delete();

            deleteLatch.await(10, TimeUnit.SECONDS);

            assertEquals(3, counter.get());

            assertTrue(atomicReference.get().isEmpty());
        }
        finally {
            bundleContext.ungetService(serviceReference);

            if (serviceRegistration != null) {
                serviceRegistration.unregister();
            }
        }
    }

    @Test
    public void testConfiguration() throws IOException, InterruptedException {
        ServiceReference<ConfigurationAdmin> serviceReference =
            bundleContext.getServiceReference(ConfigurationAdmin.class);

        ConfigurationAdmin configurationAdmin = bundleContext.getService(
            serviceReference);

        AtomicReference<Dictionary<?,?>> atomicReference =
            new AtomicReference<>(null);

        Configuration configuration = null;

        CountDownLatch countDownLatch = new CountDownLatch(1);

        try(OSGiResult result =
            configuration("test.configuration").run(
                bundleContext,
                x -> {
                    atomicReference.set(x);

                    countDownLatch.countDown();

                    return NOOP;
                }))
        {
            assertNull(atomicReference.get());

            configuration = configurationAdmin.getConfiguration(
                "test.configuration");

            configuration.update(new Hashtable<>());

            countDownLatch.await(10, TimeUnit.SECONDS);

            assertNotNull(atomicReference.get());
        }
        finally {
            bundleContext.ungetService(serviceReference);

            if (configuration != null) {
                configuration.delete();
            }
        }
    }

    @Test
    public void testConfigurationWithExistingValues()
        throws IOException, InterruptedException {

        ServiceReference<ConfigurationAdmin> serviceReference =
            bundleContext.getServiceReference(ConfigurationAdmin.class);

        ConfigurationAdmin configurationAdmin = bundleContext.getService(
            serviceReference);

        Configuration configuration = configurationAdmin.getConfiguration(
            "test.configuration");

        configuration.update(new Hashtable<>());

        AtomicReference<Dictionary<?,?>> atomicReference =
            new AtomicReference<>(null);

        AtomicInteger counter = new AtomicInteger();

        CountDownLatch countDownLatch = new CountDownLatch(2);

        ServiceRegistration<ManagedService> serviceRegistration =
            bundleContext.registerService(
                ManagedService.class, __ -> countDownLatch.countDown(),
                new Hashtable<String, Object>() {{
                    put("service.pid", "test.configuration");
                }});

        try(OSGiResult result =
                configuration("test.configuration").run(
                    bundleContext,
                    x -> {
                        atomicReference.set(x);

                        counter.incrementAndGet();

                        countDownLatch.countDown();

                        return NOOP;
                    }))
        {
            assertNotNull(atomicReference.get());

            countDownLatch.await(10, TimeUnit.SECONDS);

            assertEquals(1, counter.get());
        }
        finally {
            bundleContext.ungetService(serviceReference);

            configuration.delete();

            if (serviceRegistration != null) {
                serviceRegistration.unregister();
            }
        }
    }

    @Test
    public void testConfigurations() throws IOException, InterruptedException {
        ServiceReference<ConfigurationAdmin> serviceReference =
            bundleContext.getServiceReference(ConfigurationAdmin.class);

        ConfigurationAdmin configurationAdmin = bundleContext.getService(
            serviceReference);

        AtomicReference<Dictionary<?,?>> atomicReference =
            new AtomicReference<>(null);

        CountDownLatch countDownLatch = new CountDownLatch(1);

        Configuration configuration = null;

        try(OSGiResult result =
            configurations("test.configuration").run(
                bundleContext,
                x -> {
                    atomicReference.set(x);

                    countDownLatch.countDown();

                    return NOOP;
                }))
        {
            assertNull(atomicReference.get());

            configuration =
                configurationAdmin.createFactoryConfiguration(
                    "test.configuration");

            configuration.update(new Hashtable<>());

            countDownLatch.await(10, TimeUnit.SECONDS);

            assertNotNull(atomicReference.get());
        }
        finally {
            bundleContext.ungetService(serviceReference);

            if (configuration != null) {
                configuration.delete();
            }
        }
    }

    @Test
    public void testConfigurationsAndRegistrations()
        throws InvalidSyntaxException, IOException, InterruptedException {

        ServiceReference<ConfigurationAdmin> serviceReference =
            bundleContext.getServiceReference(ConfigurationAdmin.class);

        ConfigurationAdmin configurationAdmin = bundleContext.getService(
            serviceReference);

        /*  For each factory configuration register a service with the property
            key set to the value of the property key that comes with the
            configuration */
        OSGi<ServiceRegistration<Service>> program =
            configurations("test.configuration").
                map(d -> d.get("key")).flatMap(key ->
            register(
                Service.class, new Service(),
                new HashMap<String, Object>() {{
                    put("key", key);
                    put("test.configuration", true);
                }})
            );

        OSGiResult result = program.run(
            bundleContext);

        assertEquals(
            0,
            bundleContext.getServiceReferences(
                Service.class, "(test.configuration=*)").size());

        CountDownLatch addedLatch = new CountDownLatch(3);

        ServiceTracker serviceTracker = new ServiceTracker<Service, Service>(
            bundleContext, Service.class, null) {

            @Override
            public Service addingService(ServiceReference<Service> reference) {
                addedLatch.countDown();

                return null;
            }
        };

        serviceTracker.open();

        CountDownLatch deletedLatch = new CountDownLatch(3);

        ServiceTracker serviceTracker2 = new ServiceTracker<Service, Service>(
            bundleContext, Service.class, null) {

            @Override
            public void removedService(
                ServiceReference<Service> reference,
                Service service) {

                deletedLatch.countDown();
            }
        };

        serviceTracker2.open();

        Configuration configuration =
            configurationAdmin.createFactoryConfiguration("test.configuration");

        configuration.update(new Hashtable<String, Object>(){{
            put("key", "service one");
        }});

        Configuration configuration2 =
            configurationAdmin.createFactoryConfiguration("test.configuration");

        configuration2.update(new Hashtable<String, Object>(){{
            put("key", "service two");
        }});

        Configuration configuration3 =
            configurationAdmin.createFactoryConfiguration("test.configuration");

        configuration3.update(new Hashtable<String, Object>(){{
            put("key", "service three");
        }});

        assertTrue(addedLatch.await(10, TimeUnit.SECONDS));

        assertEquals(
            1,
            bundleContext.getServiceReferences(
                Service.class, "(key=service one)").size());
        assertEquals(
            1,
            bundleContext.getServiceReferences(
                Service.class, "(key=service two)").size());
        assertEquals(
            1,
            bundleContext.getServiceReferences(
                Service.class, "(key=service three)").size());

        configuration3.delete();

        configuration2.delete();

        configuration.delete();

        assertTrue(deletedLatch.await(10, TimeUnit.SECONDS));

        assertEquals(
            0,
            bundleContext.getServiceReferences(
                Service.class, "(test.configuration=*)").size());

        serviceTracker.close();

        serviceTracker2.close();

        result.close();

        bundleContext.ungetService(serviceReference);
    }

    @Test
    public void testHighestRankingDiscards() {
        ArrayList<ServiceReference<?>> discards = new ArrayList<>();

        OSGi<CachingServiceReference<Service>> program = highest(
            serviceReferences(Service.class),
            Comparator.naturalOrder(),
            dp ->
                dp.map(CachingServiceReference::getServiceReference).effects(
                    discards::add, discards::remove).then(nothing()));

        assertTrue(discards.isEmpty());

        try (OSGiResult result = program.run(bundleContext)) {
            ServiceRegistration<Service> serviceRegistrationOne =
                bundleContext.registerService(
                    Service.class, new Service(),
                    new Hashtable<String, Object>() {{
                        put("service.ranking", 0);
                    }});

            assertEquals(Collections.emptyList(), discards);

            ServiceRegistration<Service> serviceRegistrationTwo =
                bundleContext.registerService(
                    Service.class, new Service(),
                    new Hashtable<String, Object>() {{
                        put("service.ranking", 1);
                    }});

            assertEquals(
                Collections.singletonList(
                    serviceRegistrationOne.getReference()),
                discards);

            ServiceRegistration<Service> serviceRegistrationMinusOne =
                bundleContext.registerService(
                    Service.class, new Service(),
                    new Hashtable<String, Object>() {{
                        put("service.ranking", -1);
                    }});

            assertEquals(
                Arrays.asList(
                    serviceRegistrationOne.getReference(),
                    serviceRegistrationMinusOne.getReference()),
                discards);

            serviceRegistrationTwo.unregister();

            assertEquals(
                Arrays.asList(serviceRegistrationMinusOne.getReference()),
                discards);

            serviceRegistrationOne.unregister();

            assertTrue(discards.isEmpty());

            serviceRegistrationOne =
                bundleContext.registerService(
                    Service.class, new Service(),
                    new Hashtable<String, Object>() {{
                        put("service.ranking", 0);
                    }});

            assertEquals(
                Arrays.asList(serviceRegistrationMinusOne.getReference()),
                discards);

            serviceRegistrationMinusOne.unregister();
            serviceRegistrationOne.unregister();
        }
    }

    @Test
    public void testHighestRankingOnly() {
        AtomicReference<CachingServiceReference<Service>> current =
            new AtomicReference<>();

        OSGi<Void> program =
            highest(serviceReferences(Service.class)).
            foreach(current::set, sr -> current.set(null));

        assertNull(current.get());

        try (OSGiResult result = program.run(bundleContext)) {
            ServiceRegistration<Service> serviceRegistrationOne =
                bundleContext.registerService(
                    Service.class, new Service(),
                    new Hashtable<String, Object>() {{
                        put("service.ranking", 0);
                    }});

            assertEquals(
                serviceRegistrationOne.getReference(),
                current.get().getServiceReference());

            ServiceRegistration<Service> serviceRegistrationTwo =
                bundleContext.registerService(
                    Service.class, new Service(),
                    new Hashtable<String, Object>() {{
                        put("service.ranking", 1);
                    }});

            assertEquals(
                serviceRegistrationTwo.getReference(),
                current.get().getServiceReference());

            ServiceRegistration<Service> serviceRegistrationMinusOne =
                bundleContext.registerService(
                    Service.class, new Service(),
                    new Hashtable<String, Object>() {{
                        put("service.ranking", -1);
                    }});

            assertEquals(
                serviceRegistrationTwo.getReference(),
                current.get().getServiceReference());

            serviceRegistrationTwo.unregister();

            assertEquals(
                serviceRegistrationOne.getReference(),
                current.get().getServiceReference());

            serviceRegistrationOne.unregister();

            assertEquals(
                serviceRegistrationMinusOne.getReference(),
                current.get().getServiceReference());

            serviceRegistrationOne =
                bundleContext.registerService(
                    Service.class, new Service(),
                    new Hashtable<String, Object>() {{
                        put("service.ranking", 0);
                    }});

            assertEquals(
                serviceRegistrationOne.getReference(),
                current.get().getServiceReference());

            serviceRegistrationOne.unregister();
            serviceRegistrationMinusOne.unregister();
        }

    }

    @Test
    public void testJust() {
        AtomicInteger atomicInteger = new AtomicInteger(0);

        OSGi<Integer> just = just(25);

        assertEquals(0, atomicInteger.get());

        try (OSGiResult result = just.run(
            bundleContext, newValue -> {
                atomicInteger.set(newValue);

                return NOOP;
            }))
        {
            assertEquals(25, atomicInteger.get());
        }

        atomicInteger.set(0);

        OSGi<Integer> map = just(25).map(s -> s + 5);

        try (OSGiResult result = map.run(
            bundleContext, newValue -> {
                atomicInteger.set(newValue);

                return NOOP;
            }))
        {
            assertEquals(30, atomicInteger.get());
        }

        atomicInteger.set(0);

        OSGi<Integer> flatMap = just(25).flatMap(s -> just(s + 10));

        try (OSGiResult result = flatMap.run(
            bundleContext, newValue -> {
                atomicInteger.set(newValue);

                return NOOP;
            }))
        {
            assertEquals(35, atomicInteger.get());
        }

        atomicInteger.set(0);

        OSGi<Integer> filter = just(25).filter(s -> s % 2 == 0);

        try (OSGiResult result = filter.run(
            bundleContext, newValue -> {
                atomicInteger.set(newValue);

                return NOOP;
            }))
        {
            assertEquals(0, atomicInteger.get());
        }

        atomicInteger.set(0);

        filter = just(25).filter(s -> s % 2 != 0);

        try (OSGiResult result = filter.run(
            bundleContext, newValue -> {
                atomicInteger.set(newValue);

                return NOOP;
            }))
        {
            assertEquals(25, atomicInteger.get());
        }

    }

    @Test
    public void testJustWithError() {
        ArrayList<Integer> results = new ArrayList<>();

        OSGi<Integer> program = just(
            Arrays.asList(1, 2, 3, 4, 5)
        ).effects(
            results::add, results::remove
        );

        try(OSGiResult result = program.run(bundleContext)) {
            assertEquals(Arrays.asList(1, 2, 3, 4, 5), results);
        }

        try(OSGiResult result = program.filter(i -> {
                if (i == 5) {
                    throw new IllegalArgumentException();
                }

                return true;
            }
        ).run(
            bundleContext)
        ) {
            fail();
        }
        catch (Exception e) {
            assertEquals(Collections.emptyList(), results);
        }

    }

    @Test
    public void testAllWithErrors() {
        ArrayList<Integer> results = new ArrayList<>();

        OSGi<Integer> program = OSGi.all(
            just(1),
            just(2),
            just(3),
            just(4),
            just(5)
        ).effects(
            results::add, results::remove
        );

        try(OSGiResult result = program.run(bundleContext)) {
            assertEquals(Arrays.asList(1, 2, 3, 4, 5), results);
        }

        try(OSGiResult result = program.filter(i -> {
                if (i == 5) {
                    throw new IllegalArgumentException();
                }

                return true;
            }
        ).run(
            bundleContext)
        ) {
            fail();
        }
        catch (Exception e) {
            assertEquals(Collections.emptyList(), results);
        }
    }

    @Test
    public void testDistributeWithError() {
        ArrayList<Integer> results1 = new ArrayList<>();
        ArrayList<Integer> results2 = new ArrayList<>();
        ArrayList<Integer> results3 = new ArrayList<>();
        ArrayList<Integer> results4 = new ArrayList<>();

        OSGi<Integer> program = OSGi.just(
            Arrays.asList(1, 2, 3)
        ).distribute(
            p1 -> p1.effects(results1::add, results1::remove),
            p2 -> p2.effects(results2::add, results2::remove),
            p3 -> p3.filter(i -> {
                    if (i == 2) {
                        throw new IllegalArgumentException();
                    }

                    return true;
                }
            ).effects(results3::add, results3::remove),
            p4 -> p4.effects(results4::add, results4::remove)
        );

        try (OSGiResult run = program.run(bundleContext)) {
            fail("This should not be reached");
        } catch (Exception e) {
            assertEquals(Collections.emptyList(), results1);
            assertEquals(Collections.emptyList(), results2);
            assertEquals(Collections.emptyList(), results3);
            assertEquals(Collections.emptyList(), results4);
        }

        program = recoverWith(
            just(Arrays.asList(1, 2, 3)).
                distribute(
                p1 -> p1.effects(results1::add, results1::remove),
                p2 -> p2.effects(results2::add, results2::remove),
                p3 -> p3.filter(i -> {
                        if (i == 2) {
                            throw new IllegalArgumentException();
                        }

                        return true;
                    }
                ).effects(results3::add, results3::remove),
                p4 -> p4.effects(results4::add, results4::remove)
            ),
            (i, e) -> nothing()
        );

        try (OSGiResult run = program.run(bundleContext)) {
            assertEquals(Arrays.asList(1, 3), results1);
            assertEquals(Arrays.asList(1, 3), results2);
            assertEquals(Arrays.asList(1, 3), results3);
            assertEquals(Arrays.asList(1, 3), results4);
        }
    }

    @Test
    public void testMultipleApplies() {
        ArrayList<Integer> results = new ArrayList<>();
        AtomicInteger results2 = new AtomicInteger();

        OSGi<Integer> program = OSGi.combine(
            (a, b, c) -> a + b + c, just(Arrays.asList(5, 20)),
            just(Arrays.asList(5, 40)), just(Arrays.asList(5, 60)));

        OSGiResult or = program.run(bundleContext, newValue -> {
            results.add(newValue);

            return NOOP;
        });

        or.close();

        OSGiResult or2 = program.run(
            bundleContext, i -> {
                results2.accumulateAndGet(i, (a, b) -> a + b);

                return NOOP;
            });

        or2.close();

        assertEquals(8, results.size());
        assertEquals(540, results2.get());
    }

    @Test
    public void testOnCloseWithError() {
        ArrayList<Object> result = new ArrayList<>();
        ArrayList<Object> left = new ArrayList<>();

        OSGi<Integer> program = just(
            Arrays.asList(1, 2, 3, 4, 5, 6)
        ).recoverWith(
            (__, e) -> just(0)
        ).flatMap(t ->
            onClose(() -> left.add(t)).then(just(t))
        ).
        flatMap(t -> {
            if (t % 2 != 0) {
                throw new RuntimeException();
            }

            return just(t);
        });

        try (OSGiResult run = program.run(bundleContext, e -> {
            result.add(e);

            return NOOP;
        })) {
            assertEquals(Arrays.asList(0, 2, 0, 4, 0, 6), result);
            assertEquals(Arrays.asList(1, 3, 5), left);
        }
    }

    @Test
    public void testOnce() {
        ProbeImpl<Integer> probe = new ProbeImpl<>();


        AtomicInteger count = new AtomicInteger();

        OSGi<Integer> once =
            once(probe).effects(
                t -> count.incrementAndGet(),
                t -> count.set(0));

        once.run(bundleContext);

        Publisher<? super Integer> op = probe.getPublisher();

        assertEquals(0, count.get());

        Runnable se = op.apply(1);

        assertEquals(1, count.get());

        se.run();

        assertEquals(0, count.get());

        se = op.apply(1);
        Runnable se2 = op.apply(2);
        Runnable se3 = op.apply(3);

        assertEquals(1, count.get());

        se.run();

        assertEquals(1, count.get());

        se3.run();

        assertEquals(1, count.get());

        se2.run();

        assertEquals(0, count.get());
    }

    @Test
    public void testProgrammaticDependencies() {
        AtomicBoolean executed = new AtomicBoolean(false);
        AtomicBoolean closed = new AtomicBoolean(false);

        String[] filters = {
            "(key=service one)",
            "(key=service two)",
            "(key=service three)"
        };

        OSGi<?> program =
            onClose(() -> closed.set(true)).foreach(
            ign -> executed.set(true)
        );

        for (String filter : filters) {
            program = services(filter).then(program);
        }

        try (OSGiResult result = program.run(bundleContext)) {
            assertFalse(closed.get());
            assertFalse(executed.get());

            ServiceRegistration<Service> serviceRegistrationOne =
                bundleContext.registerService(
                    Service.class, new Service(),
                    new Hashtable<String, Object>() {{
                        put("key", "service one");
                    }});

            assertFalse(closed.get());
            assertFalse(executed.get());

            ServiceRegistration<Service> serviceRegistrationTwo =
                bundleContext.registerService(
                    Service.class, new Service(),
                    new Hashtable<String, Object>() {{
                        put("key", "service two");
                    }});

            assertFalse(closed.get());
            assertFalse(executed.get());

            ServiceRegistration<Service> serviceRegistrationThree =
                bundleContext.registerService(
                    Service.class, new Service(),
                    new Hashtable<String, Object>() {{
                        put("key", "service three");
                    }});

            assertFalse(closed.get());
            assertTrue(executed.get());

            serviceRegistrationOne.unregister();

            assertTrue(closed.get());

            serviceRegistrationTwo.unregister();
            serviceRegistrationThree.unregister();
        }

    }

    @Test
    public void testDeprecatedRecover() {
        ArrayList<Object> result = new ArrayList<>();
        ArrayList<Object> arrived = new ArrayList<>();
        ArrayList<Object> left = new ArrayList<>();

        OSGi<Integer> program = just(
            Arrays.asList(1, 2, 3, 4, 5, 6)
        ).recover(
            (__, e) -> 0
        ).effects(
            arrived::add, left::add
        ).
        effects(
            t -> {
                if (t % 2 != 0) {
                    throw new RuntimeException();
                }
            }
            , __ -> {}
        );

        try (OSGiResult run = program.run(bundleContext, e -> {
            result.add(e);

            return NOOP;
        })) {
            assertEquals(Arrays.asList(0, 2, 0, 4, 0, 6), result);
            assertEquals(Arrays.asList(1, 0, 2, 3, 0, 4, 5, 0, 6), arrived);
            assertEquals(Arrays.asList(1, 3, 5), left);

            arrived.removeAll(left);
            assertEquals(arrived, result);
        }
    }

    @Test
    public void testDeprecatedRecoverWith() {
        ArrayList<Object> result = new ArrayList<>();
        ArrayList<Object> arrived = new ArrayList<>();
        ArrayList<Object> left = new ArrayList<>();

        OSGi<Integer> program = just(
            Arrays.asList(1, 2, 3, 4, 5, 6)
        ).recoverWith(
            (__, e) -> just(0)
        ).effects(
            arrived::add, left::add
        ).effects(
            t -> {
                if (t % 2 != 0) {
                    throw new RuntimeException();
                }
            }
            , __ -> {}
        );

        try (OSGiResult run = program.run(bundleContext, e -> {
            result.add(e);

            return NOOP;
        })) {
            assertEquals(Arrays.asList(0, 2, 0, 4, 0, 6), result);
            assertEquals(Arrays.asList(1, 0, 2, 3, 0, 4, 5, 0, 6), arrived);
            assertEquals(Arrays.asList(1, 3, 5), left);

            arrived.removeAll(left);
            assertEquals(arrived, result);
        }
    }

    @Test
    public void testRecoverWith() {
        ArrayList<Object> result = new ArrayList<>();
        ArrayList<Object> arrived = new ArrayList<>();
        ArrayList<Object> left = new ArrayList<>();

        ArrayList<ProbeImpl<Integer>> probes = new ArrayList<>();

        OSGi<Integer> program =
            recoverWith(
                just(Arrays.asList(1, 2, 3, 4, 5, 6)).
                    flatMap(t -> {
                        ProbeImpl<Integer> probe = new ProbeImpl<>();

                        probes.add(probe);

                        return probe.then(just(t));
                    }).effects(
                    arrived::add, left::add
                ).effects(
                    t -> {
                        if (t % 2 != 0) {
                            throw new RuntimeException();
                        }
                    }
                    , __ -> {}
                ),
                (__, e) -> just(0)
            );

        try (OSGiResult run = program.run(bundleContext, e -> {
            result.add(e);

            return NOOP;
        })) {
            for (ProbeImpl<Integer> probe : probes) {
                Publisher<? super Integer> publisher = probe.getPublisher();
                publisher.publish(0);
            }

            assertEquals(Arrays.asList(0, 2, 0, 4, 0, 6), result);
            assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), arrived);
            assertEquals(Arrays.asList(1, 3, 5), left);

            arrived.replaceAll(t -> {
                if (left.contains(t)) return 0;
                else return t;
            });
            assertEquals(arrived, result);
        }
    }

    @Test
    public void testNestedRecover() {
        ArrayList<Integer> result = new ArrayList<>();

        OSGi<Integer> program = recover(
            recover(
                just(Arrays.asList(1, 2, 3, 4, 5, 6)).
                    effects(
                        t -> {
                            if (t % 5 == 0) {
                                throw new IllegalArgumentException();
                            }
                        }
                        , __ -> {
                        }
                    ),
                (t, e) -> 2).
                effects(t -> {
                        if (t % 2 == 0) {
                            throw new IllegalArgumentException();
                        }
                    }
                    , __ -> {
                    }),
            (t, e) -> 8);

        try (OSGiResult run = program.run(bundleContext, e -> {
            result.add(e);

            return NOOP;
        })) {
            assertEquals(Arrays.asList(1, 8, 3, 8, 8, 8), result);
        }
    }


    @Test
    public void testNestedRecoverWith() {
        ArrayList<Integer> result = new ArrayList<>();

        OSGi<Integer> program = recoverWith(
            recoverWith(
                just(Arrays.asList(1, 2, 3, 4, 5, 6)).
                    effects(
                        t -> {
                            if (t % 5 == 0) {
                                throw new IllegalArgumentException();
                            }
                        }
                        , __ -> {
                        }
                    ),
                (t, e) -> just(2)).
                    effects(t -> {
                            if (t % 2 == 0) {
                                throw new IllegalArgumentException();
                            }
                        }
                        , __ -> {
                        }),
            (t, e) -> just(8));

        try (OSGiResult run = program.run(bundleContext, e -> {
                result.add(e);

                return NOOP;
        })) {
            assertEquals(Arrays.asList(1, 8, 3, 8, 8, 8), result);
        }
    }

    @Test
    public void testNestedRecoverAndRethrow() {
        ArrayList<Integer> result = new ArrayList<>();

        class A extends RuntimeException {}
        class B extends RuntimeException {}

        OSGi<Integer> program = recover(
            recover(
                just(Arrays.asList(1, 2, 3, 4, 5, 6)).
                    effects(
                        t -> {
                            if (t % 5 == 0) {
                                throw new A();
                            }
                        }
                        , __ -> { }
                    ).
                    effects(
                        t -> {
                            if (t % 2 == 0) {
                                throw new B();
                            }
                        },
                        __ -> { }
                    ),
                (t, e) -> {
                    if (e instanceof A) return 7;
                    else throw (RuntimeException)e;
                }),
            (t, e) -> 8);

        try (OSGiResult run = program.run(bundleContext, e -> {
            result.add(e);

            return NOOP;
        })) {
            assertEquals(Arrays.asList(1, 8, 3, 8, 7, 8), result);
        }
    }


    @Test
    public void testNestedRecoverWithAndRethrow() {
        ArrayList<Integer> result = new ArrayList<>();

        class A extends RuntimeException {}
        class B extends RuntimeException {}

        OSGi<Integer> program = recoverWith(
            recoverWith(
                just(Arrays.asList(1, 2, 3, 4, 5, 6)).
                effects(
                    t -> {
                        if (t % 5 == 0) {
                            throw new A();
                        }
                    }
                    , __ -> { }
                ).
                effects(
                    t -> {
                        if (t % 2 == 0) {
                            throw new B();
                        }
                    },
                    __ -> { }
                ),
                (t, e) -> {
                    if (e instanceof A) return just(7);
                    else throw (RuntimeException)e;
                }),
            (t, e) -> just(8));

        try (OSGiResult run = program.run(bundleContext, e -> {
            result.add(e);

            return NOOP;
        })) {
            assertEquals(Arrays.asList(1, 8, 3, 8, 7, 8), result);
        }
    }

    @Test
    public void testNestedRecoverWithDelayedErrorOnRecoverBranch() {
        ArrayList<Integer> result = new ArrayList<>();

        ArrayList<ProbeImpl<Integer>> probes = new ArrayList<>();

        OSGi<Integer> program = recoverWith(
            recoverWith(
                just(Arrays.asList(1, 2, 3, 4, 5, 6)).
                    effects(
                        t -> {
                            if (t % 5 == 0) {
                                throw new IllegalArgumentException();
                            }
                        }
                        , __ -> {
                        }
                    ),
                (t, e) -> just(2).flatMap(s -> {
                    ProbeImpl<Integer> probe = new ProbeImpl<>();

                    probes.add(probe);

                    return probe.then(
                        just(0).effects(
                            __ -> {
                                throw (RuntimeException)e;
                                },
                            __ -> {}
                        ));
                })).
                effects(t -> {
                        if (t % 2 == 0) {
                            throw new IllegalArgumentException();
                        }
                    }
                    , __ -> {
                    }),
            (t, e) -> just(8));

        try (OSGiResult run = program.run(bundleContext, e -> {
            result.add(e);

            return NOOP;
        })) {
            assertEquals(Arrays.asList(1, 8, 3, 8, 8), result);

            for (ProbeImpl<Integer> probe : probes) {
                probe.getPublisher().publish(0);
            }

            assertEquals(Arrays.asList(1, 8, 3, 8, 8, 8), result);
        }
    }

    @Test
    public void testRegister() {
        assertNull(bundleContext.getServiceReference(Service.class));

        Service service = new Service();

        OSGiResult result = register(
            Service.class, service, new HashMap<>()).
            run(bundleContext);

        ServiceReference<Service> serviceReference =
            bundleContext.getServiceReference(Service.class);

        assertEquals(service, bundleContext.getService(serviceReference));

        result.close();

        assertNull(bundleContext.getServiceReference(Service.class));
    }

    @Test
    public void testServiceReferenceRefresher() {
        ServiceRegistration<Service> serviceRegistration =
            bundleContext.registerService(
                Service.class, new Service(),
                new Hashtable<String, Object>() {{
                    put("good", 0);
                    put("bad", 0);
                }});

        AtomicInteger atomicInteger = new AtomicInteger();

        try {
            /* reload only when property "good" has changed */
            OSGi<?> program = serviceReferences(
                Service.class, csr -> csr.isDirty("good")).map(
                    csr -> csr.getProperty("good"));

            program.run(bundleContext, (__) -> {
                atomicInteger.incrementAndGet();

                return NOOP;
            });

            assertEquals(1, atomicInteger.get());

            serviceRegistration.setProperties(
                new Hashtable<String, Object>() {{
                    put("good", 0);
                    put("bad", 1);
                }});

            assertEquals(1, atomicInteger.get());

            serviceRegistration.setProperties(
                new Hashtable<String, Object>() {{
                    put("good", 1);
                    put("bad", 1);
                }});

            assertEquals(2, atomicInteger.get());
        }
        finally {
            serviceRegistration.unregister();
        }
    }

    @Test
    public void testServiceReferenceUpdates() {
        AtomicReference<String> atomicReference = new AtomicReference<>();

        ServiceRegistration<Service> serviceRegistration =
            bundleContext.registerService(
                Service.class, new Service(),
                new Hashtable<String, Object>() {{
                    put("property", "original");
                }});

        AtomicInteger atomicInteger = new AtomicInteger();

        try {
            OSGi<?> program =
                serviceReferences(
                    Service.class,
                    csr -> csr.getServiceReference().getProperty("property").equals("refresh")
                ).effects(
                    csr -> {
                        atomicReference.set(
                            String.valueOf(
                                csr.getServiceReference().getProperty("property")));

                        atomicInteger.incrementAndGet();
                    },
                    __ -> {},
                    __ -> {},
                    __ -> {},
                    csr ->
                        atomicReference.set(
                            String.valueOf(
                                csr.getServiceReference().getProperty("property")))
                );

            program.run(bundleContext);

            assertEquals(1, atomicInteger.get());
            assertEquals("original", atomicReference.get());

            serviceRegistration.setProperties(
                new Hashtable<String, Object>() {{
                    put("property", "updated");
                }});

            assertEquals(1, atomicInteger.get());
            assertEquals("updated", atomicReference.get());

            serviceRegistration.setProperties(
                new Hashtable<String, Object>() {{
                    put("property", "refresh");
                }});

            assertEquals(2, atomicInteger.get());
            assertEquals("refresh", atomicReference.get());
        }
        finally {
            serviceRegistration.unregister();
        }
    }

    @Test
    public void testServiceReferenceWithFilterUpdates() {
        AtomicReference<String> atomicReference = new AtomicReference<>();

        ServiceRegistration<Service> serviceRegistration =
            bundleContext.registerService(
                Service.class, new Service(),
                new Hashtable<String, Object>() {{
                    put("property", "original");
                    put("admissible", "true");
                }});

        AtomicInteger atomicInteger = new AtomicInteger();

        try {
            OSGi<?> program =
                refreshAsUpdates(
                    serviceReferences(
                        Service.class
                    ).filter(
                        sr -> sr.getProperty("admissible").equals("true")
                    )
                ).map(
                    CachingServiceReference::getServiceReference
                ).effects(
                    sr -> {
                        atomicReference.set(
                            String.valueOf(sr.getProperty("property")));

                        atomicInteger.incrementAndGet();
                    },
                    __ -> {},
                    __ -> {},
                    __ -> atomicReference.set(null),
                    sr ->
                        atomicReference.set(
                            String.valueOf(sr.getProperty("property")))
                );

            program.run(bundleContext);

            assertEquals(1, atomicInteger.get());
            assertEquals("original", atomicReference.get());

            serviceRegistration.setProperties(
                new Hashtable<String, Object>() {{
                    put("property", "updated");
                    put("admissible", "true");
                }});

            assertEquals(1, atomicInteger.get());
            assertEquals("updated", atomicReference.get());

            serviceRegistration.setProperties(
                new Hashtable<String, Object>() {{
                    put("property", "updated");
                    put("admissible", "false");
                }});

            assertEquals(1, atomicInteger.get());
            assertNull(atomicReference.get());
        }
        finally {
            serviceRegistration.unregister();
        }
    }


    @Test
    public void testServiceReferenceUpdatesWithSelector() {
        AtomicReference<String> atomicReference = new AtomicReference<>();

        ServiceRegistration<Service> serviceRegistration =
            bundleContext.registerService(
                Service.class, new Service(),
                new Hashtable<String, Object>() {{
                    put("property", "original");
                }});

        AtomicInteger atomicInteger = new AtomicInteger();

        try {
            OSGi<?> program = refreshWhen(
                    ServiceReferences.withUpdate(Service.class),
                    csr -> csr.getServiceReference().getProperty("property").equals("refresh")
            ).effects(
                csr -> {
                    atomicReference.set(String.valueOf(csr.getServiceReference().getProperty("property")));

                    atomicInteger.incrementAndGet();
                },
                __ -> {},
                __ -> {},
                __ -> {},
                csr ->
                    atomicReference.set(
                        String.valueOf(csr.getServiceReference().getProperty("property")))
            );

            program.run(bundleContext);

            assertEquals(1, atomicInteger.get());
            assertEquals("original", atomicReference.get());

            serviceRegistration.setProperties(
                new Hashtable<String, Object>() {{
                    put("property", "updated");
                }});

            assertEquals(1, atomicInteger.get());
            assertEquals("updated", atomicReference.get());

            serviceRegistration.setProperties(
                new Hashtable<String, Object>() {{
                    put("property", "refresh");
                }});

            assertEquals(2, atomicInteger.get());
            assertEquals("refresh", atomicReference.get());
        }
        finally {
            serviceRegistration.unregister();
        }
    }

    @Test
    public void testServiceReferences() {
        AtomicReference<CachingServiceReference<Service>> atomicReference =
            new AtomicReference<>();

        ServiceRegistration<Service> serviceRegistration = null;

        try(
            OSGiResult osGiResult =
                serviceReferences(Service.class).
                run(bundleContext, newValue -> {
                    atomicReference.set(newValue);

                    return NOOP;
                })
        ) {
            assertNull(atomicReference.get());

            serviceRegistration = bundleContext.registerService(
                Service.class, new Service(), new Hashtable<>());

            assertEquals(
                serviceRegistration.getReference(),
                atomicReference.get().getServiceReference());
        }
        finally {
            if (serviceRegistration != null) {
                serviceRegistration.unregister();
            }
        }
    }

    @Test
    public void testServiceReferencesAndClose() {
        AtomicReference<CachingServiceReference<Service>> atomicReference =
            new AtomicReference<>();

        OSGi<CachingServiceReference<Service>> program =
            serviceReferences(Service.class).flatMap(ref ->
            onClose(() -> atomicReference.set(null)).
            then(just(ref))
        );

        ServiceRegistration<Service> serviceRegistration = null;

        try(
            OSGiResult osGiResult = program.run(
            bundleContext, newValue -> {
                    atomicReference.set(newValue);

                    return NOOP;
                })
        ) {
            assertNull(atomicReference.get());

            serviceRegistration = bundleContext.registerService(
                Service.class, new Service(), new Hashtable<>());

            assertEquals(
                serviceRegistration.getReference(),
                atomicReference.get().getServiceReference());
        }
        finally {
            if (serviceRegistration != null) {
                serviceRegistration.unregister();
            }
        }

        assertNull(atomicReference.get());
    }

    @Test
    public void testServicesMap() {
        AtomicReference<Map<String, String>> map = new AtomicReference<>(
            new HashMap<>());
        List<Map<String, String>> maps = new ArrayList<>();
        List<Map<String, String>> gone = new ArrayList<>();

        OSGi<Map<String, String>> mapOSGi =
            Utils.accumulateInMap(
                serviceReferences(Service.class),
                csr -> just(Arrays.asList(canonicalize(csr.getProperty("key")))),
                csr -> just(getId(csr)));

        OSGi<Map<String, String>> effects =
            mapOSGi.
                effects(map::set, __ -> {}).
                effects(maps::add, gone::add);

        OSGiResult result = effects.run(bundleContext);

        assertEquals(Collections.emptyMap(), map.get());

        ServiceRegistration<Service> serviceRegistrationOne =
            bundleContext.registerService(
                Service.class, new Service(),
                new Hashtable<String, Object>() {{
                    put("key", new String[]{"a"});
                }});

        assertEquals(
            new HashMap<String, String>() {{
                put("a", getId(serviceRegistrationOne));
            }},
            map.get());

        ServiceRegistration<Service> serviceRegistrationTwo =
            bundleContext.registerService(
                Service.class, new Service(),
                new Hashtable<String, Object>() {{
                    put("key", new String[]{"b"});
                }});

        assertEquals(
            new HashMap<String, String>() {{
                put("a", getId(serviceRegistrationOne));
                put("b", getId(serviceRegistrationTwo));
            }},
            map.get());

        ServiceRegistration<Service> serviceRegistrationThree =
            bundleContext.registerService(
                Service.class, new Service(),
                new Hashtable<String, Object>() {{
                    put("key", new String[]{"a", "b"});
                    put("service.ranking", 10);
                }});

        assertEquals(
            new HashMap<String, String>() {{
                put("a", getId(serviceRegistrationThree));
                put("b", getId(serviceRegistrationThree));
            }},
            map.get());

        serviceRegistrationThree.unregister();

        assertEquals(
            new HashMap<String, String>() {{
                put("a", getId(serviceRegistrationOne));
                put("b", getId(serviceRegistrationTwo));
            }},
            map.get());

        serviceRegistrationTwo.unregister();

        assertEquals(
            new HashMap<String, String>() {{
                put("a", getId(serviceRegistrationOne));
            }},
            map.get());

        serviceRegistrationOne.unregister();

        assertEquals(Collections.emptyMap(), map.get());

        result.close();

        assertEquals(maps, gone);
    }
    static BundleContext bundleContext = FrameworkUtil.getBundle(
        DSLTest.class).getBundleContext();

    private static String[] canonicalize(Object propertyValue) {
        if (propertyValue == null) {
            return new String[0];
        }
        if (propertyValue instanceof String[]) {
            return (String[]) propertyValue;
        }
        if (propertyValue instanceof Collection) {
            return
                ((Collection<?>)propertyValue).stream().
                    map(
                        Object::toString
                    ).toArray(
                    String[]::new
                );
        }

        return new String[]{propertyValue.toString()};
    }

    private String getId(CachingServiceReference<?> csr) {
        return csr.getProperty("service.id").toString();
    }

    private String getId(ServiceRegistration<?> sr) {
        return sr.getReference().getProperty("service.id").toString();
    }

    private class Service {}

}
