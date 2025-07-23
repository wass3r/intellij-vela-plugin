# Vela CI/CD Integration (Unofficial)

![Build](https://github.com/wass3r/intellij-vela-plugin/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/MARKETPLACE_ID.svg)](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/MARKETPLACE_ID.svg)](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID)

## Template ToDo list

- [x] Create a new [IntelliJ Platform Plugin Template][template] project.
- [x] Get familiar with the [template documentation][template].
- [x] Adjust the [pluginGroup](./gradle.properties) and [pluginName](./gradle.properties), as well as the [id](./src/main/resources/META-INF/plugin.xml) and [sources package](./src/main/kotlin).
- [x] Adjust the plugin description in `README` (see [Tips][docs:plugin-description])
- [ ] Review the [Legal Agreements](https://plugins.jetbrains.com/docs/marketplace/legal-agreements.html?from=IJPluginTemplate).
- [ ] [Publish a plugin manually](https://plugins.jetbrains.com/docs/intellij/publishing-plugin.html?from=IJPluginTemplate) for the first time.
- [ ] Set the `MARKETPLACE_ID` in the above README badges. You can obtain it once the plugin is published to JetBrains Marketplace.
- [ ] Set the [Plugin Signing](https://plugins.jetbrains.com/docs/intellij/plugin-signing.html?from=IJPluginTemplate) related [secrets](https://github.com/JetBrains/intellij-platform-plugin-template#environment-variables).
- [ ] Set the [Deployment Token](https://plugins.jetbrains.com/docs/marketplace/plugin-upload.html?from=IJPluginTemplate).
- [ ] Click the <kbd>Watch</kbd> button on the top of the [IntelliJ Platform Plugin Template][template] to be notified about releases containing new features and fixes.

<!-- Plugin description -->
# 🔧 Unofficial Vela CI/CD Plugin

**⚠️ UNOFFICIAL & EXPERIMENTAL** - This community-developed plugin integrates [Vela CI/CD](https://go-vela.github.io/docs/) pipelines into your IntelliJ IDE. It is not endorsed or supported by the official Vela project. It is developed primarily with AI assistance, and aims to provide a convenient way to manage and execute Vela pipelines directly from your development environment.

## Features

- **Pipeline Execution**: Run and validate `.vela.yml`/`.vela.yaml` files locally
- **Visual Pipeline Explorer**: Collapsible sidebar showing pipeline structure
- **Environment Management**: Secure storage and configuration of environment variables
- **Real-time Console**: Live output from Vela CLI execution
- **Auto-Detection**: Automatically discovers Vela pipeline files in your project

## Requirements

- Vela CLI installed on your system
- Access to a Vela server (optional for local execution)

## ⚠️ Important Notes

- **Unofficial**: Independent project, not associated with official Vela
- **Experimental**: Test thoroughly before production use
- **Community Support**: Updates depend on community contributions
- **Future Compatibility**: May conflict with future official plugins

## Getting Started

1. Install from JetBrains Marketplace
2. Open a project with `.vela.yml` files
3. Configure Vela CLI settings in the plugin sidebar
4. Start executing and testing your pipelines locally
<!-- Plugin description end -->

## Installation

- Using the IDE built-in plugin system:
  
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "Vela"</kbd> >
  <kbd>Install</kbd>
  
- Using JetBrains Marketplace:

  Go to [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID) and install it by clicking the <kbd>Install to ...</kbd> button in case your IDE is running.

  You can also download the [latest release](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID/versions) from JetBrains Marketplace and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

- Manually:

  Download the [latest release](https://github.com/wass3r/intellij-vela-plugin/releases/latest) and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

---
Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
[docs:plugin-description]: https://plugins.jetbrains.com/docs/intellij/plugin-user-experience.html#plugin-description-and-presentation
