# AGENTS

This document provides build, test and lint commands plus code style guidelines
for automated agents working in this repository.
It covers Maven workflows, static analysis, CI, formatting, imports, naming, types,
error handling, Javadoc, UI conventions, testing patterns and best practices.

## Build & Test Commands
### Prerequisites
- Java 21 or later (CI uses JDK 21)
- Apache Maven 3.6+

### Full Build & Install
- `mvn clean install`                    Build, run tests, install to local repo

### Package & Assembly
- `mvn clean package assembly:single`    Create self-contained JAR in target/

### Compile Only
- `mvn clean compile`                    Clean and compile sources without tests

### Run Locally
- `java -jar target/fribok-<version>-jar-with-dependencies.jar`

## Lint & Static Analysis
- `mvn checkstyle:check`                  Enforce Checkstyle rules
- `mvn spotbugs:check`                    Run SpotBugs analysis
- JDepend report is generated via `mvn site -DskipTests`
- `mvn javadoc:javadoc`                   Generate Javadoc in target/site
- `mvn site -DskipTests`                  Generate project site and reports

## Running a Single Test
- `mvn test -Dtest=MyTest`                Run specific test class
- `mvn test -Dtest=MyTest#methodName`     Run single test method
- `mvn test` runs both Surefire executions: unit tests (`excludedGroups=integration`) and `@Tag("integration")` tests in a separate forked JVM
- Uses JUnit 5 syntax; use fully qualified class names if needed
- Integration tests use `@Tag("integration")` and the shared fixtures in `src/test/java/se/swedsoft/bookkeeping/testsupport/system/`

## Continuous Integration
No CI workflow is currently committed under `.github/workflows/` in this repository snapshot.
- Use local pre-PR gate: `mvn clean install`
- For release packaging checks, run: `mvn clean package -Djpackage.profile=true`

## Documentation Generation
- `mvn javadoc:javadoc`                   Generate API docs in target/site/apidocs
- `mvn site`                              Build full project site including reports

## Testing Guidelines
### Test Structure
- Place unit tests in `src/test/java` mirroring production packages
- Store test data under `src/test/resources` for integration tests
- Schema-V2 repository/system tests set `fribok.schema.version=v2` before `SSSystemConfigContext.startupLocal(...)` and clear it in teardown; see `src/test/java/se/swedsoft/bookkeeping/persistence/SSAccountPlanV2RepositoryTest.java` and `src/test/java/se/swedsoft/bookkeeping/testsupport/system/SSV2DatabaseFixture.java`

### Test Assertions
- Use JUnit 5 assertions or AssertJ for clarity
- Keep tests focused: one logical assertion per test
- Name tests with descriptive verbs and expected outcome

### Coverage
- Aim for high coverage on core modules
- Add tests for new features and bug fixes

## Pull Request Checks
- Verify `mvn clean install` passes locally
- Ensure new code follows style guidelines and includes Javadoc
- Run static analysis and resolve warnings
- Update documentation and version metadata as needed
- Update `CHANGELOG.md` with the changes introduced by the PR

## Dependency Management
- Define dependencies in `pom.xml` with explicit versions
- Avoid unwanted transitive dependencies
- Inspect classpath via `mvn dependency:tree`
- Update dependencies via Maven Properties pattern

## IDE Integration
- For IntelliJ `.idea` projects, commit only shared project settings and exclude user-local IDE files
- Use Maven tool window for lifecycle, imports and code completion
- Enable auto-import for Maven changes
- Generate Eclipse config via `mvn eclipse:eclipse` if needed

## Additional Tools
- `mvn dependency:tree`                   Inspect dependency graph
- `mvn versions:display-dependency-updates` Report available updates
- `mvn exec:java -Dexec.mainClass=org.fribok.bookkeeping.Bookkeeping`
Launch main application via Maven

## Code Style Guidelines
### Formatting
- Indent with 4 spaces; never use tabs
- Left brace on same line (`if (cond) { ... }`)
- Line length max 120 characters; wrap before limit
- Separate methods and fields with a blank line
- No trailing whitespace

### Imports
- Avoid wildcard imports (`*`)
- Order groups: static, `java.*`, `javax.*`, `org.*`, third-party, project
- Separate groups with a single blank line
- Alphabetize within each group

### Naming Conventions
- Package names: lower case with dots (`se.swedsoft.bookkeeping`)
- Class/interface names: UpperCamelCase
- Method/variable names: lowerCamelCase
- Constants (static final): UPPER_SNAKE_CASE
- Test classes/methods end with `Test`; descriptive names

### Types & Generics
- Always use generics; avoid raw types
- Use diamond operator (`new ArrayList<>()`)
- Favor immutable collections for new code
- Return nullable values via `Optional<T>` instead of null

### Javadoc
- Public classes and methods require Javadoc comments
- Include `@param`, `@return`, `@throws` where applicable
- Keep descriptions concise; focus on contract and side effects

### UI & Resource Bundles
- Run Swing GUI updates on the Event Dispatch Thread
- Load UI strings via `SSBundle.getBundle`
- Externalize user-facing literals in resource bundles

### Error Handling
- Validate arguments with `SSUtil.verifyArgument` or `verifyNotNull`
- Throw `IllegalArgumentException` or `NullPointerException` early
- Use `SSException` for application-level errors with bundle messages
- Catch only specific exceptions; avoid broad `catch (Exception)`
- Document thrown exceptions in Javadoc
- Wrap `IOException` in runtime `SSException` when context requires

## Agent Best Practices
- Use `apply_patch` for file edits; prefer Read/Glob/Grep for file operations
- Use the project's standard shell for git and build commands (PowerShell on Windows, Bash on Linux/macOS), and avoid destructive or irreversible operations
- Parallelize independent tool calls; chain dependent commands sequentially
- Default to ASCII and follow existing conventions; ask questions only when blocked
