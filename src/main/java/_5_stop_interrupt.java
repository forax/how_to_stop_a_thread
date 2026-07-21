void loop() {
  while(true) {
    if (Thread.interrupted()) {
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
  thread.interrupt();
}