# Requirements Document

## Introduction

Android Dev Toolkit 2.0 is a complete rewrite of FloatAI into an Android software development IDE with C++ editor homepage, GitHub integration, remote project download, and AI-assisted development. The system removes legacy AI chat features and rebuilds UI architecture around project-centric workflow with drawer navigation.

## Glossary

- **System**: Android Dev Toolkit 2.0 application
- **Project Home**: Drawer first entry, project list and creation hub
- **Three Balls**: Onboarding UI with three circular steps for project creation
- **ATt Dependencies**: Android SDK, NDK, CMake, Gradle toolchain components
- **Open Source Account**: GitHub OAuth account for project sync
- **AI Service Provider**: OpenAI compatible API provider for code assistance

## Requirements

### Requirement 1 — Complete Rewrite and Architecture

**User Story:** AS developer, I want a clean 2.0 rewrite, so that legacy code and AI chat features are removed and C++ editor is primary.

#### Acceptance Criteria

1. WHEN the system starts, THE system SHALL load a new codebase with no legacy FloatAI AI chat screens
2. THE System SHALL use Android Studio style UI architecture with drawer navigation
3. WHILE the system is running, THE system SHALL present C++ editor as home screen

### Requirement 2 — First Launch Protocol and Dependency Installer

**User Story:** AS new user, I want clear onboarding, so that usage agreement is accepted before use.

#### Acceptance Criteria

1. WHEN user first launches, THE system SHALL display usage protocol screen with agreement checkboxes
2. IF user accepts protocol, THEN the system SHALL start one-click ATt dependency download wizard
3. WHILE dependencies are downloading, THE system SHALL show progress and allow cancel
4. WHEN dependencies are installed, THEN the system SHALL navigate to home page

### Requirement 3 — Home Page with Drawer Navigation

**User Story:** AS developer, I want three navigation items, so that I can access projects and settings quickly.

#### Acceptance Criteria

1. THE home page SHALL show a hamburger drawer with three items: 项目, 设置
2. THE top bar SHALL retain three-line menu icon
3. WHEN user opens drawer, THE system SHALL show 项目主页 as first item
4. WHEN user opens drawer, THE system SHALL show 设置 as second item containing update history and settings

### Requirement 4 — Project Creation Three Balls UI

**User Story:** AS developer, I want guided project creation, so that folder, service provider and dependencies are configured.

#### Acceptance Criteria

1. THE Project Home SHALL display three circular steps: 选择创建文件夹, 配置自定义服务商, 检查下载依赖
2. WHEN user selects first ball, THE system SHALL allow choosing creation folder on device
3. WHEN user selects second ball, THE system SHALL allow configuring AI service provider API endpoint and key
4. WHEN user selects third ball, THE system SHALL verify ATt dependencies existence

### Requirement 5 — Android Project Initialization

**User Story:** AS developer, I want to create Android projects inside app, so that files are initialized automatically.

#### Acceptance Criteria

1. WHEN user creates project, THE system SHALL allow selecting Android API version
2. WHEN user creates project, THE system SHALL allow selecting code language: Kotlin/Java/C++/Native
3. AFTER selection, THE system SHALL initialize project files in chosen folder
4. THE Project Home SHALL list created projects with open/build/delete actions

### Requirement 6 — Project Editor Workspace

**User Story:** AS developer, I want an editor with search and AI assistance, so that code can be edited efficiently.

#### Acceptance Criteria

1. WHEN project is opened, THE system SHALL show file tree and editor with C++ support
2. THE editor toolbar SHALL provide four actions: 文件, 搜索项目代码, AI 辅助, 同步开源地址
3. WHEN user clicks 搜索项目代码, THE system SHALL search code across project files
4. WHEN user clicks AI 辅助, THE system SHALL allow copying code and asking questions using configured provider
5. WHEN user clicks 同步开源地址, THE system SHALL push code to GitHub and pull remote changes

### Requirement 7 — GitHub Login and Sync

**User Story:** AS developer, I want GitHub integration, so that projects can be remote downloaded and synced.

#### Acceptance Criteria

1. THE Settings SHALL provide GitHub login using OAuth
2. WHEN logged in, THE system SHALL allow remote download of projects
3. WHEN project is edited, THE system SHALL allow one-click sync to GitHub repository
4. THE system SHALL support placing GitHub token in settings for access

### Requirement 8 — Build and Publish

**User Story:** AS developer, I want to build and publish, so that APK can be tested or released.

#### Acceptance Criteria

1. THE editor top bar SHALL show a build button on right side
2. WHEN user clicks build, THE system SHALL choose test install or sync and publish version
3. WHEN publishing, THE system SHALL allow setting release version number
4. AFTER build completes, THE system SHALL automatically apply full chain localization

### Requirement 9 — Settings and Updates

**User Story:** AS user, I want settings management, so that updates and configurations are accessible.

#### Acceptance Criteria

1. THE Settings screen SHALL contain update history and settings sections
2. WHEN user checks updates, THE system SHALL compare with GitHub releases
3. THE Settings SHALL retain AI service provider configuration

## Out of Scope

- Legacy FloatAI AI chat, floating window, capture features
- Package Hub, MCP, ATK modules from v1
- Non-Android project types
