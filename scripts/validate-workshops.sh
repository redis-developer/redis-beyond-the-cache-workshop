#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

python3 - "$ROOT_DIR" <<'PY'
from __future__ import annotations

import difflib
import os
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
LOCAL_COMPOSE_PATH = JAVA_DIR / "workshop-hub" / "docker-compose.local.yml"
INTERNAL_COMPOSE_PATH = JAVA_DIR / "workshop-hub" / "docker-compose.internal.yml"
ALLOWED_INFRASTRUCTURE_DEPENDENCIES = {"redis", "postgres"}
ALLOWED_REDIS_FLAVORS = {"standard", "stack"}
ALLOWED_FRONTEND_PREBUILD_VALUES = {"true", "false"}
ALLOWED_CONTENT_PAGE_TYPES = {"narrative", "stage-flow", "editor"}
CONTENT_ROOT_RELATIVE = Path("src/main/resources/workshop-content")

REGISTRY_REQUIRED_FIELDS = [
    "id",
    "title",
    "description",
    "difficulty",
    "estimatedMinutes",
    "serviceName",
    "port",
    "url",
    "dockerfile",
    "frontendServiceName",
    "frontendPort",
    "frontendDockerfile",
    "backendServiceName",
    "backendPort",
    "backendDockerfile",
    "infrastructureDependencies",
    "redisFlavor",
    "frontendPrebuild",
    "topics",
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


def fail(message: str) -> None:
    print(message, file=sys.stderr)
    raise SystemExit(1)


def parse_registry(path: Path) -> tuple[int | None, list[dict[str, object]]]:
    lines = path.read_text(encoding="utf-8").splitlines()
    version = None
    workshops: list[dict[str, object]] = []
    current: dict[str, object] | None = None
    active_section: str | None = None

    for line_number, line in enumerate(lines, start=1):
        version_match = re.match(r"^version:\s*(\d+)\s*$", line)
        if version_match:
            version = int(version_match.group(1))
            continue

        workshop_match = re.match(r"^  - id:\s*(.+?)\s*$", line)
        if workshop_match:
            if current is not None:
                workshops.append(current)
            current = {
                "id": workshop_match.group(1).strip(),
                "_line": line_number,
                "_keys": {"id"},
                "_sections": {},
            }
            active_section = None
            continue

        if current is None:
            continue

        field_match = re.match(r"^    ([A-Za-z][A-Za-z0-9]*)\s*:\s*(.*)$", line)
        if field_match:
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


def parse_included_projects(settings_path: Path) -> list[str]:
    included_projects: list[str] = []
    for line in settings_path.read_text(encoding="utf-8").splitlines():
        match = re.match(r'^\s*include\("([^"]+)"\)\s*$', line)
        if match:
            included_projects.append(match.group(1))
    return included_projects


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


def collect_workshop_content_errors(
    module_root: Path,
    module_label: str,
    allowed_workshop_ids: set[str],
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
    elif workshop_content_id not in allowed_workshop_ids:
        allowed_ids_display = ", ".join(sorted(allowed_workshop_ids))
        errors.append(
            f"{module_label} workshop content manifest has workshopId {workshop_content_id!r}; "
            f"expected one of {allowed_ids_display}"
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
    allowed_workshop_ids: set[str],
    *,
    relative_to: Path,
    require_views_directory: bool = False,
    expected_views: dict[str, dict[str, str | bool]] | None = None,
) -> None:
    content_errors = collect_workshop_content_errors(
        module_root,
        module_label,
        allowed_workshop_ids,
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
    seen_frontend_ports: set[int] = set()
    seen_backend_ports: set[int] = set()

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
        frontend_port_raw = str(workshop.get("frontendPort", "")).strip()
        backend_port_raw = str(workshop.get("backendPort", "")).strip()
        port_raw = str(workshop.get("port", "")).strip()
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

        for label, raw_value in [
            ("port", port_raw),
            ("frontendPort", frontend_port_raw),
            ("backendPort", backend_port_raw),
        ]:
            if not raw_value.isdigit():
                errors.append(f"{workshop_id} has a non-numeric {label}: {raw_value!r}")
                continue
            port_value = int(raw_value)
            if port_value <= 0:
                errors.append(f"{workshop_id} has a non-positive {label}: {port_value}")
        if frontend_port_raw.isdigit():
            frontend_port = int(frontend_port_raw)
            if frontend_port in seen_frontend_ports:
                errors.append(f"Duplicate frontendPort in workshops.yaml: {frontend_port}")
            seen_frontend_ports.add(frontend_port)
        if backend_port_raw.isdigit():
            backend_port = int(backend_port_raw)
            if backend_port in seen_backend_ports:
                errors.append(f"Duplicate backendPort in workshops.yaml: {backend_port}")
            seen_backend_ports.add(backend_port)

        if service_name and frontend_service_name and service_name != frontend_service_name:
            errors.append(
                f"{workshop_id} must keep serviceName aligned with frontendServiceName "
                f"({service_name!r} != {frontend_service_name!r})"
            )

        if port_raw.isdigit() and frontend_port_raw.isdigit() and int(port_raw) != int(frontend_port_raw):
            errors.append(
                f"{workshop_id} must keep port aligned with frontendPort "
                f"({port_raw} != {frontend_port_raw})"
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
            errors.extend(
                collect_workshop_content_errors(
                    frontend_dir,
                    f"{workshop_id} frontend module",
                    {workshop_id, f"{workshop_id}_frontend"},
                    relative_to=ROOT,
                )
            )

        for module_name in [workshop_id, f"{workshop_id}_frontend"]:
            include_line = f'include("{module_name}")'
            if include_line not in settings_text:
                errors.append(f"java-springboot/settings.gradle.kts is missing {include_line}")

    for compose_path in [LOCAL_COMPOSE_PATH, INTERNAL_COMPOSE_PATH]:
        if not compose_path.is_file():
            errors.append(f"Generated compose file is missing: {compose_path.relative_to(ROOT)}")

    if errors:
        fail(
            "Workshop standardization check failed:\n"
            + "\n".join(f"- {error}" for error in errors)
        )


def prepare_gradle_env() -> dict[str, str]:
    env = os.environ.copy()
    if not env.get("JAVA_HOME"):
        java_home_helper = Path("/usr/libexec/java_home")
        if java_home_helper.is_file() and os.access(java_home_helper, os.X_OK):
            result = subprocess.run(
                [str(java_home_helper), "-v", "21"],
                check=False,
                capture_output=True,
                text=True,
            )
            if result.returncode == 0:
                env["JAVA_HOME"] = result.stdout.strip()
    return env


def validate_compose_freshness() -> None:
    gradle_env = prepare_gradle_env()

    with tempfile.TemporaryDirectory(prefix="compose-validation.") as temp_dir_name:
        temp_dir = Path(temp_dir_name)
        temp_java_dir = temp_dir / "java-springboot"
        temp_hub_dir = temp_java_dir / "workshop-hub"

        temp_java_dir.mkdir(parents=True, exist_ok=True)
        temp_hub_dir.mkdir(parents=True, exist_ok=True)

        shutil.copy2(REGISTRY_PATH, temp_dir / "workshops.yaml")
        shutil.copy2(JAVA_DIR / "build.gradle.kts", temp_java_dir / "build.gradle.kts")
        shutil.copy2(JAVA_DIR / "settings.gradle.kts", temp_java_dir / "settings.gradle.kts")
        shutil.copy2(JAVA_DIR / "gradle.properties", temp_java_dir / "gradle.properties")
        shutil.copy2(JAVA_DIR / "gradlew", temp_java_dir / "gradlew")
        (temp_java_dir / "gradlew").chmod((temp_java_dir / "gradlew").stat().st_mode | stat.S_IXUSR)
        shutil.copytree(JAVA_DIR / "gradle", temp_java_dir / "gradle", dirs_exist_ok=True)
        shutil.copytree(JAVA_DIR / "buildSrc", temp_java_dir / "buildSrc", dirs_exist_ok=True)
        for project_name in parse_included_projects(JAVA_DIR / "settings.gradle.kts"):
            (temp_java_dir / project_name).mkdir(parents=True, exist_ok=True)
        shutil.copy2(JAVA_DIR / "workshop-hub" / "build.gradle.kts", temp_hub_dir / "build.gradle.kts")
        shutil.copytree(JAVA_DIR / "workshop-hub" / "src", temp_hub_dir / "src", dirs_exist_ok=True)

        command = [
            "./java-springboot/gradlew",
            "-p",
            "java-springboot",
            "--no-daemon",
            ":workshop-hub:generateCompose",
        ]
        result = subprocess.run(
            command,
            cwd=temp_dir,
            env=gradle_env,
            capture_output=True,
            text=True,
            check=False,
        )
        if result.returncode != 0:
            output = (result.stdout + "\n" + result.stderr).strip()
            fail(
                "Workshop standardization check failed:\n"
                "- Unable to regenerate compose files in a temporary workspace.\n"
                + (output if output else "- Gradle returned a non-zero exit code with no output.")
            )

        generated_pairs = [
            (LOCAL_COMPOSE_PATH, temp_hub_dir / "docker-compose.local.yml"),
            (INTERNAL_COMPOSE_PATH, temp_hub_dir / "docker-compose.internal.yml"),
        ]

        for actual_path, generated_path in generated_pairs:
            if not generated_path.is_file():
                fail(
                    "Workshop standardization check failed:\n"
                    f"- Compose regeneration did not produce {generated_path.name}."
                )
            actual_text = actual_path.read_text(encoding="utf-8").splitlines()
            generated_text = generated_path.read_text(encoding="utf-8").splitlines()
            if actual_text != generated_text:
                diff = "\n".join(
                    difflib.unified_diff(
                        actual_text,
                        generated_text,
                        fromfile=str(actual_path.relative_to(ROOT)),
                        tofile=f"generated/{generated_path.name}",
                        lineterm="",
                    )
                )
                fail(
                    "Workshop standardization check failed:\n"
                    f"- Generated compose output is stale for {actual_path.relative_to(ROOT)}.\n"
                    f"{diff}"
                )


def validate_scaffold_smoke() -> None:
    gradle_env = prepare_gradle_env()
    smoke_id = "99_validation_smoke"
    smoke_title = "Validation Smoke"
    smoke_service = "validation-smoke"
    frontend_port = "8099"
    backend_port = "18099"
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
            frontend_port,
        ]
        result = subprocess.run(
            command,
            cwd=temp_dir,
            env=gradle_env,
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
        if "workshop-hub/Dockerfile" in output:
            fail(
                "Workshop standardization check failed:\n"
                "- Scaffold output still references manual workshop-hub Dockerfile edits."
            )
        if ":workshop-hub:generateCompose" not in output:
            fail(
                "Workshop standardization check failed:\n"
                "- Scaffold output no longer tells contributors to regenerate compose files."
            )
        if "content manifest:" not in output or "content views:" not in output:
            fail(
                "Workshop standardization check failed:\n"
                "- Scaffold output no longer calls out the generated workshop-content files."
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
            generated_frontend_dir / "Dockerfile",
            generated_frontend_dir / "build.gradle.kts",
            generated_frontend_dir / "settings.gradle.kts",
            generated_frontend_dir / "frontend/package.json",
            generated_frontend_dir / "frontend/vue.config.js",
            generated_frontend_dir / "frontend/public/index.html",
            generated_frontend_dir / "frontend/src/main.js",
            generated_frontend_dir / "frontend/src/router/index.js",
            generated_frontend_dir / "frontend/src/utils/basePath.js",
            generated_frontend_dir / "frontend/src/utils/components.js",
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
            "port": frontend_port,
            "url": f"/workshop/{smoke_service}/",
            "dockerfile": f"java-springboot/{smoke_id}_frontend/Dockerfile",
            "frontendServiceName": smoke_service,
            "frontendPort": frontend_port,
            "frontendDockerfile": f"java-springboot/{smoke_id}_frontend/Dockerfile",
            "backendServiceName": f"{smoke_service}-api",
            "backendPort": backend_port,
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

        backend_props = (
            generated_backend_dir / "src/main/resources/application.properties"
        ).read_text(encoding="utf-8")
        if f"server.port=${{SERVER_PORT:{backend_port}}}" not in backend_props:
            fail(
                "Workshop standardization check failed:\n"
                "- Scaffold backend application.properties did not use the expected backend port template."
            )

        frontend_props = (
            generated_frontend_dir / "src/main/resources/application.properties"
        ).read_text(encoding="utf-8")
        frontend_expectations = [
            f"spring.application.name={smoke_service}-frontend",
            f"server.port=${{SERVER_PORT:{frontend_port}}}",
            f"workshop.backend.url=${{WORKSHOP_BACKEND_URL:http://127.0.0.1:{backend_port}}}",
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
            {smoke_id},
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
            generated_frontend_dir / "frontend/src/utils/workshopContent.js",
            [
                "export async function fetchWorkshopContent(viewId)",
                "/api/content/views/${encodeURIComponent(viewId)}",
            ],
            label="Scaffold workshop content client",
            relative_to=temp_dir,
        )
        assert_contains(
            generated_frontend_dir / "frontend/src/utils/components.js",
            ["WorkshopContentRenderer", "WorkshopEditorLayout"],
            label="Scaffold component exports",
            relative_to=temp_dir,
        )
        assert_contains(
            generated_frontend_dir / f"frontend/src/views/{pascal_case}Home.vue",
            [
                "WorkshopContentRenderer",
                "fetchWorkshopContent",
                f"this.content = await fetchWorkshopContent('{smoke_service}-home')",
            ],
            label="Scaffold home view",
            relative_to=temp_dir,
        )
        assert_contains(
            generated_frontend_dir / f"frontend/src/views/{pascal_case}Editor.vue",
            [
                "WorkshopEditorLayout",
                "WorkshopContentRenderer",
                "fetchWorkshopContent",
                f"this.content = await fetchWorkshopContent('{smoke_service}-editor')",
            ],
            label="Scaffold editor view",
            relative_to=temp_dir,
        )


def main() -> None:
    validate_existing_workshops()
    validate_scaffold_smoke()
    validate_compose_freshness()
    print("Workshop standardization check passed.")


if __name__ == "__main__":
    main()
PY
