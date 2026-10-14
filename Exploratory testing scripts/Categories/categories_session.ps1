$Base = "http://localhost:4567"
$OutputFile = Join-Path $PSScriptRoot "categories_session_output.txt"

Start-Transcript -Path $OutputFile -Force

function Test-Endpoint {
    param(
        [string]$Method,
        [string]$Path,
        [string]$Body = ""
    )

    Write-Host "`n=== $Method $Path ==="

    try {
        $Params = @{
            Uri     = "$Base$Path"
            Method  = $Method
            TimeoutSec = 10
        }

        if ($Body -ne "") {
            $Params.Body = $Body
            $Params.ContentType = "application/json"
        }

        $Response = Invoke-WebRequest @Params -UseBasicParsing
        Write-Host "Status: $([int]$Response.StatusCode)"
        if ($Response.Content) {
            Write-Host $Response.Content
        }
        return $Response
    }
    catch {
        if ($_.Exception.Response) {
            Write-Host "Status: $([int]$_.Exception.Response.StatusCode)"
        } else {
            Write-Host "Error: $($_.Exception.Message)"
        }
        return $null
    }
}

function Get-ResourceId {
    param(
        $Response,
        [string]$Collection,
        [string]$Title = ""
    )

    if (-not $Response -or -not $Response.Content) {
        return $null
    }

    try {
        $Data = $Response.Content | ConvertFrom-Json

        # Find the requested resource in a collection response
        if ($Collection -and $Data.$Collection) {
            $Items = @($Data.$Collection)

            if ($Title) {
                $Item = $Items | Where-Object { $_.title -eq $Title } |
                    Select-Object -First 1
            } else {
                $Item = $Items | Select-Object -First 1
            }

            if ($Item -and $Item.id) {
                return [string]$Item.id
            }
        }

        # Handle a single-resource response
        if ($Data.id) {
            return [string]$Data.id
        }

        # Handle a wrapped single-resource response
        foreach ($Name in @("category", "project", "todo")) {
            if ($Data.$Name -and $Data.$Name.id) {
                return [string]$Data.$Name.id
            }
        }
    }
    catch {
        Write-Host "Could not parse response JSON."
    }

    return $null
}

try {
    # 1. Categories collection
    Test-Endpoint GET "/categories"
    Test-Endpoint HEAD "/categories"

    # 2. Create a temporary category
    $Title = "SessionTestCategory"
    $CategoryBody = @{
        title       = $Title
        description = "Original description"
    } | ConvertTo-Json -Compress

    $Created = Test-Endpoint POST "/categories" $CategoryBody
    $CategoryId = Get-ResourceId $Created "categories" $Title

    # Find it in the collection if POST did not return its ID
    if (-not $CategoryId) {
        $List = Test-Endpoint GET "/categories"
        $CategoryId = Get-ResourceId $List "categories" $Title
    }

    if (-not $CategoryId) {
        Write-Host "Category ID not found; dependent tests cannot run."
    } else {
        Write-Host "Using category ID: $CategoryId"

        # 3. Individual category
        Test-Endpoint GET "/categories/$CategoryId"
        Test-Endpoint HEAD "/categories/$CategoryId"

        # 4. Compare POST and PUT updates
        Test-Endpoint POST "/categories/$CategoryId" `
            '{"title":"UpdatedByPOST"}'

        Test-Endpoint GET "/categories/$CategoryId"

        Test-Endpoint PUT "/categories/$CategoryId" `
            '{"title":"UpdatedByPUT"}'

        Test-Endpoint GET "/categories/$CategoryId"

        # 5. Category-project relationships
        $Projects = Test-Endpoint GET "/projects"
        $ProjectId = Get-ResourceId $Projects "projects"

        Test-Endpoint GET "/categories/$CategoryId/projects"
        Test-Endpoint HEAD "/categories/$CategoryId/projects"

        if ($ProjectId) {
            Write-Host "Using project ID: $ProjectId"

            # Check whether the relationship already exists
            $Existing = Test-Endpoint GET "/categories/$CategoryId/projects"
            $ExistingIds = @()

            if ($Existing -and $Existing.Content) {
                try {
                    $ExistingData = $Existing.Content | ConvertFrom-Json
                    $ExistingIds = @($ExistingData.projects |
                        ForEach-Object { [string]$_.id })
                } catch {}
            }

            $WasLinked = $ExistingIds -contains $ProjectId

            # Use a string ID in the relationship body
            $RelationshipBody = '{"id":"' + $ProjectId + '"}'

            Test-Endpoint POST "/categories/$CategoryId/projects" `
                $RelationshipBody

            # Repeat to check duplicate relationship behavior
            Test-Endpoint POST "/categories/$CategoryId/projects" `
                $RelationshipBody

            Test-Endpoint GET "/categories/$CategoryId/projects"

            # Delete only if this script created the relationship
            if (-not $WasLinked) {
                Test-Endpoint DELETE `
                    "/categories/$CategoryId/projects/$ProjectId"
                Test-Endpoint GET "/categories/$CategoryId/projects"
            }
        } else {
            Write-Host "No project found; relationship tests skipped."
        }

        # 6. Category-todo relationships
        $Todos = Test-Endpoint GET "/todos"
        $TodoId = Get-ResourceId $Todos "todos"

        Test-Endpoint GET "/categories/$CategoryId/todos"
        Test-Endpoint HEAD "/categories/$CategoryId/todos"

        if ($TodoId) {
            Write-Host "Using todo ID: $TodoId"

            $Existing = Test-Endpoint GET "/categories/$CategoryId/todos"
            $ExistingIds = @()

            if ($Existing -and $Existing.Content) {
                try {
                    $ExistingData = $Existing.Content | ConvertFrom-Json
                    $ExistingIds = @($ExistingData.todos |
                        ForEach-Object { [string]$_.id })
                } catch {}
            }

            $WasLinked = $ExistingIds -contains $TodoId
            $RelationshipBody = '{"id":"' + $TodoId + '"}'

            Test-Endpoint POST "/categories/$CategoryId/todos" `
                $RelationshipBody

            Test-Endpoint POST "/categories/$CategoryId/todos" `
                $RelationshipBody

            Test-Endpoint GET "/categories/$CategoryId/todos"

            if (-not $WasLinked) {
                Test-Endpoint DELETE `
                    "/categories/$CategoryId/todos/$TodoId"
                Test-Endpoint GET "/categories/$CategoryId/todos"
            }
        } else {
            Write-Host "No todo found; relationship tests skipped."
        }

        # 7. Delete the temporary category last
        Test-Endpoint DELETE "/categories/$CategoryId"
        Test-Endpoint GET "/categories/$CategoryId"
    }
}
finally {
    Stop-Transcript
}
