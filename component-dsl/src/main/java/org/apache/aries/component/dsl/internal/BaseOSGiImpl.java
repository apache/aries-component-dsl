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
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;

import java.util.IdentityHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * @author Carlos Sierra Andrés
 */
public class BaseOSGiImpl<T> implements OSGi<T> {

	protected BaseOSGiImpl(OSGiRunnable<T> operation) {
		_operation = operation;
	}

	@Override
	public OSGiResult run(ExecutionContext executionContext) {
		return run(executionContext, x -> NOOP);
	}

	public OSGiResult run(
		ExecutionContext executionContext, Publisher<? super T> op) {

		return _operation.run(executionContext, op);
	}

	static Filter buildFilter(
		ExecutionContext executionContext, String filterString, Class<?> clazz) {

		Filter filter;

		String string = buildFilterString(filterString, clazz);

		try {
			filter = executionContext.getBundleContext().createFilter(string);
		}
		catch (InvalidSyntaxException e) {
			throw new RuntimeException(e);
		}

		return filter;
	}

	static String buildFilterString(String filterString, Class<?> clazz) {
		if (filterString == null && clazz == null) {
			throw new IllegalArgumentException(
				"Both filterString and clazz can't be null");
		}

		StringBuilder stringBuilder = new StringBuilder();

		if (filterString != null) {
			stringBuilder.append(filterString);
		}

		if (clazz != null) {
			boolean extend = !(stringBuilder.length() == 0);
			if (extend) {
				stringBuilder.insert(0, "(&");
			}

			stringBuilder.
				append("(objectClass=").
				append(clazz.getName()).
				append(")");

			if (extend) {
				stringBuilder.append(")");
			}

		}

		return stringBuilder.toString();
	}

	OSGiRunnable<T> _operation;

	@Override
	public <S> OSGi<S> applyTo(OSGi<Function<T, S>> fun) {
		return new BaseOSGiImpl<>((executionContext, op) -> {
			ConcurrentDoublyLinkedList<T> identities = new ConcurrentDoublyLinkedList<>();
			ConcurrentDoublyLinkedList<Function<T,S>> functions = new ConcurrentDoublyLinkedList<>();
			IdentityHashMap<T, IdentityHashMap<Function<T, S>, OSGiResult>>
				terminators = new IdentityHashMap<>();

			OSGiResult funRun = fun.run(
				executionContext,
				op.pipe(f -> {
					synchronized(identities) {
						ConcurrentDoublyLinkedList.Node node = functions.addLast(f);

						for (T t : identities) {
							IdentityHashMap<Function<T, S>, OSGiResult> terminatorMap =
								terminators.computeIfAbsent(
									t, __ -> new IdentityHashMap<>());
							terminatorMap.put(f, op.apply(f.apply(t)));
						}

						return new OSGiResultImpl(
							() -> {
								synchronized (identities) {
									node.remove();

									identities.forEach(t -> {
										Runnable terminator = terminators.get(t).remove(f);

										terminator.run();
									});
								}
							},
							() -> {
								synchronized (identities) {
									return identities.stream().map(t -> {
										OSGiResult terminator = terminators.get(t).get(f);

										return terminator.update();
									}).reduce(
										Boolean.FALSE, Boolean::logicalOr
									);
								}
							}
						);
					}
				}
			));

			OSGiResult myRun = run(
				executionContext,
				op.pipe(t -> {
					synchronized (identities) {
						ConcurrentDoublyLinkedList.Node node = identities.addLast(t);

						for (Function<T, S> f : functions) {
							IdentityHashMap<Function<T, S>, OSGiResult> terminatorMap =
								terminators.computeIfAbsent(
									t, __ -> new IdentityHashMap<>());
							terminatorMap.put(f, op.apply(f.apply(t)));
						}

						return new OSGiResultImpl(
							() -> {
								synchronized (identities) {
									node.remove();

									functions.forEach(f -> {
										Runnable terminator = terminators.get(t).remove(f);

										terminator.run();
									});
								}
							},
							() -> {
								synchronized (identities) {
									return functions.stream().map(f -> {
										OSGiResult terminator = terminators.get(t).get(f);

										return terminator.update();
									}).reduce(
										Boolean.FALSE, Boolean::logicalOr
									);
								}
							}
						);
					}
				})
			);

			return new AggregateOSGiResult(myRun, funRun);
		});
	}

