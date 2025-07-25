# Test Directory Structure

This directory contains all test files organized by component:

- `ai_backend/` - Python AI backend tests
- `integration/` - Integration tests
- `unit/` - Unit tests
- `oracle-drive/` - Oracle Drive component tests

## Running Tests

### Python Tests

```bash
cd tests/ai_backend
python -m pytest
```

### Kotlin/Java Tests

```bash
./gradlew test
```