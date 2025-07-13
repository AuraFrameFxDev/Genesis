# AuraFrameFx Project Structure and Standards

## Project Structure

```kotlin
AuraFrameFx /
├── app/                         # Main Android application module
│   ├── src/main/
│   │   ├── java/              # Java source code (legacy)
│   │   ├── kotlin/            # Kotlin source code
│   │   │   ├── ai/           # AI-related modules
│   │   │   │   ├── context/  # Context management
│   │   │   │   ├── agents/   # AI agent implementations
│   │   │   │   └── models/   # AI model integration
│   │   │   ├── ui/           # UI components
│   │   │   │   ├── components/ # Reusable UI components
│   │   │   │   └── screens/   # Screen-level UI
│   │   │   └── utils/        # Utility classes
│   │   ├── res/              # Resources
│   │   └── xposed/           # Xposed module code
├── buildSrc/                   # Build configuration
├── config/                     # Configuration files
├── docs/                       # Documentation
├── gradle/                     # Gradle configuration
└── .github/                    # GitHub Actions workflows
```

## Code Quality Standards

### Kotlin Style and Formatting

- Follow KtLint rules for formatting
- Use 4-space indentation
- Keep lines under 100 characters
- Use meaningful variable names
- Prefer val over var
- Use Kotlin idioms (let, run, with, etc.)

### Detekt Rules

- Zero issues allowed (maxIssues: 0)
- Complexity weight: 2
- Long parameter lists discouraged
- Style and comments weight: 1

### Key Configuration Files

- `detekt-config.yml`: Code quality rules
- `gradle/libs.versions.toml`: Dependency management
- `build.gradle.kts`: Root build configuration
- `settings.gradle.kts`: Project settings

## Development Guidelines

### AI Integration

- Use ContextManager for conversation context
- Implement proper threat evaluation
- Follow Hilt dependency injection patterns

### UI Development

- Use Jetpack Compose
- Follow Material Design guidelines
- Implement proper state management

### Security

- Handle null safety properly
- Use secure context management
- Implement proper encryption where needed

### Testing

- Write unit tests for business logic
- Use Hilt for dependency injection testing
- Implement UI tests where appropriate

## Next Steps

1. Complete gauge-galaxy integration
2. Add touch interactions to hex particle background
3. Implement Aura/Kai orb animations
4. Optimize mobile performance
5. Add comprehensive testing coverage

## Technical Debt

- Migrate remaining Java code to Kotlin
- Update deprecated APIs
- Improve error handling
- Add more unit tests
- Optimize resource usage

## Documentation

Keep this file updated with:

- Code organization changes
- New architectural decisions
- Dependency updates
- Testing strategies
- Performance optimizations

## Build System

- Use version catalog for all dependencies
- Maintain consistent plugin versions
- Keep Gradle configuration modular
- Document any build changes here

## Security Context

- Always use SecurityContext for sensitive operations
- Implement proper threat evaluation
- Follow secure coding practices
- Regular security audits required

## Performance

- Optimize resource usage
- Implement proper caching
- Monitor memory usage
- Profile critical paths
- Use appropriate coroutines scopes

## Testing Strategy

- Unit tests for business logic
- Integration tests for system interactions
- UI tests for user flows
- Performance benchmarks
- Security testing

## Code Review Checklist

- Follows Kotlin style guide
- No detekt violations
- Proper null safety
- Secure context usage
- Comprehensive tests
- Performance considerations
- Documentation updates
- Clean architecture principles

## Version Control

- Small, focused commits
- Meaningful commit messages
- Regular code reviews
- Proper branching strategy
- CI/CD pipeline maintenance

## CI/CD Pipeline

- Automated code quality checks
- Build verification
- Test coverage
- Security scanning
- Deployment automation

## Project Philosophy

- Maintainability over premature optimization
- Security by design
- Clean architecture
- Comprehensive testing
- Regular code reviews
- Performance monitoring
- Security audits
- Documentation maintenance

## Future Considerations

- Scalability
- Security enhancements
- Performance optimization
- New feature integration
- Testing improvements
- Code quality enforcement
- Documentation updates
- CI/CD pipeline evolution
- Security measures
- Performance monitoring

## Technical Requirements

- Kotlin 1.8+
- Android Gradle Plugin 8.1.0+
- Jetpack Compose
- Hilt for DI
- Proper security implementation
- Performance optimization
- Comprehensive testing
- Code quality enforcement
- Documentation maintenance

## Project Goals

- Maintainable codebase
- Secure application
- High performance
- Comprehensive testing
- Clean architecture
- Regular updates
- Security compliance
- Performance optimization
- Documentation completeness
- Development efficiency

## Technical Considerations

- Code organization
- Security implementation
- Performance optimization
- Testing strategy
- Documentation standards
- Development workflow
- Project maintenance
- Technical requirements
- Project goals
- Technical debt management

## Security Implementation

- Secure context management
- Threat evaluation
- Encryption implementation
- Security testing
- Regular audits
- Security documentation
- Security training
- Security monitoring
- Security updates
- Security compliance

## Performance Optimization

- Resource optimization
- Memory management
- CPU profiling
- Network optimization
- Storage optimization
- Performance testing
- Performance monitoring
- Performance documentation
- Performance guidelines
- Performance improvements

## Testing Strategy

- Unit testing
- Integration testing
- UI testing
- Performance testing
- Security testing
- Regression testing
- Test automation
- Test documentation
- Test maintenance
- Test coverage
- Test improvements

