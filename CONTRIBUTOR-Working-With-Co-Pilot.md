## Agent Mode

In Agent Mode, Co-Pilot can execute commands and interact with the system on your behalf. This mode is useful for tasks that require automation or when you want Co-Pilot to handle repetitive actions.

- Use /explain on a module while you have the pom.xml file open to get coppilot to explain the module and its dependencies.
- Use the latest Claude for creating plans, then use the latest GPT model for executing the plan.

### Context is Everything

When working in Agent mode make sure that the following files are in the IDE plugins Context:

1. .github/copilot-instructions.md
2. /modulepath/module/README.md
3. Folder: /modulepath/module or /modulepath/module/src

Close all other open files in the IDE so co-pilot does not parse the open files and use tokens that aren't needed for the current chuck of work.

### Key Prompts for Agent Mode:

### Handling Co-Pilot Agent Mode Errors

Co-pilot works great, but the code base is huge, and it can't handle the size of it well.

Even with those instructions, you may still encounter errors like the following:

#### "Sorry, an error occurred while generating a response"

Basically, this means that the code you asked Co-Pilot to generate is too large for it to handle in one go. We reached the maximum limit for a Session.

The only real way around this is to start a new Agent session and ask Co-Pilot to Continue where it left off. You can do this by providing the last few lines of code that Co-Pilot generated, and then asking it to continue from there.

For example, say your prompt was:

"Continue refactoring of the system/services modules to Java 17 following the guidelines starting with the contentmgr package"

and then you got the error "Sorry, an error occurred while generating a response".

Keep or Undo any Pending Changes.

Then, start a new Agent session and give it the following prompt:

"Continue refactoring of the system/services modules to Java 17 following the guidelines resuming your refactoring of the contentmgr package"

It should then continue where it left off.

#### "Oops, maximum tool attempts reached. You can type "continue" to proceed or rephrase your request"

This error seems to be IDE / plugin related. No work-around is known currently.  Just type 'continue' -

it seems to help to close all class files that are open in the IDE.
It also seems to help to make sure that the context still had the instructions and the class it failed on.

Example:
'continue please'
