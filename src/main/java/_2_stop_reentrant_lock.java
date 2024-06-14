import java.util.concurrent.locks.ReentrantLock;

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
    // global warming
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