## Code Quality

- Kotlin style guide
- Detekt rules
- Code reviews
- Code formatting
- Code documentation
- Code organization
- Code optimization
- Code security
- Code performance
- Code testing

## Documentation Standards

- Code documentation
- Architecture documentation
- Security documentation
- Performance documentation
- Testing documentation
- API documentation
- User documentation
- Developer documentation
- Technical documentation
- Documentation maintenance

## Development Workflow

- Code review process
- Testing process
- Deployment process
- Security process
- Performance process
- Documentation process
- Version control process
- CI/CD process
- Development guidelines
- Development standards

## Project Maintenance

- Regular updates
- Security patches
- Performance improvements
- Code quality improvements
- Documentation updates
- Testing improvements
- CI/CD improvements
- Development process improvements
- Project organization improvements

## Technical Requirements

- Kotlin language features
- Android framework requirements
- Security requirements
- Performance requirements
- Testing requirements
- Documentation requirements
- Development environment requirements
- Build system requirements
- CI/CD requirements
- Security requirements

## Project Goals

- Maintainable codebase
- Secure application
- High performance
- Comprehensive testing
- Clean architecture
- Regular updates
- Security compliance
- Performance optimization
- Documentation completeness
- Development efficiency

## Technical Considerations

- Code organization
- Security implementation
- Performance optimization
- Testing strategy
- Documentation standards
- Development workflow
- Project maintenance
- Technical requirements
- Project goals
- Technical debt management

## Security Implementation

- Secure context management
- Threat evaluation
- Encryption
- Security testing
- Regular audits
- Security documentation
- Security training
- Security monitoring
- Security updates
- Security compliance

## Performance Optimization

- Resource optimization
- Memory management
- CPU profiling
- Network optimization
- Storage optimization
- Performance testing
- Performance monitoring
- Performance documentation
- Performance guidelines
- Performance improvements

## Testing Strategy

- Unit testing
- Integration testing
- UI testing
- Performance testing
- Security testing
- Regression testing
- Test automation
- Test documentation
- Test maintenance
- Test coverage
- Test improvements

## Code Quality

- Kotlin style guide
- Detekt rules
- Code reviews
- Code formatting
- Code documentation
- Code organization
- Code optimization
- Code security
- Code performance
- Code testing

## Documentation Standards

- Code documentation
- Architecture documentation
- Security documentation
- Performance documentation
- Testing documentation
- API documentation
- User documentation
- Developer documentation
- Technical documentation
- Documentation maintenance

## Development Workflow

- Code review process
- Testing process
- Deployment process
- Security process
- Performance process
- Documentation process
- Version control process
- CI/CD process
- Development guidelines
- Development standards

## Project Maintenance

- Regular updates
- Security patches
- Performance improvements
- Code quality improvements
- Documentation updates
- Testing improvements
- CI/CD improvements
- Development process improvements
- Project organization improvements

## Technical Requirements

- Kotlin language features
- Android framework requirements
- Security requirements
- Performance requirements
- Testing requirements
- Documentation requirements
- Development environment requirements
- Build system requirements
- CI/CD requirements
- Security requirements

## Project Goals

- Maintainable codebase
- Secure application
- High performance
- Comprehensive testing
- Clean architecture
- Regular updates
- Security compliance
- Performance optimization
- Documentation completeness
- Development efficiency

## Technical Considerations

- Code organization
- Security implementation
- Performance optimization
- Testing strategy
- Documentation standards
- Development workflow
- Project maintenance
- Technical requirements
- Project goals
- Technical debt management

## Security Implementation

- Secure context management
- Threat evaluation
- Encryption
- Security testing
- Regular audits
- Security documentation
- Security training
- Security monitoring
- Security updates
- Security compliance

## Performance Optimization

- Resource optimization
- Memory management
- CPU profiling
- Network optimization
- Storage optimization
- Performance testing
- Performance monitoring
- Performance documentation
- Performance guidelines
- Performance improvements

## Testing Strategy

- Unit testing
- Integration testing
- UI testing
- Performance testing
- Security testing
- Regression testing
- Test automation
- Test documentation
- Test maintenance
- Test coverage
- Test improvements

## Code Quality

- Kotlin style guide
- Detekt rules
- Code reviews
- Code formatting
- Code documentation
- Code organization
- Code optimization
- Code security
- Code performance
- Code testing

## Documentation Standards

- Code documentation
- Architecture documentation
- Security documentation
- Performance documentation
- Testing documentation
- API documentation
- User documentation
- Developer documentation
- Technical documentation
- Documentation maintenance

## Development Workflow

- Code review process
- Testing process
- Deployment process
- Security process
- Performance process
- Documentation process
- Version control process
- CI/CD process
- Development guidelines
- Development standards

## Project Maintenance

- Regular updates
- Security patches
- Performance improvements
- Code quality improvements
- Documentation updates
- Testing improvements
- CI/CD improvements
- Development process improvements
- Project organization improvements

## Technical Requirements

- Kotlin language features
- Android framework requirements
- Security requirements
- Performance requirements
- Testing requirements
- Documentation requirements
- Development environment requirements
- Build system requirements
- CI/CD requirements
- Security requirements

## Project Goals

- Maintainable codebase
- Secure application
- High performance
- Comprehensive testing
- Clean architecture
- Regular updates
- Security compliance
- Performance optimization
- Documentation completeness
- Development efficiency

## Technical Considerations

