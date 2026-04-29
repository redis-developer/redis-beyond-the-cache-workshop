#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

python3 - "$ROOT_DIR" <<'PY'
from __future__ import annotations

import re
import shutil
import stat
import subprocess
import sys
import tempfile
from pathlib import Path


ROOT = Path(sys.argv[1]).resolve()
JAVA_DIR = ROOT / "java-springboot"
BUILD_GRADLE_PATH = JAVA_DIR / "build.gradle.kts"
REGISTRY_PATH = ROOT / "workshops.yaml"
SETTINGS_PATH = JAVA_DIR / "settings.gradle.kts"
ALLOWED_INFRASTRUCTURE_DEPENDENCIES = {"redis", "postgres"}
ALLOWED_REDIS_FLAVORS = {"standard", "stack"}
ALLOWED_FRONTEND_PREBUILD_VALUES = {"true", "false"}
ALLOWED_RELEASE_IMAGE_ROLES = {"frontend", "backend", "combined", "init"}
ALLOWED_CONTENT_PAGE_TYPES = {"narrative", "stage-flow", "editor"}
CONTENT_ROOT_RELATIVE = Path("src/main/resources/workshop-content")
TEXT_SOURCE_SUFFIXES = {".css", ".html", ".js", ".jsx", ".ts", ".tsx", ".vue"}
LEGACY_NONSTANDARD_CONTENT_LAYOUT_IDS = {"2_full_text_search"}
APP_SOURCE_FORBIDDEN_MARKERS = [
    (
        "$refs.layout?.workshopHubUrl",
        "must not read removed WorkshopEditorLayout internals; use getWorkshopHubUrl instead",
    ),
    (
        "/api/editor/restore",
        "must not call editor restore transport directly; restore is shell-owned",
    ),
]
SHARED_EDITOR_LAYOUT_LOGO_MARKER = "@/assets/logo/small.png"

REGISTRY_REQUIRED_FIELDS = [
    "id",
    "title",
    "description",
    "difficulty",
    "estimatedMinutes",
    "serviceName",
    "url",
    "dockerfile",
    "frontendServiceName",
    "frontendDockerfile",
    "backendServiceName",
    "backendDockerfile",
    "infrastructureDependencies",
    "redisFlavor",
    "frontendPrebuild",
    "topics",
    "releases",
]

RELEASE_REQUIRED_FIELDS = [
    "releaseId",
    "releaseVersion",
    "mode",
    "defaultForWorkshop",
    "enabled",
    "environments",
    "images",
    "resourceClass",
    "sessionTtlMinutes",
    "mutableDependencies",
]

BACKEND_REQUIRED_FILES = [
    "README.md",
    "Dockerfile",
    "build.gradle.kts",
    "settings.gradle.kts",
    "src/main/resources/application.properties",
]

FRONTEND_REQUIRED_FILES = [
    "Dockerfile",
    "build.gradle.kts",
    "settings.gradle.kts",
    "frontend/package.json",
    "frontend/vue.config.js",
    "frontend/public/index.html",
    "frontend/src/main.js",
    "frontend/src/router/index.js",
    "src/main/resources/application.properties",
    "src/main/resources/workshop-manifest.yaml",
]

SHELL_REQUIRED_MAIN_MARKERS = [
    "__webpack_public_path__",
    "workshop-frontend-shared/src/styles/tokens.css",
    "workshop-frontend-shared/src/styles/dark-theme.css",
    "workshop-frontend-shared/src/styles/components.css",
]

SHELL_REQUIRED_ROUTER_MARKERS = [
    "createWebHistory(getBasePath() || '/')",
]


def fail(message: str) -> None:
    print(message, file=sys.stderr)
    raise SystemExit(1)


def parse_registry(path: Path) -> tuple[int | None, list[dict[str, object]]]:
    lines = path.read_text(encoding="utf-8").splitlines()
    version = None
    workshops: list[dict[str, object]] = []
    current: dict[str, object] | None = None
    active_section: str | None = None
    current_release: dict[str, object] | None = None
    active_release_section: str | None = None

    for line_number, line in enumerate(lines, start=1):
        version_match = re.match(r"^version:\s*(\d+)\s*$", line)
        if version_match:
            version = int(version_match.group(1))
            continue

        workshop_match = re.match(r"^  - id:\s*(.+?)\s*$", line)
        if workshop_match:
            if current_release is not None and current is not None:
                cast_releases = current["releases"]
                assert isinstance(cast_releases, list)
                cast_releases.append(current_release)
                current_release = None
            if current is not None:
                workshops.append(current)
            current = {
                "id": workshop_match.group(1).strip(),
                "_line": line_number,
                "_keys": {"id"},
                "_sections": {},
                "releases": [],
            }
            active_section = None
            active_release_section = None
            continue

        if current is None:
            continue

        release_match = re.match(r"^      - releaseId:\s*(.+?)\s*$", line)
        if release_match and active_section == "releases":
            if current_release is not None:
                cast_releases = current["releases"]
                assert isinstance(cast_releases, list)
                cast_releases.append(current_release)
            current_release = {
                "releaseId": release_match.group(1).strip(),
                "_line": line_number,
                "_keys": {"releaseId"},
                "_sections": {},
                "images": {},
            }
            active_release_section = None
            continue

        release_field_match = re.match(r"^        ([A-Za-z][A-Za-z0-9]*)\s*:\s*(.*)$", line)
        if release_field_match and current_release is not None:
            key = release_field_match.group(1)
            value = release_field_match.group(2).strip()
            current_release[key] = value
            cast_keys = current_release["_keys"]
            assert isinstance(cast_keys, set)
            cast_keys.add(key)
            if value == "":
                active_release_section = key
                cast_sections = current_release["_sections"]
                assert isinstance(cast_sections, dict)
                cast_sections.setdefault(key, [])
                if key == "images":
                    current_release["images"] = {}
            else:
                active_release_section = None
            continue

        release_list_item_match = re.match(r"^          -\s*(.+?)\s*$", line)
        if release_list_item_match and current_release is not None and active_release_section is not None:
            cast_sections = current_release["_sections"]
            assert isinstance(cast_sections, dict)
            cast_sections.setdefault(active_release_section, []).append(
                release_list_item_match.group(1).strip()
            )
            continue

        image_match = re.match(r"^          ([A-Za-z][A-Za-z0-9]*)\s*:\s*(.+?)\s*$", line)
        if image_match and current_release is not None and active_release_section == "images":
            images = current_release["images"]
            assert isinstance(images, dict)
            images[image_match.group(1)] = image_match.group(2).strip()
            continue

        field_match = re.match(r"^    ([A-Za-z][A-Za-z0-9]*)\s*:\s*(.*)$", line)
        if field_match:
            if current_release is not None:
                cast_releases = current["releases"]
                assert isinstance(cast_releases, list)
                cast_releases.append(current_release)
                current_release = None
                active_release_section = None
            key = field_match.group(1)
            value = field_match.group(2).strip()
            current[key] = value
            cast_keys = current["_keys"]
            assert isinstance(cast_keys, set)
            cast_keys.add(key)
            if value == "":
                active_section = key
                cast_sections = current["_sections"]
                assert isinstance(cast_sections, dict)
                cast_sections.setdefault(key, [])
                if key == "releases":
                    current["releases"] = []
            else:
                active_section = None
            continue

        list_item_match = re.match(r"^      -\s*(.+?)\s*$", line)
        if list_item_match and active_section is not None:
            cast_sections = current["_sections"]
            assert isinstance(cast_sections, dict)
            cast_sections.setdefault(active_section, []).append(list_item_match.group(1).strip())
            continue

        if re.match(r"^    \S", line):
            active_section = None

    if current is not None:
        if current_release is not None:
            cast_releases = current["releases"]
            assert isinstance(cast_releases, list)
            cast_releases.append(current_release)
        workshops.append(current)

    return version, workshops


