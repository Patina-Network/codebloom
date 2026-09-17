// each COMMAND must accept certain input arguments or it will fail.
// NOTE: YOU MUST NAME THE WORKFLOW FILE `${name}-command.yml`
//
// https://github.com/peter-evans/slash-command-dispatch/blob/main/docs/workflow-dispatch.md
export const COMMANDS = ["migrate", "ai", "copy"] as const;
