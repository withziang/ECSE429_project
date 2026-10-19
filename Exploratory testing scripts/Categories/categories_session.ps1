$Base = "http://localhost:4567"
$OutputFile = Join-Path $PSScriptRoot "categories_session_output.txt"

Start-Transcript -Path $OutputFile -Force

try {
    # 1. Retrieve all categories.
    Invoke-WebRequest "$Base/categories" -Method GET -UseBasicParsing

    # 2. Check the headers returned by the categories collection.
    Invoke-WebRequest "$Base/categories" -Method HEAD -UseBasicParsing

    # 3. Create a temporary category.
    $Title = "SessionTestCategory"
    $Created = Invoke-WebRequest "$Base/categories" -Method POST `
        -ContentType "application/json" `
        -Body '{"title":"SessionTestCategory","description":"Original description"}' `
        -UseBasicParsing
    $Created

    # 4. Extract the category ID from the creation response.
    $CreatedData = $Created.Content | ConvertFrom-Json
    $CategoryId = [string]$CreatedData.id

    if (-not $CategoryId -and $CreatedData.category) {
        $CategoryId = [string]$CreatedData.category.id
    }

    # 5. Retrieve categories to find the new category if necessary.
    if (-not $CategoryId) {
        $CategoryList = Invoke-WebRequest "$Base/categories" -Method GET -UseBasicParsing
        $CategoryList
        $CategoryListData = $CategoryList.Content | ConvertFrom-Json
        $CategoryId = [string](($CategoryListData.categories |
            Where-Object { $_.title -eq $Title } |
            Select-Object -First 1).id)
    }

    Write-Host "Category ID: $CategoryId"

    # 6. Retrieve the individual category.
    Invoke-WebRequest "$Base/categories/$CategoryId" -Method GET -UseBasicParsing

    # 7. Check the headers of the individual category.
    Invoke-WebRequest "$Base/categories/$CategoryId" -Method HEAD -UseBasicParsing

    # 8. Update the category using POST.
    Invoke-WebRequest "$Base/categories/$CategoryId" -Method POST `
        -ContentType "application/json" `
        -Body '{"title":"UpdatedByPOST"}' -UseBasicParsing

    # 9. Retrieve the category to inspect the POST update.
    Invoke-WebRequest "$Base/categories/$CategoryId" -Method GET -UseBasicParsing

    # 10. Update the category using PUT.
    Invoke-WebRequest "$Base/categories/$CategoryId" -Method PUT `
        -ContentType "application/json" `
        -Body '{"title":"UpdatedByPUT"}' -UseBasicParsing

    # 11. Retrieve the category to inspect the PUT update.
    Invoke-WebRequest "$Base/categories/$CategoryId" -Method GET -UseBasicParsing

    # 12. Retrieve all projects.
    $Projects = Invoke-WebRequest "$Base/projects" -Method GET -UseBasicParsing
    $Projects

    # 13. Extract the first project's ID.
    $ProjectData = $Projects.Content | ConvertFrom-Json
    $ProjectId = [string](($ProjectData.projects | Select-Object -First 1).id)

    if (-not $ProjectId -and $ProjectData.project) {
        $ProjectId = [string]$ProjectData.project.id
    }

    if (-not $ProjectId -and $ProjectData.id) {
        $ProjectId = [string]$ProjectData.id
    }

    # 14. Retrieve projects associated with the category.
    Invoke-WebRequest "$Base/categories/$CategoryId/projects" -Method GET -UseBasicParsing

    # 15. Check the headers of the category-project collection.
    Invoke-WebRequest "$Base/categories/$CategoryId/projects" -Method HEAD -UseBasicParsing

    if ($ProjectId) {
        # 16. Retrieve existing category-project relationships.
        $ExistingProjects = Invoke-WebRequest "$Base/categories/$CategoryId/projects" -Method GET -UseBasicParsing
        $ExistingProjects
        $ExistingProjectData = $ExistingProjects.Content | ConvertFrom-Json

        # 17. Record whether the project was already linked.
        $WasProjectLinked = @($ExistingProjectData.projects |
            Where-Object { [string]$_.id -eq $ProjectId }).Count -gt 0

        # 18. Associate the project with the category.
        Invoke-WebRequest "$Base/categories/$CategoryId/projects" -Method POST `
            -ContentType "application/json" `
            -Body ('{"id":"' + $ProjectId + '"}') -UseBasicParsing

        # 19. Repeat the association to test duplicate handling.
        Invoke-WebRequest "$Base/categories/$CategoryId/projects" -Method POST `
            -ContentType "application/json" `
            -Body ('{"id":"' + $ProjectId + '"}') -UseBasicParsing

        # 20. Retrieve projects to verify the relationship.
        Invoke-WebRequest "$Base/categories/$CategoryId/projects" -Method GET -UseBasicParsing

        if (-not $WasProjectLinked) {
            # 21. Delete the project relationship created by this test.
            Invoke-WebRequest "$Base/categories/$CategoryId/projects/$ProjectId" -Method DELETE -UseBasicParsing

            # 22. Verify that the project relationship was removed.
            Invoke-WebRequest "$Base/categories/$CategoryId/projects" -Method GET -UseBasicParsing
        }
    }
    else {
        Write-Host "No project ID found; project relationship tests skipped."
    }

    # 23. Retrieve all todos.
    $Todos = Invoke-WebRequest "$Base/todos" -Method GET -UseBasicParsing
    $Todos

    # 24. Extract the first todo's ID.
    $TodoData = $Todos.Content | ConvertFrom-Json
    $TodoId = [string](($TodoData.todos | Select-Object -First 1).id)

    if (-not $TodoId -and $TodoData.todo) {
        $TodoId = [string]$TodoData.todo.id
    }

    if (-not $TodoId -and $TodoData.id) {
        $TodoId = [string]$TodoData.id
    }

    # 25. Retrieve todos associated with the category.
    Invoke-WebRequest "$Base/categories/$CategoryId/todos" -Method GET -UseBasicParsing

    # 26. Check the headers of the category-todo collection.
    Invoke-WebRequest "$Base/categories/$CategoryId/todos" -Method HEAD -UseBasicParsing

    if ($TodoId) {
        # 27. Retrieve existing category-todo relationships.
        $ExistingTodos = Invoke-WebRequest "$Base/categories/$CategoryId/todos" -Method GET -UseBasicParsing
        $ExistingTodos
        $ExistingTodoData = $ExistingTodos.Content | ConvertFrom-Json

        # 28. Record whether the todo was already linked.
        $WasTodoLinked = @($ExistingTodoData.todos |
            Where-Object { [string]$_.id -eq $TodoId }).Count -gt 0

        # 29. Associate the todo with the category.
        Invoke-WebRequest "$Base/categories/$CategoryId/todos" -Method POST `
            -ContentType "application/json" `
            -Body ('{"id":"' + $TodoId + '"}') -UseBasicParsing

        # 30. Repeat the association to test duplicate handling.
        Invoke-WebRequest "$Base/categories/$CategoryId/todos" -Method POST `
            -ContentType "application/json" `
            -Body ('{"id":"' + $TodoId + '"}') -UseBasicParsing

        # 31. Retrieve todos to verify the relationship.
        Invoke-WebRequest "$Base/categories/$CategoryId/todos" -Method GET -UseBasicParsing

        if (-not $WasTodoLinked) {
            # 32. Delete the todo relationship created by this test.
            Invoke-WebRequest "$Base/categories/$CategoryId/todos/$TodoId" -Method DELETE -UseBasicParsing

            # 33. Verify that the todo relationship was removed.
            Invoke-WebRequest "$Base/categories/$CategoryId/todos" -Method GET -UseBasicParsing
        }
    }
    else {
        Write-Host "No todo ID found; todo relationship tests skipped."
    }

    # 34. Delete the temporary category.
    Invoke-WebRequest "$Base/categories/$CategoryId" -Method DELETE -UseBasicParsing

    # 35. Verify that the deleted category can no longer be retrieved.
    try {
        Invoke-WebRequest "$Base/categories/$CategoryId" -Method GET -UseBasicParsing
    }
    catch {
        Write-Host "Expected error after category deletion: $($_.Exception.Message)"
    }
}
finally {
    # Finish saving the transcript.
    Stop-Transcript
}