# Agent Instructions

## Task Tracking

When starting any non-trivial task, create a task markdown file under `tasks/` before making implementation changes.

Task files must:

- Use a clear kebab-case name that includes the related GitHub issue number when available, for example `tasks/123-add-word-info-endpoint.md`.
- Contain the full internal task information, including goal, scope, constraints, implementation notes, decisions, open questions, progress, verification, and links to related files or commits.
- Preserve deeper technical details in the task file rather than the GitHub issue.

Each task must also have a corresponding GitHub issue.

GitHub issues must:

- Store the user-facing task summary, expected outcome, acceptance criteria, and current status.
- Avoid deep technical implementation details unless they are necessary for user-facing context.
- Link back to the task markdown file when possible.

Update both the task markdown file and the corresponding GitHub issue after every meaningful decision, scope change, implementation change, blocker, or verification result.

Do not treat a task as complete until:

- The implementation work is finished.
- The task markdown file reflects the final technical state.
- The GitHub issue reflects the final user-facing state.
- Relevant verification has been run or the reason it could not be run is documented in both places.
