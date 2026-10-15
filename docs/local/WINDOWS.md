# Windows setup

Run the commands in PowerShell unless a step says otherwise. After each `winget install`, open a new PowerShell window so the new programs are on your Path.

## Do not use WSL

Install and run Codebloom in Windows itself. Do not install WSL, do not open an Ubuntu window, and do not install Docker Desktop. Docker Desktop turns WSL on. Redis on this setup is Memurai, which is a Windows service.

## Install

```powershell
winget install --id Git.Git -e
winget install --id Casey.Just -e
winget install --id Microsoft.OpenJDK.25 -e
winget install --id OpenJS.NodeJS.LTS -e
winget install --id astral-sh.uv -e
winget install --id PostgreSQL.PostgreSQL.16 -e
winget install --id Memurai.MemuraiDeveloper -e
```

If `Microsoft.OpenJDK.25` fails to install, use `EclipseAdoptium.Temurin.25.JDK` instead.

What each one is for:

1. Git for Windows. You need it to clone the repo. It also provides `bash`, which `just` uses to start Maven. That `bash` is part of Git for Windows.
1. `just`. This runs the project commands.
1. JDK 25. The backend does not start on an older Java.
1. Node.js. The frontend and `dotenvx` need it.
1. `uv`. The LeetCode auth service is Python, and `uv` downloads Python 3.14 for you.
1. PostgreSQL 16. The installer asks you to choose a password for the `postgres` user. Write that password down. Leave the port at `5432`.
1. Memurai Developer. This is Redis for Windows. It listens on port `6379`. The developer build stops itself after 10 days. Start it again with `Start-Service Memurai`.

In a new PowerShell window:

```powershell
git config --global core.autocrlf input
git config --global core.longpaths true

corepack enable
corepack prepare pnpm@10.24.0 --activate
npm install -g @dotenvx/dotenvx
```

`core.autocrlf input` keeps files as LF. Spotless rejects Java files that Windows has rewritten with CRLF.

Confirm the tools. `java -version` must say 25. `pnpm -v` must say `10.24.0`.

```powershell
java -version
node -v
pnpm -v
dotenvx --version
uv --version
psql --version
bash --version
just --version
```

If `psql` is not found, add `C:\Program Files\PostgreSQL\16\bin` to your user Path, then open a new PowerShell.

If `bash` is not found, add `C:\Program Files\Git\bin` to your user Path, then open a new PowerShell. `git` can be available while `bash` is not. `just` needs `bash`.

If `java` is not found, set `JAVA_HOME` to the JDK 25 folder (for the Microsoft build that is `C:\Program Files\Microsoft\jdk-25`) and add `%JAVA_HOME%\bin` to Path.

## PostgreSQL must be running

PostgreSQL must be running as a background service before you can connect to it.

1. Press Windows Key + R, type `services.msc`, and press Enter.
1. Scroll down and look for the `postgresql-x64-<version>` service. For the PostgreSQL 16 installer that name is `postgresql-x64-16`.
1. Look at the Status column.
   - If it is blank, right-click the service and click Start.
   - If it says Running, you are ready to connect.

Memurai should also be Running in that same window. If its Status is blank, right-click Memurai and click Start.

## Clone and create the database

```powershell
git clone https://github.com/Patina-Network/codebloom.git
cd codebloom
psql -h localhost -p 5432 -U postgres -d postgres -c "CREATE DATABASE codebloom;"
```

`psql` asks for a password. Type the password you chose in the PostgreSQL installer.

## Common errors

**`"}" was unexpected at this time.`** Command Prompt tried to run the `mvnw` shell script. `bash` is not the program `just` found, or this checkout is from before the recipes called `bash ./mvnw`. Confirm `bash --version` works in a new PowerShell. If it does not, add `C:\Program Files\Git\bin` to Path.

**`could not load .../pg_hba.conf`.** Postgres refused the file, usually after it was edited and saved from Notepad. Restore the installer contents (the `host` lines use `scram-sha-256`), save it as ANSI or UTF-8, then restart `postgresql-x64-16` from `services.msc`.
