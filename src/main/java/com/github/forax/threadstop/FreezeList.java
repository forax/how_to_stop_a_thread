package com.github.forax.threadstop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.AbstractList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.RandomAccess;

public final class FreezeList<E> extends AbstractList<E> implements RandomAccess {
  private E[] elements;
  private int size;
  private final Thread ownerThread;
  private final Arena arena;
  private final MemorySegment.Scope scope;

  // see jdk.internal.misc.ScopeMemoryAccess.Scoped
  @Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
  @Retention(RetentionPolicy.RUNTIME)
  @interface Scoped { }

  public FreezeList() {
    var arena = Arena.ofShared();
    var scope = arena.scope();
    @SuppressWarnings("unchecked")
    var elements = (E[]) new Object[16];
    this.elements = elements;
    this.ownerThread = Thread.currentThread();
    this.arena = arena;
    this.scope = scope;
    // super();
  }

  public void freeze() {
    synchronized (arena) {
      // double check locking pattern !!
      if (!scope.isAlive()) {
        return;
      }
      try {
        arena.close();
      } catch (IllegalStateException e) {
        throw new IllegalStateException("owner thread can still do mutations", e);
      }
    }
  }

  @Override
  public int size() {
    if (ownerThread != Thread.currentThread() && scope.isAlive()) {
      freeze();
    }
    return size;
  }

  @Override
  public E get(int index) {
    if (ownerThread != Thread.currentThread() && scope.isAlive()) {
      freeze();
    }
    Objects.checkIndex(index, size);
    return elements[index];
  }

  private void resize() {
    elements = Arrays.copyOf(elements, size * 2);
  }

  @Override
  @Scoped
  public boolean add(E element) {
    Objects.requireNonNull(element);
    if (ownerThread != Thread.currentThread()) {
      throw new IllegalStateException("invalid owner thread");
    }
    if (!scope.isAlive()) {
      throw new UnsupportedOperationException("list is frozen");
    }
    // modifications thus the method has to be @Scoped
    if (elements.length == size) {
      resize();  // slow path
    }
    elements[size++] = element;
    return true;
  }

  @Override
  @Scoped
  public E set(int index, E element) {
    Objects.requireNonNull(element);
    if (ownerThread != Thread.currentThread()) {
      throw new IllegalStateException("invalid owner thread");
    }
    if (!scope.isAlive()) {
      throw new UnsupportedOperationException("list is frozen");
    }
    Objects.checkIndex(index, size);
    var oldElement = elements[index];
    // modifications thus the method has to be @Scoped
    elements[index] = element;
    return oldElement;
  }

  @Override
  public Iterator<E> iterator() {
    var currentThread = Thread.currentThread();
    if (ownerThread != currentThread && scope.isAlive()) {
      freeze();
    }
    var size = this.size;
    var elements = this.elements;
    return new Iterator<>() {
      private int index;

      @Override
      public boolean hasNext() {
        if (Thread.currentThread() != currentThread) {
          throw new IllegalStateException("invalid current thread");
        }
        return index < size;
      }

      @Override
      public E next() {
        if (Thread.currentThread() != currentThread) {
          throw new IllegalStateException("invalid current thread");
        }
        if (index < size) {
          return elements[index++];
        }
        throw new NoSuchElementException();
      }
    };
  }
}
