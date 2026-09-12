# Prerequisites

The following general software needs to be installed on your local machine:

1. `JDK 25` - We use `openjdk`, but feel free to use `coretto` or any other distribution if you would like.
1. `maven` - Package manager to manage all our Java dependencies
1. `just` - The runner for `Justfiles`, which we use to consolidate our run commands.
1. `dotenvx` - Used to load environment variables from the root `.env` file.
1. `node` - Javascript runtime to run our frontend TypeScript code.
1. `corepack` - A package manager for package managers (???) to help us set a consistent `pnpm` version across all devs.
1. `pnpm@10.24.0` - Package manager that works faster than the default npm package manager.

## MacOS

The following instructions are using `homebrew` ([install instructions here](https://brew.sh/)), but it is not a requirement; you can follow along by installing all packages manually (though we would recommend against it).

1. Install `openjdk@25` (aliased to `openjdk`):

    ```bash
    brew install openjdk
    ```

1. Install `maven`:

    ```bash
    brew install maven
    ```

1. Install `node`:

    ```
    brew install node
    ```

1. You must then setup `corepack`. You can follow the instructions [here](https://github.com/nodejs/corepack?tab=readme-ov-file#how-to-install) under `Install Corepack using npm` on how to install and setup `corepack`. Once setup, simply enable `pnpm` on `corepack` like so:

    ```
    corepack enable pnpm
    ```

1. Install `dotenvx`:

    ```bash
    brew install dotenvx/brew/dotenvx
    ```

1. Install `just`:

    ```bash
    brew install just
    ```

## Windows

Unfortunately, I don't have a Windows machine that I develop on anymore, so I am unable to provide solid instructions for setup. However, you should be able to follow the exact same directions for [MacOS](#macos) but with `WinGet`/`Scoopy` or manually installing each software.

# IDE Integration

## VSCode

> **NOTE**: If you open the codebloom repository in VSCode, it will prompt you to install recommended extensions to the workspace, which will include everything below.
> <br /><img width="470" height="135" alt="image" src="https://github.com/user-attachments/assets/c017f866-e7ee-4f02-b978-32ebe318db2f" />

You need to install the following plugins:

1. **EditorConfig** - Applies consistent spacing width and type across all editors
1. **Checkstyle for Java** - Java static analyzer
1. **Prettier** - Javascript formatter
    - Helps maintain consistent styling
    - Configure format on save [following these instructions](https://stackoverflow.com/questions/39494277/how-do-you-format-code-on-save-in-vs-code)
1. **ESLint** - Javascript linter
    - Integrates with your project's ESLint configuration
1. **Babel JavaScript** - Improves JSX syntax highlighting
1. **Docker** - Provides Dockerfile IntelliSense
1. **DotENV** - `.env` file syntax highlighting
1. **Prettier Typescript Errors**: Simplifies complex TypeScript error messages
1. **Extension Pack for Java**
    - Includes debuggers, formatters, and managers
    - Supports format on save
1. **Spring Boot Extension Pack** - Additional Spring Boot-specific tooling
1. **Tailwind CSS IntelliSense** - Provides intelligent suggestions for Tailwind classes
1. **XML by RedHat**
    - Official XML language support and formatter
    - Important for editing Java XML files like pom.xml

[.vscode/](https://github.com/tahminator/codebloom/tree/main/.vscode) defines some workspace defaults to help make development consistent.

## IntelliJ

You need to install the following plugins:

1. **EditorConfig** - Applies consistent spacing width and type across all editors
1. **Checkstyle-IDEA** - Java static analyzer

The Eclipse formatter and everything else should just work out of the box. You may need to install some plugins for TypeScript support, including Prettier, ESLint, Babel, Prettier, Tailwind, and more.

## Neovim

> **NOTE**: This may vary greatly by the current configuration of Neovim, but the following setup _should_ work out the box using `LazyVim`.

You need to install the following plugins:

1. **nvim-jdtls** - LSP for Java in Neovim (Install as a plugin, not `Mason`)
    - `nvim-jdtls` may require some additional configuration. If it helps, my current config can be found [here](https://github.com/tahminator/dotfiles/blob/main/.config/nvim/lua/plugins/jdtls.lua)
1. **none-ls** - Provide a code bridge to formatting & LSP diagonostics. (Specifically used for Checkstyle formatting)
1. **vtsls** - LSP for TypeScript in Neovim (can install through `Mason`)
1. **eslint-lsp** - LSP Protocol for ESLint (can install through `Mason`)
1. **tailwindcss-language-server** - LSP for Tailwind (can install through `Mason`)
1. **json-lsp** - (Optional) LSP for JSON (can install through `Mason`)
1. **dockerfile-language-server** - (Optional) LSP for Dockerfile (can install through `Mason`)

There is a [.lazy.lua](https://github.com/tahminator/codebloom/tree/main/.lazy.lua) file in the root directory that will apply some default, but only for `LazyVim`. Of course, you can replicate the behavior in your own distribution (and create a pull request with the changes).

# Database

## Postgres

We currently use Postgres 16 locally to match production (but some members of the team have used 17 locally with no issues).

### Installation

You can feel free to download Postgres however you want, but the way we have all chose to set it up is with [Postgres.app](https://postgresapp.com) on MacOS.

#### MacOS + Postgres.app

1. Install `postgresapp` through `homebrew` (or the website if you would prefer to do so)

    ```bash
    brew install --cask postgres-unofficial
    ```

2. Open `Postgres.app` and click "Initialize" to create a new server. You are now ready to go!

    > - For `Postgres.app` instances, the password you enter to access the database doesn't matter
    > - On first connection attempt, Postgres.app will prompt you to trust the specific program
    > - After allowing access once, it won't ask again for that program

3. Configure your `$PATH` to use the included command line tools (optional):

    ```bash
    sudo mkdir -p /etc/paths.d &&
    echo /Applications/Postgres.app/Contents/Versions/latest/bin | sudo tee /etc/paths.d/postgresapp
    ```

#### Other

You may install it with Docker, or directly through [postgresql.com](https://postgresql.com) if you would like. While there are no directions I can directly offer to you, there are some very good tutorials online on how to do so.

If you would like to use Docker, I can refer you to Patina's documentation for setting up Docker which you can find [here](https://github.com/arklian/patina/blob/main/docs/postgres-on-docker.md)

### Viewer

You can feel free to use any viewer you want, but we would recommend [DataGrip](https://www.jetbrains.com/datagrip/) which is free for all non-commercial use.

## Redis

We currently use Redis 7 in production, but feel free to use a different version (although you may run into some compatibility issues).

### Installation

You can feel free to download Redis however you want, but the way we have all chose to set it up is with Homebrew.

#### Homebrew

1. Install `redis` through `homebrew`

    ```bash
    brew install redis
    ```

2. To launch the redis server, you can simply run the following command to run the server:

    ```bash
    redis-server
    ```

    > **NOTE**: This will launch the server in the foreground (when the terminal is closed, the Redis server will be closed).
    >
    > If you would like to automatically launch the server in the background (as soon as your machine is on and you are logged in), you can run this command:

    ```bash
     brew services start redis
    ```

3. The default Redis URI for Homebrew installations is simply:

    ```bash
    redis://localhost:6379/0
    ```

    > **NOTE**: By default, Redis has 16 databases (the number after the slash) from 0-15.

### Viewer

You can feel free to use any viewer you want, but we would recommend [DataGrip](https://www.jetbrains.com/datagrip/) which is free for all non-commercial use.

# Secrets

You can speed up the setup process by making a copy of `example.env` to `.env`.
You will also find explanations and documentation about how to source the value for each key.

If there is a key specific to an environment (such as `CI` or `staging` environment), please consult the tech docs within the `CI` group.
