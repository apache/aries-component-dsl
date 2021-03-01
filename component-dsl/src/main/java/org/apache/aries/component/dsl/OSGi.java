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

import org.apache.aries.component.dsl.function.Function10;
import org.apache.aries.component.dsl.function.Function14;
import org.apache.aries.component.dsl.function.Function16;
import org.apache.aries.component.dsl.function.Function19;
import org.apache.aries.component.dsl.function.Function2;
import org.apache.aries.component.dsl.function.Function20;
import org.apache.aries.component.dsl.function.Function25;
import org.apache.aries.component.dsl.function.Function4;
import org.apache.aries.component.dsl.function.Function6;
import org.apache.aries.component.dsl.function.Function8;
import org.apache.aries.component.dsl.function.Function9;
import org.apache.aries.component.dsl.internal.*;
import org.apache.aries.component.dsl.function.Function11;
import org.apache.aries.component.dsl.function.Function12;
import org.apache.aries.component.dsl.function.Function13;
import org.apache.aries.component.dsl.function.Function15;
import org.apache.aries.component.dsl.function.Function17;
import org.apache.aries.component.dsl.function.Function18;
import org.apache.aries.component.dsl.function.Function21;
import org.apache.aries.component.dsl.function.Function22;
import org.apache.aries.component.dsl.function.Function23;
import org.apache.aries.component.dsl.function.Function24;
import org.apache.aries.component.dsl.function.Function26;
import org.apache.aries.component.dsl.function.Function3;
import org.apache.aries.component.dsl.function.Function5;
import org.apache.aries.component.dsl.function.Function7;
import org.apache.aries.component.dsl.update.UpdateQuery;
import org.apache.aries.component.dsl.update.UpdateSelector;
import org.apache.aries.component.dsl.update.UpdateTuple;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.*;

/**
 * @author Carlos Sierra Andrés
 */
public interface OSGi<T> extends OSGiRunnable<T> {
	OSGiResult NOOP = () -> {};

	@SafeVarargs
	static <T> OSGi<T> all(OSGi<T> ... programs) {
		return new AllOSGi<>(programs);
	}

	static OSGi<BundleContext> bundleContext() {
		return new BundleContextOSGiImpl();
	}

	static OSGi<Bundle> bundles(int stateMask) {
		return new BundleOSGi(stateMask);
	}

	static <T> OSGi<T> changeContext(
		BundleContext bundleContext, OSGi<T> program) {

		return new ChangeContextOSGiImpl<>(program, bundleContext);
	}

	@SafeVarargs
	static <T> OSGi<T> coalesce(OSGi<T> ... programs) {
		return new CoalesceOSGiImpl<>(programs);
	}

	static <A, B, RES> OSGi<RES> combine(Function2<A, B, RES> fun, OSGi<A> a, OSGi<B> b) {
		return b.applyTo(a.applyTo(just(fun.curried())));
	}

	static <A, B, C, RES> OSGi<RES> combine(Function3<A, B, C, RES> fun, OSGi<A> a, OSGi<B> b, OSGi<C> c) {
		return c.applyTo(combine((A aa, B bb) -> fun.curried().apply(aa).apply(bb), a, b));
	}

	static <A, B, C, D, RES> OSGi<RES> combine(Function4<A, B, C, D, RES> fun, OSGi<A> a, OSGi<B> b, OSGi<C> c, OSGi<D> d) {
		return d.applyTo(combine((A aa, B bb, C cc) -> fun.curried().apply(aa).apply(bb).apply(cc), a, b, c));
	}

	static <A, B, C, D, E, RES> OSGi<RES> combine(Function5<A, B, C, D, E, RES> fun, OSGi<A> a, OSGi<B> b, OSGi<C> c, OSGi<D> d, OSGi<E> e) {
		return e.applyTo(combine((A aa, B bb, C cc, D dd) -> fun.curried().apply(aa).apply(bb).apply(cc).apply(dd), a, b, c, d));
	}

	static <A, B, C, D, E, F, RES> OSGi<RES> combine(Function6<A, B, C, D, E, F, RES> fun, OSGi<A> a, OSGi<B> b, OSGi<C> c, OSGi<D> d, OSGi<E> e, OSGi<F> f) {
		return f.applyTo(combine((A aa, B bb, C cc, D dd, E ee) -> fun.curried().apply(aa).apply(bb).apply(cc).apply(dd).apply(ee), a, b, c, d, e));
	}

	static <A, B, C, D, E, F, G, RES> OSGi<RES> combine(Function7<A, B, C, D, E, F, G, RES> fun, OSGi<A> a, OSGi<B> b, OSGi<C> c, OSGi<D> d, OSGi<E> e, OSGi<F> f, OSGi<G> g) {
		return g.applyTo(combine((A aa, B bb, C cc, D dd, E ee, F ff) -> fun.curried().apply(aa).apply(bb).apply(cc).apply(dd).apply(ee).apply(ff), a, b, c, d, e, f));
	}

	static <A, B, C, D, E, F, G, H, RES> OSGi<RES> combine(Function8<A, B, C, D, E, F, G, H, RES> fun, OSGi<A> a, OSGi<B> b, OSGi<C> c, OSGi<D> d, OSGi<E> e, OSGi<F> f, OSGi<G> g, OSGi<H> h) {
		return h.applyTo(combine((A aa, B bb, C cc, D dd, E ee, F ff, G gg) -> fun.curried().apply(aa).apply(bb).apply(cc).apply(dd).apply(ee).apply(ff).apply(gg), a, b, c, d, e, f, g));
	}

	static <A, B, C, D, E, F, G, H, I, RES> OSGi<RES> combine(Function9<A, B, C, D, E, F, G, H, I, RES> fun, OSGi<A> a, OSGi<B> b, OSGi<C> c, OSGi<D> d, OSGi<E> e, OSGi<F> f, OSGi<G> g, OSGi<H> h, OSGi<I> i) {
		return i.applyTo(combine((A aa, B bb, C cc, D dd, E ee, F ff, G gg, H hh) -> fun.curried().apply(aa).apply(bb).apply(cc).apply(dd).apply(ee).apply(ff).apply(gg).apply(hh), a, b, c, d, e, f, g, h));
	}

