# ai-shared-develop Module

The `ai-shared-develop` module is the correct location to place shared agentic files such as `AGENTS.md`, shared prompts, and shared instructions that are used when developing Percussion CMS.

## Purpose

This module is not intended to be shipped with the product and should not contain any production code or resources. It is purely for development purposes to provide a common location for AI agent instructions and resources that are used across multiple modules and multiple agentic providers (e.g., Copilot, Claude, Gemini, Kilo Code, etc.) during development.

## Guidelines

- Place shared agentic instructions, skills,prompts, and resources here that are intended to be used by multiple modules during development.
- Do not place any production code, resources, or documentation here. This module is not intended to be included in the final product and should not contain anything that is needed at runtime or for end users.
- Do not place module specific instructions or prompts here. Module specific agentic instructions and prompts should be placed in the respective module's directory (e.g., `modules/perc-jetty/AGENTS.md` for Jetty module specific instructions).
- Use clear and descriptive naming for any files placed here to indicate their purpose and intended usage.
