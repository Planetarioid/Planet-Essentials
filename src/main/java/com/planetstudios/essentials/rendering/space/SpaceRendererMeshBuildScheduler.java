package com.planetstudios.essentials.rendering.space;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.Semaphore;

/**
 * Bounded priority scheduler for CPU-only terrain mesh builds.
 *
 * <p>Build functions must consume immutable snapshots and must not touch
 * Minecraft render state or GPU resources. Completion results are polled by
 * the client/render coordination layer, which checks the section revision
 * before uploading or publishing them.</p>
 */
public final class SpaceRendererMeshBuildScheduler<T> implements AutoCloseable {
    private final Semaphore capacity;
    private final ArrayBlockingQueue<BuildResult<T>> completions;
    private final ThreadPoolExecutor executor;
    private final AtomicLong sequence = new AtomicLong();
    private final AtomicBoolean closed = new AtomicBoolean();

    public SpaceRendererMeshBuildScheduler(int workerCount, int maxInFlight, ThreadFactory threadFactory) {
        if (workerCount <= 0) throw new IllegalArgumentException("Worker count must be positive");
        if (maxInFlight < workerCount) {
            throw new IllegalArgumentException("In-flight capacity must be at least the worker count");
        }
        Objects.requireNonNull(threadFactory, "threadFactory");
        capacity = new Semaphore(maxInFlight);
        completions = new ArrayBlockingQueue<>(maxInFlight);
        executor = new ThreadPoolExecutor(workerCount, workerCount, 0L, TimeUnit.MILLISECONDS,
                new PriorityBlockingQueue<>(), threadFactory, new ThreadPoolExecutor.AbortPolicy());
        executor.prestartAllCoreThreads();
    }

    /**
     * Queues a CPU-only build. Lower priority scores run first. Returns false
     * when the bounded in-flight budget is full or the scheduler is closed.
     */
    public boolean submit(SpaceRendererSectionManager.BuildToken token,
            double priority, Callable<T> build) {
        Objects.requireNonNull(token, "token");
        Objects.requireNonNull(build, "build");
        if (!Double.isFinite(priority)) throw new IllegalArgumentException("Priority must be finite");
        if (closed.get() || !capacity.tryAcquire()) return false;
        long order = sequence.getAndIncrement();
        if (order < 0) {
            capacity.release();
            throw new IllegalStateException("Mesh build sequence exhausted");
        }
        BuildTask task = new BuildTask(token, priority, order, build);
        try {
            if (closed.get()) {
                capacity.release();
                return false;
            }
            executor.execute(task);
            return true;
        } catch (RuntimeException exception) {
            capacity.release();
            if (closed.get()) return false;
            throw exception;
        }
    }

    /** Waits briefly for one completed build. Draining releases its in-flight slot. */
    public BuildResult<T> pollCompletion(long timeout, TimeUnit unit) throws InterruptedException {
        Objects.requireNonNull(unit, "unit");
        BuildResult<T> result = completions.poll(timeout, unit);
        if (result != null) capacity.release();
        return result;
    }

    /** Non-blocking completion drain, preserving completion order. */
    public List<BuildResult<T>> drainCompletions(int maximum) {
        if (maximum < 0) throw new IllegalArgumentException("Maximum must not be negative");
        List<BuildResult<T>> results = new ArrayList<>(Math.min(maximum, completions.size()));
        completions.drainTo(results, maximum);
        if (!results.isEmpty()) capacity.release(results.size());
        return List.copyOf(results);
    }

    public int queuedBuildCount() {
        return executor.getQueue().size();
    }

    public int activeBuildCount() {
        return executor.getActiveCount();
    }

    public int pendingCompletionCount() {
        return completions.size();
    }

    public int availableCapacity() {
        return capacity.availablePermits();
    }

    public boolean isClosed() {
        return closed.get();
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) return;
        List<Runnable> neverStarted = executor.shutdownNow();
        int released = neverStarted.size();
        // Completed results are no longer consumable after close.
        int completed = completions.size();
        completions.clear();
        if (completed > 0) capacity.release(completed);
        if (released > 0) capacity.release(released);
    }

    public record BuildResult<T>(SpaceRendererSectionManager.BuildToken token,
            T value, Throwable failure) {
        public BuildResult {
            Objects.requireNonNull(token, "token");
            if (failure == null && value == null) {
                throw new IllegalArgumentException("A successful build must contain a value");
            }
            if (failure != null && value != null) {
                throw new IllegalArgumentException("A failed build cannot contain a value");
            }
        }

        public boolean succeeded() { return failure == null; }
    }

    private final class BuildTask implements Runnable, Comparable<BuildTask> {
        private final SpaceRendererSectionManager.BuildToken token;
        private final double priority;
        private final long order;
        private final Callable<T> build;

        private BuildTask(SpaceRendererSectionManager.BuildToken token,
                double priority, long order, Callable<T> build) {
            this.token = token;
            this.priority = priority;
            this.order = order;
            this.build = build;
        }

        @Override
        public int compareTo(BuildTask other) {
            int byPriority = Double.compare(priority, other.priority);
            return byPriority != 0 ? byPriority : Long.compare(order, other.order);
        }

        @Override
        public void run() {
            BuildResult<T> result;
            try {
                T value = Objects.requireNonNull(build.call(), "Mesh build returned null");
                result = new BuildResult<>(token, value, null);
            } catch (Exception failure) {
                result = new BuildResult<>(token, null, failure);
            }
            // The completion queue has the same bound as all in-flight work,
            // so offer must succeed unless shutdown concurrently drained it.
            if (!closed.get() && !completions.offer(result)) {
                releaseOnce();
            }
        }

    }
}