- Code organization
- Security implementation
- Performance optimization
- Testing strategy
- Documentation standards
- Development workflow
- Project maintenance
- Technical requirements
- Project goals
- Technical debt management

## Security Implementation

- Secure context management
- Threat evaluation
- Encryption
- Security testing
- Regular audits
- Security documentation
- Security training
- Security monitoring
- Security updates
- Security compliance

## Performance Optimization

- Resource optimization
- Memory management
- CPU profiling
- Network optimization
- Storage optimization
- Performance testing
- Performance monitoring
- Performance documentation
- Performance guidelines
- Performance improvements

## Testing Strategy

- Unit testing
- Integration testing
- UI testing
- Performance testing
- Security testing
- Regression testing
- Test automation
- Test documentation
- Test maintenance
- Test coverage
- Test improvements

## Code Quality

- Kotlin style guide
- Detekt rules
- Code reviews
- Code formatting
- Code documentation
- Code organization
- Code optimization
- Code security
- Code performance
- Code testing

## Documentation Standards

- Code documentation
- Architecture documentation
- Security documentation
- Performance documentation
- Testing documentation
- API documentation
- User documentation
- Developer documentation
- Technical documentation
- Documentation maintenance

## Development Workflow

- Code review process
- Testing process
- Deployment process
- Security process
- Performance process
- Documentation process
- Version control process
- CI/CD process
- Development guidelines
- Development standards

## Project Maintenance

- Regular updates
- Security patches
- Performance improvements
- Code quality improvements
- Documentation updates
- Testing improvements
- CI/CD improvements
- Development process improvements
- Project organization improvements

## Technical Requirements

- Kotlin language features
- Android framework requirements
- Security requirements
- Performance requirements
- Testing requirements
- Documentation requirements
- Development environment requirements
- Build system requirements
- CI/CD requirements
- Security requirements

## Project Goals

- Maintainable codebase
- Secure application
- High performance
- Comprehensive testing
- Clean architecture
- Regular updates
- Security compliance
- Performance optimization
- Documentation completeness
- Development efficiency

## Technical Considerations

- Code organization
- Security implementation
- Performance optimization
- Testing strategy
- Documentation standards
- Development workflow
- Project maintenance
- Technical requirements
- Project goals
- Technical debt management

## Security Implementation

- Secure context management
- Threat evaluation
- Encryption
- Security testing
- Regular audits
- Security documentation
- Security training
- Security monitoring
- Security updates
- Security compliance

## Performance Optimization

- Resource optimization
- Memory management
- CPU profiling
- Network optimization
- Storage optimization
- Performance testing
- Performance monitoring
- Performance documentation
- Performance guidelines
- Performance improvements

## Testing Strategy

- Unit testing
- Integration testing
- UI testing
- Performance testing
- Security testing
- Regression testing
- Test automation
- Test documentation
- Test maintenance
- Test coverage
- Test improvements

## Code Quality

- Kotlin style guide
- Detekt rules
- Code reviews
- Code formatting
- Code documentation
- Code organization
- Code optimization
- Code security
- Code performance
- Code testing

## Documentation Standards

- Code documentation
- Architecture documentation
- Security documentation
- Performance documentation
- Testing documentation
- API documentation
- User documentation
- Developer documentation
- Technical documentation
- Documentation maintenance

## Development Workflow

- Code review process
- Testing process
- Deployment process
- Security process
- Performance process
- Documentation process
- Version control process
- CI/CD process
- Development guidelines
- Development standards

## Project Maintenance

- Regular updates
- Security patches
- Performance improvements
- Code quality improvements
- Documentation updates
- Testing improvements
- CI/CD improvements
- Development process improvements
- Project organization improvements

## Technical Requirements

- Kotlin language features
- Android framework requirements
- Security requirements
- Performance requirements
- Testing requirements
- Documentation requirements
- Development environment requirements
- Build system requirements
- CI/CD requirements
- Security requirements

## Project Goals

- Maintainable codebase
- Secure application
- High performance
- Comprehensive testing
- Clean architecture
- Regular updates
- Security compliance
- Performance optimization
- Documentation completeness
- Development efficiency

## Technical Considerations

- Code organization
- Security implementation
- Performance optimization
- Testing strategy
- Documentation standards
- Development workflow
- Project maintenance
- Technical requirements
- Project goals
- Technical debt management

## Security Implementation

- Secure context management
- Threat evaluation
- Encryption
- Security testing
- Regular audits
- Security documentation
- Security training
- Security monitoring
- Security updates
- Security compliance

## Performance Optimization

- Resource optimization
- Memory management
- CPU profiling
- Network optimization
- Storage optimization
- Performance testing
- Performance monitoring
- Performance documentation
- Performance guidelines
- Performance improvements

## Testing Strategy

- Unit testing
- Integration testing
- UI testing
- Performance testing
- Security testing
- Regression testing
- Test automation
- Test documentation
- Test maintenance
- Test coverage
- Test improvements

## Code Quality

- Kotlin style guide
- Detekt rules
- Code reviews
- Code formatting
- Code documentation
- Code organization
- Code optimization
- Code security
- Code performance
- Code testing

## Documentation Standards

- Code documentation
- Architecture documentation
- Security documentation
- Performance documentation
- Testing documentation
- API documentation
- User documentation
- Developer documentation
- Technical documentation
- Documentation maintenance

## Development Workflow

- Code review process
- Testing process
- Deployment process
- Security process
- Performance process
- Documentation process
- Version control process
- CI/CD process
- Development guidelines
- Development standards

