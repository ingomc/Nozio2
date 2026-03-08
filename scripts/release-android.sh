#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'EOF'
Usage:
  ./scripts/release-android.sh <version> [--push]

Examples:
  ./scripts/release-android.sh 0.11.2
  ./scripts/release-android.sh 0.11.2 --push

Behavior:
  - updates apps/android/app/build.gradle.kts:
      versionName = "<version>"
      versionCode = current + 1
  - creates commit: "release: v<version>"
  - creates git tag: "v<version>"
  - with --push: pushes current branch and tag to origin
EOF
}

if [[ $# -lt 1 || $# -gt 2 ]]; then
  usage
  exit 1
fi

VERSION="$1"
PUSH=false

if [[ $# -eq 2 ]]; then
  if [[ "$2" == "--push" ]]; then
    PUSH=true
  else
    echo "Unknown option: $2" >&2
    usage
    exit 1
  fi
fi

if [[ ! "$VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
  echo "Version must be semver-like: <major>.<minor>.<patch>" >&2
  exit 1
fi

if ! git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  echo "Not inside a git repository." >&2
  exit 1
fi

if [[ -n "$(git status --porcelain)" ]]; then
  echo "Working tree is not clean. Please commit/stash first." >&2
  exit 1
fi

BRANCH="$(git symbolic-ref --quiet --short HEAD || true)"
if [[ -z "$BRANCH" ]]; then
  echo "Detached HEAD is not supported for release script." >&2
  exit 1
fi

TAG="v$VERSION"
if git rev-parse "$TAG" >/dev/null 2>&1; then
  echo "Tag $TAG already exists locally." >&2
  exit 1
fi

if git ls-remote --tags --exit-code origin "refs/tags/$TAG" >/dev/null 2>&1; then
  echo "Tag $TAG already exists on origin." >&2
  exit 1
fi

RELEASE_NOTES_FILE="RELEASE_NOTES_${VERSION}.md"
if [[ ! -f "$RELEASE_NOTES_FILE" ]]; then
  echo "Missing $RELEASE_NOTES_FILE. Create release notes file first." >&2
  exit 1
fi

GRADLE_FILE="apps/android/app/build.gradle.kts"
CURRENT_VERSION_CODE="$(sed -nE 's/^[[:space:]]*versionCode[[:space:]]*=[[:space:]]*([0-9]+).*/\1/p' "$GRADLE_FILE" | head -n1)"
CURRENT_VERSION_NAME="$(sed -nE 's/^[[:space:]]*versionName[[:space:]]*=[[:space:]]*"([^"]+)".*/\1/p' "$GRADLE_FILE" | head -n1)"

if [[ -z "$CURRENT_VERSION_CODE" || -z "$CURRENT_VERSION_NAME" ]]; then
  echo "Could not parse current version from $GRADLE_FILE." >&2
  exit 1
fi

if [[ "$CURRENT_VERSION_NAME" == "$VERSION" ]]; then
  echo "versionName is already $VERSION." >&2
  exit 1
fi

NEXT_VERSION_CODE=$((CURRENT_VERSION_CODE + 1))

perl -0777 -i -pe '
  s/versionCode\s*=\s*\d+/versionCode = '"$NEXT_VERSION_CODE"'/;
  s/versionName\s*=\s*"[^"]+"/versionName = "'"$VERSION"'"/;
' "$GRADLE_FILE"

git add "$GRADLE_FILE"
git commit -m "release: $TAG"
git tag "$TAG"

echo "Release prepared:"
echo "  branch: $BRANCH"
echo "  versionName: $CURRENT_VERSION_NAME -> $VERSION"
echo "  versionCode: $CURRENT_VERSION_CODE -> $NEXT_VERSION_CODE"
echo "  commit: release: $TAG"
echo "  tag: $TAG"

if [[ "$PUSH" == "true" ]]; then
  git push origin "$BRANCH"
  git push origin "$TAG"
  echo "Pushed branch and tag to origin."
else
  echo "Not pushed yet. Run:"
  echo "  git push origin $BRANCH"
  echo "  git push origin $TAG"
fi
