import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

final Arena arena = Arena.ofShared();
final MemorySegment.Scope scope = arena.scope();

void loop() {
  while(true) {
    if (!scope.isAlive()) {
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
  arena.close();
}