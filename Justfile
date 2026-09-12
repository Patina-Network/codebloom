set shell := ["bash", "-uc"]

# Migrate to local DB
migrate *args:
  dotenvx run -- ./mvnw flyway:migrate -Dflyway.locations=filesystem:./db {{args}}

# Drop local DB
drop *args:
  dotenvx run -- ./mvnw flyway:clean -Dflyway.locations=filesystem:./db -Dflyway.cleanDisabled=false {{args}}

# Manually migrate STAGING DB if needed (CAREFUL!!!)
migrate-stg *args:
  dotenvx run -f .env.staging -- ./mvnw flyway:migrate -Dflyway.locations=filesystem:./db/migration {{args}}

# Manually migrate PROD DB if needed (CAREFUL!!!)
migrate-prod *args:
  dotenvx run -f .env.production -- ./mvnw flyway:migrate -Dflyway.locations=filesystem:./db/migration {{args}}

# Manually copy production db to staging. (WILL DROP ALL STAGING DATA!!!) Check wiki for more details.
copy-stg *args:
  dotenvx run -f infra/.env.production -- bash infra/copy-prod-db.sh

# Run the backend Spring server
backend-dev *args:
  dotenvx run -f .env -f .env.shared -- ./mvnw -Dspring-boot.run.profiles=dev spring-boot:run {{args}}

# Run the backend Spring server with an exposed debugger at :5005
backend-dev-debug *args:
  dotenvx run -- ./mvnw \
    -Dspring-boot.run.profiles=dev \
    -Dspring-boot.run.jvmArguments="-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005" \
    spring-boot:run {{args}}

# Builds and installs Spring backend
backend-install *args:
  ./mvnw install -DskipTests=true {{args}}

# Run backend formatter (check only)
backend-spotless *args:
  ./mvnw spotless:check

# Run backend formatter (check & write)
backend-spotless-fix *args:
  ./mvnw spotless:apply

# Run backend linter, formatter, & tests
backend-test *args:
  just backend-spotless && dotenvx run -f .env -f .env.shared -- ./mvnw checkstyle:check verify -Dspring.profiles.active=ci {{args}}

# Run backend tests with a debugger
backend-testd *args:
  just backend-spotless && dotenvx run -f .env -f .env.shared -- ./mvnw checkstyle:check verify -Dspring.profiles.active=ci -Dmaven.surefire.debug {{args}}

# Run the frontend 
frontend-dev *args:
  cd js && pnpm i && pnpm run dev {{args}}

# Builds and installs frontend packages
frontend-install *args:
  cd js && pnpm i 

# Frontend tests 
frontend-test *args:
  cd js && pnpm run test

# Generate types through OpenAPI
type-gen *args:
  cd js && pnpm run generate {{args}}

# Run the react-email development server
email-dev *args:
  cd email && pnpm i && pnpm email dev --dir emails {{args}}

# Generate HTML output of react-email and copy to backend static folder
email-gen *args:
  cd email && bash email.sh {{args}}

# Run the dev servers (backend & frontend)
dev *args:
  npx concurrently "just backend-dev" "just frontend-dev" {{args}}

# Run the dev servers (backend & frontend) but the backend will launch a debugger server.
devd *args:
  npx concurrently "just backend-dev-debug" "just frontend-dev" {{args}}

# Interactive script to add a user's GPG public key so they can decrypt keys
git-crypt-add-user:
  cd internal/scripts && pnpm i && pnpm run git-crypt-add-user && clear && node git-crypt/add-user

# Interactive script to generate a GPG key for you and direct you to upload your public key to GitHub
git-crypt-generate-key:
  cd internal/scripts && pnpm i && pnpm run git-crypt-generate-key && clear && node git-crypt/generate-key