	@Override
	public <S> OSGi<S> choose(
		Function<T, OSGi<Boolean>> chooser, Function<OSGi<T>, OSGi<S>> then,
		Function<OSGi<T>, OSGi<S>> otherwise) {

		return new BaseOSGiImpl<>((executionContext, op) -> {
			Pad<T, S> thenPad = new Pad<>(executionContext, then, op);
			Pad<T, S> elsePad = new Pad<>(executionContext, otherwise, op);

			OSGiResult result = run(
				executionContext,
				op.pipe(t -> chooser.apply(t).run(
                    executionContext,
                    b -> {
                        if (b) {
                            return thenPad.publish(t);
                        } else {
                            return elsePad.publish(t);
                        }
                    }
                )));
			return new AggregateOSGiResult(thenPad, elsePad, result);
		});
	}

	@Override
	public <S> OSGi<S> distribute(@SuppressWarnings("unchecked") Function<OSGi<T>, OSGi<S>>... funs) {
		return new DistributeOSGiImpl<>(this, funs);
	}

	@Override
	public OSGi<T> effects(
		Consumer<? super T> onAddedBefore, Consumer<? super T> onAddedAfter,
		Consumer<? super T> onRemovedBefore,
		Consumer<? super T> onRemovedAfter,
		Consumer<? super T> onUpdate) {

		return transform(op -> t -> {
			onAddedBefore.accept(t);

			try {
				OSGiResult terminator = op.publish(t);

				OSGiResult result = new OSGiResultImpl(() -> {
					try {
						onRemovedBefore.accept(t);
					}
					catch (Exception e) {
						//TODO: logging
					}

					try {
						terminator.run();
					}
					catch (Exception e) {
						//TODO: logging
					}

					try {
						onRemovedAfter.accept(t);
					}
					catch (Exception e) {
						//TODO: logging
					}
				},
					() -> {
						AtomicBoolean atomicBoolean = new AtomicBoolean();

						UpdateSupport.deferPublication(() -> {
							if (!atomicBoolean.get()) {
								onUpdate.accept(t);
							}
						});

						atomicBoolean.set(terminator.update());

						return atomicBoolean.get();
					}
				);

				try {
					onAddedAfter.accept(t);
				}
				catch (Exception e) {
					result.run();

					throw e;
				}

				return result;
			}
			catch (Exception e) {
				try {
					onRemovedAfter.accept(t);
				}
				catch (Exception e1) {
					//TODO: logging
				}

				throw e;
			}
				}
		);
	}

	@Override
	public OSGi<T> filter(Predicate<T> predicate) {
		return transform(
			op -> t -> {
				if (predicate.test(t)) {
					return op.apply(t);
				}
				else {
					return NOOP;
				}
			}
		);
	}

	@Override
	public <S> OSGi<S> flatMap(Function<? super T, OSGi<? extends S>> fun) {
		return new BaseOSGiImpl<>((executionContext, op) ->
			run(executionContext, op.pipe(t -> fun.apply(t).run(executionContext, op))));
	}

	@Override
	public <S> OSGi<S> map(Function<? super T, ? extends S> function) {
		return transform(op -> t -> op.apply(function.apply(t)));
	}

	@Deprecated
	@Override
	public OSGi<T> recover(BiFunction<T, Exception, T> onError) {
		return new BaseOSGiImpl<>((executionContext, op) ->
			run(
				executionContext,
				t -> {
					try {
						return op.apply(t);
					}
					catch (Exception e) {
						return op.apply(onError.apply(t, e));
					}
				}
			));
	}

	@Deprecated
	@Override
	public OSGi<T> recoverWith(BiFunction<T, Exception, OSGi<T>> onError) {
		return new BaseOSGiImpl<>((executionContext, op) ->
			run(
				executionContext,
				t -> {
					try {
						return op.apply(t);
					}
					catch (Exception e) {
						return onError.apply(t, e).run(executionContext, op);
					}
				}
			));
	}

	@Override
	public <K, S> OSGi<S> splitBy(
		Function<T, OSGi<K>> mapper, BiFunction<K, OSGi<T>, OSGi<S>> fun) {

		return new BaseOSGiImpl<>((executionContext, op) -> {
			ConcurrentHashMap<K, Pad<T, S>> pads = new ConcurrentHashMap<>();

			OSGiResult result = run(
				executionContext,
				op.pipe(t -> mapper.apply(t).run(
					executionContext,
					k -> pads.computeIfAbsent(
						k,
						__ -> new Pad<>(
							executionContext,
							___ -> fun.apply(k, ___), op)
					).publish(t)
				)
			));

			return new OSGiResultImpl(
				() -> {
					pads.values().forEach(Pad::close);

					result.close();
				},
				() -> pads.values().stream().map(
					Pad::update
				).reduce(
					Boolean.FALSE, Boolean::logicalOr
				) | result.update()
			);
		});
	}

	@Override
	public <S> OSGi<S> transform(Transformer<T, S> fun) {
		return new BaseOSGiImpl<>((executionContext, op) -> run(
				executionContext, op.pipe(fun.transform(op)::apply)));
	}

}