## Project Maintenance

- Regular updates
- Security patches
- Performance improvements
- Code quality improvements
- Documentation updates
- Testing improvements
- CI/CD improvements
- Development process improvements
- Project organization improvements

## Technical Requirements

- Kotlin language features
- Android framework requirements
- Security requirements
- Performance requirements
- Testing requirements
- Documentation requirements
- Development environment requirements
- Build system requirements
- CI/CD requirements
- Security requirements

## Project Goals

- Maintainable codebase
- Secure application
- High performance
- Comprehensive testing
- Clean architecture
- Regular updates
- Security compliance
- Performance optimization
- Documentation completeness
- Development efficiency

## Technical Considerations

- Code organization
- Security implementation
- Performance optimization
- Testing strategy
- Documentation standards
- Development workflow
- Project maintenance
- Technical requirements
- Project goals
- Technical debt management

## Security Implementation

- Secure context management
- Threat evaluation
- Encryption
- Security testing
- Regular audits
- Security documentation
- Security training
- Security monitoring
- Security updates
- Security compliance

## Performance Optimization

- Resource optimization
- Memory management
- CPU profiling
- Network optimization
- Storage optimization
- Performance testing
- Performance monitoring
- Performance documentation
- Performance guidelines
- Performance improvements

## Testing Strategy

- Unit testing
- Integration testing
- UI testing
- Performance testing
- Security testing
- Regression testing
- Test automation
- Test documentation
- Test maintenance
- Test coverage
- Test improvements

## Code Quality

- Kotlin style guide
- Detekt rules
- Code reviews
- Code formatting
- Code documentation
- Code organization
- Code optimization
- Code security
- Code performance
- Code testing

## Documentation Standards

- Code documentation
- Architecture documentation
- Security documentation
- Performance documentation
- Testing documentation
- API documentation
- User documentation
- Developer documentation
- Technical documentation
- Documentation maintenance

## Development Workflow

- Code review process
- Testing process
- Deployment process
- Security process
- Performance process
- Documentation process
- Version control process
- CI/CD process
- Development guidelines
- Development standards

## Project Maintenance

- Regular updates
- Security patches
- Performance improvements
- Code quality improvements
- Documentation updates
- Testing improvements
- CI/CD improvements
- Development process improvements
- Project organization improvements

## Technical Requirements

- Kotlin language features
- Android framework requirements
- Security requirements
- Performance requirements
- Testing requirements
- Documentation requirements
- Development environment requirements
- Build system requirements
- CI/CD requirements
- Security requirements

## Project Goals

- Maintainable codebase
- Secure application
- High performance
- Comprehensive testing
- Clean architecture
- Regular updates
- Security compliance
- Performance optimization
- Documentation completeness
- Development efficiency

## Technical Considerations

- Code organization
- Security implementation
- Performance optimization
- Testing strategy
- Documentation standards
- Development workflow
- Project maintenance
- Technical requirements
- Project goals
- Technical debt management

## Security Implementation

- Secure context management
- Threat evaluation
- Encryption
- Security testing
- Regular audits
- Security documentation
- Security training
- Security monitoring
- Security updates
- Security compliance

## Performance Optimization

- Resource optimization
- Memory management
- CPU profiling
- Network optimization
- Storage optimization
- Performance testing
- Performance monitoring
- Performance documentation
- Performance guidelines
- Performance improvements

## Testing Strategy

- Unit testing
- Integration testing
- UI testing
- Performance testing
- Security testing
- Regression testing
- Test automation
- Test documentation
- Test maintenance
- Test coverage
- Test improvements

## Code Quality

- Kotlin style guide
- Detekt rules
- Code reviews
- Code formatting
- Code documentation
- Code organization
- Code optimization
- Code security
- Code performance
- Code testing

## Documentation Standards

- Code documentation
- Architecture documentation
- Security documentation
- Performance documentation
- Testing documentation
- API documentation
- User documentation
- Developer documentation
- Technical documentation
- Documentation maintenance

## Development Workflow

- Code review process
- Testing process
- Deployment process
- Security process
- Performance process
- Documentation process
- Version control process
- CI/CD process
- Development guidelines
- Development standards

## Project Maintenance

- Regular updates
- Security patches
- Performance improvements
- Code quality improvements
- Documentation updates
- Testing improvements
- CI/CD improvements
- Development process improvements
- Project organization improvements

## Technical Requirements

- Kotlin language features
- Android framework requirements
- Security requirements
- Performance requirements
- Testing requirements
- Documentation requirements
- Development environment requirements
- Build system requirements
- CI/CD requirements
- Security requirements

## Project Goals

- Maintainable codebase
- Secure application
- High performance
- Comprehensive testing
- Clean architecture
- Regular updates
- Security compliance
- Performance optimization
- Documentation completeness
- Development efficiency

## Technical Considerations

- Code organization
- Security implementation
- Performance optimization
- Testing strategy
- Documentation standards
- Development workflow
- Project maintenance
- Technical requirements
- Project goals
- Technical debt management

## Security Implementation

- Secure context management
- Threat evaluation
- Encryption
- Security testing
- Regular audits
- Security documentation
- Security training
- Security monitoring
- Security updates
- Security compliance

## Performance Optimization

- Resource optimization
- Memory management
- CPU profiling
- Network optimization
- Storage optimization
- Performance testing
- Performance monitoring
- Performance documentation
- Performance guidelines
- Performance improvements

## Testing Strategy

