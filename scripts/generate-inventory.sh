#!/bin/bash
#
# generate-inventory.sh
# Regenerates REPOSITORY_INVENTORY.md based on current repository state
#
# Usage: ./scripts/generate-inventory.sh
#
# This script is called by the pre-commit hook to keep the inventory up-to-date.
# It can also be run manually to regenerate the inventory.

set -e

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
INVENTORY_FILE="${REPO_ROOT}/REPOSITORY_INVENTORY.md"

echo "Generating repository inventory..."
echo "Repository root: ${REPO_ROOT}"

# Count files
TOTAL_FILES=$(find "${REPO_ROOT}" -type f \
    ! -path "*/.git/*" \
    ! -path "*/node_modules/*" \
    ! -path "*/target/*" \
    ! -path "*/.idea/*" \
    ! -path "*/.vscode/*" \
    ! -name "*.class" \
    ! -name "*.jar" \
    | wc -l | tr -d ' ')

# Calculate total size
TOTAL_SIZE=$(find "${REPO_ROOT}" -type f \
    ! -path "*/.git/*" \
    ! -path "*/node_modules/*" \
    ! -path "*/target/*" \
    ! -path "*/.idea/*" \
    ! -path "*/.vscode/*" \
    ! -name "*.class" \
    ! -name "*.jar" \
    -exec du -ch {} + 2>/dev/null | tail -1 | cut -f1)

# Get current date
CURRENT_DATE=$(date +%Y-%m-%d)

echo "Total files: ${TOTAL_FILES}"
echo "Total size: ${TOTAL_SIZE}"
echo "Date: ${CURRENT_DATE}"

# Check if inventory exists
if [ -f "${INVENTORY_FILE}" ]; then
    # Update the Generated date in the inventory
    if [[ "$OSTYPE" == "darwin"* ]]; then
        # macOS
        sed -i '' "s/^\*\*Generated:\*\* .*/\*\*Generated:\*\* ${CURRENT_DATE}/" "${INVENTORY_FILE}"
    else
        # Linux
        sed -i "s/^\*\*Generated:\*\* .*/\*\*Generated:\*\* ${CURRENT_DATE}/" "${INVENTORY_FILE}"
    fi

    echo "Updated generation date in ${INVENTORY_FILE}"

    # Note: Full regeneration would require more complex scripting or
    # invoking Claude Code to analyze all files again.
    # This basic script just updates the date and can be extended.

    echo ""
    echo "NOTE: For full regeneration with file analysis, run:"
    echo "  claude 'Regenerate REPOSITORY_INVENTORY.md with current file structure'"
    echo ""
else
    echo "Warning: ${INVENTORY_FILE} not found"
    echo "Run 'claude' to generate the initial inventory"
    exit 1
fi

echo "Inventory update complete!"