def ensure_file(path: Path, errors: list[str], label: str) -> None:
    if not path.is_file():
        errors.append(f"{label} is missing: {path.relative_to(ROOT)}")


def ensure_dir(path: Path, errors: list[str], label: str) -> None:
    if not path.is_dir():
        errors.append(f"{label} is missing: {path.relative_to(ROOT)}")


def parse_manifest(manifest_path: Path) -> dict[str, object]:
    module_name = None
    title = None
    editable_paths: list[str] = []
    reset_locations: list[str] = []

    for line in manifest_path.read_text(encoding="utf-8").splitlines():
        if module_name is None:
            module_match = re.match(r"^moduleName:\s*(.+?)\s*$", line)
            if module_match:
                module_name = module_match.group(1).strip()
                continue

        if title is None:
            title_match = re.match(r"^title:\s*(.+?)\s*$", line)
            if title_match:
                title = title_match.group(1).strip()
                continue

        path_match = re.match(r"^\s+path:\s*(.+?)\s*$", line)
        if path_match:
            editable_paths.append(path_match.group(1).strip())
            continue

        reset_match = re.match(r"^\s+resetContentLocation:\s*(.+?)\s*$", line)
        if reset_match:
            reset_locations.append(reset_match.group(1).strip())

    return {
        "module_name": module_name,
        "title": title,
        "editable_paths": editable_paths,
        "reset_locations": reset_locations,
    }


def normalize_scalar(value: str) -> str:
    normalized = value.strip()
    if len(normalized) >= 2 and normalized[0] == normalized[-1] and normalized[0] in {'"', "'"}:
        return normalized[1:-1]
    return normalized


def display_path(path: Path, relative_to: Path) -> str:
    try:
        return path.relative_to(relative_to).as_posix()
    except ValueError:
        return path.as_posix()


def iter_text_source_files(root: Path):
    if not root.is_dir():
        return

    for path in sorted(root.rglob("*")):
        if path.is_file() and path.suffix in TEXT_SOURCE_SUFFIXES:
            yield path


def collect_shell_app_boundary_errors() -> list[str]:
    errors: list[str] = []

    for app_source_root in sorted(JAVA_DIR.glob("*_frontend/frontend/src")):
        for source_path in iter_text_source_files(app_source_root):
            source_text = source_path.read_text(encoding="utf-8")
            for marker, reason in APP_SOURCE_FORBIDDEN_MARKERS:
                for line_number, line in enumerate(source_text.splitlines(), start=1):
                    if marker in line:
                        errors.append(
                            f"{display_path(source_path, ROOT)}:{line_number} {reason} "
                            f"(found {marker!r})"
                        )

    editor_layout_path = ROOT / "workshop-frontend-shared/src/components/WorkshopEditorLayout.vue"
    if editor_layout_path.is_file():
        editor_layout_text = editor_layout_path.read_text(encoding="utf-8")
        if SHARED_EDITOR_LAYOUT_LOGO_MARKER in editor_layout_text:
            errors.append(
                "workshop-frontend-shared/src/components/WorkshopEditorLayout.vue "
                f"must not reference app logo asset {SHARED_EDITOR_LAYOUT_LOGO_MARKER!r}; "
                "use logo props or slots instead"
            )

    return errors


def parse_workshop_content_manifest(manifest_path: Path) -> dict[str, object]:
    schema_version = None
    workshop_id = None
    views: list[dict[str, object]] = []
    current: dict[str, object] | None = None

    for line_number, line in enumerate(manifest_path.read_text(encoding="utf-8").splitlines(), start=1):
        schema_match = re.match(r"^schemaVersion:\s*(\d+)\s*$", line)
        if schema_match:
            schema_version = int(schema_match.group(1))
            continue

        workshop_match = re.match(r"^workshopId:\s*(.+?)\s*$", line)
        if workshop_match:
            workshop_id = normalize_scalar(workshop_match.group(1))
            continue

        view_match = re.match(r"^  - viewId:\s*(.+?)\s*$", line)
        if view_match:
            if current is not None:
                views.append(current)
            current = {
                "viewId": normalize_scalar(view_match.group(1)),
                "_line": line_number,
            }
            continue

        if current is None:
            continue

        field_match = re.match(r"^    (route|pageType|file):\s*(.+?)\s*$", line)
        if field_match:
            current[field_match.group(1)] = normalize_scalar(field_match.group(2))

    if current is not None:
        views.append(current)

    return {
        "schema_version": schema_version,
        "workshop_id": workshop_id,
        "views": views,
    }


def parse_workshop_content_view(view_path: Path) -> dict[str, object]:
    schema_version = None
    view_id = None
    route = None
    page_type = None
    title = None
    slot = None
    summary = None
    has_summary = False
    stage_ids: list[str] = []
    section_ids: list[str] = []
    block_types: list[str] = []

    for line in view_path.read_text(encoding="utf-8").splitlines():
        schema_match = re.match(r"^schemaVersion:\s*(\d+)\s*$", line)
        if schema_match:
            schema_version = int(schema_match.group(1))
            continue

        field_match = re.match(r"^(viewId|route|pageType|title|slot):\s*(.+?)\s*$", line)
        if field_match:
            field_name = field_match.group(1)
            field_value = normalize_scalar(field_match.group(2))
            if field_name == "viewId":
                view_id = field_value
            elif field_name == "route":
                route = field_value
            elif field_name == "pageType":
                page_type = field_value
            elif field_name == "title":
                title = field_value
            elif field_name == "slot":
                slot = field_value
            continue

        summary_match = re.match(r"^summary:\s*(.*?)\s*$", line)
        if summary_match:
            has_summary = True
            summary = normalize_scalar(summary_match.group(1))
            continue

        stage_match = re.match(r"^  - stageId:\s*(.+?)\s*$", line)
        if stage_match:
            stage_ids.append(normalize_scalar(stage_match.group(1)))
            continue

        section_match = re.match(r"^  - sectionId:\s*(.+?)\s*$", line)
        if section_match:
            section_ids.append(normalize_scalar(section_match.group(1)))
            continue

        block_match = re.match(r"^\s+- type:\s*(.+?)\s*$", line)
        if block_match:
            block_types.append(normalize_scalar(block_match.group(1)))

    return {
        "schema_version": schema_version,
        "view_id": view_id,
        "route": route,
        "page_type": page_type,
        "title": title,
        "slot": slot,
        "summary": summary,
        "has_summary": has_summary,
        "stage_ids": stage_ids,
        "section_ids": section_ids,
        "block_types": block_types,
    }


def parse_vue_router_paths(router_path: Path) -> list[tuple[str, int]]:
    if not router_path.is_file():
        return []

    paths: list[tuple[str, int]] = []
    for line_number, line in enumerate(router_path.read_text(encoding="utf-8").splitlines(), start=1):
        path_match = re.search(r"\bpath\s*:\s*['\"]([^'\"]+)['\"]", line)
        if path_match:
            paths.append((path_match.group(1), line_number))

    return paths