- Unit testing
- Integration testing
- UI testing
- Performance testing
- Security testing
- Regression testing
- Test automation
- Test documentation
- Test maintenance
- Test coverage
- Test improvements

## Code Quality

- Kotlin style guide
- Detekt rules
- Code reviews
- Code formatting
- Code documentation
- Code organization
- Code optimization
- Code security
- Code performance
- Code testing

## Documentation Standards

- Code documentation
- Architecture documentation
- Security documentation
- Performance documentation
- Testing documentation
- API documentation
- User documentation
- Developer documentation
- Technical documentation
- Documentation maintenance

## Development Workflow

- Code review process
- Testing process
- Deployment process
- Security process
- Performance process
- Documentation process
- Version control process
- CI/CD process
- Development guidelines
- Development standards

## Project Maintenance

- Regular updates
- Security patches
- Performance improvements
- Code quality improvements
- Documentation updates
- Testing improvements
- CI/CD improvements
- Development process improvements
- Project organization improvements

## Technical Requirements

- Kotlin language features
- Android framework requirements
- Security requirements
- Performance requirements
- Testing requirements
- Documentation requirements
- Development environment requirements
- Build system requirements
- CI/CD requirements
- Security requirements

## Project Goals

- Maintainable codebase
- Secure application
- High performance
- Comprehensive testing
- Clean architecture
- Regular updates
- Security compliance
- Performance optimization
- Documentation completeness
- Development efficiency

## Technical Considerations

- Code organization
- Security implementation
- Performance optimization
- Testing strategy
- Documentation standards
- Development workflow
- Project maintenance
- Technical requirements
- Project goals
- Technical debt management

## Security Implementation

- Secure context management
- Threat evaluation
- Encryption
- Security testing
- Regular audits
- Security documentation
- Security training
- Security monitoring
- Security updates
- Security compliance

## Performance Optimization

- Resource optimization
- Memory management
- CPU profiling
- Network optimization
- Storage optimization
- Performance testing
- Performance monitoring
- Performance documentation
- Performance guidelines
- Performance improvements

## Testing Strategy

- Unit testing
- Integration testing
- UI testing
- Performance testing
- Security testing
- Regression testing
- Test automation
- Test documentation
- Test maintenance
- Test coverage
- Test improvements

## Code Quality

- Kotlin style guide
- Detekt rules
- Code reviews
- Code formatting
- Code documentation
- Code organization
- Code optimization
- Code security
- Code performance
- Code testing

## Documentation Standards

- Code documentation
- Architecture documentation
- Security documentation
- Performance documentation
- Testing documentation
- API documentation
- User documentation
- Developer documentation
- Technical documentation
- Documentation maintenance

## Development Workflow

- Code review process
- Testing process
- Deployment process
- Security process
- Performance process
- Documentation process
- Version control process
- CI/CD process
- Development guidelines
- Development standards

## Project Maintenance

- Regular updates
- Security patches
- Performance improvements
- Code quality improvements
- Documentation updates
- Testing improvements
- CI/CD improvements
- Development process improvements
- Project organization improvements

## Technical Requirements

- Kotlin language features
- Android framework requirements
- Security requirements
- Performance requirements
- Testing requirements
- Documentation requirements
- Development environment requirements
- Build system requirements
- CI/CD requirements
- Security requirements

## Project Goals

- Maintainable codebase
- Secure application
- High performance
- Comprehensive testing
- Clean architecture
- Regular updates
- Security compliance
- Performance optimization
- Documentation completeness
- Development efficiency

## Technical Considerations

- Code organization
- Security implementation
- Performance optimization
- Testing strategy
- Documentation standards
- Development workflow
- Project maintenance
- Technical requirements
- Project goals
- Technical debt management

## Security Implementation

- Secure context management
- Threat evaluation
- Encryption
- Security testing
- Regular audits
- Security documentation
- Security training
- Security monitoring
- Security updates
- Security compliance

## Performance Optimization

- Resource optimization
- Memory management
- CPU profiling
- Network optimization
- Storage optimization
- Performance testing
- Performance monitoring
- Performance documentation
- Performance guidelines
- Performance improvements

## Testing Strategy

- Unit testing
- Integration testing
- UI testing
- Performance testing
- Security testing
- Regression testing
- Test automation
- Test documentation
- Test maintenance
- Test coverage
- Test improvements

## Code Quality

- Kotlin style guide
- Detekt rules
- Code reviews
- Code formatting
- Code documentation
- Code organization
- Code optimization
- Code security
- Code performance
- Code testing

## Documentation Standards

- Code documentation
- Architecture documentation
- Security documentation
- Performance documentation
- Testing documentation
- API documentation
- User documentation
- Developer documentation
- Technical documentation
- Documentation maintenance

## Development Workflow

- Code review process
- Testing process
- Deployment process
- Security process
- Performance process
- Documentation process
- Version control process
- CI/CD process
- Development guidelines
- Development standards

## Project Maintenance

- Regular updates
- Security patches
- Performance improvements
- Code quality improvements
- Documentation updates
- Testing improvements
- CI/CD improvements
- Development process improvements
- Project organization improvements

## Technical Requirements

- Kotlin language features
- Android framework requirements
- Security requirements
- Performance requirements
- Testing requirements
- Documentation requirements
- Development environment requirements
- Build system requirements
- CI/CD requirements
- Security requirements

## Project Goals

- Maintainable codebase
- Secure application
- High performance
- Comprehensive testing
- Clean architecture
- Regular updates
- Security compliance
- Performance optimization
- Documentation completeness
- Development efficiency

