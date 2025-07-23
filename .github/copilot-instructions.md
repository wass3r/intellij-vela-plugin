# Copilot Instructions for This Project

## Purpose

This project is an unofficial IntelliJ plugin for Vela CI/CD, developed primarily with AI assistance. To ensure maintainability, clarity, and professionalism, all future AI-generated code and suggestions must adhere to the following guidelines.

## Product Requirements

- **Reference the PRD:**  
  Always consult [`docs/prd.md`](../docs/prd.md) for product requirements, feature priorities, and project goals.  
  Use the PRD as a guiding document to focus development activity and ensure alignment with the intended functionality and user experience.

## General Principles

- **Follow Official Guidelines:**  
  Always consult and adhere to the latest [IntelliJ Platform Plugin documentation](https://plugins.jetbrains.com/docs/intellij/welcome.html) for UI components, plugin configuration, and architecture.
- **Kotlin Best Practices:**  
  Use idiomatic Kotlin, favor concise and expressive code, and leverage language features appropriately.
- **Consistency:**  
  Maintain consistency with the existing codebase in terms of structure, naming, and design patterns.

## UI Development

- **Use JetBrains UI Components:**  
  Prefer `JBPanel`, `JBLabel`, `JBScrollPane`, and other JetBrains-specific Swing components for UI.
- **Theming and Styling:**  
  Use `JBUI` for borders, padding, and colors to ensure proper theming and platform consistency.
- **Icons:**  
  Follow [plugin icon guidelines](https://plugins.jetbrains.com/docs/intellij/plugin-icon-file.html) for naming, sizing, and placement.
- **Layout:**  
  Avoid unnecessary complexity in layouts. Use `BoxLayout`, `BorderLayout`, or recommended layouts as appropriate for tool windows.
- **Kotlin UI DSL:**  
  Only use Kotlin UI DSL for dialogs and settings forms, not for tool windows, as per JetBrains recommendations.

## Code Quality

- **Comments:**  
  Write comments that explain "why" something is done, not "what" is done (unless the "what" is non-obvious). Remove or avoid boilerplate comments.
- **Redundancy:**  
  Avoid duplicate code, magic numbers, and unnecessary complexity. Use constants and helper functions where appropriate.
- **Configuration:**  
  Ensure `plugin.xml` and related configuration files follow official standards and are free of repetition and ambiguity.
- **Resource Bundles:**  
  Use resource bundles for localization only when needed. Avoid referencing unused bundles.

## Documentation

- **README and Plugin Description:**  
  Keep the README and plugin description in sync. Clearly state the unofficial status and experimental nature of the plugin.
- **Disclaimers:**  
  Prominently display that this is an unofficial, community-maintained plugin to avoid confusion with any future official Vela plugin.

## Review and Testing

- **Manual Review:**  
  Regularly review code for adherence to these guidelines, especially after AI-assisted changes.
- **Visual Inspection:**  
  Test UI changes in the IDE to ensure correct alignment, spacing, and behavior.
- **Marketplace Readiness:**  
  Ensure all configuration, icons, and descriptions meet JetBrains Marketplace requirements before publishing.

## Commit Style

- **Follow Conventional Commits:**  
  All commit messages must follow the [Conventional Commits](https://www.conventionalcommits.org/) specification.  
  See [`CONTRIBUTING.md`](../CONTRIBUTING.md) for details and examples.