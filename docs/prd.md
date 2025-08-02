# PRD: IntelliJ Vela Plugin

## 1. Product overview

### 1.1 Document title and version

* PRD: IntelliJ Vela Plugin
* Version: 1.0.0 (Draft, July 18, 2025)

### 1.2 Product summary

The IntelliJ Vela Plugin provides developers and DevOps engineers with seamless access to the Vela CLI directly within their IntelliJ IDE. By integrating core Vela pipeline operations—execution, validation, preview, and build status—into the IDE, the plugin eliminates context switching and streamlines the CI/CD workflow.

The plugin exposes essential Vela CLI features through intuitive IDE actions and settings, following JetBrains plugin UX guidelines. It aims to deliver a secure, efficient, and delightful developer experience for teams using Vela for continuous integration and deployment.

## 2. Goals

### 2.1 Business goals

* Increase developer productivity by reducing context switching.
* Accelerate pipeline troubleshooting and time to resolution.
* Drive adoption of Vela CI/CD by improving developer experience.
* Reduce support burden by empowering users to self-serve pipeline issues.

### 2.2 User goals

* Run, validate, and preview Vela pipelines without leaving the IDE.
* View current build status for the active project.
* Configure Vela CLI path and authentication easily.
* Troubleshoot and iterate on pipelines quickly.

### 2.3 Non-goals

* Support for multiple Vela environments in a single session.
* Full CLI command coverage beyond execution, validation, preview, and build status.
* Remote telemetry or analytics collection.

## 3. User personas

### 3.1 Key user types

* Developers
* DevOps engineers

### 3.2 Basic persona details

* **Developer**: Writes and maintains code, frequently iterates on CI/CD pipelines, values rapid feedback and minimal friction.
* **DevOps engineer**: Manages build and deployment pipelines, supports teams in troubleshooting and optimizing CI/CD workflows.

### 3.3 Role-based access

* **User**: Can execute, validate, preview pipelines, and view build status.
* **Admin**: (Future) May configure organization-wide settings (not in scope for v1).

## 4. Functional requirements

* **Pipeline execution** (Priority: High)
  * Allow users to execute the Vela pipeline for the current project using the local CLI.
  * Expose CLI options/parameters via IDE dialogs.
* **Pipeline validation** (Priority: High)
  * Validate the current pipeline configuration and display results in the IDE.
* **Pipeline preview/compile** (Priority: High)
  * Show the compiled pipeline (including templates) before execution.
* **Build status viewer** (Priority: High)
  * Display current and recent build statuses for the active project.
* **Settings integration** (Priority: High)
  * Configure Vela CLI path and (optionally) authentication token via the native IntelliJ Settings dialog.
* **Security** (Priority: High)
  * Ensure all CLI executions are sandboxed and user-approved.
  * Allow secure storage of authentication tokens (if provided).
* **Logging** (Priority: Medium)
  * Provide local logging for troubleshooting plugin actions and errors.

## 5. User experience

### 5.1 Entry points & first-time user flow

* Plugin actions available via context menu, toolbar, and command palette.
* First launch prompts user to configure Vela CLI path if not detected.
* Settings accessible via IntelliJ Preferences/Settings.

### 5.2 Core experience

* **Run pipeline**: User triggers execution, selects options, views output in IDE console.
  * Ensures rapid feedback and minimal friction.
* **Validate pipeline**: User validates configuration, sees results inline.
  * Reduces errors before commit.
* **Preview pipeline**: User previews compiled pipeline, including templates.
  * Increases confidence in changes.
* **View build status**: User checks current/recent builds for the project.
  * Enables quick troubleshooting.

### 5.3 Advanced features & edge cases

* Handle missing/invalid CLI path gracefully.
* Warn if pipeline execution may affect local system.
* Support for projects with multiple pipeline files (prompt user to select).
* Display helpful error messages for CLI failures.

### 5.4 UI/UX highlights

* Follows JetBrains plugin UX guidelines.
* Uses native dialogs, notifications, and settings panels.
* Clear, actionable error and success messages.
* Consistent iconography and theming.

## 6. Narrative

