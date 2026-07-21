// To start, execute java -jar jvisualbook-*.jar on the command line
// jvisualbook is a notebook program that runs in the browser

// # How to stop a thread?
// ` `

// Remi Forax

// JCrete, July 2026


// ## This is a JVisualBook!
// I'm using [JVisualBook](https://github.com/forax/jvisualbook),
// an interactive Java notebook environment

import module java.base;

IO.println("Java version " + Runtime.version());


// ## How not to stop a thread?
// Why this code does not work as intended?
/*class Demo {
  boolean stop;
  void loop() {
    while(true) {
      if (stop) { break; }
    }
    IO.println("stopped !");
  }
  void stop() { stop = true; }
}
var demo = new Demo();
var thread = Thread.ofPlatform().start(demo::loop);
Thread.sleep(200);
demo.stop();
thread.join();*/


// ## Why it does not work?
// The field `stop` is not changed by the thread, so the JIT hoist the access
// to `stop` outside of the thread

// The code of the `Runnable`
// ```java
//   while(true) {
//     if (stop) {
//       return;
//     }
//   }
// ```
// becomes:
// ```java
//   if (stop) {
//     return;
//   }
//   while(true) {}
// ```

// This is a transformation allowed by the Java Memory Model


// ## How to stop a thread?
// Using `synchronized`

class DemoSynchronized {
  final Object lock = new Object();
  boolean stop;
  void loop() {
    while(true) {
      synchronized(lock) {
        if (stop) { break; }
      }
    }
    IO.println("stopped !");
  }
  void stop() { synchronized(lock) { stop = true; } }
}
var demo = new DemoSynchronized();
var thread = Thread.ofPlatform().start(demo::loop);
Thread.sleep(200);
demo.stop();
thread.join();


// ## Is it fast?

// Two questions:
// - is stopping the thread slow?
// - what is the overhead of checking to stop?

// I'm only interested in the latter question


// ## JMH Benchmark template
// We use the same template to see the impact of the stop check

// ```java
// private final int[] array = new Random(0).ints(100_000).toArray();
//
// @Benchmark
// public int no_stop() {
//   var sum = 0;
//   for(var i = 0; i < array.length; i++) {
//     // checks if thread must stop here!
//     sum += array[i];
//   }
//   return sum;
// }
// ```


// ## Benchmark `synchronized`
// It works, but it is slow!

// ```text
// MacBook Air M2 (jdk 26)
// Benchmark                                           Mode  Cnt    Score   Error  Units
// ThreadStopLoopArrayAccessBench.no_stop              avgt    5   14.416 ± 0.141  us/op
// ThreadStopLoopArrayAccessBench.stop_synchronized    avgt    5  551.283 ± 4.412  us/op
// ```


// ## Why using `synchronized` is slow?
// ` `

// Entering and leaving a synchronized block creates memory barriers,
// enforced by both the JIT and the CPU, preventing many compiler optimizations.

// Repeat that `100_000` times => slooooow


// ## Benchmark `synchronized` (2)
// It works, but it is slow!

// ```text
// MacBook Air M2 (jdk 26)
// Benchmark                                           Mode  Cnt    Score   Error  Units
// ThreadStopLoopArrayAccessBench.no_stop              avgt    5   14.416 ± 0.141  us/op
// ThreadStopLoopArrayAccessBench.stop_synchronized    avgt    5  551.283 ± 4.412  us/op
//
// Intel(R) Xeon(R) Gold 6240R CPU @ 2.40GHz (jdk 26)
// Benchmark                                           Mode  Cnt     Score    Error  Units
// ThreadStopLoopArrayAccessBench.no_stop              avgt    5     3.571 ±  0.047  us/op
// ThreadStopLoopArrayAccessBench.stop_synchronized    avgt    5  1581.669 ±  2.749  us/op
// ```


// ## Why are the results different on M2 and Xeon?
// ` `

// `no_stop` use auto-vectorisation, SIMD inside the loop

// M2 uses 128 bits register vs Xeon uses 512 bits register, so Xeon is 4x faster

// Also, [Emanuel Peter](https://eme64.github.io/blog/) did a lot of work on the vectorisation in jdk26,
// previously, auto-vectorisation only used 64 bits on arm64 :(


// ## How to stop a thread?
// I heard that `ReentrantLock` are faster?

class DemoReentrant {
  final ReentrantLock lock = new ReentrantLock();
  boolean stop;
  void loop() {
    while(true) {
      lock.lock();
      try {
        if (stop) { break; }
      } finally { lock.unlock(); }
    }
    IO.println("stopped !");
  }
  void stop() { lock.lock(); try { stop = true; } finally { lock.unlock(); } }
}
var demo = new DemoReentrant();
var thread = Thread.ofPlatform().start(demo::loop);
Thread.sleep(200);
demo.stop();
thread.join();


// ## Benchmark `ReentrantLock`
// ` `