	static <A, B, C, D, E, F, G, H, I, J, RES> OSGi<RES> combine(Function10<A, B, C, D, E, F, G, H, I, J, RES> fun, OSGi<A> a, OSGi<B> b, OSGi<C> c, OSGi<D> d, OSGi<E> e, OSGi<F> f, OSGi<G> g, OSGi<H> h, OSGi<I> i, OSGi<J> j) {
		return j.applyTo(combine((A aa, B bb, C cc, D dd, E ee, F ff, G gg, H hh, I ii) -> fun.curried().apply(aa).apply(bb).apply(cc).apply(dd).apply(ee).apply(ff).apply(gg).apply(hh).apply(ii), a, b, c, d, e, f, g, h, i));
	}

	static <A, B, C, D, E, F, G, H, I, J, K, RES> OSGi<RES> combine(Function11<A, B, C, D, E, F, G, H, I, J, K, RES> fun, OSGi<A> a, OSGi<B> b, OSGi<C> c, OSGi<D> d, OSGi<E> e, OSGi<F> f, OSGi<G> g, OSGi<H> h, OSGi<I> i, OSGi<J> j, OSGi<K> k) {
		return k.applyTo(combine((A aa, B bb, C cc, D dd, E ee, F ff, G gg, H hh, I ii, J jj) -> fun.curried().apply(aa).apply(bb).apply(cc).apply(dd).apply(ee).apply(ff).apply(gg).apply(hh).apply(ii).apply(jj), a, b, c, d, e, f, g, h, i, j));
	}

	static <A, B, C, D, E, F, G, H, I, J, K, L, RES> OSGi<RES> combine(Function12<A, B, C, D, E, F, G, H, I, J, K, L, RES> fun, OSGi<A> a, OSGi<B> b, OSGi<C> c, OSGi<D> d, OSGi<E> e, OSGi<F> f, OSGi<G> g, OSGi<H> h, OSGi<I> i, OSGi<J> j, OSGi<K> k, OSGi<L> l) {
		return l.applyTo(combine((A aa, B bb, C cc, D dd, E ee, F ff, G gg, H hh, I ii, J jj, K kk) -> fun.curried().apply(aa).apply(bb).apply(cc).apply(dd).apply(ee).apply(ff).apply(gg).apply(hh).apply(ii).apply(jj).apply(kk), a, b, c, d, e, f, g, h, i, j, k));
	}

	static <A, B, C, D, E, F, G, H, I, J, K, L, M, RES> OSGi<RES> combine(Function13<A, B, C, D, E, F, G, H, I, J, K, L, M, RES> fun, OSGi<A> a, OSGi<B> b, OSGi<C> c, OSGi<D> d, OSGi<E> e, OSGi<F> f, OSGi<G> g, OSGi<H> h, OSGi<I> i, OSGi<J> j, OSGi<K> k, OSGi<L> l, OSGi<M> m) {
		return m.applyTo(combine((A aa, B bb, C cc, D dd, E ee, F ff, G gg, H hh, I ii, J jj, K kk, L ll) -> fun.curried().apply(aa).apply(bb).apply(cc).apply(dd).apply(ee).apply(ff).apply(gg).apply(hh).apply(ii).apply(jj).apply(kk).apply(ll), a, b, c, d, e, f, g, h, i, j, k, l));
	}

	static <A, B, C, D, E, F, G, H, I, J, K, L, M, N, RES> OSGi<RES> combine(Function14<A, B, C, D, E, F, G, H, I, J, K, L, M, N, RES> fun, OSGi<A> a, OSGi<B> b, OSGi<C> c, OSGi<D> d, OSGi<E> e, OSGi<F> f, OSGi<G> g, OSGi<H> h, OSGi<I> i, OSGi<J> j, OSGi<K> k, OSGi<L> l, OSGi<M> m, OSGi<N> n) {
		return n.applyTo(combine((A aa, B bb, C cc, D dd, E ee, F ff, G gg, H hh, I ii, J jj, K kk, L ll, M mm) -> fun.curried().apply(aa).apply(bb).apply(cc).apply(dd).apply(ee).apply(ff).apply(gg).apply(hh).apply(ii).apply(jj).apply(kk).apply(ll).apply(mm), a, b, c, d, e, f, g, h, i, j, k, l, m));
	}

	static <A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, RES> OSGi<RES> combine(Function15<A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, RES> fun, OSGi<A> a, OSGi<B> b, OSGi<C> c, OSGi<D> d, OSGi<E> e, OSGi<F> f, OSGi<G> g, OSGi<H> h, OSGi<I> i, OSGi<J> j, OSGi<K> k, OSGi<L> l, OSGi<M> m, OSGi<N> n, OSGi<O> o) {
		return o.applyTo(combine((A aa, B bb, C cc, D dd, E ee, F ff, G gg, H hh, I ii, J jj, K kk, L ll, M mm, N nn) -> fun.curried().apply(aa).apply(bb).apply(cc).apply(dd).apply(ee).apply(ff).apply(gg).apply(hh).apply(ii).apply(jj).apply(kk).apply(ll).apply(mm).apply(nn), a, b, c, d, e, f, g, h, i, j, k, l, m, n));
	}

	static <A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, RES> OSGi<RES> combine(Function16<A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, RES> fun, OSGi<A> a, OSGi<B> b, OSGi<C> c, OSGi<D> d, OSGi<E> e, OSGi<F> f, OSGi<G> g, OSGi<H> h, OSGi<I> i, OSGi<J> j, OSGi<K> k, OSGi<L> l, OSGi<M> m, OSGi<N> n, OSGi<O> o, OSGi<P> p) {
		return p.applyTo(combine((A aa, B bb, C cc, D dd, E ee, F ff, G gg, H hh, I ii, J jj, K kk, L ll, M mm, N nn, O oo) -> fun.curried().apply(aa).apply(bb).apply(cc).apply(dd).apply(ee).apply(ff).apply(gg).apply(hh).apply(ii).apply(jj).apply(kk).apply(ll).apply(mm).apply(nn).apply(oo), a, b, c, d, e, f, g, h, i, j, k, l, m, n, o));
	}

