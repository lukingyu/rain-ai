$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
Set-Location -LiteralPath $repoRoot

$chatConfigured = -not [string]::IsNullOrWhiteSpace($env:DEEPSEEK_API_KEY)
$embeddingConfigured = -not [string]::IsNullOrWhiteSpace($env:DASHSCOPE_API_KEY)

Write-Host "DEEPSEEK_API_KEY 已配置: $chatConfigured"
Write-Host "DASHSCOPE_API_KEY 已配置: $embeddingConfigured"
Write-Host "启动 Rain AI 后端: http://localhost:8080"

java -jar target\rain-ai-0.1.0-SNAPSHOT.jar