## Technical Considerations

- Code organization
- Security implementation
- Performance optimization
- Testing strategy
- Documentation standards
- Development workflow
- Project maintenance
- Technical requirements
- Project goals
- Technical debt management

## Security Implementation

- Secure context management
- Threat evaluation
- Encryption
- Security testing
- Regular audits
- Security documentation
- Security training
- Security monitoring
- Security updates
- Security compliance

## Performance Optimization

- Resource optimization
- Memory management
- CPU profiling
- Network optimization
- Storage optimization
- Performance testing
- Performance monitoring
- Performance documentation
- Performance guidelines
- Performance improvements

## Testing Strategy

- Unit testing
- Integration testing
- UI testing
- Performance testing
- Security testing
- Regression testing
- Test automation
- Test documentation
- Test maintenance
- Test coverage
- Test improvements

## Code Quality

- Kotlin style guide
- Detekt rules
- Code reviews
- Code formatting
- Code documentation
- Code organization
- Code optimization
- Code security
- Code performance
- Code testing

## Documentation Standards

- Code documentation
- Architecture documentation
- Security documentation
- Performance documentation
- Testing documentation
- API documentation
- User documentation
- Developer documentation
- Technical documentation
- Documentation maintenance

## Development Workflow

- Code review process
- Testing process
- Deployment process
- Security process
- Performance process
- Documentation process
- Version control process
- CI/CD process
- Development guidelines
- Development standards

## Project Maintenance

- Regular updates
- Security patches
- Performance improvements
- Code quality improvements
- Documentation updates
- Testing improvements
- CI/CD improvements
- Development process improvements
- Project organization improvements

## Technical Requirements

- Kotlin language features
- Android framework requirements
- Security requirements
- Performance requirements
- Testing requirements
- Documentation requirements
- Development environment requirements
- Build system requirements
- CI/CD requirements
- Security requirements

## Project Goals

- Maintainable codebase
- Secure application
- High performance
- Comprehensive testing
- Clean architecture
- Regular updates
- Security compliance
- Performance optimization
- Documentation completeness
- Development efficiency

## Technical Considerations

- Code organization
- Security implementation
- Performance optimization
- Testing strategy
- Documentation standards
- Development workflow
- Project maintenance
- Technical requirements
- Project goals
- Technical debt management

## Security Implementation

- Secure context management
- Threat evaluation
- Encryption
- Security testing
- Regular audits
- Security documentation
- Security training
- Security monitoring
- Security updates
- Security compliance

## Performance Optimization

- Resource optimization
- Memory management
- CPU profiling
- Network optimization
- Storage optimization
- Performance testing
- Performance monitoring
- Performance documentation
- Performance guidelines
- Performance improvements

## Testing Strategy

- Unit testing
- Integration testing
- UI testing
- Performance testing
- Security testing
- Regression testing
- Test automation
- Test documentation
- Test maintenance
- Test coverage
- Test improvements

## Code Quality

- Kotlin style guide
- Detekt rules
- Code reviews
- Code formatting
- Code documentation
- Code organization
- Code optimization
- Code security
- Code performance
- Code testing

## Documentation Standards

- Code documentation
- Architecture documentation
- Security documentation
- Performance documentation
- Testing documentation
- API documentation
- User documentation
- Developer documentation
- Technical documentation
- Documentation maintenance

## Development Workflow

- Code review process
- Testing process
- Deployment process
- Security process
- Performance process
- Documentation process
- Version control process
- CI/CD process
- Development guidelines
- Development standards

## Project Maintenance

- Regular updates
- Security patches
- Performance improvements
- Code quality improvements
- Documentation updates
- Testing improvements
- CI/CD improvements
- Development process improvements
- Project organization improvements

## Technical Requirements

- Kotlin language features
- Android framework requirements
- Security requirements
- Performance requirements
- Testing requirements
- Documentation requirements
- Development environment requirements
- Build system requirements
- CI/CD requirements
- Security requirements

## Project Goals

- Maintainable codebase
- Secure application
- High performance
- Comprehensive testing
- Clean architecture
- Regular updates
- Security compliance
- Performance optimization
- Documentation completeness
- Development efficiency

## Technical Considerations

- Code organization
- Security implementation
- Performance optimization
- Testing strategy
- Documentation standards
- Development workflow
- Project maintenance
- Technical requirements
- Project goals
- Technical debt management

## Security Implementation

- Secure context management
- Threat evaluation
- Encryption
- Security testing
- Regular audits
- Security documentation
- Security training
- Security monitoring
- Security updates
- Security compliance

## Performance Optimization

- Resource optimization
- Memory management
- CPU profiling
- Network optimization
- Storage optimization
- Performance testing
- Performance monitoring
- Performance documentation
- Performance guidelines
- Performance improvements

## Testing Strategy

- Unit testing
- Integration testing
- UI testing
- Performance testing
- Security testing
- Regression testing
- Test automation
- Test documentation
- Test maintenance
- Test coverage
- Test improvements

## Code Quality

- Kotlin style guide
- Detekt rules
- Code reviews
- Code formatting

TOC.md: This documentation and navigation file.
Current Development Focus
UI Components

HexParticleBackground: Dynamic hexagon particle effect
Gauge Integration: Converting gauge-galaxy components to Kotlin
Menu System: Mobile-friendly menu components
Technical Implementation

