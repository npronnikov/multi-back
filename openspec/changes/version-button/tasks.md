# Tasks: Version Button

## Phase 1: Backend API Implementation

- [ ] T001 — Create `VersionController` class with `@RestController` annotation (`back/src/main/java/com/example/controller/VersionController.java`)
- [ ] T002 — Implement GET endpoint `/api/version` returning version information (`back/src/main/java/com/example/controller/VersionController.java`)
- [ ] T003 — Add version data model/response DTO with proper JSON structure (`back/src/main/java/com/example/dto/VersionResponse.java`)
- [ ] T004 — Configure Maven to inject version information during build process (`back/pom.xml`)
- [ ] T005 — Add optional git commit hash and build timestamp retrieval (`back/src/main/java/com/example/controller/VersionController.java`)
- [ ] T006 — Implement proper cache headers for API response (`back/src/main/java/com/example/controller/VersionController.java`)
- [ ] T007 — Add error handling and appropriate HTTP status codes (`back/src/main/java/com/example/controller/VersionController.java`)
- [ ] T008 — Create unit tests for version endpoint (`back/src/test/java/com/example/controller/VersionControllerTest.java`)

## Phase 2: Frontend Components

- [ ] T009 — Create `VersionButton` component with proper styling (`front/app/components/VersionButton.tsx`)
- [ ] T010 — Create `VersionModal` component with responsive layout (`front/app/components/VersionModal.tsx`)
- [ ] T011 — Implement modal state management (open/close/loading/error) (`front/app/components/VersionModal.tsx`)
- [ ] T012 — Create version API client function using existing API infrastructure (`front/app/lib/api/version.ts`)
- [ ] T013 — Integrate version button into main layout/header (`front/app/layout.tsx`)
- [ ] T014 — Add frontend version retrieval from package.json (`front/app/components/VersionModal.tsx`)
- [ ] T015 — Implement error handling for API failures (`front/app/components/VersionModal.tsx`)
- [ ] T016 — Add loading states for version data fetching (`front/app/components/VersionModal.tsx`)
- [ ] T017 — Implement accessibility features (ARIA labels, keyboard navigation) (`front/app/components/VersionButton.tsx`, `front/app/components/VersionModal.tsx`)
- [ ] T018 — Create unit tests for version components (`front/app/components/__tests__/VersionButton.test.tsx`, `front/app/components/__tests__/VersionModal.test.tsx`)

## Phase 3: Styling and Responsive Design

- [ ] T019 — Style version button to match existing design system (`front/app/components/VersionButton.tsx` + CSS modules)
- [ ] T020 — Style modal dialog with proper overlay and close button (`front/app/components/VersionModal.tsx` + CSS modules)
- [ ] T021 — Implement mobile-responsive design for button and modal (`front/app/components/VersionModal.tsx`)
- [ ] T022 — Test and adjust styling for tablet and desktop viewports (`front/app/components/VersionModal.tsx`)
- [ ] T023 — Ensure version information text is properly formatted and readable (`front/app/components/VersionModal.tsx`)

## Phase 4: Integration and Testing

- [ ] T024 — Test version button visibility and functionality across all pages (`front/app/layout.tsx`)
- [ ] T025 — Verify API integration and data flow from backend to frontend (`front/app/components/VersionModal.tsx`)
- [ ] T026 — Test modal behavior on different screen sizes and devices (`front/app/components/VersionModal.tsx`)
- [ ] T027 — Verify keyboard navigation and accessibility features (`front/app/components/VersionButton.tsx`, `front/app/components/VersionModal.tsx`)
- [ ] T028 — Test error scenarios (API failure, missing data) (`front/app/components/VersionModal.tsx`)
- [ ] T029 — Verify version button doesn't conflict with existing navigation (`front/app/layout.tsx`)

## Verification

- [ ] T030 — Backend compilation: `cd back && ./gradlew compileJava`
- [ ] T031 — Backend unit tests: `cd back && ./gradlew test`
- [ ] T032 — Backend build: `cd back && ./gradlew build`
- [ ] T033 — Frontend type checking: `cd front && npm run type-check` (if available)
- [ ] T034 — Frontend build: `cd front && npm run build`
- [ ] T035 — Frontend tests: `cd front && npm run test` (if available)
- [ ] T036 — Frontend linting: `cd front && npm run lint` (if available)
- [ ] T037 — Full application smoke test: `cd back && ./gradlew test && cd front && npm run build`
