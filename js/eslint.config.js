import js from "@eslint/js";
import noRelativeImportPaths from "eslint-plugin-no-relative-import-paths";
import perfectionist from "eslint-plugin-perfectionist";
import react from "eslint-plugin-react";
import reactHooks from "eslint-plugin-react-hooks";
import reactRefresh from "eslint-plugin-react-refresh";
import globals from "globals";
import tseslint from "typescript-eslint";

export default tseslint.config(
  {
    plugins: {
      perfectionist,
    },
    rules: {
      // perfectionist v5 added `^@/.+` to the default internalPattern, which splits
      // `@/` imports into a separate group. Keep the repo's existing convention of
      // sorting `@/` imports alphabetically alongside external packages.
      "perfectionist/sort-imports": [
        "error",
        { internalPattern: ["^~/.+", "^#.+"] },
      ],
    },
  },
  { ignores: ["dist", "coverage"] },
  {
    extends: [js.configs.recommended, ...tseslint.configs.recommended],
    files: ["**/*.{ts,tsx}"],
    languageOptions: {
      ecmaVersion: 2020,
      globals: globals.browser,
    },
    plugins: {
      "react-hooks": reactHooks,
      "react-refresh": reactRefresh,
      "no-relative-import-paths": noRelativeImportPaths,
      react: react,
    },
    settings: {
      "import/resolver": {
        alias: {
          map: [["@", "./src"]],
          extensions: [".ts", ".tsx", ".js", ".jsx"],
        },
      },
    },
    rules: {
      ...reactHooks.configs.recommended.rules,
      "react-hooks/exhaustive-deps": "error",
      "no-relative-import-paths/no-relative-import-paths": [
        "error",
        { allowSameFolder: false, rootDir: "src", prefix: "@" },
      ],
      "react/jsx-newline": [
        "error",
        {
          prevent: true,
          allowMultilines: false,
        },
      ],
      "react/jsx-key": [2, { checkFragmentShorthand: true }],
      "react-refresh/only-export-components": [
        "warn",
        { allowConstantExport: true },
      ],
      "@typescript-eslint/no-namespace": ["off"],
      "@typescript-eslint/no-non-null-assertion": ["error"],
      "@typescript-eslint/no-unused-vars": [
        "error",
        {
          argsIgnorePattern: "^_",
          varsIgnorePattern: "^_",
          caughtErrorsIgnorePattern: "^_",
        },
      ],
    },
  },
);
