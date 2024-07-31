package com.github.forax.threadstop.bench;

import com.github.forax.threadstop.FreezeList;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toCollection;

// Benchmark                                       Mode  Cnt  Score   Error  Units
// FreezeListBench.arrayListIteratorLoop           avgt    5  4.219 ± 0.014  us/op
// FreezeListBench.arrayListLoop                   avgt    5  4.122 ± 0.005  us/op
// FreezeListBench.freezeListIteratorLoop          avgt    5  4.165 ± 0.005  us/op
// FreezeListBench.freezeListLoop                  avgt    5  3.989 ± 0.003  us/op

// $JAVA_HOME/bin/java -jar target/benchmarks.jar -prof dtraceasm
@Warmup(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Fork(value = 1)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
public class FreezeListBench {

  private List<Integer> arrayList = IntStream.range(0, 10_000)
      .boxed()
      .collect(toCollection(ArrayList::new));

  private List<Integer> freezeList = IntStream.range(0, 10_000)
      .boxed()
      .collect(toCollection(FreezeList::new));

  @Benchmark
  public int freezeListLoop() {
    var sum = 0;
    for (var i = 0; i < freezeList.size(); i++) {
      var element = freezeList.get(i);
      sum += element;
    }
    return sum;
  }

  @Benchmark
  public int freezeListIteratorLoop() {
    var sum = 0;
    for(var element : freezeList) {
      sum += element;
    }
    return sum;
  }

  @Benchmark
  public int arrayListLoop() {
    var sum = 0;
    for (var i = 0; i < arrayList.size(); i++) {
      var element = arrayList.get(i);
      sum += element;
    }
    return sum;
  }

  @Benchmark
  public int arrayListIteratorLoop() {
    var sum = 0;
    for(var element : arrayList) {
      sum += element;
    }
    return sum;
  }
}


