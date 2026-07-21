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
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;
import java.lang.invoke.VarHandle;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

// Benchmark                                           Mode  Cnt    Score   Error  Units
// ThreadStopLoopArrayAccessBench.no_stop              avgt    5   29,675 ± 0,352  us/op
// ThreadStopLoopArrayAccessBench.stop_arena           avgt    5   29,556 ± 0,332  us/op
// ThreadStopLoopArrayAccessBench.stop_interrupt       avgt    5   63,205 ± 0,304  us/op
// ThreadStopLoopArrayAccessBench.stop_opaque          avgt    5   29,788 ± 0,104  us/op
// ThreadStopLoopArrayAccessBench.stop_reentrant_lock  avgt    5  850,208 ± 2,851  us/op
// ThreadStopLoopArrayAccessBench.stop_synchronized    avgt    5  554,027 ± 3,947  us/op
// ThreadStopLoopArrayAccessBench.stop_volatile        avgt    5   63,170 ± 0,357  us/op

// $JAVA_HOME/bin/java -jar target/benchmarks.jar -prof dtraceasm
@Warmup(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Fork(value = 1)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
public class ThreadStopLoopArrayAccessBench {
  private final int[] array = new Random(0).ints(100_000).toArray();

  @Benchmark
  public int no_stop() {
    var sum = 0;
    for(var i = 0; i < array.length; i++) {
      sum += array[i];
    }
    return sum;
  }


  boolean synchronized_stop;
  final Object synchronized_lock = new Object();

  @Benchmark
  public int stop_synchronized() {
    var sum = 0;
    for(var i = 0; i < array.length; i++) {
      synchronized (synchronized_lock) {
        if (synchronized_stop) {
          break;
        }
      }
      sum += array[i];
    }
    return sum;
  }


  boolean reentrant_lock_stop;
  final ReentrantLock reentrant_lock = new ReentrantLock();

  @Benchmark
  public int stop_reentrant_lock() {
    var sum = 0;
    for(var i = 0; i < array.length; i++) {
      reentrant_lock.lock();
      try {
        if (reentrant_lock_stop) {
          break;
        }
      } finally {
        reentrant_lock.unlock();
      }
      sum += array[i];
    }
    return sum;
  }


  @Benchmark
  public int stop_interrupt() {
    var sum = 0;
    for(var i = 0; i < array.length; i++) {
      if (Thread.interrupted()) {
        break;
      }
      sum += array[i];
    }
    return sum;
  }


  volatile boolean volatile_stop;

  @Benchmark
  public int stop_volatile() {
    var sum = 0;
    for(var i = 0; i < array.length; i++) {
      if (volatile_stop) {
        break;
      }
      sum += array[i];
    }
    return sum;
  }


  boolean opaque_stop;
  static final VarHandle OPAQUE_STOP;
  static {
    var lookup = MethodHandles.lookup();
    try {
      OPAQUE_STOP = lookup.findVarHandle(ThreadStopLoopArrayAccessBench.class, "opaque_stop", boolean.class);
    } catch (NoSuchFieldException | IllegalAccessException e) {
      throw new AssertionError(e);
    }
  }

  @Benchmark
  public int stop_opaque() {
    var sum = 0;
    for(var i = 0; i < array.length; i++) {
      if ((boolean) OPAQUE_STOP.get(this)) {
        break;
      }
      sum += array[i];
    }
    return sum;
  }


  static final class Stop extends MutableCallSite {
    public Stop() {
      super(MethodType.methodType(boolean.class));
      setTarget(MethodHandles.constant(boolean.class, false));
    }
  }

//  static final Stop STOP = new Stop();
//  static final MethodHandle STOP_MH = STOP.dynamicInvoker();
//
//  @Benchmark
//  public int stop_callsite() throws Throwable {
//    var sum = 0;
//    for(var i = 0; i < array.length; i++) {
//      if ((boolean) STOP_MH.invokeExact()) {
//        break;
//      }
//      sum += array[i];
//    }
//    return sum;
//  }


  final Arena arena = Arena.ofShared();
  final MemorySegment.Scope scope = arena.scope();

  @Benchmark
  public int stop_arena() {
    var sum = 0;
    for(var i = 0; i < array.length; i++) {
      if (!scope.isAlive()) {
        break;
      }
      sum += array[i];
    }
    return sum;
  }
}