def collect_duplicate_router_path_errors(router_path: Path, module_label: str, *, relative_to: Path) -> list[str]:
    errors: list[str] = []
    seen_paths: dict[str, int] = {}

    for route_path, line_number in parse_vue_router_paths(router_path):
        first_line = seen_paths.get(route_path)
        if first_line is not None:
            errors.append(
                f"{module_label} router declares duplicate route {route_path!r} at "
                f"{display_path(router_path, relative_to)}:{line_number}; first declared at line {first_line}"
            )
            continue
        seen_paths[route_path] = line_number

    return errors


def collect_workshop_content_errors(
    module_root: Path,
    module_label: str,
    expected_workshop_id: str,
    *,
    relative_to: Path,
    require_views_directory: bool = False,
    expected_views: dict[str, dict[str, str | bool]] | None = None,
) -> list[str]:
    errors: list[str] = []
    content_root = module_root / CONTENT_ROOT_RELATIVE

    if not content_root.is_dir():
        return [
            f"{module_label} workshop content directory is missing: {display_path(content_root, relative_to)}"
        ]

    manifest_path = content_root / "manifest.yaml"
    if not manifest_path.is_file():
        return [
            f"{module_label} workshop content manifest is missing: {display_path(manifest_path, relative_to)}"
        ]

    manifest = parse_workshop_content_manifest(manifest_path)
    if manifest["schema_version"] != 1:
        errors.append(
            f"{module_label} workshop content manifest must declare schemaVersion 1: "
            f"{display_path(manifest_path, relative_to)}"
        )

    workshop_content_id = str(manifest.get("workshop_id") or "").strip()
    if not workshop_content_id:
        errors.append(
            f"{module_label} workshop content manifest is missing workshopId: "
            f"{display_path(manifest_path, relative_to)}"
        )
    elif workshop_content_id != expected_workshop_id:
        errors.append(
            f"{module_label} workshop content manifest has workshopId {workshop_content_id!r}; "
            f"expected {expected_workshop_id!r} from workshops.yaml"
        )

    views = manifest["views"]
    assert isinstance(views, list)
    if not views:
        errors.append(
            f"{module_label} workshop content manifest must declare at least one view: "
            f"{display_path(manifest_path, relative_to)}"
        )
        return errors

    seen_view_ids: set[str] = set()
    seen_routes: set[str] = set()
    seen_files: set[str] = set()
    parsed_views: dict[str, dict[str, object]] = {}
    manifest_views_by_id: dict[str, dict[str, object]] = {}

    for manifest_view in views:
        line_number = int(manifest_view.get("_line", 0))
        view_id = str(manifest_view.get("viewId", "")).strip()
        route = str(manifest_view.get("route", "")).strip()
        page_type = str(manifest_view.get("pageType", "")).strip()
        file_value = str(manifest_view.get("file", "")).strip()

        manifest_views_by_id[view_id] = manifest_view

        missing_manifest_fields = [
            field_name
            for field_name, field_value in [
                ("viewId", view_id),
                ("route", route),
                ("pageType", page_type),
                ("file", file_value),
            ]
            if not field_value
        ]
        if missing_manifest_fields:
            errors.append(
                f"{module_label} workshop content manifest view at line {line_number} is missing fields: "
                + ", ".join(missing_manifest_fields)
            )
            continue

        if view_id in seen_view_ids:
            errors.append(
                f"{module_label} workshop content manifest declares duplicate viewId {view_id!r}"
            )
        seen_view_ids.add(view_id)

        if route in seen_routes:
            errors.append(
                f"{module_label} workshop content manifest declares duplicate route {route!r}"
            )
        seen_routes.add(route)

        if page_type not in ALLOWED_CONTENT_PAGE_TYPES:
            errors.append(
                f"{module_label} workshop content manifest view {view_id!r} has unsupported "
                f"pageType {page_type!r}"
            )

        relative_view_path = Path(file_value)
        if relative_view_path.is_absolute():
            errors.append(
                f"{module_label} workshop content manifest view {view_id!r} must use a relative file path"
            )
            continue
        if ".." in relative_view_path.parts:
            errors.append(
                f"{module_label} workshop content manifest view {view_id!r} must not escape workshop-content/"
            )
            continue
        if relative_view_path.suffix != ".yaml":
            errors.append(
                f"{module_label} workshop content manifest view {view_id!r} must point to a .yaml file"
            )
        if require_views_directory and (not relative_view_path.parts or relative_view_path.parts[0] != "views"):
            errors.append(
                f"{module_label} workshop content manifest view {view_id!r} must live under workshop-content/views/"
            )

        normalized_file = relative_view_path.as_posix()
        if normalized_file in seen_files:
            errors.append(
                f"{module_label} workshop content manifest declares duplicate file {normalized_file!r}"
            )
        seen_files.add(normalized_file)

        view_path = content_root / relative_view_path
        if not view_path.is_file():
            errors.append(
                f"{module_label} workshop content view file is missing for {view_id!r}: "
                f"{display_path(view_path, relative_to)}"
            )
            continue

        parsed_view = parse_workshop_content_view(view_path)
        parsed_views[view_id] = parsed_view

        if parsed_view["schema_version"] != 1:
            errors.append(
                f"{module_label} workshop content view {view_id!r} must declare schemaVersion 1: "
                f"{display_path(view_path, relative_to)}"
            )
        if parsed_view["view_id"] != view_id:
            errors.append(
                f"{module_label} workshop content view file {display_path(view_path, relative_to)} "
                f"declares viewId {parsed_view['view_id']!r}; expected {view_id!r}"
            )
        if parsed_view["route"] != route:
            errors.append(
                f"{module_label} workshop content view {view_id!r} must declare route {route!r}"
            )
        if parsed_view["page_type"] != page_type:
            errors.append(
                f"{module_label} workshop content view {view_id!r} must declare pageType {page_type!r}"
            )
        if not parsed_view["title"]:
            errors.append(
                f"{module_label} workshop content view {view_id!r} is missing a title"
            )
        if parsed_view["slot"] != "instructions":
            errors.append(
                f"{module_label} workshop content view {view_id!r} must declare slot 'instructions'"
            )

        stage_ids = parsed_view["stage_ids"]
        section_ids = parsed_view["section_ids"]
        block_types = parsed_view["block_types"]
        assert isinstance(stage_ids, list)
        assert isinstance(section_ids, list)
        assert isinstance(block_types, list)

        if page_type == "stage-flow" and not stage_ids:
            errors.append(
                f"{module_label} workshop content view {view_id!r} must declare at least one top-level stage"
            )
        if page_type in {"narrative", "editor"} and not section_ids:
            errors.append(
                f"{module_label} workshop content view {view_id!r} must declare at least one top-level section"
            )
        if not block_types:
            errors.append(
                f"{module_label} workshop content view {view_id!r} must declare at least one content block"
            )

    if expected_views is not None:
        expected_view_ids = set(expected_views)
        actual_view_ids = set(manifest_views_by_id)
        if actual_view_ids != expected_view_ids:
            errors.append(
                f"{module_label} workshop content manifest expected views {sorted(expected_view_ids)!r}, "
                f"found {sorted(actual_view_ids)!r}"
            )

        for expected_view_id, expectations in expected_views.items():
            manifest_view = manifest_views_by_id.get(expected_view_id)
            if manifest_view is None:
                continue

            for manifest_field in ["route", "pageType", "file"]:
                expected_value = expectations.get(manifest_field)
                if expected_value is None:
                    continue
                actual_value = str(manifest_view.get(manifest_field, "")).strip()
                if actual_value != expected_value:
                    errors.append(
                        f"{module_label} scaffold manifest view {expected_view_id!r} expected "
                        f"{manifest_field} {expected_value!r}, found {actual_value!r}"
                    )

            parsed_view = parsed_views.get(expected_view_id)
            if parsed_view is None:
                continue

            for parsed_field, expectation_key in [
                ("title", "title"),
                ("slot", "slot"),
                ("summary", "summary"),
            ]:
                expected_value = expectations.get(expectation_key)
                if expected_value is None:
                    continue
                actual_value = str(parsed_view.get(parsed_field, "") or "").strip()
                if actual_value != expected_value:
                    errors.append(
                        f"{module_label} scaffold view {expected_view_id!r} expected "
                        f"{parsed_field} {expected_value!r}, found {actual_value!r}"
                    )

            require_summary = expectations.get("requireSummary")
            if require_summary is True and not parsed_view["has_summary"]:
                errors.append(
                    f"{module_label} scaffold view {expected_view_id!r} must declare a summary"
                )

    return errors


