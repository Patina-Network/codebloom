# Observability & Metrics

<img src="./dashboard.png" alt="Grafana">

Codebloom uses Spring Boot Actuator and Prometheus to provide operational insights into the running application. The application deployment now targets Kubernetes; see [infrastructure documentation](../../infra/README.md). OpenSearch and Grafana infrastructure configuration is not stored in this repository. Both of these data sources are then fed to a Grafana instance hosted on [monitor.tahmid.io](https://monitor.tahmid.io)

## Grafana

All metrics for production & staging can be viewed on [monitor.tahmid.io](https://monitor.tahmid.io).

> [!NOTE]
> Please reach out to [@tahminator](https://github.com/tahminator) if you are on the Codebloom dev team & need the credentials to login & view the dashboard

## Actuator Endpoints

Spring Boot Actuator exposes operational information about the running application through HTTP endpoints.

### Security

The checked-in [SecurityConfig.java](../../src/main/java/org/patinanetwork/codebloom/api/auth/security/SecurityConfig.java) permits these requests and does not configure HTTP Basic authentication or an `ACTUATOR` role. `ACTUATOR_USERNAME` and `ACTUATOR_PASSWORD` are not wired into the application configuration. Any access restrictions applied by deployment infrastructure must be checked in that infrastructure separately.

### Available Endpoints

Currently exposed endpoints:

- **`/actuator/health`** - Application health
- **`/actuator/prometheus`** - Prometheus-formatted metrics endpoint for scraping

## Prometheus Metrics

Prometheus metrics provide detailed insights into application performance, JVM statistics, HTTP requests, and custom business metrics.

### Accessing Metrics

**Local Development:**

```
http://localhost:8080/actuator/prometheus
```



**Staging:**

```
https://stg.codebloom.patinanetwork.org/actuator/prometheus
```

**Production:**

```
https://codebloom.patinanetwork.org/actuator/prometheus
```

### Testing Locally

To test the actuator endpoint locally:

1. Start the application (`just dev`).
2. Request an exposed endpoint:

```bash
curl http://localhost:8080/actuator/prometheus
curl http://localhost:8080/actuator/health
```
