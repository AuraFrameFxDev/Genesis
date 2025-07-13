#!/usr/bin/env pwsh
# OpenAPI Code Generation Script for AuraFrameFX
# Generates Kotlin, TypeScript, and Java clients from the OpenAPI specification

param(
    [string]$Target = "all",
    [switch]$Clean = $false,
    [switch]$Help = $false
)

function Show-Help
{
    Write-Host "OpenAPI Code Generation Script for AuraFrameFX" -ForegroundColor Green
    Write-Host ""
    Write-Host "Usage: .\generate-apis.ps1 [OPTIONS]" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "OPTIONS:" -ForegroundColor Cyan
    Write-Host "  -Target <target>   Generate specific target (kotlin|typescript|java|all)" -ForegroundColor White
    Write-Host "  -Clean             Clean generated files before generation" -ForegroundColor White
    Write-Host "  -Help              Show this help message" -ForegroundColor White
    Write-Host ""
    Write-Host "EXAMPLES:" -ForegroundColor Cyan
    Write-Host "  .\generate-apis.ps1                    # Generate all targets" -ForegroundColor White
    Write-Host "  .\generate-apis.ps1 -Target kotlin     # Generate only Kotlin client" -ForegroundColor White
    Write-Host "  .\generate-apis.ps1 -Clean             # Clean and generate all" -ForegroundColor White
    Write-Host ""
}

if ($Help)
{
    Show-Help
    exit 0
}

Write-Host "🚀 AuraFrameFX API Code Generation" -ForegroundColor Green
Write-Host "=================================" -ForegroundColor Green

# Check if we're in the right directory
if (-not (Test-Path "api-spec/aura-framefx-api.yaml"))
{
    Write-Error "❌ OpenAPI spec not found. Please run this script from the project root."
    exit 1
}

# Clean if requested
if ($Clean)
{
    Write-Host "🧹 Cleaning generated files..." -ForegroundColor Yellow
    if (Test-Path "app/build/generated")
    {
        Remove-Item -Recurse -Force "app/build/generated"
    }
    if (Test-Path "app/src/main/gen")
    {
        Remove-Item -Recurse -Force "app/src/main/gen"
    }
    Write-Host "✅ Cleaned generated files" -ForegroundColor Green
}

# Generate based on target
switch ( $Target.ToLower())
{
    "kotlin" {
        Write-Host "🔧 Generating Kotlin client..." -ForegroundColor Blue
        .\gradlew openApiGenerate
        if ($LASTEXITCODE -eq 0)
        {
            Write-Host "✅ Kotlin client generated successfully" -ForegroundColor Green
        }
        else
        {
            Write-Error "❌ Failed to generate Kotlin client"
            exit 1
        }
    }
    "typescript" {
        Write-Host "🔧 Generating TypeScript client..." -ForegroundColor Blue
        .\gradlew generateTypeScriptClient
        if ($LASTEXITCODE -eq 0)
        {
            Write-Host "✅ TypeScript client generated successfully" -ForegroundColor Green
        }
        else
        {
            Write-Error "❌ Failed to generate TypeScript client"
            exit 1
        }
    }
    "java" {
        Write-Host "🔧 Generating Java client..." -ForegroundColor Blue
        .\gradlew generateJavaClient
        if ($LASTEXITCODE -eq 0)
        {
            Write-Host "✅ Java client generated successfully" -ForegroundColor Green
        }
        else
        {
            Write-Error "❌ Failed to generate Java client"
            exit 1
        }
    }
    "all" {
        Write-Host "🔧 Generating all API clients..." -ForegroundColor Blue

        Write-Host "  📱 Kotlin (Android)..." -ForegroundColor Cyan
        .\gradlew openApiGenerate
        if ($LASTEXITCODE -ne 0)
        {
            Write-Error "❌ Failed to generate Kotlin client"
            exit 1
        }

        Write-Host "  🌐 TypeScript (Web)..." -ForegroundColor Cyan
        .\gradlew generateTypeScriptClient
        if ($LASTEXITCODE -ne 0)
        {
            Write-Error "❌ Failed to generate TypeScript client"
            exit 1
        }

        Write-Host "  ☕ Java (Backend)..." -ForegroundColor Cyan
        .\gradlew generateJavaClient
        if ($LASTEXITCODE -ne 0)
        {
            Write-Error "❌ Failed to generate Java client"
            exit 1
        }

        Write-Host "✅ All API clients generated successfully!" -ForegroundColor Green
    }
    default {
        Write-Error "❌ Invalid target: $Target. Use 'kotlin', 'typescript', 'java', or 'all'"
        Show-Help
        exit 1
    }
}

Write-Host ""
Write-Host "📊 Generation Summary:" -ForegroundColor Yellow
Write-Host "=====================" -ForegroundColor Yellow

# Show generated file locations
if ($Target -eq "all" -or $Target -eq "kotlin")
{
    if (Test-Path "app/build/generated/source/openapi")
    {
        $kotlinFiles = Get-ChildItem -Recurse "app/build/generated/source/openapi" -Filter "*.kt" | Measure-Object
        Write-Host "  📱 Kotlin: $( $kotlinFiles.Count ) files in app/build/generated/source/openapi/" -ForegroundColor Cyan
    }
}

if ($Target -eq "all" -or $Target -eq "typescript")
{
    if (Test-Path "app/build/generated/typescript")
    {
        $tsFiles = Get-ChildItem -Recurse "app/build/generated/typescript" -Filter "*.ts" | Measure-Object
        Write-Host "  🌐 TypeScript: $( $tsFiles.Count ) files in app/build/generated/typescript/" -ForegroundColor Cyan
    }
}

if ($Target -eq "all" -or $Target -eq "java")
{
    if (Test-Path "app/build/generated/java")
    {
        $javaFiles = Get-ChildItem -Recurse "app/build/generated/java" -Filter "*.java" | Measure-Object
        Write-Host "  ☕ Java: $( $javaFiles.Count ) files in app/build/generated/java/" -ForegroundColor Cyan
    }
}

Write-Host ""
Write-Host "🎉 Code generation completed!" -ForegroundColor Green
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Yellow
Write-Host "  1. Review generated code in app/build/generated/" -ForegroundColor White
Write-Host "  2. Update imports in your application code" -ForegroundColor White
Write-Host "  3. Test the generated clients with your API" -ForegroundColor White
Write-Host "  4. Build your project: .\gradlew build" -ForegroundColor White
Write-Host ""