A developer working on a project with Vela CI/CD can execute, validate, and preview their pipeline directly from IntelliJ, viewing build statuses and troubleshooting issues without leaving the IDE. This streamlined workflow reduces context switching, accelerates feedback, and improves the overall developer experience with Vela.

## 7. Success metrics

### 7.1 User-centric metrics

* Number of pipelines executed/validated via the plugin.
* Reduction in time to resolve pipeline issues.
* User satisfaction (via reviews/feedback).

### 7.2 Business metrics

* Plugin adoption rate (downloads/installs).
* Increase in Vela CI/CD usage among plugin users.

### 7.3 Technical metrics

* Plugin error rate (local logs).
* Average execution/validation time.
* Number of support requests related to plugin usage.

## 8. Technical considerations

### 8.1 Integration points

* Vela CLI (local binary)
* IntelliJ Platform Plugin APIs (actions, settings, notifications)

### 8.2 Data storage & privacy

* Store CLI path and (optionally) auth token securely using IDE mechanisms.
* No remote data collection or telemetry.
* Local logs only.

### 8.3 Scalability & performance

* Must not block IDE UI during CLI operations (use background tasks).
* Efficient handling of large pipeline files.

### 8.4 Potential challenges

* Handling CLI errors and edge cases robustly.
* Ensuring secure execution of potentially unsafe pipelines.
* Supporting various project structures and pipeline file locations.

## 9. Milestones & sequencing

### 9.1 Project estimate

* Size: Medium
* Estimate: 6-8 weeks for MVP

### 9.2 Team size & composition

* Team size: 2-3
* Roles: Kotlin/Java developer, QA, UX designer

### 9.3 Suggested phases

* **Phase 1**: Core CLI integration (execution, validation, preview) (2-3 weeks)
  * Deliverables: CLI actions, settings panel, basic error handling
* **Phase 2**: Build status viewer and advanced UX (2-3 weeks)
  * Deliverables: Build status UI, notifications, improved error messages
* **Phase 3**: Security, logging, and polish (2 weeks)
  * Deliverables: Secure token storage, local logging, UX refinements

## 10. User stories

### 10.1. Execute pipeline from IDE

* **ID**: GH-001
* **Description**: As a user, I want to execute the Vela pipeline for my project from within IntelliJ, so I can test changes quickly.
* **Acceptance criteria**:
  * User can trigger pipeline execution via IDE action.
  * CLI options/parameters are configurable in a dialog.
  * Visual pipeline step control.
  * Plugin autodetects the pipeline file based on project files.
  * User can override the detected pipeline file and specify another file if desired.
  * CLI argument for pipeline file is supported.
  * Output is displayed in the IDE console.
  * Build output is searchable within the IDE.
  * Errors are clearly reported.

### 10.2. Validate pipeline configuration

* **ID**: GH-002
* **Description**: As a user, I want to validate my pipeline configuration in the IDE, so I can catch errors before running builds.
* **Acceptance criteria**:
  * User can validate the pipeline via IDE action.
  * Validation results are shown inline or in a tool window.
  * Errors and warnings are clearly indicated.

### 10.3. Preview compiled pipeline

* **ID**: GH-003
* **Description**: As a user, I want to preview the compiled pipeline (including templates) before execution, so I can verify correctness.
* **Acceptance criteria**:
  * User can preview the compiled pipeline via IDE action.
  * Compiled YAML is displayed in a readable format.
  * Errors in compilation are reported.

### 10.4. View build status for current project

* **ID**: GH-004
* **Description**: As a user, I want to view the current and recent build statuses for my project in the IDE, so I can monitor progress and troubleshoot issues.
* **Acceptance criteria**:
  * Build status is accessible from the IDE.
  * Statuses are updated in near real-time.
  * Errors in fetching status are handled gracefully.

### 10.5. Configure Vela CLI path and authentication

* **ID**: GH-005
* **Description**: As a user, I want to configure the Vela CLI path and (optionally) provide an authentication token securely via the IDE settings.
* **Acceptance criteria**:
  * Settings panel allows user to set CLI path.
  * User can (optionally) provide an auth token, stored securely.
  * Plugin validates CLI path and token on save.