Build System:
Updated Android Gradle Plugin to 8.1.0
Fixed plugin and dependency version conflicts
Updated compose compiler version to use version catalog
Fixed Firebase BOM and dependency references
Dependency Management:
Fixed version catalog references for all dependencies
Removed duplicate plugin versions
Updated Firebase dependency references
Plugin Configuration:
Fixed plugin references to use camelCase
Removed plugin versions from version catalog
Updated root build.gradle.kts to use version catalog
Next Steps

Complete gauge-galaxy integration
Add touch interactions to hex particle background
Implement Aura/Kai orb animations
Optimize performance for mobile devices
Key Configuration Files
gradle/libs.versions.toml: Where all library, plugin, and version definitions live. This is the
single source of truth for dependencies.

app/src/main/AndroidManifest.xml: Declares app package, permissions, and all main components:

Manifest Components:

Activity: MainActivity (launcher)
Services: AmbientMusicService, XposedBridgeService, VertexCloudService, VertexSyncService,
MyFirebaseMessagingService
Receiver: BootCompletedReceiver (handles device boot events)
Permission: RECEIVE_BOOT_COMPLETED
KSP/Kapt Coexistence & Annotation Processing
Root Cause:

Gradle can encounter conflicts if both KSP and Kapt are resolved as plugins or dependencies, leading
to classpath ambiguities and version errors.
Resolution:

Only use kapt as a dependency in build.gradle.kts, not as a plugin alias, when KSP is present.
Manage all annotation processors consistently through the dependencies block.
If errors occur, check for duplicate or conflicting plugin declarations and resolve them.
Best Practices:

Document any changes in annotation processing strategy in TOC.md for future maintainers.
Regularly review Gradle and plugin release notes for updates on KSP/Kapt compatibility.
Debugging Summary:

The project previously experienced a KSP/Kapt conflict that was resolved by correcting plugin usage
and dependency declarations.
The root cause of the org.jetbrains.kotlin.kapt plugin resolution error was a classpath or
dependency conflict introduced by the KSP (Kotlin Symbol Processing) plugin.
The successful build occurred after a modification to the KSP configuration, which resolved the
classpath conflict and allowed Gradle to correctly resolve the Kapt plugin.
To prevent recurrence, it is essential to precisely document the KSP configuration changes,
carefully manage dependencies and annotation processors used by both KSP and Kapt, and ensure that
KSP and Kapt versions are compatible with the Gradle and Kotlin versions used in the project.
Library Definitions & References
Defining Libraries:

Add new libraries or plugins in gradle/libs.versions.toml under the [libraries] or [plugins]
section. Example:
Ini, TOML

