package org.elasticsearch.experiments;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

@State(Scope.Benchmark)
public class ClassBasedConfigurableSetOnceBenchmarks {

    private ClassBasedConfigurableSetOnce<Integer> mySetOnce;

    @Setup(Level.Invocation)
    public void setUp() {
        mySetOnce = new ClassBasedConfigurableSetOnce<>(0);
    }
    @Benchmark
    public void readIntensiveSetOnceBenchmark(Blackhole blackhole) {
        mySetOnce.set(10);
        for (int i = 0; i < 100; ++i) {
            blackhole.consume(mySetOnce.get());
        }
    }
}
