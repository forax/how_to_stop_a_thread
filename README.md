# how_to_stop_a_thread
A study of the different ways to stop a thread in Java.

For each way to stop a thread, we bench the cost of reading the value `stop` value once
or multiple times in a loop of an array of 100 000 elements.

The following results are on my MacBook Air M2:
```
// ThreadStopBench.stop_synchronized    avgt    5  5.055 ± 0.023  ns/op
// ThreadStopBench.stop_reentrant_lock  avgt    5  8.406 ± 0.035  ns/op
// ThreadStopBench.stop_interrupt       avgt    5  0.490 ± 0.002  ns/op
// ThreadStopBench.stop_volatile        avgt    5  0.496 ± 0.004  ns/op
// ThreadStopBench.stop_opaque          avgt    5  0.409 ± 0.004  ns/op
// ThreadStopBench.stop_callsite        avgt    5  0.306 ± 0.002  ns/op
// ThreadStopBench.stop_arena           avgt    5  0.613 ± 0.004  ns/op
```

```
// ThreadStopLoopBench.stop_synchronized    avgt    5  510.287 ± 24.235  us/op
// ThreadStopLoopBench.stop_reentrant_lock  avgt    5  833.998 ±  1.591  us/op
// ThreadStopLoopBench.stop_interrupt       avgt    5   51.382 ±  0.423  us/op
// ThreadStopLoopBench.stop_volatile        avgt    5   51.867 ±  0.303  us/op
// ThreadStopLoopBench.stop_opaque          avgt    5   30.584 ±  0.075  us/op
// ThreadStopLoopBench.stop_callsite        avgt    5   30.604 ±  0.071  us/op
// ThreadStopLoopBench.stop_arena           avgt    5   30.592 ±  0.091  us/op
```

1. Using synchronized

Here, we are using the keyword synchronized to protect the access to the field `stop`.

```java
boolean stop;
final Object lock = new Object();

void loop() {
  while(true) {
    synchronized (lock) {
      if (stop) {
        break;
      }
    }
    // ...
  }
  System.out.println("end !");
}

void main() throws InterruptedException {
  var thread = new Thread(this::loop);
  thread.start();

  Thread.sleep(1_000);
  synchronized (lock) {
    stop = true;
  }
}
```

2. Using ReentrantLock

Same code but using a reentrant lock instead of a synchronized block.

```java
boolean stop;
final ReentrantLock lock = new ReentrantLock();

void loop() {
  while(true) {
    lock.lock();
    try {
      if (stop) {
        break;
      }
    } finally {
      lock.unlock();
    }
    // ...
  }
  System.out.println("end !");
}

void main() throws InterruptedException {
  var thread = new Thread(this::loop);
  thread.start();

  Thread.sleep(1_000);
  lock.lock();
  try {
    stop = true;
  } finally {
    lock.unlock();
  }
}
```

3. Using interrupted

Java has its own mechanism to interrupt a thread.

```java
void loop() {
  while(true) {
    if (Thread.interrupted()) {
      break;
    }
    // ...
  }
  System.out.println("end !");
}

void main() throws InterruptedException {
  var thread = new Thread(this::loop);
  thread.start();

  Thread.sleep(1_000);
  thread.interrupt();
}
```

4. Using volatile

Thread.interrupted()/interrupt are using internally a volatile field.

```java
volatile boolean stop;

void loop() {
  while(true) {
    if (stop) {
      break;
    }
    // ...
  }
  System.out.println("end !");
}

void main() throws InterruptedException {
  var thread = new Thread(this::loop);
  thread.start();

  Thread.sleep(1_000);
  stop = true;
}
```

5. Using opaque (VarHandle)

Instead of using the volatile semantics, we can use the `opaque` semantics.
This is usually faster that using the keyword `volatile` but sadly, this is usually
not the semantics we want, `opaque` does not guarantee that the preview writes will be seen
by the thread that reads the values when stop becomes true

```java
static final VarHandle STOP = createVH();

private static VarHandle createVH() {
  var lookup = MethodHandles.lookup();
  try {
    return lookup.findVarHandle(lookup.lookupClass(), "stop", boolean.class);
  } catch (NoSuchFieldException | IllegalAccessException e) {
    throw new AssertionError(e);
  }
}

boolean stop;

void loop() {
  while(true) {
    var stop = (boolean) STOP.getOpaque(this);
    if (stop) {
      break;
    }
    // ...
  }
  System.out.println("end !");
}

void main() throws InterruptedException {
  var thread = new Thread(this::loop);
  thread.start();

  Thread.sleep(1_000);
  STOP.setOpaque(this, true);
}
```

6. using a MutableCallSite

We can cheat and say that because there only one thread we can use a global state
and then uses a MutableCallSite to first always return `false` and then always return `true`.
Internally the VM will first generate a code that skip the branch of the `if` because `code`
is always `true`and then when de-optimize the code when the code is changed to return `true`.

```java
static final class Stop extends MutableCallSite {
  public Stop() {
    super(MethodType.methodType(boolean.class));
    setTarget(MethodHandles.constant(boolean.class, false));
  }
}

static final Stop STOP = new Stop();
static final MethodHandle STOP_MH = STOP.dynamicInvoker();

void loop() {
  while(true) {
    boolean stop;
    try {
      stop = (boolean) STOP_MH.invokeExact();
    } catch (RuntimeException | Error e) {
      throw e;
    } catch (Throwable e) {
      throw new AssertionError(e);
    }
    if (stop) {
      break;
    }
    // ...
  }
  System.out.println("end !");
}

void main() throws InterruptedException {
  var thread = new Thread(this::loop);
  thread.start();

  Thread.sleep(1_000);
  STOP.setTarget(MethodHandles.constant(boolean.class, true));
  MutableCallSite.syncAll(new MutableCallSite[] { STOP });
}
```

7. Using foreign memory Arena

When the `Arena` created using `ofAshared()` is closed, it forces all the other threads to go to a GC safepoint,
so the cost of calling `scope.isAlive()` is a simple read.

```java
final Arena arena = Arena.ofShared();
final MemorySegment.Scope scope = arena.scope();

void loop() {
  while(true) {
    if (!scope.isAlive()) {
      break;
    }
    // ...
  }
  System.out.println("end !");
}

void main() throws InterruptedException {
  var thread = new Thread(this::loop);
  thread.start();

  Thread.sleep(1_000);
  arena.close();
}
```


