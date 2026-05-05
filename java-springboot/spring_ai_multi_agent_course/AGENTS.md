# Spring AI Multi Agent Course Instructions

1. Treat this directory as an organizing parent only.
2. Keep runnable workshops in child backend and frontend module pairs.
3. For every child workshop, mirror distributed session management.
4. Use `java-springboot/1_session_management_frontend/src/main/resources/workshop-content/views/*.yaml` as the YAML content reference.
5. Name learner pages as numbered stages with `STAGE N: ...` titles.
6. Put previous and next labels in each stage YAML `navigation` block.
7. Render workshop instructions with `WorkshopContentRenderer` inside the Vue view and include previous and next arrow buttons like `SessionHome.vue`.
8. Do not replace the stage arrows with numeric page controls.
9. Use the same frontend shell split as distributed session management: instructions on the left and `WorkshopAppFrame` on the right.
10. Let the right panel switch in place between the learner app and Redis Insight with `WorkshopToolFrame`.
11. Use `WorkshopCodeEditorShell` for the code editor route so VS Code is embedded next to editor instructions.
12. Keep hands on edits limited to files that teach the Spring AI concept for that stage.
