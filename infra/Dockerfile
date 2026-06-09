FROM mirror.gcr.io/library/node:22 AS frontend-build

WORKDIR /js

# Fix a bug with corepack by installing corepack globally
RUN npm i -g corepack@latest

COPY js/package.json js/pnpm-lock.yaml ./
COPY js/patches/ ./patches/

RUN corepack enable pnpm && corepack use $(node -p "require('./package.json').packageManager") && pnpm i --frozen-lockfile

COPY js/ ./

ARG VITE_STAGING
ENV VITE_STAGING=${VITE_STAGING}
RUN pnpm run build

FROM mirror.gcr.io/library/node:22 AS email-build

WORKDIR /email

COPY email/package.json email/pnpm-lock.yaml ./

RUN npm i -g corepack@latest
RUN corepack enable pnpm && corepack use $(node -p "require('./package.json').packageManager") && pnpm i --frozen-lockfile

COPY email/ ./

RUN pnpm email export --dir emails --outDir out

FROM mirror.gcr.io/library/eclipse-temurin:25-jdk-jammy AS backend-build

WORKDIR /app

COPY mvnw pom.xml ./
COPY .mvn/ .mvn/

RUN ./mvnw dependency:resolve -B

COPY src ./src
COPY --from=frontend-build /js/dist/ src/main/resources/static/
RUN mkdir -p src/main/resources/static/email && rm -f src/main/resources/static/email/*.html
COPY --from=email-build /email/out/*.html src/main/resources/static/email/

ARG COMMIT_SHA
RUN ./mvnw package -DskipTests -Dcommit.sha="${COMMIT_SHA}"

FROM mirror.gcr.io/library/eclipse-temurin:25-jre-jammy

WORKDIR /app

COPY mvnw pom.xml ./
COPY .mvn/ .mvn/
RUN ./mvnw -B exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install --with-deps"
COPY --from=backend-build /app/target/*.jar app.jar

ARG SERVER_PROFILES=prod
ENV SPRING_PROFILES_ACTIVE=${SERVER_PROFILES}

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
