set shell := ["bash", "-uc"]

### Azure cmds

# logout and authenticate again
re-auth:
  az logout && just auth

# authenticate
auth:
  just install-pre-scripts && az login

# Migrate to local DB
migrate *args:
  dotenvx run -- bash ./mvnw flyway:migrate -Dflyway.locations=filesystem:./db {{args}}

# Drop local DB
drop *args:
  dotenvx run -- bash ./mvnw flyway:clean -Dflyway.locations=filesystem:./db -Dflyway.cleanDisabled=false {{args}}

# Manually migrate STAGING DB if needed (CAREFUL!!!)
migrate-stg *args:
  dotenvx run -f .env.staging -- bash ./mvnw flyway:migrate -Dflyway.locations=filesystem:./db/migration {{args}}

# Manually migrate PROD DB if needed (CAREFUL!!!)
migrate-prod *args:
  dotenvx run -f .env.production -- bash ./mvnw flyway:migrate -Dflyway.locations=filesystem:./db/migration {{args}}

# Manually copy production db to staging. (WILL DROP ALL STAGING DATA!!!) Check wiki for more details.
copy-stg *args:
  dotenvx run -f infra/.env.production -- bash infra/copy-prod-db.sh

# Run the backend Spring server
backend-dev *args:
  sops exec-env .env.shared.yaml 'dotenvx run --overload -f .env -- bash ./mvnw -Dspring-boot.run.profiles=dev spring-boot:run {{args}}'

# Run the leetcode auth service
las-dev *args:
  cd ./internal/leetcode-auth-service && cargo build && sops exec-env ../../.env.shared.yaml 'dotenvx run --overload -f ../../.env -- cargo run {{args}}'

# Run the leetcode auth stealer service
lass-dev *args:
  cd ./internal/leetcode-auth-stealer-service && uv sync && GRPC_REFLECTION=true GRPC_VERBOSITY=ERROR uv run src/server.py

# Generate the leetcode auth stealer gRPC stubs for the main codebloom service
lass-gen *args:
  cd ./internal/leetcode-auth-stealer-service && uv sync && uv run src/generate.py

# Run the backend Spring server with an exposed debugger at :5005
backend-dev-debug *args:
  dotenvx run -- bash ./mvnw \
    -Dspring-boot.run.profiles=dev \
    -Dspring-boot.run.jvmArguments="-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005" \
    spring-boot:run {{args}}

# Builds and installs Spring backend
backend-install *args:
  bash ./mvnw install -DskipTests=true {{args}}

# Run backend formatter (check only)
backend-spotless *args:
  bash ./mvnw spotless:check

# Run backend formatter (check & write)
backend-spotless-fix *args:
  bash ./mvnw spotless:apply

# Run backend linter, formatter, & tests
backend-test *args:
  just backend-spotless && dotenvx run -f .env -f .env.shared -- bash ./mvnw checkstyle:check verify -Dspring.profiles.active=ci {{args}}

# Run backend tests with a debugger
backend-testd *args:
  just backend-spotless && dotenvx run -f .env -f .env.shared -- bash ./mvnw checkstyle:check verify -Dspring.profiles.active=ci -Dmaven.surefire.debug {{args}}

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

# Use if you created a new secret file and need to encrypt it with SOPS
# If you are editing a file that has already been encrypted, see `just edit`
encrypt file *args:
  just install-pre-scripts && sops --encrypt --in-place {{ file }} {{ args }}

# Securely edit any secret file that is encrypted with SOPS
# You can change the editor it will call on by changing the $EDITOR environment variable
# 
# you can choose to set it one time for the scope of the command: `EDITOR="nvim" just edit secrets.yaml`
# or you can put `export EDITOR=nvim` inside of your `~/.zshrc`, then restart your terminal & run: `just edit secrets.yaml`
#
# if you would like to use VSCode, `EDITOR="code --wait"` (You may have to follow this first: https://code.visualstudio.com/docs/setup/mac#_launch-vs-code-from-the-command-line)
edit file *args:
  just install-pre-scripts && sops edit {{ file }} {{ args }}

# Run the dev servers (backend & frontend & auth service)
dev *args:
  just install-pre-scripts && npx mprocs "just backend-dev" "just frontend-dev" "just las-dev" "just lass-dev" {{args}}

# Git hooks are installed on almost every command
# so that we don't accidentally add unencrypted secrets to the git history.
install-pre-scripts:
  just install-pre-commit && just install-pre-push

# `--git-path` resolves the shared hooks dir, so this also works inside a worktree.
install-pre-commit:
  cp pre-commit "$(git rev-parse --git-path hooks)/pre-commit" && chmod +x "$(git rev-parse --git-path hooks)/pre-commit"

install-pre-push:
  cp pre-push "$(git rev-parse --git-path hooks)/pre-push" && chmod +x "$(git rev-parse --git-path hooks)/pre-push"

# Run the dev servers (backend & frontend & auth service) but the backend will launch a debugger server.
devd *args:
  npx concurrently "just backend-dev-debug" "just frontend-dev" "just-auth-dev" {{args}}
