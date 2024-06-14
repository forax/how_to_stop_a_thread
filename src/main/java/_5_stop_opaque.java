import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

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
    // global warming
  }
  System.out.println("end !");
}

void main() throws InterruptedException {
  var thread = new Thread(this::loop);
  thread.start();

  Thread.sleep(1_000);
  STOP.setOpaque(this, true);
}