	static <A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, RES> OSGi<RES> combine(Function17<A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, RES> fun, OSGi<A> a, OSGi<B> b, OSGi<C> c, OSGi<D> d, OSGi<E> e, OSGi<F> f, OSGi<G> g, OSGi<H> h, OSGi<I> i, OSGi<J> j, OSGi<K> k, OSGi<L> l, OSGi<M> m, OSGi<N> n, OSGi<O> o, OSGi<P> p, OSGi<Q> q) {
		return q.applyTo(combine((A aa, B bb, C cc, D dd, E ee, F ff, G gg, H hh, I ii, J jj, K kk, L ll, M mm, N nn, O oo, P pp) -> fun.curried().apply(aa).apply(bb).apply(cc).apply(dd).apply(ee).apply(ff).apply(gg).apply(hh).apply(ii).apply(jj).apply(kk).apply(ll).apply(mm).apply(nn).apply(oo).apply(pp), a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p));
	}

	static <A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, RES> OSGi<RES> combine(Function18<A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, RES> fun, OSGi<A> a, OSGi<B> b, OSGi<C> c, OSGi<D> d, OSGi<E> e, OSGi<F> f, OSGi<G> g, OSGi<H> h, OSGi<I> i, OSGi<J> j, OSGi<K> k, OSGi<L> l, OSGi<M> m, OSGi<N> n, OSGi<O> o, OSGi<P> p, OSGi<Q> q, OSGi<R> r) {
		return r.applyTo(combine((A aa, B bb, C cc, D dd, E ee, F ff, G gg, H hh, I ii, J jj, K kk, L ll, M mm, N nn, O oo, P pp, Q qq) -> fun.curried().apply(aa).apply(bb).apply(cc).apply(dd).apply(ee).apply(ff).apply(gg).apply(hh).apply(ii).apply(jj).apply(kk).apply(ll).apply(mm).apply(nn).apply(oo).apply(pp).apply(qq), a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q));
	}

	static <A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, RES> OSGi<RES> combine(Function19<A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, RES> fun, OSGi<A> a, OSGi<B> b, OSGi<C> c, OSGi<D> d, OSGi<E> e, OSGi<F> f, OSGi<G> g, OSGi<H> h, OSGi<I> i, OSGi<J> j, OSGi<K> k, OSGi<L> l, OSGi<M> m, OSGi<N> n, OSGi<O> o, OSGi<P> p, OSGi<Q> q, OSGi<R> r, OSGi<S> s) {
		return s.applyTo(combine((A aa, B bb, C cc, D dd, E ee, F ff, G gg, H hh, I ii, J jj, K kk, L ll, M mm, N nn, O oo, P pp, Q qq, R rr) -> fun.curried().apply(aa).apply(bb).apply(cc).apply(dd).apply(ee).apply(ff).apply(gg).apply(hh).apply(ii).apply(jj).apply(kk).apply(ll).apply(mm).apply(nn).apply(oo).apply(pp).apply(qq).apply(rr), a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r));
	}

	static <A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, RES> OSGi<RES> combine(Function20<A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, RES> fun, OSGi<A> a, OSGi<B> b, OSGi<C> c, OSGi<D> d, OSGi<E> e, OSGi<F> f, OSGi<G> g, OSGi<H> h, OSGi<I> i, OSGi<J> j, OSGi<K> k, OSGi<L> l, OSGi<M> m, OSGi<N> n, OSGi<O> o, OSGi<P> p, OSGi<Q> q, OSGi<R> r, OSGi<S> s, OSGi<T> t) {
		return t.applyTo(combine((A aa, B bb, C cc, D dd, E ee, F ff, G gg, H hh, I ii, J jj, K kk, L ll, M mm, N nn, O oo, P pp, Q qq, R rr, S ss) -> fun.curried().apply(aa).apply(bb).apply(cc).apply(dd).apply(ee).apply(ff).apply(gg).apply(hh).apply(ii).apply(jj).apply(kk).apply(ll).apply(mm).apply(nn).apply(oo).apply(pp).apply(qq).apply(rr).apply(ss), a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s));
	}

	static <A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, RES> OSGi<RES> combine(Function21<A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, RES> fun, OSGi<A> a, OSGi<B> b, OSGi<C> c, OSGi<D> d, OSGi<E> e, OSGi<F> f, OSGi<G> g, OSGi<H> h, OSGi<I> i, OSGi<J> j, OSGi<K> k, OSGi<L> l, OSGi<M> m, OSGi<N> n, OSGi<O> o, OSGi<P> p, OSGi<Q> q, OSGi<R> r, OSGi<S> s, OSGi<T> t, OSGi<U> u) {
		return u.applyTo(combine((A aa, B bb, C cc, D dd, E ee, F ff, G gg, H hh, I ii, J jj, K kk, L ll, M mm, N nn, O oo, P pp, Q qq, R rr, S ss, T tt) -> fun.curried().apply(aa).apply(bb).apply(cc).apply(dd).apply(ee).apply(ff).apply(gg).apply(hh).apply(ii).apply(jj).apply(kk).apply(ll).apply(mm).apply(nn).apply(oo).apply(pp).apply(qq).apply(rr).apply(ss).apply(tt), a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t));
	}

	static <A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V, RES> OSGi<RES> combine(Function22<A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V, RES> fun, OSGi<A> a, OSGi<B> b, OSGi<C> c, OSGi<D> d, OSGi<E> e, OSGi<F> f, OSGi<G> g, OSGi<H> h, OSGi<I> i, OSGi<J> j, OSGi<K> k, OSGi<L> l, OSGi<M> m, OSGi<N> n, OSGi<O> o, OSGi<P> p, OSGi<Q> q, OSGi<R> r, OSGi<S> s, OSGi<T> t, OSGi<U> u, OSGi<V> v) {
		return v.applyTo(combine((A aa, B bb, C cc, D dd, E ee, F ff, G gg, H hh, I ii, J jj, K kk, L ll, M mm, N nn, O oo, P pp, Q qq, R rr, S ss, T tt, U uu) -> fun.curried().apply(aa).apply(bb).apply(cc).apply(dd).apply(ee).apply(ff).apply(gg).apply(hh).apply(ii).apply(jj).apply(kk).apply(ll).apply(mm).apply(nn).apply(oo).apply(pp).apply(qq).apply(rr).apply(ss).apply(tt).apply(uu), a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, u));
	}

