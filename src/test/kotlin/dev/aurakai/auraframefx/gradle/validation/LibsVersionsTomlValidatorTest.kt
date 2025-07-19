@Test
fun `validate should handle TOML files with very long lines exceeding typical buffer sizes`() {
    val longLine = "test-key-${"x".repeat(10000)} = \"1.0.0\""
    val longLineToml = """
        [versions]
        $longLine

        [libraries]
        lib = { module = "group:artifact", version = "1.0.0" }
    """.trimIndent()

    testFile.writeText(longLineToml)

    val result = validator.validate()

    // Should handle long lines gracefully without crashing
    assertTrue(result.isValid || result.errors.isNotEmpty())
}