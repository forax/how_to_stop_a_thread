package com.github.forax.threadstop;

import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

public final class FreezeListTest {
  @Test
  public void add() {
    var list = new FreezeList<String>();
    list.add("foo");
    assertAll(
        () -> assertEquals(1, list.size()),
        () -> assertEquals("foo", list.get(0))
    );
  }

  @Test
  public void set() {
    var list = new FreezeList<String>();
    list.add("foo");
    list.set(0, "bar");
    assertAll(
        () -> assertEquals(1, list.size()),
        () -> assertEquals("bar", list.get(0))
    );
  }

  @Test
  public void addAfterFreeze() {
    var list = new FreezeList<String>();
    list.freeze();
    assertThrows(UnsupportedOperationException.class, () -> list.add("foo"));
  }

  @Test
  public void setAfterFreeze() {
    var list = new FreezeList<String>();
    list.add("foo");
    list.freeze();
    assertThrows(UnsupportedOperationException.class, () -> list.set(0, "bar"));
  }

  @Test
  public void indexedLoop() {
    var list = new FreezeList<Integer>();
    IntStream.range(0, 10).forEach(list::add);
    var sum = 0;
    for (var i = 0; i < list.size(); i++) {
      var value = list.get(i);
      sum += value;
    }
    assertEquals(45, sum);
  }

  @Test
  public void iteratorLoop() {
    var list = new FreezeList<Integer>();
    IntStream.range(0, 10).forEach(list::add);
    var sum = 0;
    for (var value : list) {
      sum += value;
    }
    assertEquals(45, sum);
  }

  @Test
  public void twoThreads() throws InterruptedException, ExecutionException {
    var list = new FreezeList<String>();
    list.add("foo");
    try(var executor = Executors.newSingleThreadExecutor()) {
      var task = executor.submit(() -> {
        assertAll(
            () -> assertEquals(1, list.size()),
            () -> assertEquals("foo", list.get(0))
        );
      });
      task.get();
    }
  }

  @Test
  public void twoThreadsIndexedLoop() throws InterruptedException, ExecutionException {
    var list = new FreezeList<Integer>();
    IntStream.range(0, 10).forEach(list::add);
    try(var executor = Executors.newSingleThreadExecutor()) {
      var task = executor.submit(() -> {
        var sum = 0;
        for (var i = 0; i < list.size(); i++) {
          var value = list.get(i);
          sum += value;
        }
        assertEquals(45, sum);
      });
      task.get();
    }
  }

  @Test
  public void twoThreadsIteratorLoop() throws InterruptedException, ExecutionException {
    var list = new FreezeList<Integer>();
    IntStream.range(0, 10).forEach(list::add);
    try(var executor = Executors.newSingleThreadExecutor()) {
      var task = executor.submit(() -> {
        var sum = 0;
        for (var value : list) {
          sum += value;
        }
        assertEquals(45, sum);
      });
      task.get();
    }
  }

  @Test
  public void iteratorWrongThreadCallHasNext() throws InterruptedException, ExecutionException {
    var list = new FreezeList<Integer>();
    list.add(101);
    var iterator = list.iterator();
    try(var executor = Executors.newSingleThreadExecutor()) {
      var task = executor.submit(() -> {
        assertThrows(IllegalStateException.class, iterator::hasNext);
      });
      task.get();
    }
  }

  @Test
  public void iteratorWrongThreadCallNext() throws InterruptedException, ExecutionException {
    var list = new FreezeList<Integer>();
    list.add(101);
    var iterator = list.iterator();
    try(var executor = Executors.newSingleThreadExecutor()) {
      var task = executor.submit(() -> {
        assertThrows(IllegalStateException.class, iterator::next);
      });
      task.get();
    }
  }

  @Test
  public void twoThreadsAdd() throws InterruptedException, ExecutionException {
    var list = new FreezeList<String>();
    list.add("foo");
    try(var executor = Executors.newSingleThreadExecutor()) {
      var task = executor.submit(() -> {
        assertThrows(IllegalStateException.class, () -> list.add("bar"));
      });
      task.get();
    }
  }

  @Test
  public void twoThreadsSet() throws InterruptedException, ExecutionException {
    var list = new FreezeList<String>();
    list.add("foo");
    try(var executor = Executors.newSingleThreadExecutor()) {
      var task = executor.submit(() -> {
        assertThrows(IllegalStateException.class, () -> list.set(0, "bar"));
      });
      task.get();
    }
  }

  /*@Test FIXME
  public void twoThreadsAddAndGet() throws InterruptedException, ExecutionException {
    var list = new FreezeList<Integer>();
    try(var executor = Executors.newSingleThreadExecutor()) {
      var task = executor.submit(() -> {
        while(!Thread.interrupted()) {
          assertNotNull(list.get(list.size() - 1));
        }
      });
      //Thread.sleep(1);
      for(var i = 0; i < 1_000_000; i++) {
        list.add(i);
      }
      System.err.println("cancel");
      task.cancel(true);
    }
  }*/
}