#!/bin/bash
# Runs the ore sampler once per seed on a fresh normal world.
# usage: tools/ore-sampling/run_sampler.sh <label> <repo_dir> <seed>...
#   label    "mod" for this repository, "control" for a worktree of the pre-change commit
#   repo_dir checkout to run (it must contain OreSampler and the runOreSampler task)
# Output: <repo_dir>/build/ore-sampler/<seed>-<label>-*.csv/png; logs in <repo_dir>/build/ore-sampler-logs.
set -u
label=$1; repo=$2; shift 2
logs="$repo/build/ore-sampler-logs"; mkdir -p "$logs"
for seed in "$@"; do
  rd="$repo/build/run/oreSampler"
  rm -rf "$rd/world"; mkdir -p "$rd"
  echo "eula=true" > "$rd/eula.txt"
  cat > "$rd/server.properties" <<PROPS
level-seed=$seed
level-type=minecraft\:normal
online-mode=false
max-tick-time=-1
spawn-protection=0
view-distance=3
simulation-distance=3
generate-structures=true
sync-chunk-writes=false
PROPS
  start=$(date +%s)
  (cd "$repo" && ./gradlew runOreSampler -PoreSamplerLabel="$label" --console=plain) > "$logs/$label-$seed.log" 2>&1
  echo "$label $seed exit $? in $(( $(date +%s) - start ))s" | tee -a "$logs/summary.txt"
done
