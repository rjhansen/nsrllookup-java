# nsrllookup-java

_This is not yet ready for end users._

## Usage

0. You will need Java 11 or later.
1. Clone the repo.  You may need to adjust the `pom.xml` file to comment
   out the block related to signing the jar.
2. `mvn package` builds two jarfiles: one has `log4j2` included in it,
   the other doesn't. If you already use `log4j2` in your project use
   the lightweight jar; otherwise use the heavyweight one and save 
   yourself a dependency.

## Code sample

```
var known = Arrays.stream(new String[]{
    "000000A603173B73A381F93C70AF9330",
    "000000A603173B73A381F93C70AF9330",
    "000000E2E580978FA6E621B7842D8FBF",
    "000000E2E580978FA6E621B7842D8FBF",
    "000001A29434521CF0C4D4FC143E8792",
    "DECAFBADDECAFBADDECADBADDECAFBAD",
    "DECAFBADDECAFBADDECADBADDECAFBAD",
    "DEADBEEFDEADBEEFDEADBEEFDEADBEEF",
    "DEADBEEFDEADBEEFDEADBEEFDEADBEEF",
    "BADD00D5BADD00D5BADD00D5BADD00D5"
});

var conn = new Connection("nsrllookup.com", 9120);
var stream = conn.hits(known);
stream.forEach(System.out::println);
```

### Code notes
`.hits()` and `.misses()` both take a `java.nio.Stream<String>` as a 
parameter.  The stream is fully read and is garbage-safe: the code 
validates its inputs correctly.  Once all hashes are read the valid
inputs are sorted, deduplicated, and only then does it start a 
connection to the `nsrlsvr`.

These methods return a `Stream<String>` of hashes that resulted in `NSRL
RDS` hits or misses, respectively.  The stream is ordered and
duplicate-free.