	static <A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V, W, RES> OSGi<RES> combine(Function23<A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V, W, RES> fun, OSGi<A> a, OSGi<B> b, OSGi<C> c, OSGi<D> d, OSGi<E> e, OSGi<F> f, OSGi<G> g, OSGi<H> h, OSGi<I> i, OSGi<J> j, OSGi<K> k, OSGi<L> l, OSGi<M> m, OSGi<N> n, OSGi<O> o, OSGi<P> p, OSGi<Q> q, OSGi<R> r, OSGi<S> s, OSGi<T> t, OSGi<U> u, OSGi<V> v, OSGi<W> w) {
		return w.applyTo(combine((A aa, B bb, C cc, D dd, E ee, F ff, G gg, H hh, I ii, J jj, K kk, L ll, M mm, N nn, O oo, P pp, Q qq, R rr, S ss, T tt, U uu, V vv) -> fun.curried().apply(aa).apply(bb).apply(cc).apply(dd).apply(ee).apply(ff).apply(gg).apply(hh).apply(ii).apply(jj).apply(kk).apply(ll).apply(mm).apply(nn).apply(oo).apply(pp).apply(qq).apply(rr).apply(ss).apply(tt).apply(uu).apply(vv), a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, u, v));
	}

	static <A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V, W, X, RES> OSGi<RES> combine(Function24<A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V, W, X, RES> fun, OSGi<A> a, OSGi<B> b, OSGi<C> c, OSGi<D> d, OSGi<E> e, OSGi<F> f, OSGi<G> g, OSGi<H> h, OSGi<I> i, OSGi<J> j, OSGi<K> k, OSGi<L> l, OSGi<M> m, OSGi<N> n, OSGi<O> o, OSGi<P> p, OSGi<Q> q, OSGi<R> r, OSGi<S> s, OSGi<T> t, OSGi<U> u, OSGi<V> v, OSGi<W> w, OSGi<X> x) {
		return x.applyTo(combine((A aa, B bb, C cc, D dd, E ee, F ff, G gg, H hh, I ii, J jj, K kk, L ll, M mm, N nn, O oo, P pp, Q qq, R rr, S ss, T tt, U uu, V vv, W ww) -> fun.curried().apply(aa).apply(bb).apply(cc).apply(dd).apply(ee).apply(ff).apply(gg).apply(hh).apply(ii).apply(jj).apply(kk).apply(ll).apply(mm).apply(nn).apply(oo).apply(pp).apply(qq).apply(rr).apply(ss).apply(tt).apply(uu).apply(vv).apply(ww), a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, u, v, w));
	}

	static <A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V, W, X, Y, RES> OSGi<RES> combine(Function25<A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V, W, X, Y, RES> fun, OSGi<A> a, OSGi<B> b, OSGi<C> c, OSGi<D> d, OSGi<E> e, OSGi<F> f, OSGi<G> g, OSGi<H> h, OSGi<I> i, OSGi<J> j, OSGi<K> k, OSGi<L> l, OSGi<M> m, OSGi<N> n, OSGi<O> o, OSGi<P> p, OSGi<Q> q, OSGi<R> r, OSGi<S> s, OSGi<T> t, OSGi<U> u, OSGi<V> v, OSGi<W> w, OSGi<X> x, OSGi<Y> y) {
		return y.applyTo(combine((A aa, B bb, C cc, D dd, E ee, F ff, G gg, H hh, I ii, J jj, K kk, L ll, M mm, N nn, O oo, P pp, Q qq, R rr, S ss, T tt, U uu, V vv, W ww, X xx) -> fun.curried().apply(aa).apply(bb).apply(cc).apply(dd).apply(ee).apply(ff).apply(gg).apply(hh).apply(ii).apply(jj).apply(kk).apply(ll).apply(mm).apply(nn).apply(oo).apply(pp).apply(qq).apply(rr).apply(ss).apply(tt).apply(uu).apply(vv).apply(ww).apply(xx), a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, u, v, w, x));
	}

	static <A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V, W, X, Y, Z, RES> OSGi<RES> combine(Function26<A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V, W, X, Y, Z, RES> fun, OSGi<A> a, OSGi<B> b, OSGi<C> c, OSGi<D> d, OSGi<E> e, OSGi<F> f, OSGi<G> g, OSGi<H> h, OSGi<I> i, OSGi<J> j, OSGi<K> k, OSGi<L> l, OSGi<M> m, OSGi<N> n, OSGi<O> o, OSGi<P> p, OSGi<Q> q, OSGi<R> r, OSGi<S> s, OSGi<T> t, OSGi<U> u, OSGi<V> v, OSGi<W> w, OSGi<X> x, OSGi<Y> y, OSGi<Z> z) {
		return z.applyTo(combine((A aa, B bb, C cc, D dd, E ee, F ff, G gg, H hh, I ii, J jj, K kk, L ll, M mm, N nn, O oo, P pp, Q qq, R rr, S ss, T tt, U uu, V vv, W ww, X xx, Y yy) -> fun.curried().apply(aa).apply(bb).apply(cc).apply(dd).apply(ee).apply(ff).apply(gg).apply(hh).apply(ii).apply(jj).apply(kk).apply(ll).apply(mm).apply(nn).apply(oo).apply(pp).apply(qq).apply(rr).apply(ss).apply(tt).apply(uu).apply(vv).apply(ww).apply(xx).apply(yy), a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, u, v, w, x, y));
	}

	static <A, B, RES> OSGi<RES> flatCombine(Function2<A, B, OSGi<RES>> fun, OSGi<A> a, OSGi<B> b) {
		return combine(fun, a, b).flatMap(Function.identity());
	}

	static <A, B, C, RES> OSGi<RES> flatCombine(Function3<A, B, C, OSGi<RES>> fun, OSGi<A> a, OSGi<B> b, OSGi<C> c) {
		return combine(fun, a, b, c).flatMap(Function.identity());
	}

	static <A, B, C, D, RES> OSGi<RES> flatCombine(Function4<A, B, C, D, OSGi<RES>> fun, OSGi<A> a, OSGi<B> b, OSGi<C> c, OSGi<D> d) {
		return combine(fun, a, b, c, d).flatMap(Function.identity());
	}

	static <A, B, C, D, E, RES> OSGi<RES> flatCombine(Function5<A, B, C, D, E, OSGi<RES>> fun, OSGi<A> a, OSGi<B> b, OSGi<C> c, OSGi<D> d, OSGi<E> e) {
		return combine(fun, a, b, c, d, e).flatMap(Function.identity());
	}