// ```text
// MacBook Air M2 (jdk 26) SIMD 128bits
// Benchmark                                           Mode  Cnt    Score   Error  Units
// ThreadStopLoopArrayAccessBench.no_stop              avgt    5   14.416 ± 0.141  us/op
// ThreadStopLoopArrayAccessBench.stop_reentrant_lock  avgt    5  841.415 ± 4.100  us/op
// ThreadStopLoopArrayAccessBench.stop_synchronized    avgt    5  551.283 ± 4.412  us/op
//
// Intel(R) Xeon(R) Gold 6240R CPU @ 2.40GHz (jdk 26) SIMD 512bits
// Benchmark                                           Mode  Cnt     Score    Error  Units
// ThreadStopLoopArrayAccessBench.no_stop              avgt    5     3.571 ±  0.047  us/op
// ThreadStopLoopArrayAccessBench.stop_reentrant_lock  avgt    5  1568.583 ± 50.770  us/op
// ThreadStopLoopArrayAccessBench.stop_synchronized    avgt    5  1581.669 ±  2.749  us/op
// ```


// ## ReentrantLock is not faster
// They should not be slower either?

// I don't fully know why, maybe some missing optimization on ARM64?


// ## Let's use `thread.interrupt()`

class DemoInterrupt {
  void loop() {
    while(true) {
      if (Thread.interrupted()) { break; }
    }
    IO.println("stopped !");
  }
}
var demo = new DemoInterrupt();
var thread = Thread.ofPlatform().start(demo::loop);
Thread.sleep(200);
thread.interrupt();
thread.join();


// ## Benchmark `Thread.interrupted()`
// This is at least better than `synchronized`, but still has side effects

// ```text
// MacBook Air M2 (jdk 26) SIMD 128bits
// Benchmark                                           Mode  Cnt    Score   Error  Units
// ThreadStopLoopArrayAccessBench.no_stop              avgt    5   14.416 ± 0.141  us/op
// ThreadStopLoopArrayAccessBench.stop_interrupt       avgt    5   63.002 ± 0.347  us/op
// ThreadStopLoopArrayAccessBench.stop_synchronized    avgt    5  551.283 ± 4.412  us/op
//
// Intel(R) Xeon(R) Gold 6240R CPU @ 2.40GHz (jdk 26) SIMD 512bits
// Benchmark                                           Mode  Cnt     Score    Error  Units
// ThreadStopLoopArrayAccessBench.no_stop              avgt    5     3.571 ±  0.047  us/op
// ThreadStopLoopArrayAccessBench.stop_interrupt       avgt    5    81.599 ±  0.135  us/op
// ThreadStopLoopArrayAccessBench.stop_synchronized    avgt    5  1581.669 ±  2.749  us/op
// ```


// ## How to stop a thread?
// Using `volatile`

class DemoVolatile {
  volatile boolean stop;
  void loop() {
    while(true) {
      if (stop) { break; }
    }
    IO.println("stopped !");
  }
  void stop() { stop = true; }
}
var demo = new DemoVolatile();
var thread = Thread.ofPlatform().start(demo::loop);
Thread.sleep(200);
demo.stop();
thread.join();


// ## Benchmark `volatile`

// ```text
// MacBook Air M2 (jdk 26) SIMD 128bits
// Benchmark                                           Mode  Cnt    Score   Error  Units
// ThreadStopLoopArrayAccessBench.no_stop              avgt    5   14.416 ± 0.141  us/op
// ThreadStopLoopArrayAccessBench.stop_interrupt       avgt    5   63.002 ± 0.347  us/op
// ThreadStopLoopArrayAccessBench.stop_synchronized    avgt    5  551.283 ± 4.412  us/op
// ThreadStopLoopArrayAccessBench.stop_volatile        avgt    5   63.100 ± 0.585  us/op
//
// Intel(R) Xeon(R) Gold 6240R CPU @ 2.40GHz (jdk 26) SIMD 512bits
// Benchmark                                           Mode  Cnt     Score    Error  Units
// ThreadStopLoopArrayAccessBench.no_stop              avgt    5     3.571 ±  0.047  us/op
// ThreadStopLoopArrayAccessBench.stop_interrupt       avgt    5    81.599 ±  0.135  us/op
// ThreadStopLoopArrayAccessBench.stop_synchronized    avgt    5  1581.669 ±  2.749  us/op
// ThreadStopLoopArrayAccessBench.stop_volatile        avgt    5    81.710 ±  0.236  us/op
// ```


// ## Effects of volatile
// According to the Java Memory Model, `volatile` has side effects on other reads/writes

// In terms of memory effects, `volatile` reads/writes have similar effects
// to `synchronized`: both prevent compiler and CPU reorderings,
// which inhibits loop optimizations

// When [virtual threads were added](https://github.com/openjdk/jdk/commit/9583e3657e43cc1c6f2101a64534564db2a9bd84),
// `Thread.interrupted()` was rewritten to use volatile instead of using a native C++ method


// ## How to stop a thread?
// Using `opaque` read/write

// First, we need to create a `VarHandle`:

