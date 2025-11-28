#!/bin/bash
# POC Dating Database Backup Script
# Version: 2.0
#
# PURPOSE: Create database backups with retention
#
# USAGE:
# ./backup.sh [full|schema|data]
#
# SCHEDULE: Run via cron
# 0 2 * * * /path/to/backup.sh full

set -e

# ========================================
# CONFIGURATION
# ========================================
BACKUP_DIR="/var/backups/dating_db"
RETENTION_DAYS=7
DB_NAME="dating_db"
DB_USER="dating_user"
DB_HOST="${POSTGRES_HOST:-localhost}"
DB_PORT="${POSTGRES_PORT:-5432}"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

# Create backup directory if not exists
mkdir -p "${BACKUP_DIR}"

# ========================================
# FUNCTIONS
# ========================================

log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1"
}

full_backup() {
    log "Starting full backup..."

    BACKUP_FILE="${BACKUP_DIR}/${DB_NAME}_full_${TIMESTAMP}.sql.gz"

    pg_dump \
        -h "${DB_HOST}" \
        -p "${DB_PORT}" \
        -U "${DB_USER}" \
        -d "${DB_NAME}" \
        --format=custom \
        --compress=9 \
        --verbose \
        --file="${BACKUP_FILE}"

    log "Full backup completed: ${BACKUP_FILE}"
    log "Size: $(du -h "${BACKUP_FILE}" | cut -f1)"
}

schema_backup() {
    log "Starting schema-only backup..."

    BACKUP_FILE="${BACKUP_DIR}/${DB_NAME}_schema_${TIMESTAMP}.sql"

    pg_dump \
        -h "${DB_HOST}" \
        -p "${DB_PORT}" \
        -U "${DB_USER}" \
        -d "${DB_NAME}" \
        --schema-only \
        --no-owner \
        --no-privileges \
        --file="${BACKUP_FILE}"

    gzip "${BACKUP_FILE}"

    log "Schema backup completed: ${BACKUP_FILE}.gz"
}

data_backup() {
    log "Starting data-only backup..."

    BACKUP_FILE="${BACKUP_DIR}/${DB_NAME}_data_${TIMESTAMP}.sql.gz"

    pg_dump \
        -h "${DB_HOST}" \
        -p "${DB_PORT}" \
        -U "${DB_USER}" \
        -d "${DB_NAME}" \
        --data-only \
        --format=custom \
        --compress=9 \
        --file="${BACKUP_FILE}"

    log "Data backup completed: ${BACKUP_FILE}"
    log "Size: $(du -h "${BACKUP_FILE}" | cut -f1)"
}

cleanup_old_backups() {
    log "Cleaning up backups older than ${RETENTION_DAYS} days..."

    find "${BACKUP_DIR}" -name "${DB_NAME}_*.sql*" -type f -mtime +${RETENTION_DAYS} -delete

    log "Cleanup completed."
}

verify_backup() {
    LATEST_BACKUP=$(ls -t "${BACKUP_DIR}"/${DB_NAME}_full_*.sql.gz 2>/dev/null | head -1)

    if [ -n "${LATEST_BACKUP}" ]; then
        log "Verifying backup: ${LATEST_BACKUP}"

        # Test restore to /dev/null
        if pg_restore --list "${LATEST_BACKUP}" > /dev/null 2>&1; then
            log "Backup verification: PASSED"
        else
            log "Backup verification: FAILED"
            exit 1
        fi
    else
        log "No backup found to verify"
    fi
}

list_backups() {
    log "Available backups:"
    ls -lh "${BACKUP_DIR}"/${DB_NAME}_*.sql* 2>/dev/null || echo "No backups found"
}

# ========================================
# MAIN
# ========================================

case "${1:-full}" in
    full)
        full_backup
        cleanup_old_backups
        verify_backup
        ;;
    schema)
        schema_backup
        ;;
    data)
        data_backup
        ;;
    cleanup)
        cleanup_old_backups
        ;;
    verify)
        verify_backup
        ;;
    list)
        list_backups
        ;;
    *)
        echo "Usage: $0 [full|schema|data|cleanup|verify|list]"
        exit 1
        ;;
esac

log "Backup operation completed successfully!"
