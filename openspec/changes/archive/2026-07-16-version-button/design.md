# Design: Version Button

## Overview
Implement a comprehensive version display feature using a balanced approach that provides functionality without excessive complexity. The solution uses a modal dialog triggered by a header button, leveraging both static frontend version info and dynamic backend API data. This approach (Approach B from exploration) balances user experience with implementation complexity.

## Decisions

### Decision: UI Placement and Interaction
**Выбор:** Version button in header/navigation with modal dialog display
**Обоснование:** Header placement ensures consistent visibility across all pages while maintaining accessibility. Modal dialog provides space for comprehensive version information without cluttering the main interface.
**Альтернативы:** 
- Footer placement (less visible)
- Tooltip on hover (limited space, not mobile-friendly)
- Separate settings page (overkill for this feature)

### Decision: API Endpoint Strategy
**Выбор:** Create dedicated `/api/version` endpoint returning JSON with backend version and optional build details
**Обоснование:** Provides flexibility for future enhancements and separates concerns. Endpoint can be cached and reused by other components. Allows dynamic version updates without frontend redeployment.
**Альтернативы:**
- Static version from package.json only (no backend integration)
- Add version info to existing endpoints (coupling concerns)

### Decision: Version Data Sources
**Выбор:** Frontend version from package.json, Backend version from pom.xml, optional git hash from build
**Обоснование:** Uses existing build artifacts without requiring additional infrastructure. Maven provides reliable version from pom.xml. Optional git information available through standard build plugins.
**Альтернативы:**
- Manual version constants (error-prone, requires updates)
- External version service (overkill for simple version display)

### Decision: Technology Stack
**Выбор:** React components for frontend modal/button, Spring Boot @RestController for API endpoint
**Обоснование:** Consistent with existing application stack. React provides component-based UI with state management. Spring Boot offers straightforward REST API creation with proper JSON serialization.
**Альтернативы:**
- External UI libraries (adds dependencies)
- Server-side rendering (unnecessary complexity for modal)

### Decision: State Management Approach
**Выбор:** Local component state for modal open/close, no global state management
**Обоснование:** Modal state is simple and doesn't require global access. Component state reduces complexity and follows React best practices for isolated UI components.
**Альтернативы:**
- Redux/global state (overkill for simple modal)
- URL parameters (unnecessary for modal state)

## Architecture

### Component Structure
```
Frontend (React):
┌─────────────────────────────────────┐
│          App Layout                 │
│  ┌──────────────────────────────┐  │
│  │ Header/Navigation            │  │
│  │  ┌────────────────────────┐  │  │
│  │  │ VersionButton          │  │  │
│  │  └────────────────────────┘  │  │
│  └──────────────────────────────┘  │
│                                     │
│  ┌──────────────────────────────┐  │
│  │ VersionModal (conditional)  │  │
│  │  - Frontend version          │  │
│  │  - Backend version           │  │
│  │  - Additional details        │  │
│  └──────────────────────────────┘  │
└─────────────────────────────────────┘

Backend (Spring Boot):
┌─────────────────────────────────────┐
│  VersionController                   │
│    └── GET /api/version             │
│         ├── Returns backend version │
│         ├── Includes git hash if    │
│         │   available              │
│         └── Returns JSON response   │
└─────────────────────────────────────┘
```

### Data Flow
1. User clicks version button
2. Component state sets modal to open
3. Modal component makes API call to `/api/version`
4. Backend returns version information
5. Frontend displays version data in modal
6. User closes modal, state resets

### API Interaction Pattern
- **Request:** GET `/api/version` (no authentication required for public version info)
- **Response:** JSON with version fields
- **Error Handling:** Display error message in modal if API call fails
- **Caching:** Frontend can cache version data for session duration

## Data Model

### API Response Schema
```json
{
  "backendVersion": "0.0.1-SNAPSHOT",
  "frontendVersion": "0.1.0",
  "gitCommitHash": "abc123def456...",  // Optional
  "buildTimestamp": "2024-01-15T10:30:00Z"  // Optional
}
```

### Component State Structure
```typescript
interface VersionModalState {
  isOpen: boolean;
  versionData: {
    backendVersion: string;
    frontendVersion: string;
    gitCommitHash?: string;
    buildTimestamp?: string;
  } | null;
  isLoading: boolean;
  error: string | null;
}
```

## Implementation Notes

### Frontend Implementation
- **Button Component:** Create or reuse existing button component with "version" label
- **Modal Component:** Implement as conditional render with proper overlay and close mechanisms
- **API Client:** Use existing API infrastructure (likely axios or fetch wrapper)
- **Styling:** Follow existing design system, ensure mobile responsiveness
- **Error Handling:** Graceful degradation if API fails, show frontend version at minimum

### Backend Implementation
- **Controller:** Create `VersionController` with `@RestController` and `@RequestMapping("/api")`
- **Version Source:** Read from `pom.xml` using Maven resource filtering or Spring Boot's `BuildProperties`
- **Build Information:** Use Spring Boot Actuator or Maven build number plugin for git details
- **Caching:** Add appropriate HTTP cache headers (Cache-Control: max-age=300)
- **Error Handling:** Return proper HTTP status codes and error messages

### Integration Considerations
- **Layout Integration:** Modify `layout.tsx` to include version button in header
- **Navigation:** Ensure button doesn't interfere with existing navigation patterns
- **Responsive Design:** Test on mobile, tablet, and desktop screen sizes
- **Accessibility:** Add ARIA labels, keyboard navigation, and screen reader support

### Performance Considerations
- API endpoint should be lightweight (< 100ms response time)
- Version data can be cached on frontend for session duration
- Modal should use React.memo or similar optimization to prevent unnecessary re-renders
- Lazy load modal component to reduce initial bundle size

## Risks

### Technical Risks
- **R1:** UI space constraints in header/navigation may require layout adjustments
  - **Mitigation:** Test on various screen sizes, consider collapsible button for mobile
- **R2:** Version information accuracy depends on build process consistency
  - **Mitigation:** Implement proper build-time version injection and testing
- **R3:** API endpoint availability affects feature functionality
  - **Mitigation:** Implement graceful fallback to frontend version only

### Integration Risks
- **R4:** Conflicts with existing UI components or navigation patterns
  - **Mitigation:** Thorough testing on different pages and screen sizes
- **R5:** Accessibility compliance with existing application standards
  - **Mitigation:** Follow WCAG guidelines and test with screen readers

### Future Considerations
- **Version Update Notifications:** Foundation for displaying "new version available" messages
- **Multi-component Versioning:** Pattern can be extended for microservices or component versioning
- **Debug/Support Mode:** Version information foundation for debug modes or support workflows
- **Internationalization:** Modal can be extended for multi-language support
