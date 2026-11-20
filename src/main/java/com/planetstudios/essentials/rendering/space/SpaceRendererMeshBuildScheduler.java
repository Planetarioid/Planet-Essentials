package com.planetstudios.essentials.rendering.space;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Bounded priority scheduler for CPU-only immutable terrain mesh builds.
 * GPU and Minecraft render state must only be touched by the consumer thread.
 */
public final class SpaceRendererMeshBuildScheduler<T> implements AutoCloseable {
    private final Semaphore capacity;
    private final ArrayBlockingQueue<BuildResult<T>> completions;
    private final ThreadPoolExecutor executor;
    private final AtomicLong sequence = new AtomicLong();
    private final AtomicBoolean closed = new AtomicBoolean();

    public SpaceRendererMeshBuildScheduler(int workers, int maxInFlight, ThreadFactory factory) {
        if (workers <= 0 || maxInFlight < workers) throw new IllegalArgumentException("Invalid worker or in-flight capacity");
        Objects.requireNonNull(factory, "factory");
        capacity = new Semaphore(maxInFlight);
        completions = new ArrayBlockingQueue<>(maxInFlight);
        executor = new ThreadPoolExecutor(workers, workers, 0L, TimeUnit.MILLISECONDS,
                new PriorityBlockingQueue<>(), factory, new ThreadPoolExecutor.AbortPolicy());
        executor.prestartAllCoreThreads();
    }

    public boolean submit(SpaceRendererSectionManager.BuildToken token, double priority, Callable<T> build) {
        Objects.requireNonNull(token, "token");
        Objects.requireNonNull(build, "build");
        if (!Double.isFinite(priority)) throw new IllegalArgumentException("Priority must be finite");
        if (closed.get() || !capacity.tryAcquire()) return false;
        long order = sequence.getAndIncrement();
        if (order < 0) { capacity.release(); throw new IllegalStateException("Build sequence exhausted"); }
        try {
            if (closed.get()) { capacity.release(); return false; }
            executor.execute(new BuildTask(token, priority, order, build));
            return true;
        } catch (RuntimeException failure) {
            capacity.release();
            if (closed.get()) return false;
            throw failure;
        }
    }

    public BuildResult<T> pollCompletion(long timeout, TimeUnit unit) throws InterruptedException {
        BuildResult<T> result = completions.poll(timeout, Objects.requireNonNull(unit, "unit"));
        if (result != null) capacity.release();
        return result;
    }
    public List<BuildResult<T>> drainCompletions(int maximum) {
        if (maximum < 0) throw new IllegalArgumentException("Maximum must not be negative");
        List<BuildResult<T>> result = new ArrayList<>(Math.min(maximum, completions.size()));
        completions.drainTo(result, maximum);
        if (!result.isEmpty()) capacity.release(result.size());
        return List.copyOf(result);
    }
    public int queuedBuildCount() { return executor.getQueue().size(); }
    public int activeBuildCount() { return executor.getActiveCount(); }
    public int pendingCompletionCount() { return completions.size(); }
    public int availableCapacity() { return capacity.availablePermits(); }
    public boolean isClosed() { return closed.get(); }

    @Override public void close() {
        if (!closed.compareAndSet(false, true)) return;
        List<Runnable> neverStarted = executor.shutdownNow();
        int completed = completions.size();
        completions.clear();
        int released = neverStarted.size() + completed;
        if (released > 0) capacity.release(released);
    }

    public record BuildResult<T>(SpaceRendererSectionManager.BuildToken token, T value, Exception failure) {
        public BuildResult {
            Objects.requireNonNull(token, "token");
            if ((failure == null) == (value == null)) throw new IllegalArgumentException("Result must contain either value or failure");
        }
        public boolean succeeded() { return failure == null; }
    }

    private final class BuildTask implements Runnable, Comparable<BuildTask> {
        private final SpaceRendererSectionManager.BuildToken token;
        private final double priority;
        private final long order;
        private final Callable<T> build;
        private BuildTask(SpaceRendererSectionManager.BuildToken token, double priority, long order, Callable<T> build) {
            this.token = token; this.priority = priority; this.order = order; this.build = build;
        }
        @Override public int compareTo(BuildTask other) {
            int result = Double.compare(priority, other.priority);
            return result != 0 ? result : Long.compare(order, other.order);
        }
        @Override public void run() {
            BuildResult<T> result;
            try { result = new BuildResult<>(token, Objects.requireNonNull(build.call(), "Build returned null"), null); }
            catch (Exception failure) { result = new BuildResult<>(token, null, failure); }
            if (!closed.get() && !completions.offer(result)) {
                // Capacity accounting makes this unreachable unless shutdown races the worker.
                capacity.release();
            }
        }
    }
}