	static <A, B, C, D, E, F, RES> OSGi<RES> flatCombine(Function6<A, B, C, D, E, F, OSGi<RES>> fun, OSGi<A> a, OSGi<B> b, OSGi<C> c, OSGi<D> d, OSGi<E> e, OSGi<F> f) {
		return combine(fun, a, b, c, d, e, f).flatMap(Function.identity());
	}

	static <A, B, C, D, E, F, G, RES> OSGi<RES> flatCombine(Function7<A, B, C, D, E, F, G, OSGi<RES>> fun, OSGi<A> a, OSGi<B> b, OSGi<C> c, OSGi<D> d, OSGi<E> e, OSGi<F> f, OSGi<G> g) {
		return combine(fun, a, b, c, d, e, f, g).flatMap(Function.identity());
	}

	static <A, B, C, D, E, F, G, H, RES> OSGi<RES> flatCombine(Function8<A, B, C, D, E, F, G, H, OSGi<RES>> fun, OSGi<A> a, OSGi<B> b, OSGi<C> c, OSGi<D> d, OSGi<E> e, OSGi<F> f, OSGi<G> g, OSGi<H> h) {
		return combine(fun, a, b, c, d, e, f, g, h).flatMap(Function.identity());
	}

	static <A, B, C, D, E, F, G, H, I, RES> OSGi<RES> flatCombine(Function9<A, B, C, D, E, F, G, H, I, OSGi<RES>> fun, OSGi<A> a, OSGi<B> b, OSGi<C> c, OSGi<D> d, OSGi<E> e, OSGi<F> f, OSGi<G> g, OSGi<H> h, OSGi<I> i) {
		return combine(fun, a, b, c, d, e, f, g, h, i).flatMap(Function.identity());
	}

	static <A, B, C, D, E, F, G, H, I, J, RES> OSGi<RES> flatCombine(Function10<A, B, C, D, E, F, G, H, I, J, OSGi<RES>> fun, OSGi<A> a, OSGi<B> b, OSGi<C> c, OSGi<D> d, OSGi<E> e, OSGi<F> f, OSGi<G> g, OSGi<H> h, OSGi<I> i, OSGi<J> j) {
		return combine(fun, a, b, c, d, e, f, g, h, i, j).flatMap(Function.identity());
	}

	static <A, B, C, D, E, F, G, H, I, J, K, RES> OSGi<RES> flatCombine(Function11<A, B, C, D, E, F, G, H, I, J, K, OSGi<RES>> fun, OSGi<A> a, OSGi<B> b, OSGi<C> c, OSGi<D> d, OSGi<E> e, OSGi<F> f, OSGi<G> g, OSGi<H> h, OSGi<I> i, OSGi<J> j, OSGi<K> k) {
		return combine(fun, a, b, c, d, e, f, g, h, i, j, k).flatMap(Function.identity());
	}

	static <A, B, C, D, E, F, G, H, I, J, K, L, RES> OSGi<RES> flatCombine(Function12<A, B, C, D, E, F, G, H, I, J, K, L, OSGi<RES>> fun, OSGi<A> a, OSGi<B> b, OSGi<C> c, OSGi<D> d, OSGi<E> e, OSGi<F> f, OSGi<G> g, OSGi<H> h, OSGi<I> i, OSGi<J> j, OSGi<K> k, OSGi<L> l) {
		return combine(fun, a, b, c, d, e, f, g, h, i, j, k, l).flatMap(Function.identity());
	}

	static <A, B, C, D, E, F, G, H, I, J, K, L, M, RES> OSGi<RES> flatCombine(Function13<A, B, C, D, E, F, G, H, I, J, K, L, M, OSGi<RES>> fun, OSGi<A> a, OSGi<B> b, OSGi<C> c, OSGi<D> d, OSGi<E> e, OSGi<F> f, OSGi<G> g, OSGi<H> h, OSGi<I> i, OSGi<J> j, OSGi<K> k, OSGi<L> l, OSGi<M> m) {
		return combine(fun, a, b, c, d, e, f, g, h, i, j, k, l, m).flatMap(Function.identity());
	}

	static <A, B, C, D, E, F, G, H, I, J, K, L, M, N, RES> OSGi<RES> flatCombine(Function14<A, B, C, D, E, F, G, H, I, J, K, L, M, N, OSGi<RES>> fun, OSGi<A> a, OSGi<B> b, OSGi<C> c, OSGi<D> d, OSGi<E> e, OSGi<F> f, OSGi<G> g, OSGi<H> h, OSGi<I> i, OSGi<J> j, OSGi<K> k, OSGi<L> l, OSGi<M> m, OSGi<N> n) {
		return combine(fun, a, b, c, d, e, f, g, h, i, j, k, l, m, n).flatMap(Function.identity());
	}

	static <A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, RES> OSGi<RES> flatCombine(Function15<A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, OSGi<RES>> fun, OSGi<A> a, OSGi<B> b, OSGi<C> c, OSGi<D> d, OSGi<E> e, OSGi<F> f, OSGi<G> g, OSGi<H> h, OSGi<I> i, OSGi<J> j, OSGi<K> k, OSGi<L> l, OSGi<M> m, OSGi<N> n, OSGi<O> o) {
		return combine(fun, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o).flatMap(Function.identity());
	}

	static <A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, RES> OSGi<RES> flatCombine(Function16<A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, OSGi<RES>> fun, OSGi<A> a, OSGi<B> b, OSGi<C> c, OSGi<D> d, OSGi<E> e, OSGi<F> f, OSGi<G> g, OSGi<H> h, OSGi<I> i, OSGi<J> j, OSGi<K> k, OSGi<L> l, OSGi<M> m, OSGi<N> n, OSGi<O> o, OSGi<P> p) {
		return combine(fun, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p).flatMap(Function.identity());
	}

	static <A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, RES> OSGi<RES> flatCombine(Function17<A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, OSGi<RES>> fun, OSGi<A> a, OSGi<B> b, OSGi<C> c, OSGi<D> d, OSGi<E> e, OSGi<F> f, OSGi<G> g, OSGi<H> h, OSGi<I> i, OSGi<J> j, OSGi<K> k, OSGi<L> l, OSGi<M> m, OSGi<N> n, OSGi<O> o, OSGi<P> p, OSGi<Q> q) {
		return combine(fun, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q).flatMap(Function.identity());
	}