### 10.6. Secure execution and logging

* **ID**: GH-006
* **Description**: As a user, I want all CLI executions to be secure and logged locally, so I can trust the plugin and troubleshoot issues.
* **Acceptance criteria**:
  * All executions are sandboxed and user-approved.
  * Local logs are available for troubleshooting.
  * No data is sent externally.

## 11. Testing & quality assurance

### 11.1 Testing strategy

Following [JetBrains Plugin Testing Guidelines](https://plugins.jetbrains.com/docs/intellij/testing-plugins.html), the testing approach includes:

#### **Unit Testing**

* **Core logic tests**: CLI integration, settings, error handling, and pipeline parsing
* **Service layer tests**: `VelaCliService`, `PipelineService` with mocked dependencies
* **Model validation**: Pipeline data structures and environment variable handling
* **Utility functions**: File detection, path resolution, and security utilities

#### **Integration Testing**

* **End-to-end flows**: Pipeline execution, validation, preview, and build status retrieval
* **File system integration**: Pipeline file detection, watching, and auto-refresh
* **CLI process integration**: Command execution with real/mocked Vela CLI
* **Settings persistence**: Configuration storage and retrieval

#### **UI Testing**

* **Component tests**: Tool window panels, dialogs, and form interactions using IntelliJ test framework
* **Action tests**: Menu actions, toolbar buttons, and keyboard shortcuts
* **Console functionality**: Output display, search capabilities, and user interactions
* **Settings UI**: Configuration panels and validation behavior

#### **Platform Testing**

* **Light platform tests**: For services and non-UI components using `LightPlatformTestCase`
* **Heavy platform tests**: For full IDE integration using `HeavyPlatformTestCase`
* **Fixture-based tests**: Using `CodeInsightTestFixture` for file-based testing

#### **Mocking Strategy**

* **External dependencies**: Vela CLI process execution and file system operations
* **IntelliJ services**: Project services and platform APIs for isolated testing
* **Network calls**: Any future remote API integrations (build status, authentication)

### 11.2 Test framework integration

#### **IntelliJ Platform Test Framework**

* Leverage `com.intellij.testFramework.*` for platform-specific testing
* Use `PlatformTestUtil` for test data management and assertions
* Implement proper test lifecycle with setup/teardown for IDE state

#### **Test Data Organization**

* **Test fixtures**: Sample `.vela.yml` files for various pipeline configurations
* **Mock responses**: CLI output samples for success/error scenarios  
* **Configuration files**: Test project structures and settings

### 11.3 Quality assurance

#### **Automated Testing**

* All user stories must have clear, testable acceptance criteria
* Automated tests must pass before release (CI/CD integration)
* Code coverage targets: >80% for core logic, >60% for UI components
* Performance benchmarks for CLI execution and file parsing

#### **Manual Testing**

* **Cross-platform validation**: Windows, macOS, and Linux compatibility
* **IDE version compatibility**: Target IntelliJ versions (2024.2+)
* **Edge case scenarios**: Large pipeline files, network failures, CLI errors
* **User workflow testing**: Complete user journeys for each persona

#### **Security Testing**

* **Credential handling**: Secure storage and retrieval of authentication tokens
* **CLI execution**: Sandboxing and validation of user-provided commands
* **File access**: Proper permissions and path validation

#### **Regression Testing**

* Automated regression suite for each release cycle
* Compatibility testing with IntelliJ Platform updates
* Performance regression monitoring for large projects

#### **Documentation Testing**

* Validate all code examples and procedures in user documentation
* Test installation and setup instructions across platforms
* Verify troubleshooting guides with real scenarios

### 11.4 Testing resources

* **JetBrains Documentation**: [Testing IntelliJ Platform Plugins](https://plugins.jetbrains.com/docs/intellij/testing-plugins.html)
* **Test Framework Reference**: [IntelliJ Platform Test Framework](https://plugins.jetbrains.com/docs/intellij/testing-faq.html)
* **CI/CD Integration**: GitHub Actions for automated testing and validation
