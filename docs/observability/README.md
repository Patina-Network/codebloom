# Observability & Metrics

<img src="./dashboard.png" alt="Grafana">

Codebloom uses Spring Boot Actuator and Prometheus to provide operational insights (metrics) into the running application. 
Promtail also pulls logs directly from the K8s instance.

Both of these data sources are then fed to a Grafana instance hosted on [grafana.patinanetwork.org](https://grafana.patinanetwork.org)

## Grafana

All metrics for production & staging can be viewed on [grafana.patinanetwork.org](https://grafana.patinanetwork.org).

> [!NOTE]
> Please reach out to Henry [@arklian](https://github.com/arklian) if you are on the dev team & need the credentials to login & view the dashboard

## Actuator Endpoints

Spring Boot Actuator exposes operational information about the running application through HTTP endpoints.

### Security

All actuator endpoints are protected with HTTP Basic Authentication to prevent unauthorized access to sensitive operational data.

**Authentication Details:**

- **Authentication Type:** HTTP Basic Auth
- **Role Required:** `ACTUATOR`
- **Credentials:** Stored in environment variables
  - `ACTUATOR_USERNAME` - Username for actuator endpoints
  - `ACTUATOR_PASSWORD` - Password for actuator endpoints (generate with `openssl rand -base64 48 | head -c 64`)

### Available Endpoints

Currently exposed endpoints:

- **`/actuator/prometheus`** - Prometheus-formatted metrics endpoint for scraping

## Prometheus Metrics

Prometheus metrics provide detailed insights into application performance, JVM statistics, HTTP requests, and custom business metrics.

### Accessing Metrics

**Local Development:**

```
http://localhost:8080/actuator/prometheus
```

(Requires HTTP Basic Auth with actuator credentials)

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

1. Ensure your `.env` file has `ACTUATOR_USERNAME` and `ACTUATOR_PASSWORD` set
2. Start the application (`just dev`)
3. Access the endpoint using curl:

```bash
curl -u actuator:your_password http://localhost:8080/actuator/prometheus
```

Or use a browser and enter the username/password when prompted.