	static <A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, RES> OSGi<RES> flatCombine(Function18<A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, OSGi<RES>> fun, OSGi<A> a, OSGi<B> b, OSGi<C> c, OSGi<D> d, OSGi<E> e, OSGi<F> f, OSGi<G> g, OSGi<H> h, OSGi<I> i, OSGi<J> j, OSGi<K> k, OSGi<L> l, OSGi<M> m, OSGi<N> n, OSGi<O> o, OSGi<P> p, OSGi<Q> q, OSGi<R> r) {
		return combine(fun, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r).flatMap(Function.identity());
	}

	static <A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, RES> OSGi<RES> flatCombine(Function19<A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, OSGi<RES>> fun, OSGi<A> a, OSGi<B> b, OSGi<C> c, OSGi<D> d, OSGi<E> e, OSGi<F> f, OSGi<G> g, OSGi<H> h, OSGi<I> i, OSGi<J> j, OSGi<K> k, OSGi<L> l, OSGi<M> m, OSGi<N> n, OSGi<O> o, OSGi<P> p, OSGi<Q> q, OSGi<R> r, OSGi<S> s) {
		return combine(fun, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s).flatMap(Function.identity());
	}

	static <A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, RES> OSGi<RES> flatCombine(Function20<A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, OSGi<RES>> fun, OSGi<A> a, OSGi<B> b, OSGi<C> c, OSGi<D> d, OSGi<E> e, OSGi<F> f, OSGi<G> g, OSGi<H> h, OSGi<I> i, OSGi<J> j, OSGi<K> k, OSGi<L> l, OSGi<M> m, OSGi<N> n, OSGi<O> o, OSGi<P> p, OSGi<Q> q, OSGi<R> r, OSGi<S> s, OSGi<T> t) {
		return combine(fun, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t).flatMap(Function.identity());
	}

	static <A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, RES> OSGi<RES> flatCombine(Function21<A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, OSGi<RES>> fun, OSGi<A> a, OSGi<B> b, OSGi<C> c, OSGi<D> d, OSGi<E> e, OSGi<F> f, OSGi<G> g, OSGi<H> h, OSGi<I> i, OSGi<J> j, OSGi<K> k, OSGi<L> l, OSGi<M> m, OSGi<N> n, OSGi<O> o, OSGi<P> p, OSGi<Q> q, OSGi<R> r, OSGi<S> s, OSGi<T> t, OSGi<U> u) {
		return combine(fun, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, u).flatMap(Function.identity());
	}

	static <A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V, RES> OSGi<RES> flatCombine(Function22<A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V, OSGi<RES>> fun, OSGi<A> a, OSGi<B> b, OSGi<C> c, OSGi<D> d, OSGi<E> e, OSGi<F> f, OSGi<G> g, OSGi<H> h, OSGi<I> i, OSGi<J> j, OSGi<K> k, OSGi<L> l, OSGi<M> m, OSGi<N> n, OSGi<O> o, OSGi<P> p, OSGi<Q> q, OSGi<R> r, OSGi<S> s, OSGi<T> t, OSGi<U> u, OSGi<V> v) {
		return combine(fun, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, u, v).flatMap(Function.identity());
	}

	static <A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V, W, RES> OSGi<RES> flatCombine(Function23<A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V, W, OSGi<RES>> fun, OSGi<A> a, OSGi<B> b, OSGi<C> c, OSGi<D> d, OSGi<E> e, OSGi<F> f, OSGi<G> g, OSGi<H> h, OSGi<I> i, OSGi<J> j, OSGi<K> k, OSGi<L> l, OSGi<M> m, OSGi<N> n, OSGi<O> o, OSGi<P> p, OSGi<Q> q, OSGi<R> r, OSGi<S> s, OSGi<T> t, OSGi<U> u, OSGi<V> v, OSGi<W> w) {
		return combine(fun, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, u, v, w).flatMap(Function.identity());
	}

	static <A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V, W, X, RES> OSGi<RES> flatCombine(Function24<A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V, W, X, OSGi<RES>> fun, OSGi<A> a, OSGi<B> b, OSGi<C> c, OSGi<D> d, OSGi<E> e, OSGi<F> f, OSGi<G> g, OSGi<H> h, OSGi<I> i, OSGi<J> j, OSGi<K> k, OSGi<L> l, OSGi<M> m, OSGi<N> n, OSGi<O> o, OSGi<P> p, OSGi<Q> q, OSGi<R> r, OSGi<S> s, OSGi<T> t, OSGi<U> u, OSGi<V> v, OSGi<W> w, OSGi<X> x) {
		return combine(fun, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, u, v, w, x).flatMap(Function.identity());
	}

	static <A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V, W, X, Y, RES> OSGi<RES> flatCombine(Function25<A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V, W, X, Y, OSGi<RES>> fun, OSGi<A> a, OSGi<B> b, OSGi<C> c, OSGi<D> d, OSGi<E> e, OSGi<F> f, OSGi<G> g, OSGi<H> h, OSGi<I> i, OSGi<J> j, OSGi<K> k, OSGi<L> l, OSGi<M> m, OSGi<N> n, OSGi<O> o, OSGi<P> p, OSGi<Q> q, OSGi<R> r, OSGi<S> s, OSGi<T> t, OSGi<U> u, OSGi<V> v, OSGi<W> w, OSGi<X> x, OSGi<Y> y) {
		return combine(fun, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, u, v, w, x, y).flatMap(Function.identity());
	}

	static <A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V, W, X, Y, Z, RES> OSGi<RES> flatCombine(Function26<A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V, W, X, Y, Z, OSGi<RES>> fun, OSGi<A> a, OSGi<B> b, OSGi<C> c, OSGi<D> d, OSGi<E> e, OSGi<F> f, OSGi<G> g, OSGi<H> h, OSGi<I> i, OSGi<J> j, OSGi<K> k, OSGi<L> l, OSGi<M> m, OSGi<N> n, OSGi<O> o, OSGi<P> p, OSGi<Q> q, OSGi<R> r, OSGi<S> s, OSGi<T> t, OSGi<U> u, OSGi<V> v, OSGi<W> w, OSGi<X> x, OSGi<Y> y, OSGi<Z> z) {
		return combine(fun, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, u, v, w, x, y, z).flatMap(Function.identity());
	}
	
