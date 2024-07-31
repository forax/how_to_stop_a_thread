package com.github.forax.threadstop.bench;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;
import java.lang.invoke.VarHandle;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

// Benchmark                            Mode  Cnt  Score   Error  Units
// ThreadStopBench.stop_arena           avgt    5  0.613 ± 0.004  ns/op
// ThreadStopBench.stop_callsite        avgt    5  0.306 ± 0.002  ns/op
// ThreadStopBench.stop_interrupt       avgt    5  0.490 ± 0.002  ns/op
// ThreadStopBench.stop_opaque          avgt    5  0.409 ± 0.004  ns/op
// ThreadStopBench.stop_reentrant_lock  avgt    5  8.406 ± 0.035  ns/op
// ThreadStopBench.stop_synchronized    avgt    5  5.055 ± 0.023  ns/op
// ThreadStopBench.stop_volatile        avgt    5  0.496 ± 0.004  ns/op

// $JAVA_HOME/bin/java -jar target/benchmarks.jar -prof dtraceasm
/*@Warmup(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Fork(value = 1)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
public class ThreadStopBench {

  boolean synchronized_stop;
  final Object synchronized_lock = new Object();

  @Benchmark
  public boolean stop_synchronized() {
    synchronized (synchronized_lock) {
      return synchronized_stop;
    }
  }


  boolean reentrant_lock_stop;
  final ReentrantLock reentrant_lock = new ReentrantLock();

  @Benchmark
  public boolean stop_reentrant_lock() {
    reentrant_lock.lock();
    try {
      return reentrant_lock_stop;
    } finally {
      reentrant_lock.unlock();
    }
  }


  @Benchmark
  public boolean stop_interrupt() {
    return Thread.interrupted();
  }


  volatile boolean volatile_stop;

  @Benchmark
  public boolean stop_volatile() {
    return volatile_stop;
  }


  boolean opaque_stop;
  static final VarHandle OPAQUE_STOP;
  static {
    var lookup = MethodHandles.lookup();
    try {
      OPAQUE_STOP = lookup.findVarHandle(ThreadStopBench.class, "opaque_stop", boolean.class);
    } catch (NoSuchFieldException | IllegalAccessException e) {
      throw new AssertionError(e);
    }
  }

  @Benchmark
  public boolean stop_opaque() {
    return (boolean) OPAQUE_STOP.get(this);
  }


  static final class Stop extends MutableCallSite {
    public Stop() {
      super(MethodType.methodType(boolean.class));
      setTarget(MethodHandles.constant(boolean.class, false));
    }
  }

  static final Stop STOP = new Stop();
  static final MethodHandle STOP_MH = STOP.dynamicInvoker();

  @Benchmark
  public boolean stop_callsite() throws Throwable {
    return (boolean) STOP_MH.invokeExact();
  }


  final Arena arena = Arena.ofShared();
  final MemorySegment.Scope scope = arena.scope();

  @Benchmark
  public boolean stop_arena() {
    return !scope.isAlive();
  }
}*/


