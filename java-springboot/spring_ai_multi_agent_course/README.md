# Spring AI Multi Agent Course

This directory groups the workshop modules for the Spring AI Multi Agent course.

The course directory is only an organizer. Runnable workshops live in the child backend and frontend module pairs.

## Workshop Structure Contract

Spring AI course submodules should mirror the distributed session workshop structure:

- Use `java-springboot/1_session_management_frontend/src/main/resources/workshop-content/views/*.yaml` as the content shape reference.
- Organize learner pages as numbered stages with `STAGE N: ...` titles.
- Put page-to-page navigation labels in each YAML file under `navigation`.
- Render instructions through the shared content renderer inside the workshop view, then add the same previous and next stage arrow navigation used by distributed session management.
- Use the distributed session frontend shell pattern: instructions on one side, the learner app in `WorkshopAppFrame` on the other side, Redis Insight replacing that panel in place, and the code editor embedded with `WorkshopCodeEditorShell`.
- Keep the parent course directory as organization only. The runnable workshops stay in the child backend and frontend module pairs.