	static OSGi<Dictionary<String, ?>> configuration(String pid) {
		return new ConfigurationOSGiImpl(pid);
	}

	static OSGi<Dictionary<String, ?>> configurations(String factoryPid) {
		return new ConfigurationsOSGiImpl(factoryPid);
	}

	static OSGi<Void> effect(Effect<Void> effect) {
		return new EffectsOSGi(
			() -> effect.getOnIncoming().accept(null),
			NOOP,
			NOOP,
			() -> effect.getOnLeaving().accept(null),
			UpdateQuery.onUpdate());
	}

	static OSGi<Void> effects(Runnable onAdding, Runnable onRemoving) {
		return new EffectsOSGi(onAdding, NOOP, NOOP, onRemoving, UpdateQuery.onUpdate());
	}

	static OSGi<Void> effects(
		Runnable onAddingBefore, Runnable onAddingAfter,
		Runnable onRemovingBefore, Runnable onRemovingAfter) {

		return new EffectsOSGi(
			onAddingBefore, onAddingAfter, onRemovingBefore, onRemovingAfter,
			UpdateQuery.onUpdate());
	}

	static OSGi<Void> effects(
		Runnable onAddingBefore, Runnable onAddingAfter,
		Runnable onRemovingBefore, Runnable onRemovingAfter, UpdateQuery<Void> updateQuery) {

		return new EffectsOSGi(
			onAddingBefore, onAddingAfter, onRemovingBefore, onRemovingAfter, updateQuery);
	}

	static <T> OSGi<T> fromOsgiRunnable(OSGiRunnable<T> runnable) {
		return getOsgiFactory().create(
			(ec, op) -> new OSGiResultImpl(runnable.run(ec, op), __ -> true));
	}

	static <T> OSGi<T> fromOsgiRunnableWithUpdateSupport(OSGiRunnable<T> runnable) {
		return getOsgiFactory().create(runnable::run);
	}

	static OSGiFactory getOsgiFactory() {
		return OSGiImpl::create;
	}

	static OSGi<Void> ignore(OSGi<?> program) {
		return new IgnoreImpl(program);
	}

	static <S> OSGi<S> join(OSGi<OSGi<S>> program) {
		return program.flatMap(x -> x);
	}

	static <S> OSGi<S> just(S s) {
		return new JustOSGiImpl<>(s);
	}

	static <S> OSGi<S> just(Collection<S> s) {
		return new JustOSGiImpl<>(s);
	}

	static <S> OSGi<S> just(Supplier<S> s) {
		return new JustOSGiImpl<>(() -> Collections.singletonList(s.get()));
	}

	static <S> OSGi<S> nothing() {
		return new NothingOSGiImpl<>();
	}

	@Deprecated()
	/**
	 * @deprecated see {@link #effects(Runnable, Runnable)}
	 */
	static OSGi<Void> onClose(Runnable action) {
		return effects(NOOP, action);
	}

	static <T> OSGi<T> once(OSGi<T> program) {
		return program.transform(op -> {
			AtomicInteger count = new AtomicInteger();

			AtomicReference<Runnable> terminator = new AtomicReference<>();

			return t -> {
				if (count.getAndIncrement() == 0) {
					UpdateSupport.deferPublication(
						() -> terminator.set(op.apply(t)));
				}

				return () -> {
					if (count.decrementAndGet() == 0) {
						UpdateSupport.deferTermination(() -> {
							Runnable runnable = terminator.getAndSet(NOOP);

							runnable.run();
						});
					}

				};
			};
		});
	}

	static OSGi<ServiceObjects<Object>> prototypes(String filterString) {
		return prototypes(null, filterString);
	}

	static <T> OSGi<ServiceObjects<T>> prototypes(Class<T> clazz) {
		return prototypes(clazz, null);
	}

	static <T> OSGi<ServiceObjects<T>> prototypes(
		Class<T> clazz, String filterString) {

		return
		bundleContext().flatMap(
		bundleContext ->

		serviceReferences(clazz, filterString).map(
			CachingServiceReference::getServiceReference
		).map(
			bundleContext::getServiceObjects)
		);
	}

	static <T> OSGi<ServiceObjects<T>> prototypes(
		CachingServiceReference<T> serviceReference) {

		return
			bundleContext().flatMap(bundleContext ->
			just(bundleContext.getServiceObjects(
				serviceReference.getServiceReference())));
	}

	static <T> OSGi<ServiceObjects<T>> prototypes(
		ServiceReference<T> serviceReference) {

		return
			bundleContext().flatMap(bundleContext ->
			just(bundleContext.getServiceObjects(serviceReference)));
	}

	static <T> OSGi<ServiceObjects<T>> prototypes(
		OSGi<ServiceReference<T>> serviceReference) {

		return serviceReference.flatMap(OSGi::prototypes);
	}

	static <T> OSGi<T> recover(OSGi<T> program, BiFunction<T, Exception, T> function) {
		return new RecoverOSGi<>(program, function);
	}

	static <T> OSGi<T> recoverWith(OSGi<T> program, BiFunction<T, Exception, OSGi<T>> function) {
		return new RecoverWithOSGi<>(program, function);
	}

	static <T> OSGi<T> refreshWhen(OSGi<T> program, BiPredicate<UpdateSelector, T> refresher) {
		return new RefreshWhenOSGi<>(program, refresher);
	}

	static <T> OSGi<ServiceRegistration<T>> register(
		Class<T> clazz, T service, Map<String, Object> properties) {

		return register(clazz, () -> service, () -> properties);
	}

	static <T> OSGi<ServiceRegistration<T>> register(
		Class<T> clazz, ServiceFactory<T> service,
		Map<String, Object> properties) {

		return register(clazz, service, () -> properties);
	}

	static OSGi<ServiceRegistration<?>> register(
		String[] classes, Object service, Map<String, ?> properties) {

		return new ServiceRegistrationOSGiImpl(
			classes, () -> service, () -> properties);
	}

	static <T> OSGi<ServiceRegistration<T>> register(
		Class<T> clazz, Supplier<T> service,
		Supplier<Map<String, ?>> properties) {

		return new ServiceRegistrationOSGiImpl<>(clazz, service, properties);
	}

