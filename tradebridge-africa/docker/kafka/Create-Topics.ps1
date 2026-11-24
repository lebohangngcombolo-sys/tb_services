<#
.SYNOPSIS
    TradeBridge Africa — Kafka topic bootstrap (Windows PowerShell).

.DESCRIPTION
    Creates the four domain event topics + their DLQ counterparts.
    Idempotent: re-running is safe.

.EXAMPLE
    .\Create-Topics.ps1

.EXAMPLE
    .\Create-Topics.ps1 -InDocker -Bootstrap "kafka:9092"
#>
param(
    [string]$Bootstrap    = "localhost:9092",
    [int]   $Partitions   = 3,
    [int]   $Replication  = 1,
    [switch]$InDocker,
    [string]$Container    = "tradebridge-kafka"
)

$ErrorActionPreference = "Stop"

# Topic catalogue — 4 events + 4 DLQs
$Topics = @(
    "identity.events",
    "identity.events.dlq",
    "country.events",
    "country.events.dlq",
    "customer.events",
    "customer.events.dlq",
    "notification.events",
    "notification.events.dlq"
)

function Write-Log {
    param([string]$Message)
    Write-Host "[kafka-init] $Message"
}

function Invoke-Kafka {
    param([Parameter(ValueFromRemainingArguments=$true)][string[]]$Arguments)
    if ($InDocker) {
        & docker exec -i $Container @Arguments
    } else {
        & @Arguments
    }
    if ($LASTEXITCODE -ne 0) {
        throw "Kafka command failed with exit code $LASTEXITCODE"
    }
}

function Wait-ForKafka {
    Write-Log "Waiting for Kafka at $Bootstrap ..."
    $attempts = 0
    while ($attempts -lt 60) {
        $null = Invoke-Kafka kafka-topics --bootstrap-server $Bootstrap --list 2>$null
        if ($LASTEXITCODE -eq 0) {
            Write-Log "Kafka is reachable"
            return
        }
        Start-Sleep -Seconds 2
        $attempts++
    }
    throw "Kafka not reachable after 60 attempts"
}

function Get-TopicList {
    $raw = (Invoke-Kafka kafka-topics --bootstrap-server $Bootstrap --list) -join "`n"
    return $raw -split "`r?`n" | Where-Object { $_ -ne "" }
}

function New-Topic {
    param([string]$Topic)
    $existing = Get-TopicList
    if ($existing -contains $Topic) {
        Write-Log "  [skip] $Topic already exists"
        return
    }
    Write-Log "  [create] $Topic"
    Invoke-Kafka kafka-topics --bootstrap-server $Bootstrap `
        --create `
        --topic $Topic `
        --partitions $Partitions `
        --replication-factor $Replication `
        --config cleanup.policy=delete `
        --config retention.ms=604800000 `
        --config segment.bytes=1073741824
}

function Set-DlqRetention {
    param([string]$Topic)
    Write-Log "  [config] $Topic retention=30d"
    Invoke-Kafka kafka-configs --bootstrap-server $Bootstrap `
        --entity-type topics --entity-name $Topic `
        --alter --add-config retention.ms=2592000000
}

# ------------------------------------------------------------------ main
Write-Log "=========================================================="
Write-Log "TradeBridge Africa — Kafka topic initialization"
Write-Log "Bootstrap:   $Bootstrap"
Write-Log "Partitions:  $Partitions"
Write-Log "Replication: $Replication"
Write-Log "In Docker:   $InDocker (container: $Container)"
Write-Log "=========================================================="

Wait-ForKafka

Write-Log "Creating $($Topics.Count) topics..."
foreach ($t in $Topics) {
    New-Topic $t
}

Write-Log "Configuring DLQ retention..."
foreach ($t in $Topics) {
    if ($t.EndsWith(".dlq")) {
        Set-DlqRetention $t
    }
}

Write-Log "=========================================================="
Write-Log "Current topics:"
(Get-TopicList | Sort-Object) | ForEach-Object { Write-Log "  $_" }
Write-Log "=========================================================="
Write-Log "Done."