def validate_workshop_content(
    module_root: Path,
    module_label: str,
    expected_workshop_id: str,
    *,
    relative_to: Path,
    require_views_directory: bool = False,
    expected_views: dict[str, dict[str, str | bool]] | None = None,
) -> None:
    content_errors = collect_workshop_content_errors(
        module_root,
        module_label,
        expected_workshop_id,
        relative_to=relative_to,
        require_views_directory=require_views_directory,
        expected_views=expected_views,
    )
    if content_errors:
        fail(
            "Workshop standardization check failed:\n"
            + "\n".join(f"- {error}" for error in content_errors)
        )


def assert_contains(path: Path, required_substrings: list[str], *, label: str, relative_to: Path) -> None:
    content = path.read_text(encoding="utf-8")
    missing = [substring for substring in required_substrings if substring not in content]
    if missing:
        formatted_missing = ", ".join(repr(item) for item in missing)
        fail(
            "Workshop standardization check failed:\n"
            f"- {label} is missing expected content markers in {display_path(path, relative_to)}: "
            f"{formatted_missing}"
        )


def assert_not_contains(path: Path, forbidden_substrings: list[str], *, label: str, relative_to: Path) -> None:
    content = path.read_text(encoding="utf-8")
    present = [substring for substring in forbidden_substrings if substring in content]
    if present:
        formatted_present = ", ".join(repr(item) for item in present)
        fail(
            "Workshop standardization check failed:\n"
            f"- {label} contains forbidden content markers in {display_path(path, relative_to)}: "
            f"{formatted_present}"
        )


def parse_bool_scalar(value: object) -> bool | None:
    normalized = str(value).strip().lower()
    if normalized == "true":
        return True
    if normalized == "false":
        return False
    return None


def collect_release_errors(
    workshop_id: str,
    releases: list[dict[str, object]],
    *,
    seen_release_ids: set[str],
) -> list[str]:
    errors: list[str] = []

    if not releases:
        return [f"workshops.yaml entry for {workshop_id} must declare at least one release"]

    default_count = 0
    seen_versions: set[str] = set()

    for release in releases:
        release_id = str(release.get("releaseId", "")).strip()
        release_line = release.get("_line", "?")
        release_keys = release.get("_keys", set())
        release_sections = release.get("_sections", {})
        images = release.get("images", {})
        assert isinstance(release_keys, set)
        assert isinstance(release_sections, dict)
        assert isinstance(images, dict)

        missing_fields = [field for field in RELEASE_REQUIRED_FIELDS if field not in release_keys]
        if missing_fields:
            errors.append(
                f"{workshop_id} release {release_id or '<unknown>'} (line {release_line}) is missing required fields: "
                + ", ".join(missing_fields)
            )

        if not release_id:
            errors.append(f"{workshop_id} release at line {release_line} is missing releaseId")
        elif release_id in seen_release_ids:
            errors.append(f"Duplicate releaseId in workshops.yaml: {release_id}")
        else:
            seen_release_ids.add(release_id)

        release_version = str(release.get("releaseVersion", "")).strip()
        if not release_version:
            errors.append(f"{workshop_id} release {release_id or '<unknown>'} is missing releaseVersion")
        elif release_version in seen_versions:
            errors.append(
                f"{workshop_id} declares duplicate releaseVersion {release_version!r}"
            )
        else:
            seen_versions.add(release_version)

        for field_name in ["mode", "resourceClass"]:
            if not str(release.get(field_name, "")).strip():
                errors.append(f"{workshop_id} release {release_id or '<unknown>'} is missing {field_name}")

        default_for_workshop = parse_bool_scalar(release.get("defaultForWorkshop", ""))
        if default_for_workshop is None:
            errors.append(
                f"{workshop_id} release {release_id or '<unknown>'} defaultForWorkshop must be true or false"
            )
        elif default_for_workshop:
            default_count += 1

        enabled = parse_bool_scalar(release.get("enabled", ""))
        if enabled is None:
            errors.append(f"{workshop_id} release {release_id or '<unknown>'} enabled must be true or false")

        session_ttl = str(release.get("sessionTtlMinutes", "")).strip()
        if not session_ttl.isdigit() or int(session_ttl) <= 0:
            errors.append(
                f"{workshop_id} release {release_id or '<unknown>'} must define a positive sessionTtlMinutes"
            )

        environments = release_sections.get("environments", [])
        if not isinstance(environments, list) or not environments:
            errors.append(f"{workshop_id} release {release_id or '<unknown>'} must declare at least one environment")

        mutable_dependencies = release_sections.get("mutableDependencies", [])
        if not isinstance(mutable_dependencies, list):
            errors.append(f"{workshop_id} release {release_id or '<unknown>'} mutableDependencies must be a list")
        else:
            unknown_dependencies = sorted(
                dependency
                for dependency in mutable_dependencies
                if dependency not in ALLOWED_INFRASTRUCTURE_DEPENDENCIES
            )
            if unknown_dependencies:
                errors.append(
                    f"{workshop_id} release {release_id or '<unknown>'} declares unsupported mutableDependencies: "
                    + ", ".join(unknown_dependencies)
                )

        if not images:
            errors.append(f"{workshop_id} release {release_id or '<unknown>'} must define at least one image")
        for image_role, image_reference in images.items():
            role = str(image_role)
            reference = str(image_reference).strip()
            if role not in ALLOWED_RELEASE_IMAGE_ROLES:
                errors.append(
                    f"{workshop_id} release {release_id or '<unknown>'} uses unsupported image role {role!r}"
                )
            if not reference:
                errors.append(
                    f"{workshop_id} release {release_id or '<unknown>'} image {role!r} must not be blank"
                )
            elif not re.match(r"^[^@\s]+@sha256:[0-9a-f]{64}$", reference):
                errors.append(
                    f"{workshop_id} release {release_id or '<unknown>'} image {role!r} must be digest pinned"
                )

    if default_count != 1:
        errors.append(
            f"{workshop_id} must declare exactly one release with defaultForWorkshop true; found {default_count}"
        )

    return errors


