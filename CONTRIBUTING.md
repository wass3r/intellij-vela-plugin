# Contributing to intellij-vela-plugin

Thank you for your interest in contributing! This document outlines the guidelines and best practices for contributing to this project.

## Table of Contents
- [Contributing to intellij-vela-plugin](#contributing-to-intellij-vela-plugin)
  - [Table of Contents](#table-of-contents)
  - [Code of Conduct](#code-of-conduct)
  - [How to Contribute](#how-to-contribute)
  - [Commit Style](#commit-style)
    - [Format](#format)
      - [Types](#types)
      - [Examples](#examples)
      - [Scope (Optional)](#scope-optional)
      - [Description](#description)
      - [Body (Optional)](#body-optional)
      - [Footer (Optional)](#footer-optional)
  - [Code Style](#code-style)
  - [Pull Requests](#pull-requests)
  - [Product Requirements](#product-requirements)
  - [Review and Testing](#review-and-testing)

---

## Code of Conduct

Please be respectful and constructive in all interactions. Follow the [JetBrains Code of Conduct](https://www.jetbrains.com/legal/code-of-conduct.html) and treat all contributors and users with respect.

## How to Contribute

- Fork the repository and create your branch from `main` or the appropriate feature branch.
- Make your changes, following the guidelines below.
- Submit a pull request (PR) with a clear description of your changes and reference any related issues.

## Commit Style

This project uses the [Conventional Commits](https://www.conventionalcommits.org/) specification for all commit messages. This helps keep the commit history readable, automates changelog generation, and improves collaboration.

### Format
```
<type>[optional scope]: <description>

[optional body]
[optional footer(s)]
```

#### Types
- **feat**: A new feature
- **fix**: A bug fix
- **docs**: Documentation only changes
- **style**: Changes that do not affect the meaning of the code (white-space, formatting, missing semi-colons, etc)
- **refactor**: A code change that neither fixes a bug nor adds a feature
- **perf**: A code change that improves performance
- **test**: Adding missing tests or correcting existing tests
- **build**: Changes that affect the build system or external dependencies
- **ci**: Changes to CI configuration files and scripts
- **chore**: Other changes that don't modify src or test files
- **revert**: Reverts a previous commit

#### Examples
```
feat(ui): add pipeline file auto-detection panel
fix: correct file chooser filter for .yaml files
docs: update README with plugin disclaimer
refactor: extract constants for UI spacing
chore: update gradle wrapper version
```

#### Scope (Optional)
The scope can be anything specifying the place of the commit change. For example, `ui`, `core`, `docs`, `build`, etc.

#### Description
Use the imperative mood (e.g., "add", not "added" or "adds"). Keep it concise and clear.

#### Body (Optional)
Provide additional contextual information about the change.

#### Footer (Optional)
Reference issues, breaking changes, or other important notes.

---

## Code Style

- Follow idiomatic Kotlin and JetBrains [IntelliJ Platform Plugin guidelines](https://plugins.jetbrains.com/docs/intellij/welcome.html).
- Use JetBrains UI components (`JBPanel`, `JBLabel`, etc.) and `JBUI` for theming.
- Avoid magic numbers; use constants and helper functions.
- Write comments that explain "why" something is done, not "what" (unless non-obvious).
- Keep code and documentation consistent and up-to-date.

## Pull Requests

- Ensure your branch is up to date with the target branch before submitting a PR.
- Provide a clear description of your changes and reference any related issues or PRD items.
- Run tests and verify your changes in the IDE before submitting.
- Address any review feedback promptly.

## Product Requirements

- Always consult [`docs/prd.md`](docs/prd.md) for product requirements, feature priorities, and project goals.
- Use the PRD as a guiding document to focus development activity and ensure alignment with the intended functionality and user experience.

## Review and Testing

- Manually review code for adherence to these guidelines, especially after AI-assisted changes.
- Test UI changes in the IDE to ensure correct alignment, spacing, and behavior.
- Ensure all configuration, icons, and descriptions meet JetBrains Marketplace requirements before publishing.
