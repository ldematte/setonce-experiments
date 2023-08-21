Alternative experimental implementation(s) to Lucene `SetOnce<>`, using `VarHandle` directly 
instead of `AtomicReference<>` like the original implementation.

The various classes implement slight variations on the same theme; most preserve 
the original class semantics, with a couple of variants to measure impact on performance
of the various constructs involved.

In particular, `FunctionBasedConfigurableSetOnce` and `SwitchBasedConfigurableSetOnce` add the
ability of using a different mode for read (i.e. value `get` - the same could be done for value `set` too), which maps 
to different access semantics (volatile, acquire/release, opaque).

Some microbenchmarks are included; they can be run using `gradle jmh`.