def validate_existing_workshops() -> None:
    errors: list[str] = []

    if not REGISTRY_PATH.is_file():
        fail(f"workshop registry is missing: {REGISTRY_PATH}")
    if not SETTINGS_PATH.is_file():
        fail(f"Gradle settings are missing: {SETTINGS_PATH}")
    if not BUILD_GRADLE_PATH.is_file():
        fail(f"Gradle build file is missing: {BUILD_GRADLE_PATH}")

    version, workshops = parse_registry(REGISTRY_PATH)
    if version != 1:
        errors.append(f"workshops.yaml version must be 1, found {version!r}")
    if not workshops:
        errors.append("workshops.yaml does not define any workshops")

    build_gradle_text = BUILD_GRADLE_PATH.read_text(encoding="utf-8")
    for required_marker in [
        'val sharedFrontendDir = project.rootProject.projectDir.parentFile.resolve("workshop-frontend-shared")',
        "if (sharedFrontendDir.exists()) {",
        "inputs.dir(sharedFrontendDir)",
    ]:
        if required_marker not in build_gradle_text:
            errors.append(
                "java-springboot/build.gradle.kts must track workshop-frontend-shared as a buildFrontend input "
                f"(missing {required_marker!r})"
            )

    settings_text = SETTINGS_PATH.read_text(encoding="utf-8")
    seen_ids: set[str] = set()
    seen_service_names: set[str] = set()
    seen_frontend_service_names: set[str] = set()
    seen_backend_service_names: set[str] = set()
    seen_release_ids: set[str] = set()

    for workshop in workshops:
        workshop_id = str(workshop["id"])
        workshop_line = workshop["_line"]
        keys = workshop["_keys"]
        sections = workshop["_sections"]
        assert isinstance(keys, set)
        assert isinstance(sections, dict)

        missing_fields = [field for field in REGISTRY_REQUIRED_FIELDS if field not in keys]
        if missing_fields:
            errors.append(
                f"workshops.yaml entry for {workshop_id} (line {workshop_line}) is missing required fields: "
                + ", ".join(missing_fields)
            )

        topics = sections.get("topics", [])
        if not isinstance(topics, list) or not topics:
            errors.append(f"workshops.yaml entry for {workshop_id} must declare at least one topic")

        releases = workshop.get("releases", [])
        if isinstance(releases, list):
            errors.extend(
                collect_release_errors(
                    workshop_id,
                    releases,
                    seen_release_ids=seen_release_ids,
                )
            )
        else:
            errors.append(f"workshops.yaml entry for {workshop_id} releases must be a list")

        infrastructure_dependencies = sections.get("infrastructureDependencies", [])
        if not isinstance(infrastructure_dependencies, list) or not infrastructure_dependencies:
            errors.append(
                f"workshops.yaml entry for {workshop_id} must declare at least one infrastructure dependency"
            )
        else:
            unknown_dependencies = sorted(
                dependency
                for dependency in infrastructure_dependencies
                if dependency not in ALLOWED_INFRASTRUCTURE_DEPENDENCIES
            )
            if unknown_dependencies:
                errors.append(
                    f"{workshop_id} declares unsupported infrastructureDependencies: "
                    + ", ".join(unknown_dependencies)
                )

        service_name = str(workshop.get("serviceName", "")).strip()
        frontend_service_name = str(workshop.get("frontendServiceName", "")).strip()
        backend_service_name = str(workshop.get("backendServiceName", "")).strip()
        redis_flavor = str(workshop.get("redisFlavor", "")).strip()
        frontend_prebuild = str(workshop.get("frontendPrebuild", "")).strip().lower()

        if workshop_id in seen_ids:
            errors.append(f"Duplicate workshop id in workshops.yaml: {workshop_id}")
        seen_ids.add(workshop_id)

        for label, value, seen in [
            ("serviceName", service_name, seen_service_names),
            ("frontendServiceName", frontend_service_name, seen_frontend_service_names),
            ("backendServiceName", backend_service_name, seen_backend_service_names),
        ]:
            if not value:
                continue
            if value in seen:
                errors.append(f"Duplicate {label} in workshops.yaml: {value}")
            seen.add(value)

        if service_name and frontend_service_name and service_name != frontend_service_name:
            errors.append(
                f"{workshop_id} must keep serviceName aligned with frontendServiceName "
                f"({service_name!r} != {frontend_service_name!r})"
            )

        expected_url = f"/workshop/{service_name}/"
        actual_url = str(workshop.get("url", "")).strip()
        if service_name and actual_url != expected_url:
            errors.append(
                f"{workshop_id} has url {actual_url!r}, expected {expected_url!r}"
            )

        if redis_flavor not in ALLOWED_REDIS_FLAVORS:
            errors.append(
                f"{workshop_id} has redisFlavor {redis_flavor!r}, expected one of "
                + ", ".join(sorted(ALLOWED_REDIS_FLAVORS))
            )

        if frontend_prebuild not in ALLOWED_FRONTEND_PREBUILD_VALUES:
            errors.append(
                f"{workshop_id} has frontendPrebuild {frontend_prebuild!r}, expected true or false"
            )

        for docker_key in ["dockerfile", "frontendDockerfile", "backendDockerfile"]:
            dockerfile_value = str(workshop.get(docker_key, "")).strip()
            if not dockerfile_value:
                continue
            dockerfile_path = ROOT / dockerfile_value
            if not dockerfile_path.is_file():
                errors.append(
                    f"{workshop_id} references a missing {docker_key}: {dockerfile_value}"
                )

        backend_dir = JAVA_DIR / workshop_id
        frontend_dir = JAVA_DIR / f"{workshop_id}_frontend"

        ensure_dir(backend_dir, errors, f"{workshop_id} backend module")
        ensure_dir(frontend_dir, errors, f"{workshop_id} frontend module")

        for relative_path in BACKEND_REQUIRED_FILES:
            ensure_file(backend_dir / relative_path, errors, f"{workshop_id} backend file")

        for relative_path in FRONTEND_REQUIRED_FILES:
            ensure_file(frontend_dir / relative_path, errors, f"{workshop_id} frontend file")

        frontend_main_path = frontend_dir / "frontend/src/main.js"
        if frontend_main_path.is_file():
            frontend_main_text = frontend_main_path.read_text(encoding="utf-8")
            for marker in SHELL_REQUIRED_MAIN_MARKERS:
                if marker not in frontend_main_text:
                    errors.append(
                        f"{workshop_id} frontend shell wiring is missing {marker!r} in "
                        f"{frontend_main_path.relative_to(ROOT)}"
                    )

        frontend_router_path = frontend_dir / "frontend/src/router/index.js"
        if frontend_router_path.is_file():
            frontend_router_text = frontend_router_path.read_text(encoding="utf-8")
            for marker in SHELL_REQUIRED_ROUTER_MARKERS:
                if marker not in frontend_router_text:
                    errors.append(
                        f"{workshop_id} frontend router is missing shell base path marker {marker!r} in "
                        f"{frontend_router_path.relative_to(ROOT)}"
                    )
            errors.extend(
                collect_duplicate_router_path_errors(
                    frontend_router_path,
                    f"{workshop_id} frontend module",
                    relative_to=ROOT,
                )
            )

        frontend_view_text = ""
        if frontend_dir.is_dir():
            frontend_view_text = "\n".join(
                path.read_text(encoding="utf-8")
                for path in sorted((frontend_dir / "frontend/src/views").glob("*.vue"))
                if path.is_file()
            )
        if frontend_view_text and "WorkshopHeader" not in frontend_view_text and "WorkshopShell" not in frontend_view_text:
            errors.append(
                f"{workshop_id} frontend shell readiness check failed: "
                "must use the shared shell or shared header in at least one app view"
            )
        for marker, reason in [
            ("WorkshopEditorLayout", "must use the shared editor shell"),
            ("show-session-restart-controls", "must expose shared restart and rebuild controls"),
        ]:
            if frontend_view_text and marker not in frontend_view_text:
                errors.append(f"{workshop_id} frontend shell readiness check failed: {reason}")

        if not list(backend_dir.glob("src/main/java/**/*.java")):
            errors.append(f"{workshop_id} backend module does not contain any src/main/java sources")

        if not list(frontend_dir.glob("src/main/java/**/*.java")):
            errors.append(f"{workshop_id} frontend module does not contain any src/main/java sources")

        if not list(frontend_dir.glob("src/test/java/**/*.java")):
            errors.append(f"{workshop_id} frontend module does not contain any src/test/java sources")

        backend_props_path = backend_dir / "src/main/resources/application.properties"
        if backend_props_path.is_file():
            backend_props = backend_props_path.read_text(encoding="utf-8")
            if "${SERVER_PORT:" not in backend_props:
                errors.append(
                    f"{workshop_id} backend application.properties must derive server.port from SERVER_PORT"
                )

        frontend_props_path = frontend_dir / "src/main/resources/application.properties"
        if frontend_props_path.is_file():
            frontend_props = frontend_props_path.read_text(encoding="utf-8")
            if "workshop.backend.url=" not in frontend_props:
                errors.append(
                    f"{workshop_id} frontend application.properties is missing workshop.backend.url"
                )
            if "workshop.source.path=" not in frontend_props:
                errors.append(
                    f"{workshop_id} frontend application.properties is missing workshop.source.path"
                )

        manifest_path = frontend_dir / "src/main/resources/workshop-manifest.yaml"
        if manifest_path.is_file():
            manifest = parse_manifest(manifest_path)
            if manifest["module_name"] != workshop_id:
                errors.append(
                    f"{workshop_id} workshop-manifest.yaml has moduleName {manifest['module_name']!r}"
                )
            if not manifest["title"]:
                errors.append(f"{workshop_id} workshop-manifest.yaml is missing a title")

            editable_paths = manifest["editable_paths"]
            reset_locations = manifest["reset_locations"]
            assert isinstance(editable_paths, list)
            assert isinstance(reset_locations, list)

            if not editable_paths:
                errors.append(f"{workshop_id} workshop-manifest.yaml does not declare editable files")
            if len(editable_paths) != len(reset_locations):
                errors.append(
                    f"{workshop_id} workshop-manifest.yaml has {len(editable_paths)} editable paths "
                    f"but {len(reset_locations)} reset locations"
                )

            for editable_path in editable_paths:
                backend_editable = backend_dir / editable_path
                if not backend_editable.exists():
                    errors.append(
                        f"{workshop_id} manifest references missing editable file: "
                        f"{backend_editable.relative_to(ROOT)}"
                    )

            for reset_location in reset_locations:
                reset_path = frontend_dir / "src/main/resources" / reset_location
                if not reset_path.is_file():
                    errors.append(
                        f"{workshop_id} manifest reset file is missing: {reset_path.relative_to(ROOT)}"
                    )

        if frontend_dir.is_dir():
            require_standard_content_layout = workshop_id not in LEGACY_NONSTANDARD_CONTENT_LAYOUT_IDS
            expected_content_workshop_id = (
                f"{workshop_id}_frontend"
                if workshop_id in LEGACY_NONSTANDARD_CONTENT_LAYOUT_IDS
                else workshop_id
            )
            errors.extend(
                collect_workshop_content_errors(
                    frontend_dir,
                    f"{workshop_id} frontend module",
                    expected_content_workshop_id,
                    relative_to=ROOT,
                    require_views_directory=require_standard_content_layout,
                )
            )

        for module_name in [workshop_id, f"{workshop_id}_frontend"]:
            include_line = f'include("{module_name}")'
            if include_line not in settings_text:
                errors.append(f"java-springboot/settings.gradle.kts is missing {include_line}")

    errors.extend(collect_shell_app_boundary_errors())

    if errors:
        fail(
            "Workshop standardization check failed:\n"
            + "\n".join(f"- {error}" for error in errors)
        )


