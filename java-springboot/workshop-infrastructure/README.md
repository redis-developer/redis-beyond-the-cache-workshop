# Workshop Infrastructure Module

## 🎯 Purpose

This is a **shared library module** that provides reusable workshop infrastructure for all Redis workshop modules. It enables in-browser code editing functionality that can be used across multiple workshops without duplication.

## ✨ Key Features

- **100% Reusable** - Works with any workshop module
- **Configuration-Driven** - No hardcoded file paths or content
- **Dependency Injection** - Clean architecture using Spring DI
- **Secure** - Only configured files can be edited
- **Multi-Language Support** - Syntax highlighting for 10+ languages
- **Zero Duplication** - Write once, use everywhere

## 📦 What's Included

### `WorkshopConfig.java` (Interface)
Generic configuration interface that defines:
- `getEditableFiles()` - Map of files that can be edited
- `getOriginalContent(fileName)` - Original content for lab reset
- `getBasePath()` - Base path calculation (with smart defaults)
- `getModuleName()` - Module directory name
- `getLanguage(fileName)` - Syntax highlighting language
- `getWorkshopTitle()` - Workshop title for display
- `getWorkshopDescription()` - Workshop description

### `EditorController.java` (Controller)
Generic Spring MVC controller that provides:
- `/editor` - Main editor page with Monaco Editor
- `/editor/file/{fileName}` - Read file content (GET)
- `/editor/file/{fileName}` - Save file content (POST)
- `/editor/restore` - Restore all files to original state (POST)

### `editor.html` (Template)
Thymeleaf template with:
- Full-screen split-pane layout
- Monaco Editor (VS Code's editor engine)
- File tabs for switching between files
- Workshop instructions with auto-fix buttons
- Hint tooltips for each step
- Save and restore functionality

## 🚀 How to Use in a Workshop Module

### Step 1: Add Dependency

In your workshop module's `build.gradle.kts`:

```kotlin
dependencies {
    // Shared workshop infrastructure
    implementation(project(":workshop-infrastructure"))
    
    // Your other dependencies...
}
```

### Step 2: Create Workshop Configuration

Create a class implementing `WorkshopConfig`:

```java
package com.redis.workshop.yourmodule.infrastructure;

import com.redis.workshop.infrastructure.WorkshopConfig;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class YourWorkshopConfig implements WorkshopConfig {
    
    private static final Map<String, String> EDITABLE_FILES = Map.of(
        "config.yml", "src/main/resources/config.yml",
        "AppConfig.java", "src/main/java/com/redis/workshop/yourmodule/config/AppConfig.java"
    );
    
    private static final Map<String, String> ORIGINAL_CONTENTS = Map.of(
        "config.yml", """
            # Your original YAML content
            """,
        "AppConfig.java", """
            // Your original Java content
            """
    );
    
    @Override
    public Map<String, String> getEditableFiles() {
        return EDITABLE_FILES;
    }
    
    @Override
    public String getOriginalContent(String fileName) {
        return ORIGINAL_CONTENTS.get(fileName);
    }
    
    @Override
    public String getModuleName() {
        return "2_your_module_name";
    }
    
    @Override
    public String getWorkshopTitle() {
        return "Your Workshop Title";
    }
    
    @Override
    public String getWorkshopDescription() {
        return "Learn about Redis feature X";
    }
}
```

### Step 3: Link to Editor

In your workshop's HTML template:

```html
<a href="/editor">Open Code Editor →</a>
```

### Step 4: That's It!

The infrastructure automatically:
- Detects your configuration via Spring component scanning
- Injects it into the `EditorController`
- Provides full editor functionality
- Handles file reading, writing, and restoration

## 📋 Supported Languages

Automatic syntax highlighting for:
- Java (`.java`)
- Kotlin (`.kts`, `.kt`)
- Properties (`.properties`)
- YAML (`.yaml`, `.yml`)
- JSON (`.json`)
- XML (`.xml`)
- JavaScript (`.js`)
- TypeScript (`.ts`)
- Python (`.py`)
- Go (`.go`)

## 🏗️ Architecture

```
┌──────────────────────────────────────┐
│   workshop-infrastructure module    │
│   (Shared Library)                   │
│                                      │
│   - WorkshopConfig (interface)       │
│   - EditorController (controller)    │
│   - editor.html (template)           │
└──────────────┬───────────────────────┘
               │
               │ Used by (dependency)
               │
    ┌──────────┴──────────┬─────────────────┐
    │                     │                  │
    ▼                     ▼                  ▼
┌─────────────┐   ┌─────────────┐   ┌─────────────┐
│ 1_session   │   │ 2_caching   │   │ 3_pubsub    │
│ _management │   │             │   │             │
│             │   │             │   │             │
│ Implements  │   │ Implements  │   │ Implements  │
│ Workshop    │   │ Workshop    │   │ Workshop    │
│ Config      │   │ Config      │   │ Config      │
└─────────────┘   └─────────────┘   └─────────────┘
```

## 🎓 Benefits

### For Workshop Creators:
- **Write once, use everywhere** - No code duplication
- **Consistent experience** - Same editor across all workshops
- **Easy maintenance** - Update infrastructure in one place
- **Fast development** - New workshop in minutes

### For Students:
- **Consistent interface** - Same editor for all workshops
- **No IDE required** - Edit code directly in browser
- **Guided learning** - Auto-fix buttons with hints
- **Safe experimentation** - Easy lab reset

## 📝 Example: Session Management Workshop

See `1_session_management` module for a complete example:
- `SessionManagementWorkshopConfig.java` - Configuration implementation
- Links to `/editor` from `welcome.html`
- Provides 3 editable files with auto-fix functionality

## 🔧 Technical Details

- **Spring Boot 3.5.10** - Modern Spring framework
- **Monaco Editor** - VS Code's editor engine (via CDN)
- **Thymeleaf** - Server-side templating
- **REST API** - JSON-based file operations
- **Component Scanning** - Automatic configuration detection
- **Dependency Injection** - Clean, testable architecture

