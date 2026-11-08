#!/usr/bin/env sh
set -eu
umask 077
project_dir=${FAMILYFIT_ROOT:-/opt/familyfit}
backup_root=${FAMILYFIT_BACKUP_ROOT:-/var/backups/familyfit}
stamp=$(date -u +%Y%m%d-%H%M%S)
destination="$backup_root/$stamp"
backup_name="familyfit-$stamp.db"
mkdir -p "$destination"
cd "$project_dir"
compose_file=${FAMILYFIT_COMPOSE_FILE:-compose.registry.yaml}
docker compose -f "$compose_file" exec -T web dotnet FamilyFit.Web.dll --backup --backup-name "$backup_name"
docker compose -f "$compose_file" cp "web:/data/backups/$backup_name" "$destination/familyfit.db"
docker compose -f "$compose_file" cp web:/data/keys "$destination/keys"
docker compose -f "$compose_file" cp web:/data/media "$destination/media"
printf 'Sauvegarde créée dans %s\n' "$destination"
# Replicate this directory to the operator's existing external backup system.
# No deletion/retention policy is applied automatically.