def validate_scaffold_smoke() -> None:
    smoke_id = "99_validation_smoke"
    smoke_title = "Validation Smoke"
    smoke_service = "validation-smoke"
    default_frontend_port = "8080"
    default_backend_port = "18080"
    package_name = "validationsmoke"
    pascal_case = "ValidationSmoke"

    with tempfile.TemporaryDirectory(prefix="scaffold-validation.") as temp_dir_name:
        temp_dir = Path(temp_dir_name)
        temp_scripts_dir = temp_dir / "scripts"
        temp_java_dir = temp_dir / "java-springboot"
        temp_logo_dir = temp_java_dir / "1_session_management_frontend" / "frontend" / "src" / "assets" / "logo"

        temp_scripts_dir.mkdir(parents=True, exist_ok=True)
        temp_java_dir.mkdir(parents=True, exist_ok=True)
        temp_logo_dir.mkdir(parents=True, exist_ok=True)

        shutil.copy2(ROOT / "scripts" / "new-workshop.sh", temp_scripts_dir / "new-workshop.sh")
        (temp_scripts_dir / "new-workshop.sh").chmod(
            (temp_scripts_dir / "new-workshop.sh").stat().st_mode | stat.S_IXUSR
        )
        shutil.copy2(REGISTRY_PATH, temp_dir / "workshops.yaml")
        shutil.copy2(SETTINGS_PATH, temp_java_dir / "settings.gradle.kts")

        logo_source = ROOT / "java-springboot/1_session_management_frontend/frontend/src/assets/logo/small.png"
        if logo_source.is_file():
            shutil.copy2(logo_source, temp_logo_dir / "small.png")

        command = [
            "./scripts/new-workshop.sh",
            smoke_id,
            smoke_title,
            smoke_service,
        ]
        result = subprocess.run(
            command,
            cwd=temp_dir,
            capture_output=True,
            text=True,
            check=False,
        )
        if result.returncode != 0:
            output = (result.stdout + "\n" + result.stderr).strip()
            fail(
                "Workshop standardization check failed:\n"
                "- Scaffold smoke test could not create a sample workshop.\n"
                + (output if output else "- new-workshop.sh returned a non-zero exit code with no output.")
            )

        output = result.stdout.strip()
        if "bash scripts/validate-workshops.sh" not in output:
            fail(
                "Workshop standardization check failed:\n"
                "- Scaffold output no longer tells contributors to run workshop validation."
            )
        if f"bash scripts/run-workshop.sh up {smoke_id}" not in output:
            fail(
                "Workshop standardization check failed:\n"
                "- Scaffold output no longer tells contributors how to start the workshop locally."
            )
        if "content manifest:" not in output or "content views:" not in output:
            fail(
                "Workshop standardization check failed:\n"
                "- Scaffold output no longer calls out the generated workshop-content files."
            )
        if "learner app iframe" not in output:
            fail(
                "Workshop standardization check failed:\n"
                "- Scaffold output no longer calls out the generated learner app iframe."
            )

        generated_backend_dir = temp_java_dir / smoke_id
        generated_frontend_dir = temp_java_dir / f"{smoke_id}_frontend"

        required_smoke_files = [
            generated_backend_dir / "README.md",
            generated_backend_dir / "Dockerfile",
            generated_backend_dir / "build.gradle.kts",
            generated_backend_dir / "settings.gradle.kts",
            generated_backend_dir / "src/main/resources/application.properties",
            generated_backend_dir
            / f"src/main/java/com/redis/workshop/{package_name}/{pascal_case}Application.java",
            generated_backend_dir
            / f"src/main/java/com/redis/workshop/{package_name}/{pascal_case}LearnerAppController.java",
            generated_frontend_dir / "Dockerfile",
            generated_frontend_dir / "build.gradle.kts",
            generated_frontend_dir / "settings.gradle.kts",
            generated_frontend_dir / "frontend/package.json",
            generated_frontend_dir / "frontend/vue.config.js",
            generated_frontend_dir / "frontend/public/index.html",
            generated_frontend_dir / "frontend/src/main.js",
            generated_frontend_dir / "frontend/src/router/index.js",
            generated_frontend_dir / "frontend/src/utils/workshopContent.js",
            generated_frontend_dir / f"frontend/src/views/{pascal_case}Home.vue",
            generated_frontend_dir / f"frontend/src/views/{pascal_case}Editor.vue",
            generated_frontend_dir / "src/main/resources/application.properties",
            generated_frontend_dir / "src/main/resources/workshop-content/manifest.yaml",
            generated_frontend_dir / f"src/main/resources/workshop-content/views/{smoke_service}-home.yaml",
            generated_frontend_dir / f"src/main/resources/workshop-content/views/{smoke_service}-editor.yaml",
            generated_frontend_dir / "src/main/resources/workshop-manifest.yaml",
            generated_frontend_dir / "src/main/resources/workshop-manifest-reset/build.gradle.kts",
            generated_frontend_dir / "src/main/resources/workshop-manifest-reset/application.properties",
            generated_frontend_dir
            / f"src/main/resources/workshop-manifest-reset/{pascal_case}Application.java",
            generated_frontend_dir
            / f"src/main/java/com/redis/workshop/{package_name}/frontend/{pascal_case}FrontendApplication.java",
            generated_frontend_dir
            / f"src/main/java/com/redis/workshop/{package_name}/frontend/infrastructure/{pascal_case}SpaController.java",
            generated_frontend_dir
            / f"src/main/java/com/redis/workshop/{package_name}/frontend/infrastructure/{pascal_case}WorkshopManifestConfiguration.java",
            generated_frontend_dir
            / f"src/test/java/com/redis/workshop/{package_name}/frontend/{pascal_case}FrontendIntegrationTest.java",
        ]

        missing_smoke_files = [
            path.relative_to(temp_dir).as_posix()
            for path in required_smoke_files
            if not path.is_file()
        ]
        if missing_smoke_files:
            fail(
                "Workshop standardization check failed:\n"
                "- Scaffold smoke test is missing expected files:\n"
                + "\n".join(f"  - {path}" for path in missing_smoke_files)
            )

        forbidden_smoke_files = [
            generated_frontend_dir / "frontend/src/utils/basePath.js",
            generated_frontend_dir / "frontend/src/utils/components.js",
            generated_frontend_dir / "frontend/src/assets/logo/small.png",
        ]
        unexpected_smoke_files = [
            path.relative_to(temp_dir).as_posix()
            for path in forbidden_smoke_files
            if path.exists()
        ]
        if unexpected_smoke_files:
            fail(
                "Workshop standardization check failed:\n"
                "- Scaffold smoke test generated files that should now be shell-owned or app-provided:\n"
                + "\n".join(f"  - {path}" for path in unexpected_smoke_files)
            )

        settings_text = (temp_java_dir / "settings.gradle.kts").read_text(encoding="utf-8")
        for include_line in [f'include("{smoke_id}")', f'include("{smoke_id}_frontend")']:
            if include_line not in settings_text:
                fail(
                    "Workshop standardization check failed:\n"
                    f"- Scaffold smoke test did not append {include_line} to settings.gradle.kts."
                )

        _, workshops = parse_registry(temp_dir / "workshops.yaml")
        matching = [workshop for workshop in workshops if workshop.get("id") == smoke_id]
        if len(matching) != 1:
            fail(
                "Workshop standardization check failed:\n"
                "- Scaffold smoke test did not append exactly one registry entry for the sample workshop."
            )

        scaffold_entry = matching[0]
        scaffold_sections = scaffold_entry["_sections"]
        assert isinstance(scaffold_sections, dict)
        expected_fields = {
            "title": smoke_title,
            "serviceName": smoke_service,
            "url": f"/workshop/{smoke_service}/",
            "dockerfile": f"java-springboot/{smoke_id}_frontend/Dockerfile",
            "frontendServiceName": smoke_service,
            "frontendDockerfile": f"java-springboot/{smoke_id}_frontend/Dockerfile",
            "backendServiceName": f"{smoke_service}-api",
            "backendDockerfile": f"java-springboot/{smoke_id}/Dockerfile",
            "difficulty": "Beginner",
            "estimatedMinutes": "30",
            "redisFlavor": "standard",
            "frontendPrebuild": "true",
        }
        for field_name, expected_value in expected_fields.items():
            actual_value = str(scaffold_entry.get(field_name, "")).strip()
            if actual_value != expected_value:
                fail(
                    "Workshop standardization check failed:\n"
                    f"- Scaffold registry field {field_name} expected {expected_value!r}, found {actual_value!r}."
                )

        forbidden_registry_fields = ["port", "frontendPort", "backendPort"]
        present_forbidden_fields = [
            field_name for field_name in forbidden_registry_fields if field_name in scaffold_entry
        ]
        if present_forbidden_fields:
            fail(
                "Workshop standardization check failed:\n"
                "- Scaffold registry entry must not include fixed port fields: "
                + ", ".join(present_forbidden_fields)
            )

        topics = scaffold_sections.get("topics", [])
        if topics != ["TODO"]:
            fail(
                "Workshop standardization check failed:\n"
                f"- Scaffold topics expected ['TODO'], found {topics!r}."
            )

        infrastructure_dependencies = scaffold_sections.get("infrastructureDependencies", [])
        if infrastructure_dependencies != ["redis"]:
            fail(
                "Workshop standardization check failed:\n"
                f"- Scaffold infrastructureDependencies expected ['redis'], found {infrastructure_dependencies!r}."
            )

        scaffold_releases = scaffold_entry.get("releases", [])
        if not isinstance(scaffold_releases, list):
            fail(
                "Workshop standardization check failed:\n"
                "- Scaffold registry releases must be a list."
            )
        release_errors = collect_release_errors(
            smoke_id,
            scaffold_releases,
            seen_release_ids=set(),
        )
        if release_errors:
            fail(
                "Workshop standardization check failed:\n"
                + "\n".join(f"- {error}" for error in release_errors)
            )
        scaffold_release = scaffold_releases[0] if scaffold_releases else {}
        scaffold_release_sections = scaffold_release.get("_sections", {})
        scaffold_release_images = scaffold_release.get("images", {})
        assert isinstance(scaffold_release_sections, dict)
        assert isinstance(scaffold_release_images, dict)
        expected_release_fields = {
            "releaseId": f"{smoke_service}-0.1.0",
            "releaseVersion": "0.1.0",
            "mode": "LAB",
            "defaultForWorkshop": "true",
            "enabled": "true",
            "resourceClass": "small",
            "sessionTtlMinutes": "60",
        }
        for field_name, expected_value in expected_release_fields.items():
            actual_value = str(scaffold_release.get(field_name, "")).strip()
            if actual_value != expected_value:
                fail(
                    "Workshop standardization check failed:\n"
                    f"- Scaffold release field {field_name} expected {expected_value!r}, found {actual_value!r}."
                )
        if scaffold_release_sections.get("environments") != ["local", "cloud-run"]:
            fail(
                "Workshop standardization check failed:\n"
                f"- Scaffold release environments expected ['local', 'cloud-run'], found {scaffold_release_sections.get('environments')!r}."
            )
        if scaffold_release_sections.get("mutableDependencies") != ["redis"]:
            fail(
                "Workshop standardization check failed:\n"
                f"- Scaffold release mutableDependencies expected ['redis'], found {scaffold_release_sections.get('mutableDependencies')!r}."
            )
        expected_combined_image = (
            f"registry.example.com/workshops/{smoke_service}-runner@sha256:"
            "0000000000000000000000000000000000000000000000000000000000000000"
        )
        if scaffold_release_images.get("combined") != expected_combined_image:
            fail(
                "Workshop standardization check failed:\n"
                f"- Scaffold release combined image expected {expected_combined_image!r}, found {scaffold_release_images.get('combined')!r}."
            )

        backend_props = (
            generated_backend_dir / "src/main/resources/application.properties"
        ).read_text(encoding="utf-8")
        if f"server.port=${{SERVER_PORT:{default_backend_port}}}" not in backend_props:
            fail(
                "Workshop standardization check failed:\n"
                "- Scaffold backend application.properties did not use the expected backend port template."
            )

        frontend_props = (
            generated_frontend_dir / "src/main/resources/application.properties"
        ).read_text(encoding="utf-8")
        frontend_expectations = [
            f"spring.application.name={smoke_service}-frontend",
            f"server.port=${{SERVER_PORT:{default_frontend_port}}}",
            f"workshop.backend.url=${{WORKSHOP_BACKEND_URL:http://127.0.0.1:{default_backend_port}}}",
            "workshop.source.path=${WORKSHOP_SOURCE_PATH:${WORKSHOP_BASE_PATH:}}",
        ]
        for expected_line in frontend_expectations:
            if expected_line not in frontend_props:
                fail(
                    "Workshop standardization check failed:\n"
                    f"- Scaffold frontend application.properties is missing {expected_line!r}."
                )

        validate_workshop_content(
            generated_frontend_dir,
            "Scaffold smoke frontend module",
            smoke_id,
            relative_to=temp_dir,
            require_views_directory=True,
            expected_views={
                f"{smoke_service}-home": {
                    "route": "/",
                    "pageType": "narrative",
                    "file": f"views/{smoke_service}-home.yaml",
                    "title": smoke_title,
                    "slot": "instructions",
                    "requireSummary": True,
                },
                f"{smoke_service}-editor": {
                    "route": "/editor",
                    "pageType": "editor",
                    "file": f"views/{smoke_service}-editor.yaml",
                    "title": smoke_title,
                    "slot": "instructions",
                    "requireSummary": True,
                },
            },
        )
        assert_contains(
            generated_frontend_dir / f"src/main/resources/workshop-content/views/{smoke_service}-home.yaml",
            [
                "type: markdown",
                "{{sessionId}}",
                "{{links.redisInsight}}",
                "{{links.learnerApp}}",
                "id: openApp",
            ],
            label="Scaffold markdown-first home content",
            relative_to=temp_dir,
        )

        generated_router_path = generated_frontend_dir / "frontend/src/router/index.js"
        router_paths = [route_path for route_path, _ in parse_vue_router_paths(generated_router_path)]
        for expected_route in ["/", "/editor"]:
            if expected_route not in router_paths:
                fail(
                    "Workshop standardization check failed:\n"
                    f"- Scaffold router is missing expected route {expected_route!r}."
                )
        duplicate_router_errors = collect_duplicate_router_path_errors(
            generated_router_path,
            "Scaffold smoke frontend module",
            relative_to=temp_dir,
        )
        if duplicate_router_errors:
            fail(
                "Workshop standardization check failed:\n"
                + "\n".join(f"- {error}" for error in duplicate_router_errors)
            )

        assert_contains(
            generated_frontend_dir / "frontend/src/utils/workshopContent.js",
            [
                "getApiUrl",
                "../../../../../workshop-frontend-shared/src/",
                "export async function fetchWorkshopContent(viewId)",
                "/api/content/views/${encodeURIComponent(viewId)}",
            ],
            label="Scaffold workshop content client",
            relative_to=temp_dir,
        )
        assert_not_contains(
            generated_frontend_dir / "frontend/src/utils/workshopContent.js",
            ["from './basePath'", 'from "./basePath"'],
            label="Scaffold workshop content client",
            relative_to=temp_dir,
        )
        assert_contains(
            generated_frontend_dir / f"frontend/src/views/{pascal_case}Home.vue",
            [
                "WorkshopShell",
                "getApiUrl",
                "getRedisInsightUrl",
                "getWorkshopHubUrl",
                "../../../../../workshop-frontend-shared/src/",
                "learnerAppUrl",
                "/api/learner-app",
                "fetchWorkshopContent",
                f"this.content = await fetchWorkshopContent('{smoke_service}-home')",
                "openApp",
            ],
            label="Scaffold home view",
            relative_to=temp_dir,
        )
        assert_not_contains(
            generated_frontend_dir / f"frontend/src/views/{pascal_case}Home.vue",
            ["../utils/components", "../utils/basePath"],
            label="Scaffold home view",
            relative_to=temp_dir,
        )
        assert_contains(
            generated_frontend_dir / f"frontend/src/views/{pascal_case}Editor.vue",
            [
                "WorkshopEditorLayout",
                "WorkshopContentRenderer",
                "getWorkshopHubUrl",
                "../../../../../workshop-frontend-shared/src/",
                "show-session-restart-controls",
                "fetchWorkshopContent",
                f"this.content = await fetchWorkshopContent('{smoke_service}-editor')",
            ],
            label="Scaffold editor view",
            relative_to=temp_dir,
        )
        assert_not_contains(
            generated_frontend_dir / f"frontend/src/views/{pascal_case}Editor.vue",
            ["../utils/components", "../utils/basePath"],
            label="Scaffold editor view",
            relative_to=temp_dir,
        )
        assert_contains(
            generated_backend_dir
            / f"src/main/java/com/redis/workshop/{package_name}/{pascal_case}LearnerAppController.java",
            [
                "/api/learner-app",
                "MediaType.TEXT_HTML_VALUE",
                "Replace this endpoint with the actual app UI for your workshop.",
            ],
            label="Scaffold learner app controller",
            relative_to=temp_dir,
        )


def main() -> None:
    validate_existing_workshops()
    validate_scaffold_smoke()
    print("Workshop standardization check passed.")


if __name__ == "__main__":
    main()
PY
