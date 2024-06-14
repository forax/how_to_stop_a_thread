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