	static <T> OSGi<ServiceRegistration<T>> register(
		Class<T> clazz, ServiceFactory<T> service,
		Supplier<Map<String, ?>> properties) {

		return new ServiceRegistrationOSGiImpl<>(clazz, service, properties);
	}

	static OSGi<ServiceRegistration<?>> register(
		String[] classes, Supplier<Object> service,
		Supplier<Map<String, ?>> properties) {

		return new ServiceRegistrationOSGiImpl(classes, service, properties);
	}

	static <T> OSGi<T> service(ServiceReference<T> serviceReference) {
		return
			bundleContext().flatMap(bundleContext -> {
				T service = bundleContext.getService(serviceReference);

				return
					onClose(() -> bundleContext.ungetService(serviceReference)).
						then(
					just(service));
			});
	}

	static <T> OSGi<T> service(CachingServiceReference<T> serviceReference) {
		return
			bundleContext().flatMap(bundleContext -> {
				T service = bundleContext.getService(
					serviceReference.getServiceReference());

				return
					onClose(() -> bundleContext.ungetService(
						serviceReference.getServiceReference())).
						then(
							just(service));
			});
	}

	static <T> OSGi<T> service(
		OSGi<CachingServiceReference<T>> serviceReference) {

		return serviceReference.flatMap(OSGi::service);
	}

	static <T> OSGi<CachingServiceReference<T>> serviceReferences(
		Class<T> clazz) {

		return new ServiceReferenceOSGi<>(null, clazz).map(UpdateTuple::getT);
	}

	static OSGi<CachingServiceReference<Object>> serviceReferences(
		String filterString) {

		return new ServiceReferenceOSGi<>(filterString, null).map(UpdateTuple::getT);
	}

	static <T> OSGi<CachingServiceReference<T>> serviceReferences(
		Class<T> clazz, String filterString) {

		return new ServiceReferenceOSGi<>(filterString, clazz).map(UpdateTuple::getT);
	}

	static <T> OSGi<CachingServiceReference<T>> serviceReferences(
		Class<T> clazz, String filterString,
		Refresher<? super CachingServiceReference<T>> onModified) {

		return refreshWhen(
			serviceReferences(clazz, filterString),
			(__, csr) -> onModified.test(csr));

	}

	static <T> OSGi<CachingServiceReference<T>> serviceReferences(
		Class<T> clazz,
		Refresher<? super CachingServiceReference<T>> onModified) {

		return refreshWhen(
			serviceReferences(clazz, (String)null),
			(__, csr) -> onModified.test(csr));
	}

	static OSGi<CachingServiceReference<Object>> serviceReferences(
		String filterString,
		Refresher<? super CachingServiceReference<Object>> onModified) {

		return refreshWhen(
			serviceReferences(null, filterString),
			(__, csr) -> onModified.test(csr));
	}

	static <T> OSGi<UpdateTuple<CachingServiceReference<T>>> serviceReferencesUpdatable(
		Class<T> clazz) {

		return new ServiceReferenceOSGi<>(null, clazz);
	}

	static OSGi<UpdateTuple<CachingServiceReference<Object>>> serviceReferencesUpdatable(
		String filterString) {

		return new ServiceReferenceOSGi<>(filterString, null);
	}

	static <T> OSGi<UpdateTuple<CachingServiceReference<T>>> serviceReferencesUpdatable(
		Class<T> clazz, String filterString) {

		return new ServiceReferenceOSGi<>(filterString, clazz);
	}

	static <T> OSGi<T> services(Class<T> clazz) {
		return services(clazz, null);
	}

	static <T> OSGi<Object> services(String filterString) {
		return services(null, filterString);
	}

	static <T> OSGi<T> services(Class<T> clazz, String filterString) {
		return
			bundleContext().flatMap(
			bundleContext ->

			serviceReferences(clazz, filterString).map(
				CachingServiceReference::getServiceReference
			).flatMap(
				sr -> {
					T service = bundleContext.getService(sr);

					return
						onClose(() -> bundleContext.ungetService(sr)).then(
						just(service)
					);
			}
		));
	}

	<S> OSGi<S> applyTo(OSGi<Function<T, S>> fun);

	<S> OSGi<S> choose(
		Function<T, OSGi<Boolean>> chooser, Function<OSGi<T>, OSGi<S>> then,
		Function<OSGi<T>, OSGi<S>> otherwise);

	<S> OSGi<S> distribute(Function<OSGi<T>, OSGi<S>> ... funs);

	default OSGi<T> effects(
		Consumer<? super T> onAdded, Consumer<? super T> onRemoved) {

		return effects(onAdded, __ -> {}, __ -> {}, onRemoved);
	}

	OSGi<T> effects(
		Consumer<? super T> onAddedBefore, Consumer<? super T> onAddedAfter,
		Consumer<? super T> onRemovedBefore,
		Consumer<? super T> onRemovedAfter);

	default OSGi<T> effects(Effect<? super T> effect) {
		return effects(effect.getOnIncoming(), effect.getOnLeaving());
	}

	OSGi<T> filter(Predicate<T> predicate);

	<S> OSGi<S> flatMap(Function<? super T, OSGi<? extends S>> fun);

	default OSGi<Void> foreach(Consumer<? super T> onAdded) {
		return foreach(onAdded, __ -> {});
	}

	default OSGi<Void> foreach(
		Consumer<? super T> onAdded, Consumer<? super T> onRemoved) {

		return ignore(effects(onAdded, onRemoved));
	}

	<S> OSGi<S> map(Function<? super T, ? extends S> function);

	@Deprecated
	/**
	 * @deprecated in favor of {@link OSGi#recover(OSGi, BiFunction)}
	 */
	OSGi<T> recover(BiFunction<T, Exception, T> onError);

	@Deprecated
	/**
	 * @deprecated in favor of {@link OSGi#recoverWith(OSGi, BiFunction)}
	 */
	OSGi<T> recoverWith(BiFunction<T, Exception, OSGi<T>> onError);

	<K, S> OSGi<S> splitBy(
		Function<T, OSGi<K>> mapper, BiFunction<K, OSGi<T>, OSGi<S>> fun);

	default public <S> OSGi<S> then(OSGi<S> next) {
		return flatMap(__ -> next);
	}

	<S> OSGi<S> transform(Transformer<T, S> fun);

}
