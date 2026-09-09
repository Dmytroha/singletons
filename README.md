# singletons

[![Java tests](https://github.com/Dmytroha/singletons/actions/workflows/tests.yml/badge.svg?branch=main)](https://github.com/Dmytroha/singletons/actions/workflows/tests.yml)

Lazy singleton implementations in Java from our discussion: three correct variants and one intentionally unsafe example without `volatile`. The code includes English explanations and multithreaded tests with no third-party dependencies.

## Implementations

| Class | Construction and publication | Status |
| --- | --- | --- |
| [`BoundedCasSingleton`](src/main/java/examples/singletons/BoundedCasSingleton.java) | At most two successfully constructed candidates; every call returns the CAS winner | Correct example |
| [`HolderSingleton`](src/main/java/examples/singletons/HolderSingleton.java) | One instance created during nested class initialization | Correct example |
| [`DoubleCheckedSingleton`](src/main/java/examples/singletons/DoubleCheckedSingleton.java) | One instance, double-checked locking, and `volatile` | Correct example |
| [`BrokenDoubleCheckedSingleton`](src/main/java/examples/singletons/unsafe/BrokenDoubleCheckedSingleton.java) | Double-checked locking without `volatile`; no safe publication for the fast path | **Unsafe example, do not use** |

Each example has an ordinary, non-`final` field named `value`, which the constructor sets to `42`. It demonstrates safe publication; the code does not modify the field after construction.

## Building and running

JDK 17 or newer is required. The `build.sh` script also requires Bash, for example on Linux, macOS, or WSL; Maven, Gradle, and external libraries are not needed.

```bash
# Compile and run the tests in three separate JVMs.
./build.sh

# Run the tests in ten separate JVMs.
./build.sh 10

# Run a simple demonstration after building.
java -cp out/main examples.singletons.Demo
```

Each test run starts a new JVM to exercise concurrent first access again. For each correct implementation, 64 threads are started, and each thread makes 5,000 calls.

The tests check:

- **Reference identity:** every thread receives the same object, and subsequent calls keep returning it.
- **State:** each worker thread reads `value == 42` immediately after obtaining the object.
- **Termination:** waits have timeouts, and failures produce a nonzero exit code.

These tests do not prove correctness under the Java Memory Model or cover every possible thread schedule. They also do not count internal constructor calls or guarantee that the CAS implementation actually creates a second candidate in a particular run; the two-candidate bound follows from the algorithm.

The unsafe example is compiled but deliberately excluded from the passing test suite and `Demo`. A run without an observed failure does not make it safe.

## Automated tests with GitHub Actions

The [`Java tests`](.github/workflows/tests.yml) workflow runs on every `push`, when a pull request is opened or updated, and when started manually from the Actions tab. Its configuration is stored in `.github/workflows/tests.yml`.

- **Java versions:** 17, 21, and 25, using the Temurin distribution on Linux.
- **Checks:** `./build.sh 10` compiles the project and runs the multithreaded tests in ten separate JVMs for each Java version, followed by `Demo`.
- **Permissions:** only `contents: read`, with no additional user-provided secrets; checkout does not retain credentials for subsequent Git commands.
- **Limits:** each job has a ten-minute timeout, and a newer run cancels an outdated run for the same branch or pull request. A failure on one Java version does not cancel the checks for the other versions.
- **CI dependencies:** `actions/checkout` and `actions/setup-java` are pinned to specific commit SHAs.

The result of each run is available in the repository's Actions tab. The unsafe example without `volatile` is still only compiled and is not included in the passing test suite.

## Main branch protection

The `main` branch is protected by GitHub rules. A successful local run of `./build.sh` is useful for checking changes, but does not replace the required GitHub Actions checks before merging.

- **Required checks:** `Java 17`, `Java 21`, and `Java 25`. Each name is tied to GitHub Actions as its source, so a check with the same name from another app does not satisfy the requirement.
- **Up-to-date branch:** the pull request branch must be up to date with `main` before merging. If new commits have been added to `main`, update your branch and wait for the checks on the updated version.
- **Administrators:** the restrictions also apply to repository administrators; bypassing the checks is not enabled for them.
- **History protection:** force pushes to `main` and deletion of `main` are prohibited.
- **Reviews:** mandatory review approvals are not configured. This does not remove the requirement for test checks.

Recommended contribution workflow:

1. Create a separate branch from the latest `main` and make your changes.
2. Run `./build.sh` locally if possible, then push the branch to GitHub and open a pull request targeting `main`.
3. Update your branch with changes from `main` if necessary, and wait for `Java 17`, `Java 21`, and `Java 25` to pass.
4. Merge the pull request normally, without disabling branch protection.

The enabled rules require checks, but do not impose a separate requirement that every change must go through a pull request: using PRs is the recommended process here. Required checks and enforcement for administrators are described in the [GitHub documentation](https://docs.github.com/repositories/configuring-branches-and-merges-in-your-repository/defining-the-mergeability-of-pull-requests/about-protected-branches).

## CAS with at most two candidates

`AtomicReference.compareAndSet(null, candidate)` atomically publishes a candidate only if the current value is `null`, so there is a single winner ([AtomicReference documentation](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/concurrent/atomic/AtomicReference.html)). In this example, "first" means the first successfully published object, not the one whose constructor started or finished first.

`Semaphore(2)` uses two permits to limit the number of threads in the construction section ([Semaphore documentation](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/Semaphore.html)). In this algorithm, a thread holds its permit until it publishes or discards its candidate; the next thread to acquire a permit re-reads `INSTANCE` and does not create another object once a winner has been published.

Successful initialization may create one or two objects, but only the winner is returned. After initialization, the fast path does not use the semaphore; initialization itself may block, so this implementation is not a fully nonblocking algorithm.

The losing candidate's `dispose()` method is called to release external resources owned specifically by that candidate. This example has no such resources, so the method is empty.

`dispose()` does not delete the object from memory: once no references keep it reachable, it becomes eligible for garbage collection, with no guarantee of immediate memory reclamation ([GC documentation](https://docs.oracle.com/en/java/javase/21/gctuning/other-considerations.html)). There is no need to call `System.gc()` for this.

## Holder

The nested `Holder` class is initialized on the first access to its `INSTANCE` field; the JVM synchronizes class initialization and provides the required visibility guarantees ([JLS, Chapter 12](https://docs.oracle.com/javase/specs/jls/se22/html/jls-12.html)). No explicit `synchronized`, `volatile`, or CAS is needed here.

Of these implementations, I would choose this one for a simple singleton without initialization parameters. It is shorter and does not create extra candidates.

## Double-checked locking with volatile

The first check avoids acquiring the monitor after the instance has been created. The second check runs inside `synchronized`, after the required re-read of `instance`, because another thread may already have created the object.

Making the reference `volatile` ensures safe publication: a write to the field happens-before a subsequent read of the same field, and transitivity extends this guarantee to preceding writes made by the constructor ([JLS, Chapter 17](https://docs.oracle.com/javase/specs/jls/se8/html/jls-17.html)). The local variable allows the fast path to use a single `volatile` read for both the check and the return value.

```text
Thread A                         Thread B

value = 42
    |
volatile write to instance ---> volatile read of instance
                                    |
                               read value: 42
```

## Why omitting volatile is unsafe

A thread may observe a non-null reference on the fast path, skip the monitor, and lack a happens-before relationship with the constructor's writes; in particular, it is permitted to observe the initial value `0` instead of `42` for an ordinary field ([JLS, Chapter 17](https://docs.oracle.com/javase/specs/jls/se8/html/jls-17.html)). A detailed step-by-step scenario is provided in [`docs/without-volatile.md`](docs/without-volatile.md).

This does not necessarily mean that instructions are literally reordered so that "the reference is written before the constructor runs." The absence of a guarantee about which writes from another thread the reader observes is sufficient, as described by the memory model ([JLS, Chapter 17](https://docs.oracle.com/javase/specs/jls/se8/html/jls-17.html)).

## Scope and limitations

- **Guarantee scope:** ordinary calls to `getInstance()` for a single loaded version of the class; preventing access to the private constructor through reflection, `Unsafe`, or other special mechanisms is outside the scope of these examples.
- **Initialization:** the example constructors do not throw exceptions, publish `this`, or call `getInstance()` recursively. If initialization code that can fail is added, a recovery strategy must be defined separately.
- **Constructor failures:** CAS and DCL can retry after exceptions; the CAS limit applies to successfully constructed candidates, not to an arbitrary number of failed attempts. A failure during `Holder` initialization puts the class into an erroneous state, and subsequent accesses do not retry normal initialization ([JLS, Chapter 12](https://docs.oracle.com/javase/specs/jls/se22/html/jls-12.html)).
- **Side effects:** for CAS, constructing a second candidate must be acceptable, and its resources must be independently releasable. `dispose()` must not throw exceptions or affect the winner's resources.
- **Mutable state:** safe publication does not automatically make arbitrary later field updates thread-safe; conflicting accesses need their own synchronization guarantees ([JLS, Chapter 17](https://docs.oracle.com/javase/specs/jls/se8/html/jls-17.html)).