static VarHandle createVarHandle(Class<?> clazz) {
  var lookup = MethodHandles.lookup();
  try {
    return lookup.findVarHandle(clazz, "stop", boolean.class)
        .withInvokeExactBehavior();
  } catch (NoSuchFieldException | IllegalAccessException e) {
    throw new AssertionError(e);
  }
}

// `.withInvokeExactBehavior()` asks the VM to not do any runtime adaptations


// ## How to stop a thread?
// Using a VarHandle `opaque` read/write

class DemoOpaque {
  static final VarHandle STOP = createVarHandle(DemoOpaque.class);
  boolean stop;
  void loop() {
    while(true) {
      var stop = (boolean) STOP.getOpaque(this);
      if (stop) { break; }
    }
    IO.println("stopped !");
  }
  void stop() { STOP.setOpaque(this, true); }
}
var demo = new DemoOpaque();
var thread = Thread.ofPlatform().start(demo::loop);
Thread.sleep(200);
demo.stop();
thread.join();


// ## Benchmark `opaque`

// ```text
// MacBook Air M2 (jdk 26) SIMD 128bits
// Benchmark                                           Mode  Cnt    Score   Error  Units
// ThreadStopLoopArrayAccessBench.no_stop              avgt    5   14.416 ± 0.141  us/op
// ThreadStopLoopArrayAccessBench.stop_opaque          avgt    5   14.591 ± 0.637  us/op
// ThreadStopLoopArrayAccessBench.stop_volatile        avgt    5   63.100 ± 0.585  us/op
//
// Intel(R) Xeon(R) Gold 6240R CPU @ 2.40GHz (jdk 26) SIMD 512bits
// Benchmark                                           Mode  Cnt     Score    Error  Units
// ThreadStopLoopArrayAccessBench.no_stop              avgt    5     3.571 ±  0.047  us/op
// ThreadStopLoopArrayAccessBench.stop_opaque          avgt    5     3.594 ±  0.130  us/op
// ThreadStopLoopArrayAccessBench.stop_volatile        avgt    5    81.710 ±  0.236  us/op
// ```


// ## Takeaway
// ` `

// | Approach               | Correct? | Overhead (M2) | Overhead (Xeon) | Verdict    |
// | ---------------------- | -------- | ------------- | --------------- | ---------- |
// | plain boolean          | [ ]      | N/A (broken)  | N/A (broken)    | Don't use  |
// | `synchronized`         | [X]      | 38x           | 443x            | Too slow   |
// | `ReentrantLock`        | [X]      | 58x           | 439x            | Too slow   |
// | `Thread.interrupted()` | [X]      | 4.4x          | 23x             | Okayish    |
// | `volatile`             | [X]      | 4.4x          | 23x             | Okayish    |
// | **`VarHandle` opaque** | [X]      | **~0%**       | **~0%**         | **Best**   |

// Note that a huge part of the Xeon overhead is due to a SIMD width of 512 bits


// ## Conclusion

// Reading or writing a `volatile` variable can inhibit optimizations in surrounding code

// Volatile is not free, ask yourself if you need ordering guarantees
// with respect to other field accesses,  if it's not the case, `opaque` is great


// ## Supplementary slide
// The Full Benchmark

// ```text
// MacBook Air M2 (jdk 26) SIMD 128bits
// Benchmark                                           Mode  Cnt    Score   Error  Units
// ThreadStopLoopArrayAccessBench.no_stop              avgt    5   14.416 ± 0.141  us/op
// ThreadStopLoopArrayAccessBench.stop_interrupt       avgt    5   63.002 ± 0.347  us/op
// ThreadStopLoopArrayAccessBench.stop_opaque          avgt    5   14.591 ± 0.637  us/op
// ThreadStopLoopArrayAccessBench.stop_reentrant_lock  avgt    5  841.415 ± 4.100  us/op
// ThreadStopLoopArrayAccessBench.stop_synchronized    avgt    5  551.283 ± 4.412  us/op
// ThreadStopLoopArrayAccessBench.stop_volatile        avgt    5   63.100 ± 0.585  us/op
//
// Intel(R) Xeon(R) Gold 6240R CPU @ 2.40GHz (jdk 26) SIMD 512bits
// Benchmark                                           Mode  Cnt     Score    Error  Units
// ThreadStopLoopArrayAccessBench.no_stop              avgt    5     3.571 ±  0.047  us/op
// ThreadStopLoopArrayAccessBench.stop_interrupt       avgt    5    81.599 ±  0.135  us/op
// ThreadStopLoopArrayAccessBench.stop_opaque          avgt    5     3.594 ±  0.130  us/op
// ThreadStopLoopArrayAccessBench.stop_reentrant_lock  avgt    5  1568.583 ± 50.770  us/op
// ThreadStopLoopArrayAccessBench.stop_synchronized    avgt    5  1581.669 ±  2.749  us/op
// ThreadStopLoopArrayAccessBench.stop_volatile        avgt    5    81.710 ±  0.236  us/op
// ```
