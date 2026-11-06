# Java 25 parity test report: cpp-context-subscriptions

| Copy | Branch | From | JDK | Hibernate (unit tests) |
|---|---|---|---|---|
| Java 25 | `dev/25.104-chd-j25-j17-parity` | `dev/25.104-chd-j25` at `bbaa002b` (PR #56) | 25.0.5 | 6.6.1.Final (persistence also run on 7.3.2.Final) |
| Java 17 | `dev/CHD-2878-chd-parity-with-j25` (never merged) | `origin/main` at `cd839c7c` | 17 | 5.4.24.Final with DeltaSpike Data 1.9.6, H2 1.4.196 |

Risks come from the Stage 2 review (CHD-01..CHD-14) and the BC catalogue on
[Parity Testing Java17 → Java25](https://hmcts.atlassian.net/wiki/spaces/CTP/pages/279752455).

Every parity test asserts the **Java 17 behaviour**, so a Java 25 failure is a behaviour change to triage in Stage 5.
No production code was changed. One expectation (CHD-05) was corrected in both copies after the Java 17 run; see "Java 17 results".

**The JVM default zone is UTC, so other zones aren't a scenario.** The framework's `EntityManagerProducer.create()`
calls `TimeZone.setDefault(UTC)` (persistence-deltaspike 17.x and persistence-jpa 25.104.x), and the containers run in
UTC. This came out of the cpp-context-results parity review (PR #228), where the time zone tests were removed. The
tests here therefore use UTC input only. See "Removed parity tests".

## Results

| ID | Severity | What could change | Test(s) | Java 17 result | Java 25 result | Suggested fix |
|---|---|---|---|---|---|---|
| CHD-01 (BC-04) | Medium | `active` is a primitive `boolean` on a nullable column; a NULL row must still throw on load | `SubscriptionsRepositoryTest.chd01_loadingASubscriptionWithANullActiveColumnThrows`, `SubscriberRepositoryTest.chd01_loadingASubscriberWithANullActiveColumnThrows` | ✅ Pass | ✅ Pass (throws `PersistenceException`) on Hibernate 6.6.1 and 7.3.2 | None for parity. Optional hardening on both lines: NOT NULL + default in Liquibase |
| CHD-02 (BC-20) | Medium | Drools 10 could load 0 rules, so deny tests pass vacuously | `SubscriptionsCommandRuleExecutorTest.chd02_commandApiKieBaseLoadsEveryRuleInTheDrl` (9), `SubscriptionsQueryRuleExecutorTest.chd02_queryApiKieBaseLoadsEveryRuleInTheDrl` (3) | ✅ Pass | ✅ Pass | None. Keep the guards |
| CHD-03 (BC-08) | Low | A UTC `sentTime` from the command must reach the stored event unchanged | `NotificationAggregateTest.chd03_utcSentTimeFromTheCommandIsWrittenToTheStoredEventUnchanged` | ✅ Pass | ✅ Pass (`2026-07-04T05:06:07.890Z` in, same string out) | None. The in-memory zone (`Z` vs `Z[UTC]`) is only visible through `toString()`/`equals()`, and nothing reads `sentTime` back from the event |
| CHD-04 | Low | `save()` always `merge()`s: create, update and redelivery of the same id | `SubscriptionsRepositoryTest.chd04_saveWithAnExistingIdUpdatesTheRowAndRepeatingTheSaveIsIdempotent` | ✅ Pass | ✅ Pass | None |
| CHD-05 | Low | `remove(null)` for an unknown subscription id | `SubscriptionsRepositoryTest.chd05_removeOfANullSubscriptionThrowsWithIllegalArgumentExceptionAsTheRootCause` | ✅ Pass (`QueryInvocationException` wrapping `IllegalArgumentException`) | ✅ Pass (`IllegalArgumentException` directly) | None |
| CHD-06 (BC-01/02) | Low | `findSubscriberByEmailAddressForSubscription` no-match / multi-match | `SubscriberRepositoryTest.chd06_findSubscriberByEmailAddressForSubscriptionThrowsNoResultExceptionWhenNothingMatches` | ✅ Pass | ❌ **Fail**: nothing thrown (returns `Optional.empty()`) | No production caller, so **accept** and document; or restore `Subscriber` + `getSingleResult()` if the team wants strict parity |
|  |  |  | `SubscriberRepositoryTest.chd06_findSubscriberByEmailAddressForSubscriptionThrowsNonUniqueResultExceptionWhenSeveralMatch` | ✅ Pass | ❌ **Fail**: nothing thrown (`findFirst()` picks one) | Same as above |
| CHD-07 (BC-24) | Low | Rewritten finders: empty result and court filtering | `SubscriptionsRepositoryTest.chd07_findersReturnAnEmptyListWhenNothingMatches`, `SubscriptionsRepositoryTest.chd07_findByCourtIdReturnsOnlyTheSubscriptionsForThatCourt` | ✅ Pass | ✅ Pass | None. pgjdbc UUID binding is only covered by the ITs (H2 here) |
| CHD-08 (BC-15) | Medium | core-domain 25.104.6 models dropping fields from a real `hearing.resulted` payload | `NotificationEventRuleExecutorTest.chd08_hearingResultedPayloadKeepsEveryFieldThroughTheCoreDomainModels`, `NotificationEventRuleExecutorTest.chd08_hearingResultedWithDefendantInitiationCodeKeepsEveryFieldThroughTheCoreDomainModels` | ✅ Pass | ✅ Pass (no field lost, LENIENT compare) | None |
| CHD-09 (BC-13) | Low–Medium | everit date-time strictness on inbound `notification-sent.sentTime` | `NotificationEventProcessorTest.chd09_*` (3 accept: no fraction, millis, micros; 2 reject: no zone, date only) | ✅ Pass | ✅ Pass (all 5) | None |
| CHD-10 | Low | `hearing.version` 17.104.161 → 17.104.200 (RAML only) | Not tested (build input, not behaviour) | — | — | Use the same `hearing.version` in the Java 17 copy |
| CHD-11 (BC-07) | — | Liquibase 5 rejects `hub.mode` | Not tested (config, already fixed) | — | — | Fixed in PR #56 |
| CHD-12 | Low | anonymise module deleted | Not tested (build, not runtime) | — | — | Confirm with team |
| CHD-13 (BC-06) | Low | Lazy back-references loaded outside a transaction | Not tested (the query view only reads EAGER collections) | — | — | None |
| CHD-14 (BC-12/16/17/21) | Low | Framework tier | Not unit-testable here; command/query REST covered by the 35 ITs (green) | — | — | Check on live run |

## Failing tests (to triage in Stage 5)

1. `SubscriberRepositoryTest.chd06_findSubscriberByEmailAddressForSubscriptionThrowsNoResultExceptionWhenNothingMatches`
2. `SubscriberRepositoryTest.chd06_findSubscriberByEmailAddressForSubscriptionThrowsNonUniqueResultExceptionWhenSeveralMatch`

## Java 25 unit totals (JDK 25.0.5, `mvn clean install`)

| | Run | Failures | Errors | Skipped |
|---|---|---|---|---|
| Before parity tests | 208 | 0 | 0 | 0 |
| With parity tests | 226 | 2 | 0 | 0 |

18 parity tests: 16 pass, 2 fail.

## Removed parity tests

Removed because they relied on a zone other than UTC, which doesn't happen at runtime:

| ID | Removed | Why |
|---|---|---|
| CHD-03 | `chd03_sentTimeIsWrittenToTheStoredEventAsUtcWithMillisecondsAndZ` (Europe/London input) | The command's `sentTime` is parsed as UTC; replaced by the UTC round-trip test above |
| CHD-03 | `chd03_sentTimeReadBackFromTheStoredEventIsInTheUtcRegionZone` (was failing) | Compared `Z[UTC]` with `Z`, i.e. two zone IDs for the same instant. The stored string is identical on both stacks, and no listener or query reads `sentTime` back, so the difference is never observed |
| CHD-09 | `chd09_notificationSentSchemaAcceptsSentTimeWithAnOffset` (`+01:00`) | notificationnotify sends `sentTime` in UTC |

### Persistence tests on both Hibernate versions

`subscriptions-viewstore-persistence` (23 tests) gives identical results on Hibernate 6.6.1.Final (BOM default) and
7.3.2.Final (WildFly 40 runtime, via a temporary `dependencyManagement` override, not committed): 21 pass, and the
same two CHD-06 tests fail.

The 7.3.2 run also needed the six entity classes listed in the test `persistence.xml`, because Hibernate 7 doesn't
find entities through `<jar-file>`. That was part of the temporary override and isn't kept: the build runs on 6.6.1,
which passes with the unchanged `persistence.xml`. Production entity scanning is done by WildFly.

## Java 17 results

Java 17 copy on `dev/CHD-2878-chd-parity-with-j25` (git worktree `../cpp-context-subscriptions-j17`), JDK 17,
`mvn clean install`:

| | Run | Failures | Errors | Skipped |
|---|---|---|---|---|
| With parity tests | 220 | 0 | 0 | 0 |

All 18 parity tests pass, and the 18 method names are identical in both copies. Every parity test compiled on Java 17.

Harness changes only; method bodies are unchanged:
- `NotificationEventProcessorTest`: `javax.json.JsonObject` instead of `jakarta.json.JsonObject` (already different
  on `main`).
- `SubscriptionsRepositoryTest`, `SubscriberRepositoryTest`: keep `main`'s DeltaSpike `CdiTestRunner` +
  `BaseTransactionalJunit4Test` harness. The helpers use an `@Inject EntityManager` instead of
  `HibernateTestEntityManagerProvider`, and the imports are `javax.persistence`. `assertThrows` still comes from
  `junit-jupiter-api`, which is on the Java 17 test classpath.
- The other four classes are identical to the Java 25 versions.

Library versions on Java 17: Jackson 2.12.7, everit 1.6.0, org.json 20231013.

Expectations confirmed by the run:
- **CHD-06:** DeltaSpike's `getSingleResult()` throws `NoResultException` and `NonUniqueResultException`, so the two
  Java 25 failures are real behaviour changes.
- **CHD-01:** a NULL `active` throws `PersistenceException` on Hibernate 5 as well.
- **CHD-09:** everit 1.6.0 accepts microseconds, as on Java 25.

Expectation corrected:
- **CHD-05:** the first version expected `IllegalArgumentException` from `remove(null)`. On Java 17 it failed: the
  exception is `org.apache.deltaspike.data.api.QueryInvocationException`, wrapping an `InvocationTargetException`,
  wrapping `IllegalArgumentException: attempt to create delete event with null entity`. On Java 25 the
  `IllegalArgumentException` is thrown directly. Both stacks throw, so the `subscription-deleted` listener fails for an
  unknown id either way, and nothing catches either type. The test in both copies now asserts a `RuntimeException`
  whose root cause is `IllegalArgumentException`, renamed
  `chd05_removeOfANullSubscriptionThrowsWithIllegalArgumentExceptionAsTheRootCause`. It passes on both JDKs. The
  persistence module was re-run on JDK 25 (Hibernate 6.6.1) after the change; the 7.3.2 comparison above was run
  before it.
