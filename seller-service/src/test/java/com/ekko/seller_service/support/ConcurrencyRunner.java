package com.ekko.seller_service.support;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.IntFunction;

public final class ConcurrencyRunner {

    private ConcurrencyRunner() {
    }

    public static <T> List<T> run(String label, int threads, IntFunction<Callable<T>> taskFactory) {
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<T>> futures = new ArrayList<>(threads);

        for (int i = 0; i < threads; i++) {
            Callable<T> task = taskFactory.apply(i);
            futures.add(executor.submit(() -> {
                ready.countDown();
                if (!start.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("timeout esperando señal de arranque");
                }
                return task.call();
            }));
        }

        try {
            if (!ready.await(10, TimeUnit.SECONDS)) {
                throw new AssertionError(label + ": no todos los hilos estuvieron listos");
            }
            start.countDown();

            List<T> results = new ArrayList<>(threads);
            for (Future<T> future : futures) {
                results.add(future.get(60, TimeUnit.SECONDS));
            }
            return results;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(label + ": ejecución interrumpida", e);
        } catch (ExecutionException e) {
            throw new AssertionError(label + ": error concurrente -> " + e.getCause(), e);
        } catch (TimeoutException e) {
            throw new AssertionError(label + ": timeout de 60s", e);
        } finally {
            executor.shutdownNow();
        }
    }
}
