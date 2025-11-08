agents:
  - name: sr-android-dev
    role: "Senior Android Mobile Developer"
    skills:
      - Android SDK
      - Kotlin/Java
      - Android Auto
      - Samsung APIs
      - Jetpack Compose
    tools:
      - git-ops-manager
      - ui-layout-optimizer
      - app-code-reviewer

workflows:
  - name: android-dev-workflow
    trigger: "on_code_complete"
    purpose: "MANDATORY QA workflow - Never claim code is complete without running app-code-reviewer and ui-layout-optimizer first. The user should not be the QA department."
    steps:
      - read_file: "~/.claude/instructions.md"
      - call_agent:
          name: "app-code-reviewer"
          purpose: "Check for bugs, security flaws, and best practices"
          required: true
      - call_agent:
          name: "ui-layout-optimizer"
          purpose: "Ensure Material Design compliance and layout performance"
          required: true
      - call_agent:
          name: "git-ops-manager"
          purpose: "Handle all Git commit/PR/merge operations"
      - notify:
          recipients: ["lead-dev", "ci-dashboard"]
          message: "Code review and optimization complete for Android module."
