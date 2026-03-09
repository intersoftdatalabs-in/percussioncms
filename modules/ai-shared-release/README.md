# ai-shared-release Module

The `ai-shared-release` module is the correct location to place shared agentic files such as `AGENTS.md`, shared prompts, and shared instructions that are intended to be shipped with Percussion CMS for end users to utilize.

## Purpose

This module is intended for production, release, instructions, skills, prompts, chatmodes, and tools for use by end users and administrators of Percussion CMS.

## Guidelines

- When adding resources, add the corresponding end users documentation to the site/resources/markdown and site.xml.
- Place shared agentic instructions, skills, prompts, and resources here that are intended to be used by end users and administrators of Percussion CMS.
- Do not place any development-only code, resources, or documentation here. This module is intended to be included in the final product and should only contain files that are needed at runtime or for end users.  Use `ai-shared-develop` for development-only resources.
