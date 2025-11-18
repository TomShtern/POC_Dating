# CI/CD Pipeline Implementation Prompt

## Context
You are implementing a simple, beginner-friendly CI/CD pipeline for a POC Dating application using **GitHub Actions**. The user is new to CI/CD, so prioritize simplicity and clarity over advanced features. You have **full internet access** to research GitHub Actions syntax, workflow patterns, and troubleshooting.

**Why GitHub Actions over CircleCI:** Free for public repos, integrated with GitHub, simpler YAML syntax, extensive marketplace actions, better documentation for beginners.

**Scale:** 100-10K users (small scale, optimize for simplicity not performance)

## ⚠️ CRITICAL: Code Quality Requirements

**WRITE CLEAN, MAINTAINABLE, SIMPLE WORKFLOWS.**

This is non-negotiable. Every workflow must be:
- **SIMPLE** - Minimal steps, clear purpose, easy to understand
- **MODULAR** - Reusable jobs, separate workflows per concern
- **DOCUMENTED** - Comments explaining every step, clear job names
- **BEGINNER-FRIENDLY** - No advanced features unless necessary, verbose output

**Simplicity Rules:**
```yaml
# ✅ GOOD: Clear, documented steps
name: Build and Test

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

jobs:
  build:
    name: Build and Test Application  # Clear job name
    runs-on: ubuntu-latest

    steps:
      # Step 1: Get the code
      - name: Checkout code
        uses: actions/checkout@v4

      # Step 2: Set up Java 21
      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: maven  # Cache dependencies for speed

      # Step 3: Build and test
      - name: Build with Maven
        run: mvn clean verify --batch-mode
        working-directory: ./backend

# ❌ BAD: Cryptic, undocumented
jobs:
  j1:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - run: mvn verify  # What does this do? Why?
```

**Why This Matters:** You need to understand and modify this pipeline later. Simple, documented workflows save hours of debugging.

## Scope
Create these GitHub Actions workflows in `.github/workflows/`:

1. **ci.yml** - Build and test on every push/PR
2. **code-quality.yml** - Linting, formatting checks
3. **security-scan.yml** - Basic dependency vulnerability scanning
4. **deploy-staging.yml** - Deploy to staging (manual trigger)

## Implementation Tasks

### Task 1: Main CI Workflow (ci.yml)
```yaml
name: CI Pipeline

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

jobs:
  build:
    name: Build Application
    runs-on: ubuntu-latest
    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: maven

      - name: Build all services
        run: mvn clean compile --batch-mode
        working-directory: ./backend

      - name: Run unit tests
        run: mvn test --batch-mode
        working-directory: ./backend

      - name: Generate test report
        if: always()  # Run even if tests fail
        uses: dorny/test-reporter@v1
        with:
          name: Test Results
          path: '**/target/surefire-reports/*.xml'
          reporter: java-junit

  test-coverage:
    name: Test Coverage
    needs: build
    runs-on: ubuntu-latest
    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: maven

      - name: Run tests with coverage
        run: mvn clean test jacoco:report --batch-mode
        working-directory: ./backend

      - name: Check coverage threshold
        run: |
          # Simple coverage check - fail if below 70%
          echo "Coverage report generated at backend/*/target/site/jacoco/index.html"

      - name: Upload coverage report
        uses: actions/upload-artifact@v4
        with:
          name: coverage-report
          path: backend/*/target/site/jacoco/
```

### Task 2: Code Quality Workflow (code-quality.yml)
```yaml
name: Code Quality

on:
  pull_request:
    branches: [main, develop]

jobs:
  checkstyle:
    name: Check Code Style
    runs-on: ubuntu-latest
    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: maven

      - name: Run Checkstyle
        run: mvn checkstyle:check --batch-mode
        working-directory: ./backend
        continue-on-error: true  # Don't fail build, just report
```

### Task 3: Security Scan Workflow (security-scan.yml)
```yaml
name: Security Scan

on:
  schedule:
    - cron: '0 0 * * 1'  # Weekly on Monday
  workflow_dispatch:  # Allow manual trigger

jobs:
  dependency-check:
    name: Check Dependencies
    runs-on: ubuntu-latest
    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: maven

      - name: Check for vulnerable dependencies
        run: mvn dependency-check:check --batch-mode
        working-directory: ./backend
        continue-on-error: true

      - name: Upload security report
        uses: actions/upload-artifact@v4
        with:
          name: dependency-check-report
          path: backend/target/dependency-check-report.html
```

### Task 4: Deploy Staging Workflow (deploy-staging.yml)
```yaml
name: Deploy to Staging

on:
  workflow_dispatch:  # Manual trigger only
    inputs:
      service:
        description: 'Service to deploy (all, user, match, chat)'
        required: true
        default: 'all'

jobs:
  deploy:
    name: Deploy to Staging
    runs-on: ubuntu-latest
    environment: staging  # Requires approval if configured

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: maven

      - name: Build JAR files
        run: mvn clean package -DskipTests --batch-mode
        working-directory: ./backend

      - name: Upload build artifacts
        uses: actions/upload-artifact@v4
        with:
          name: service-jars
          path: backend/*/target/*.jar

      # Add actual deployment steps later (SSH, cloud CLI, etc.)
      - name: Deploy notification
        run: echo "✅ Build artifacts ready for deployment"
```

### Task 5: Branch Protection Rules
Document in README or wiki:
```markdown
## Branch Protection Setup

1. Go to Settings → Branches → Add rule
2. Branch name pattern: `main`
3. Enable:
   - ✅ Require pull request before merging
   - ✅ Require status checks to pass (select "Build Application")
   - ✅ Require branches to be up to date
4. Save changes
```

## Iteration Loop (Repeat Until Complete)

### Phase 1: Create Workflows
```bash
mkdir -p .github/workflows
# Create each workflow file
```

### Phase 2: Validate Syntax
```bash
# Use actionlint to validate workflows locally
# Or push to a branch and check GitHub Actions tab
```
- If syntax errors → fix YAML → re-validate
- Use internet to research GitHub Actions syntax

### Phase 3: Test Workflows
```bash
# Push to a feature branch
git checkout -b test-ci
git add .github/
git commit -m "ci: add GitHub Actions workflows"
git push origin test-ci
```
- Check GitHub Actions tab for workflow runs
- If jobs fail → read logs → fix → push again

### Phase 4: Verify All Jobs Pass
- Create a test PR to main
- Verify all checks appear and pass
- Check artifacts are uploaded correctly

## Success Criteria
- [ ] ci.yml runs on every push to main/develop
- [ ] ci.yml runs on every PR to main
- [ ] Tests run and report results clearly
- [ ] Coverage report is generated and uploaded
- [ ] Code quality checks run on PRs
- [ ] Security scan can be triggered manually
- [ ] Deploy workflow creates JAR artifacts
- [ ] All workflow files have clear comments
- [ ] Branch protection rules are documented

## When Stuck
1. **Search internet** for GitHub Actions examples, workflow syntax
2. **Check logs** in GitHub Actions tab (very detailed)
3. **Validate locally** with actionlint tool
4. **Read:** https://docs.github.com/en/actions

## DO NOT
- Use Docker builds (not needed for this scale)
- Add complex matrix builds (keep it simple)
- Use self-hosted runners (use GitHub's runners)
- Skip documentation (you need to understand this later)
- Add deployment to production (staging only for now)
- Use secrets without documenting what they're for

## Future Enhancements (Not Now)
- Docker image builds when ready
- Production deployment pipeline
- Performance testing in CI
- Automated release notes

---
**Iterate until all workflows run successfully. Use internet access freely to resolve GitHub Actions issues.**
