volatile boolean stop;

void loop() {
  while(true) {
    if (stop) {
      break;
    }
    // ...
  }
  IO.println("end !");
}

void main() throws InterruptedException {
  var thread = new Thread(this::loop);
  thread.start();

  Thread.sleep(1_000);
  stop = true;
}