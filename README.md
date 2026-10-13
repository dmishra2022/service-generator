# Service Generator

Automate forking the Micronaut DDD + Hexagonal starter into a new bounded context without
hand-renaming anything. One command, every file, every reference, zero drift.

Replaces this error-prone recipe:

```sh
cp -R order-management-starter billing-management
cd billing-management
# then by hand:
#   rename every module directory order-* to billing-*
#   edit every pom.xml to swap order-management-parent and the five order-* artifact ids
#   search-and-replace com.enterprise.starter.order throughout Java sources
#   update <base.package> in the parent pom
#   update Packages.BASE in architecture-tests/.../Packages.java
```

with:

```sh
service-generator \
  --source /path/to/order-management-starter \
  --target /path/to/billing-management \
  --context billing
```

## What it does

Given a template with:

- parent `pom.xml` carrying `<artifactId>order-management-parent</artifactId>` and
  `<base.package>com.enterprise.starter.order</base.package>`
- module directories prefixed with the old context (`order-domain`, `order-application`, ...)
- Java sources rooted at `com.enterprise.starter.order`

the tool produces a byte-for-byte copy with:

| Step | What changes |
| --- | --- |
| 1 | Every module directory `order-*` becomes `${newContext}-*` |
| 2 | Every `pom.xml` has `order-management-parent` and the `order-*` module artifact ids rewritten |
| 3 | Every Java source file (and yaml, SQL, markdown) has `com.enterprise.starter.order` rewritten to `com.enterprise.starter.${newContext}` |
| 4 | The parent pom's `<base.package>` property is updated |
| 5 | `Packages.BASE` in `architecture-tests` is updated |

Non-context-prefixed modules (for example `architecture-tests`) are preserved verbatim.
Build outputs (`target/`, `build/`), IDE directories (`.idea/`, `.vscode/`), and VCS state
(`.git/`) are skipped during the copy.

## Build

```sh
mvn verify
```

Produces:

- `generator-cli/target/service-generator.jar` - the shaded CLI jar.
- `generator-web/target/generator-web-*.jar` - the Micronaut web application.

## CLI usage

```
service-generator --source <path> --target <path> --context <name> [options]
```

| Option | Meaning |
| --- | --- |
| `-s`, `--source` | Path to the template with a parent `pom.xml`. |
| `-t`, `--target` | Where to write the renamed project. |
| `-c`, `--context` | New bounded-context name (lowercase, `[a-z][a-z0-9_]*`, 2 to 32 chars, not a Java reserved word). |
| `--old-context`   | Context present in the template. Defaults to `order`. |
| `-f`, `--force` | Overwrite a non-empty target directory. |
| `-n`, `--dry-run` | Print the plan and the file list without writing. |
| `--plan` | Print just the renaming plan and exit. |
| `-v`, `--verbose` | Include every file touched in the output. |

Exit codes: `0` success, `2` validation failure, `3` invalid template, `4` generation failure, `1` unexpected.

### Example

```sh
java -jar generator-cli/target/service-generator.jar \
  --source /Users/you/Documents/ddd \
  --target /Users/you/Documents/billing-management \
  --context billing

# Dry run first
java -jar generator-cli/target/service-generator.jar \
  --source /Users/you/Documents/ddd \
  --target /Users/you/Documents/billing-management \
  --context billing --dry-run --verbose
```

## Web usage

```sh
java -jar generator-web/target/generator-web-0.1.0-SNAPSHOT.jar
# then open http://localhost:8080
```

Two endpoints:

| Verb | Path | Purpose |
| --- | --- | --- |
| POST | `/api/v1/preview` | Validate and scan; return the detected template shape and the computed renaming plan. Never writes. |
| POST | `/api/v1/generate` | Validate, scan, plan, and write a transformed copy. Returns the generation report. |

Both accept the same JSON body:

```json
{
  "source": "/path/to/order-management-starter",
  "target": "/path/to/billing-management",
  "newContext": "billing",
  "oldContext": "order",
  "force": false,
  "dryRun": false
}
```

Validation and scan failures return RFC 7807 `application/problem+json`:

```json
{
  "type": "about:blank",
  "title": "Validation failed",
  "status": 400,
  "detail": "newContext: must match [a-z][a-z0-9_]*",
  "errors": [
    { "field": "newContext", "message": "must match [a-z][a-z0-9_]*" }
  ]
}
```

## Library usage

Use `generator-core` directly from Java:

```java
GenerationRequest request = GenerationRequest.builder()
    .source(Path.of("/path/to/ddd"))
    .target(Path.of("/path/to/billing-management"))
    .newContext("billing")
    .build();                                      // throws ValidationException on bad input

GenerationReport report = new TemplateCopier().generate(request);
System.out.println(report.summary());
```

## Validation (no shortcuts)

Every input is checked server-side. Nothing destructive runs if any check fails:

- `source` must exist, be a readable directory, and contain `pom.xml`.
- `target` must not equal `source`, must not be nested inside it, must not contain it, must either
  be absent or empty (unless `--force`); its parent must be writable.
- `newContext` must match `[a-z][a-z0-9_]*`, be 2 to 32 chars, and must not be a Java reserved word,
  literal, restricted identifier (`record`, `sealed`, `yield`, ...), or a common `java.lang` type
  (`String`, `Object`, `Thread`, ...).
- `newContext` must differ from `oldContext`.
- The template's parent `pom.xml` must declare `<artifactId>` and `<base.package>`.
- Every `<module>` the parent declares must exist on disk.

All validation errors are collected and reported in one response.

## How it works

Three Maven modules:

- `generator-core`  Framework-free engine. Depends only on SLF4J. Houses the value objects, the
  validator, the template scanner, the plan factory, and the copier.
- `generator-cli`  Picocli adapter around the engine. Produces a shaded `service-generator.jar`.
- `generator-web`  Micronaut 4 adapter. Produces an HTTP front door with an inline HTML form.

The engine itself is split into four packages, each with one responsibility:

```
spec/   value objects and validation      -> GenerationRequest built via builder
scan/   inspect the template on disk      -> TemplateInspection
plan/   derive ordered rename rules       -> RenamingPlan
apply/  walk the tree, write the output   -> GenerationReport
```

82 tests exercise every layer. The end-to-end test generates a billing-starter from the real DDD
starter and runs `mvn verify` on the output.

## Known limits

- Only renames the bounded-context identifier (`order` -> `billing`). It does not rename aggregate
  class names (`Order`, `OrderController`, `OrderId`); the user does that for their own domain.
- Does not rewrite the Flyway migration file name (`V1__create_orders.sql`). Content inside the SQL
  file is left alone because `orders` is the SQL table name tied to the Order aggregate.
- Does not touch binary files (fonts, images, class files).

## Troubleshooting

| Message | Fix |
| --- | --- |
| `source: directory does not exist` | Pass an absolute path to a directory that contains `pom.xml`. |
| `source: does not look like a Maven project` | The directory has no `pom.xml`. |
| `target: already exists and is not empty` | Pass `--force` to overwrite, or pick a fresh directory. |
| `newContext: must match ...` | Lowercase ASCII only, start with a letter, no hyphens or dots. |
| `newContext: '<name>' is a Java reserved word` | Pick another name. |
| `Template looks invalid: parent pom is missing <base.package>` | The parent pom needs `<base.package>com....</base.package>` so the tool can detect the current context. |
