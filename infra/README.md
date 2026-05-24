# `infra/`

> [!NOTE]
> Our staging to production migration script is located [here](https://github.com/tahminator/codebloom/tree/main/.github/scripts/copy-prod-db/index.ts#L28).

[`clean-stg-db.SQL`](./clean-stg-db.SQL) is a SQL script used to clean and scramble staging data after copying the production database.

This directory contains the Dockerfile used to build the main Codebloom image, which is then uploaded to [hub.docker.com/r/patinanetwork/codebloom](https://hub.docker.com/r/patinanetwork/codebloom).

The image is then deployed to Patina's Kubernetes cluster. [Patina-Network/k8s-manifests](https://github.com/Patina-Network/k8s-manifests)

There is a Bun Shell script which helps us manage the workflow for deployments across production and staging, which can be found at [`.github/scripts/redeploy/index.ts`](../.github/scripts/redeploy/index.ts).