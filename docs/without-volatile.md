# Why double-checked locking without volatile is unsafe

This explanation uses the educational [`BrokenDoubleCheckedSingleton`](../src/main/java/examples/singletons/unsafe/BrokenDoubleCheckedSingleton.java) example. Its `instance` reference is not `volatile`, and `value` is an ordinary, non-`final` field:

```java
private static BrokenDoubleCheckedSingleton instance;
private int value;

private BrokenDoubleCheckedSingleton() {
    value = 42;
}
```

## A possible scenario

The order of actions within one thread does not, by itself, guarantee which writes another thread observes without the required happens-before relationship ([Java Memory Model](https://docs.oracle.com/javase/specs/jls/se8/html/jls-17.html)). The scenario below describes a permitted observation, not a guarantee of reproducing it on every run or a specific ordering of machine instructions.

| Step | Thread A | Thread B |
| --- | --- | --- |
| 1 | Sees `null`, acquires the monitor, and checks for `null` again. | |
| 2 | Creates an object with the initial value `value == 0`. | |
| 3 | The constructor writes `value = 42`. | |
| 4 | Writes the reference to the ordinary `instance` field. | |
| 5 | | Reads a non-null reference from `instance`. |
| 6 | | Skips `synchronized` and returns the object. |
| 7 | | Calls `getValue()` and may read `0` rather than `42`. |

Without safe publication, visibility of the new reference does not imply visibility of all preceding writes to the object; a read of an ordinary field is not required to observe the constructor's write without the necessary happens-before relationship ([JLS, Section 17.4.5](https://docs.oracle.com/javase/specs/jls/se8/html/jls-17.html)).

## Why synchronized does not protect the fast path

Releasing a monitor happens-before a subsequent acquisition of the same monitor ([JLS, Chapter 17](https://docs.oracle.com/javase/specs/jls/se8/html/jls-17.html)). However, thread B does not acquire that monitor in this scenario, so the guarantee does not apply to its fast path.

## What volatile fixes

The necessary change is in the reference declaration, not in the `value` field:

```java
private static volatile DoubleCheckedSingleton instance;
```

A write to a `volatile` field happens-before a subsequent read of that field; combined with program order in each thread, this creates a transitive chain from the write `value = 42` to the read of `value` by the thread receiving the reference ([JLS, Chapter 17](https://docs.oracle.com/javase/specs/jls/se8/html/jls-17.html)). If the field is not modified after construction, the recipient of the published instance reads `42`.

## Why an ordinary test may always pass

Being unsafe means lacking the required guarantee, not necessarily exhibiting a failure in every execution ([Java Memory Model](https://docs.oracle.com/javase/specs/jls/se8/html/jls-17.html)). This project therefore does not treat a successful run of the unsafe example as proof of safety, and no test requires observing `0`.