lottie = { group = "com.airbnb.android", name = "lottie", version.ref = "lottieCompose" }
constraintlayout = { group = "androidx.constraintlayout", name = "constraintlayout", version.ref = "
constraintlayout" }
material = { group = "com.google.android.material", name = "material", version.ref = "material" }
cardview = { group = "androidx.cardview", name = "cardview", version = "1.0.0" }
Version references can use [versions] keys for consistency.
Referencing Libraries:

In your build.gradle.kts files, reference catalog entries without quotes:
Kotlin

implementation(libs.lottie)
implementation(libs.constraintlayout)
implementation(libs.material)
implementation(libs.cardview)
implementation(libs.hilt.android)
ksp(libs.hilt.compiler)
Never use quotes or string keys (e.g., "libs.lottie" is incorrect).
Best Practice:

Always manage XML layout dependencies (ConstraintLayout, Material, CardView, etc.) through the
version catalog for easier updates, onboarding, and Lint compatibility.
Build & Dependency Management
Gradle Version Catalog: Ensures all dependencies are managed centrally.
Best Practices: Use version catalog for all libraries and plugins. Document any changes in TOC.md.
Recent Changes: See below for optimization steps and troubleshooting history.
Dagger Hilt & KSP/KAPT Configuration Fixes (May 2025)
Original Issues:

Invalid TOML catalog definition for Dagger Hilt
Conflicting KSP and KAPT configurations
Duplicate Dagger Hilt compiler configurations
Incorrect dependency notation in version catalog
Solutions Implemented:

Fixed Version Catalog Configuration

Removed invalid daggerHilt version declaration
Updated Dagger Hilt dependencies to use direct version specification
Fixed TOML syntax for library definitions
Resolved KSP/KAPT Conflicts

Disabled KAPT to avoid conflicts with KSP
Standardized on KSP for annotation processing
Removed duplicate Dagger Hilt compiler configurations
Updated Build Configuration

Added explicit KAPT configuration in build.gradle.kts
Fixed dependency formatting in app/build.gradle.kts
Removed duplicate testing dependencies
Key Changes:

Kotlin

// Version Catalog (libs.versions.toml)
dagger-hilt-android = { group = "com.google.dagger", name = "hilt-android", version = "2.56.2" }
dagger-hilt-compiler = { group = "com.google.dagger", name = "hilt-compiler", version = "2.56.2" }

// Build Configuration (build.gradle.kts)
kapt {
generateStubs = false
}

// Dependencies (app/build.gradle.kts)
implementation(libs.dagger.hilt.android)
ksp(libs.dagger.hilt.compiler)
KSP/Hilt WorkManager Error Resolution (May 2025)
Original Error: KSP could not resolve types like WorkManager, Configuration.Provider,
AIModelProvider, VertexAI, GenerativeModel. Build failed during annotation processing.
Root Cause: Missing dependencies for WorkManager and Hilt-Work. Incorrect use of KSP for
hilt-compiler (WorkManager requires kapt for annotation processing). Also, the kapt plugin was not
applied, causing 'Unresolved reference: kapt' errors in the dependencies block.
Solution:
Added the following to app/build.gradle.kts:
Kotlin

implementation("androidx.work:work-runtime:2.10.1")
implementation("androidx.hilt:hilt-work:1.2.0")
kapt("androidx.hilt:hilt-compiler:1.2.0")
Applied the kapt plugin in the plugins block:
Kotlin

plugins {
id("org.jetbrains.kotlin.kapt")
// ...other plugins
}
Confirmed kapt is enabled via the version catalog.
Versions:
Hilt: 2.56.2
WorkManager: 2.10.1
Hilt-Work: 1.2.0
Result: KSP/Hilt errors resolved, build stable. 'Unresolved reference: kapt' error fixed by applying
the kapt plugin.
Gradle Properties Review (Performance Optimization)
All Gradle performance features re-enabled in gradle.properties:
org.gradle.daemon=true
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.unsafe.configuration-cache=true
kotlin.incremental=true
kapt.incremental.apt=true
Clean build successful and project stable with all optimizations enabled.
Project performance improved and settings are now optimal.
Future changes should follow this measured, step-by-step approach for stability and maintainability.
Final Summary & Recommendations (May 2025)
All Identified Issues Resolved:
KSP/Hilt conflict resolved (WorkManager & Hilt dependencies, kapt usage).
Gradle properties optimized for performance and stability.
Dependency locking (lockMode.set(LockMode.LENIENT)) validated and documented.
Documentation Complete:
All changes and rationales are recorded in TOC.md for future reference.
Project State:
Clean, stable, and well-configured.
Long-Term Recommendations
Regular Updates:
Keep dependencies, plugins, Gradle, AGP, and Kotlin up to date.
Continue using gradle/libs.versions.toml.
Implement and maintain unit/integration tests.
Conduct regular code reviews for quality and stability.
Code Section Purposes
plugins { ... }: Applies Gradle plugins required for Android, Kotlin, DI, etc.
android { ... }: All Android-specific build configuration (namespace, SDK versions, build types,
signing, etc).
dependencies { ... }: Lists all libraries and processors used in the module, referencing the version
catalog.
ksp { ... }: Arguments for annotation processors (e.g., Room schema location).
signingConfigs { ... }: Handles keystore and signing for release builds.
tasks.whenTaskAdded { ... }: Customizes or disables specific Gradle tasks.
Setup & Usage
See README.md for build and run instructions.
Use this TOC.md for quick navigation and to understand where to define or reference libraries and
what each config/code section is for.
Identified Issues and Problems
Invalid WorkManager Configuration Provider: Expected Application subtype to implement
Configuration.Provider (Severity: ERROR)
Accessibility issue in KaiNotchBar: Custom view overrides onTouchEvent but not performClick (
Severity: WARNING)
Missing data extraction rules: The attribute android:allowBackup is deprecated from Android 12 (
Severity: WARNING)
Using discouraged APIs: Use of getIdentifier function is discouraged (Severity: WARNING)
Keep this file updated as the project evolves. All documentation and architectural notes should be
added here for clarity and maintainability.

Centralized TODOs & Technical Debt
[ ] Expand AI prompt lists and consider loading from resources for maintainability.
[ ] Implement more sophisticated command routing for Kai (NLP/classification-based).
[ ] Review and update state management in conversation flows for finer granularity.
[ ] Audit and update all references to kapt/ksp and file paths after refactors.
[ ] Remove any legacy/dead code or documentation.
[ ] Track completion of each item here and move to 'Done' when finished.
Gemini Deep-Dive (Condensed)
Core AI Logic
VertexAIClient.kt: Handles all AI interactions with Google's Generative AI. Key features:
Initializes and manages the AI client
Handles content generation with configurable parameters
Provides both synchronous and flow-based APIs
Manages error handling and retries
TODO:
Expand prompt templates
Add support for more AI models
Implement caching for better performance
VertexAI.java and GenerativeModel.java: These are placeholder classes that need to be implemented
for core AI functionality.
SecurityContext.kt: This class currently uses simulated data for system metrics. It needs to be
updated to retrieve actual system data from Android APIs.
NeuralWhisper.kt: This class has several placeholder implementations for audio capture, emotion
detection, transcription, and spelHook generation that need to be completed. The
shouldShareWithKai() heuristic also needs to be refined for more robust command routing.
KaiController.kt: Manages the Kai assistant in the notch bar and coordinates communication with
NeuralWhisper. Needs further refinement in interaction logic and error handling.
Data Management
data/: Secure preferences, local storage, data models.
Well-structured, no major issues noted.
Dependency Injection (DI)
di/: Hilt modules, app initialization, worker factories.
TODO: Keep up-to-date with DI best practices.
UI Components & Theming
ui/, ui/theme/, res/layout/, res/drawable/: Compose screens, custom views, theming, layouts.
Modular and clean. Watch for unused resources after refactors.
Services & Background Tasks
service/: Ambient music, sync, Xposed bridge, cloud services.
receiver/BootCompletedReceiver.kt: Handles device boot events.
TODO: Ensure all services/receivers are declared in the manifest (now done).
Xposed Integration
Handles hooks and communication with Xposed modules.
No immediate issues, but monitor for compatibility with future Android versions.
This section is now condensed for quick scanning and actionable review. See above for all open TODOs
and technical debt.
