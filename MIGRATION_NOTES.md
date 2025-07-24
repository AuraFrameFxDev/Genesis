# Project Reorganization - Import Updates Needed

## Oracle Drive Imports
Old: `import dev.aurakai.auraframefx.oracledrive.*`
New: `import dev.aurakai.auraframefx.oracle.integration.*`

## Test File Locations
Old: `app/ai_backend/test_*.py`
New: `tests/ai_backend/test_*.py`

## Sandbox UI
Old: `sandbox-ui/` module
New: `app/src/main/java/dev/aurakai/auraframefx/sandbox/ui/`

## Next Steps Required:
1. Update import statements in Kotlin/Java files
2. Update test configurations to point to new test directory
3. Update CI/CD pipelines to use new test paths
4. Remove empty oracle-drive-integration and oracledrive directories after verification