import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;

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
    // global warming
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