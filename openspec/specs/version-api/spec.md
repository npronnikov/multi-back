# Version API Specification

## Purpose
This specification defines requirements for a REST API endpoint that provides version information from the backend, including the backend application version and additional build details.

## Requirements

### Requirement: Version Endpoint
A REST API endpoint must be available to provide backend version information.

##### Scenario: GET request to version endpoint
- **WHEN** a client makes a GET request to `/api/version`
- **THEN** the endpoint returns HTTP status 200 (OK)
- **AND** the response body contains version information in JSON format
- **AND** the response includes the backend application version

### Requirement: Version Response Format
The version endpoint must return version information in a structured JSON format.

##### Scenario: Standard version response structure
- **WHEN** a client requests the version endpoint
- **THEN** the response includes the following fields:
  - `backendVersion`: String representing the backend application version (e.g., "0.0.1-SNAPSHOT")
  - `frontendVersion`: String representing the frontend application version (e.g., "0.1.0") if available
  - `gitCommitHash`: String representing the git commit hash if available
  - `buildTimestamp`: String representing the build timestamp if available
- **AND** all string fields are properly quoted in JSON format
- **AND** optional fields are omitted if not available rather than being null

##### Scenario: Response format validation
- **WHEN** the version endpoint is called
- **THEN** the returned JSON is valid and parseable
- **AND** the `backendVersion` field is always present and non-empty
- **AND** all present fields contain valid string values

### Requirement: Version Data Source
The endpoint must retrieve version information from reliable application sources.

##### Scenario: Version from application configuration
- **WHEN** the version endpoint processes a request
- **THEN** the backend version is retrieved from the application's build configuration (pom.xml for Maven projects)
- **AND** the version reflects the actual deployed application version
- **AND** the version is consistent across all requests

##### Scenario: Build information integration
- **WHEN** build information is available during the build process
- **THEN** the endpoint includes git commit hash if the project is built from git
- **AND** the endpoint includes build timestamp if available from the build system
- **AND** this information is consistent with the actual build that produced the running application

### Requirement: Endpoint Performance and Caching
The version endpoint should be efficient and cacheable for performance.

##### Scenario: Response time performance
- **WHEN** a client requests the version endpoint
- **THEN** the response is returned within a reasonable time (typically < 100ms for static version info)
- **AND** the endpoint does not perform expensive operations or database queries

##### Scenario: Cache headers for version data
- **WHEN** the version endpoint responds to a request
- **THEN** appropriate cache headers are set (e.g., Cache-Control: max-age=300)
- **AND** the version data can be safely cached by clients and intermediaries
- **AND** cached responses remain valid for the application version lifetime

### Requirement: Error Handling
The endpoint must handle errors gracefully and provide meaningful responses.

##### Scenario: Unavailable version information
- **WHEN** version information cannot be retrieved due to configuration issues
- **THEN** the endpoint returns HTTP status 500 (Internal Server Error)
- **AND** the response includes an error message explaining the issue
- **AND** the error message is safe to expose to clients (no sensitive information)

##### Scenario: Endpoint availability
- **WHEN** the application is running and healthy
- **THEN** the version endpoint is accessible and responds to requests
- **AND** the endpoint does not require authentication (assuming public version info)
- **AND** the endpoint is available regardless of other application state

## Change History
- **2026-07-16**: Initial specification added as part of version-button change (openspec/changes/version-button)
