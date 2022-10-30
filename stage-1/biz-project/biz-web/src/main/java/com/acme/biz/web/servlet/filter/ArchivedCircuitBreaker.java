package com.acme.biz.web.servlet.filter;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.vavr.CheckedConsumer;
import io.vavr.CheckedFunction0;
import io.vavr.CheckedRunnable;
import io.vavr.collection.Map;
import io.vavr.control.Either;
import io.vavr.control.Try;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 归档的circuit-breaker
 *
 * @author <a href="mailto:maimengzzz@gmail.com">韩超</a>
 * @since 1.0.0
 */
public class ArchivedCircuitBreaker implements CircuitBreaker {

    private String archivedName;
    private CircuitBreaker delegate;

    public ArchivedCircuitBreaker(String archivedName, CircuitBreaker delegate) {
        this.archivedName = archivedName;
        this.delegate = delegate;
    }


    @Override
    public boolean tryAcquirePermission() {
        return delegate.tryAcquirePermission();
    }

    @Override
    public void releasePermission() {
        delegate.releasePermission();
    }

    @Override
    public void acquirePermission() {
        delegate.acquirePermission();
    }

    @Override
    public void onError(long duration, TimeUnit durationUnit, Throwable throwable) {
        delegate.onError(duration, durationUnit, throwable);
    }

    @Override
    public void onSuccess(long duration, TimeUnit durationUnit) {
        delegate.onSuccess(duration, durationUnit);
    }

    @Override
    public void onResult(long duration, TimeUnit durationUnit, Object result) {
        delegate.onResult(duration, durationUnit, result);
    }

    @Override
    public void reset() {
        delegate.reset();
    }

    @Override
    public void transitionToClosedState() {
        delegate.transitionToClosedState();
    }

    @Override
    public void transitionToOpenState() {
        delegate.transitionToOpenState();
    }

    @Override
    public void transitionToHalfOpenState() {
        delegate.transitionToHalfOpenState();
    }

    @Override
    public void transitionToDisabledState() {
        delegate.transitionToDisabledState();
    }

    @Override
    public void transitionToMetricsOnlyState() {
        delegate.transitionToMetricsOnlyState();
    }

    @Override
    public void transitionToForcedOpenState() {
        delegate.transitionToForcedOpenState();
    }

    @Override
    public String getName() {
        return this.archivedName;
    }

    @Override
    public State getState() {
        return delegate.getState();
    }

    @Override
    public CircuitBreakerConfig getCircuitBreakerConfig() {
        return delegate.getCircuitBreakerConfig();
    }

    @Override
    public Metrics getMetrics() {
        return delegate.getMetrics();
    }

    @Override
    public Map<String, String> getTags() {
        return delegate.getTags();
    }

    @Override
    public EventPublisher getEventPublisher() {
        return delegate.getEventPublisher();
    }

    @Override
    public long getCurrentTimestamp() {
        return delegate.getCurrentTimestamp();
    }

    @Override
    public TimeUnit getTimestampUnit() {
        return delegate.getTimestampUnit();
    }

    @Override
    public <T> T executeSupplier(Supplier<T> supplier) {
        return delegate.executeSupplier(supplier);
    }

    @Override
    public <T> Supplier<T> decorateSupplier(Supplier<T> supplier) {
        return delegate.decorateSupplier(supplier);
    }

    @Override
    public <T> Either<Exception, T> executeEitherSupplier(Supplier<Either<? extends Exception, T>> supplier) {
        return delegate.executeEitherSupplier(supplier);
    }

    @Override
    public <T> Supplier<Try<T>> decorateTrySupplier(Supplier<Try<T>> supplier) {
        return delegate.decorateTrySupplier(supplier);
    }

    @Override
    public <T> Try<T> executeTrySupplier(Supplier<Try<T>> supplier) {
        return delegate.executeTrySupplier(supplier);
    }

    @Override
    public <T> Supplier<Either<Exception, T>> decorateEitherSupplier(Supplier<Either<? extends Exception, T>> supplier) {
        return delegate.decorateEitherSupplier(supplier);
    }

    @Override
    public <T> T executeCallable(Callable<T> callable) throws Exception {
        return delegate.executeCallable(callable);
    }

    @Override
    public <T> Callable<T> decorateCallable(Callable<T> callable) {
        return delegate.decorateCallable(callable);
    }

    @Override
    public void executeRunnable(Runnable runnable) {
        delegate.executeRunnable(runnable);
    }

    @Override
    public Runnable decorateRunnable(Runnable runnable) {
        return delegate.decorateRunnable(runnable);
    }

    @Override
    public <T> CompletionStage<T> executeCompletionStage(Supplier<CompletionStage<T>> supplier) {
        return delegate.executeCompletionStage(supplier);
    }

    @Override
    public <T> Supplier<CompletionStage<T>> decorateCompletionStage(Supplier<CompletionStage<T>> supplier) {
        return delegate.decorateCompletionStage(supplier);
    }

    @Override
    public <T> T executeCheckedSupplier(CheckedFunction0<T> checkedSupplier) throws Throwable {
        return delegate.executeCheckedSupplier(checkedSupplier);
    }

    @Override
    public <T> CheckedFunction0<T> decorateCheckedSupplier(CheckedFunction0<T> checkedSupplier) {
        return delegate.decorateCheckedSupplier(checkedSupplier);
    }

    @Override
    public CheckedRunnable decorateCheckedRunnable(CheckedRunnable runnable) {
        return delegate.decorateCheckedRunnable(runnable);
    }

    @Override
    public void executeCheckedRunnable(CheckedRunnable runnable) throws Throwable {
        delegate.executeCheckedRunnable(runnable);
    }

    @Override
    public <T> Consumer<T> decorateConsumer(Consumer<T> consumer) {
        return delegate.decorateConsumer(consumer);
    }

    @Override
    public <T> CheckedConsumer<T> decorateCheckedConsumer(CheckedConsumer<T> consumer) {
        return delegate.decorateCheckedConsumer(consumer);
    }

    @Override
    public <T> Supplier<Future<T>> decorateFuture(Supplier<Future<T>> supplier) {
        return delegate.decorateFuture(supplier);
    }
}
