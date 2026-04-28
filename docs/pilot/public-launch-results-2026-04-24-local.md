# Public Launch Results 2026 04 24 Local

## Environment

1. control plane: `http://localhost:8080`
2. hub: `http://localhost:9000`
3. environment class: local public style stack
4. release model: local pilot releases active for all four public workshops

## Evidence Files

1. steady: [steady-20260424T140209Z.jsonl](/Users/raphaeldelio/Documents/GitHub/workshops/redis-beyond-the-cache-workshop/scripts/loadtest/output/public/steady-20260424T140209Z.jsonl)
2. burst: [burst-20260424T140731Z.jsonl](/Users/raphaeldelio/Documents/GitHub/workshops/redis-beyond-the-cache-workshop/scripts/loadtest/output/public/burst-20260424T140731Z.jsonl)
3. soak: [public-soak-20260424T140817Z.jsonl](/Users/raphaeldelio/Documents/GitHub/workshops/redis-beyond-the-cache-workshop/scripts/loadtest/output/public/public-soak-20260424T140817Z.jsonl)

## Results

### Steady

1. requests: `4`
2. successful launches: `4`
3. failed launches: `0`
4. average launch duration: `72.5s`
5. cleanup lag p50: `0s`
6. cleanup lag p95: `0s`

Per workshop launch duration:

1. `1_session_management`: `83s`
2. `2_full_text_search`: `72s`
3. `3_distributed_locks`: `73s`
4. `4_agent_memory`: `62s`

### Burst

1. requests: `4`
2. successful launches: `4`
3. failed launches: `0`
4. average launch duration: `35.5s`
5. cleanup lag p50: `0s`
6. cleanup lag p95: `0s`

Per workshop launch duration:

1. `1_session_management`: `36s`
2. `2_full_text_search`: `36s`
3. `3_distributed_locks`: `34s`
4. `4_agent_memory`: `36s`

### Soak

1. requests: `4`
2. successful launches: `4`
3. failed launches: `0`
4. average launch duration: `19s`
5. cleanup lag p50: `0s`
6. cleanup lag p95: `0s`

Per workshop launch duration:

1. `1_session_management`: `20s`
2. `2_full_text_search`: `20s`
3. `3_distributed_locks`: `18s`
4. `4_agent_memory`: `18s`

## Readiness And Rollback Checks

1. [public-launch-preflight.sh](/Users/raphaeldelio/Documents/GitHub/workshops/redis-beyond-the-cache-workshop/scripts/ops/public-launch-preflight.sh) passed.
2. [public-readiness-check.sh](/Users/raphaeldelio/Documents/GitHub/workshops/redis-beyond-the-cache-workshop/scripts/ops/public-readiness-check.sh) passed with live route retirement checks enabled.
3. [public-cleanup-check.sh](/Users/raphaeldelio/Documents/GitHub/workshops/redis-beyond-the-cache-workshop/scripts/loadtest/public-cleanup-check.sh) passed against the latest soak evidence.
4. [public-rollback-validation.sh](/Users/raphaeldelio/Documents/GitHub/workshops/redis-beyond-the-cache-workshop/scripts/ops/public-rollback-validation.sh) passed after the wrapper was corrected to validate the active rollback target model for the current environment.

## Repair Notes

Two defects were found and fixed during the evidence pass:

1. [public-readiness-check.sh](/Users/raphaeldelio/Documents/GitHub/workshops/redis-beyond-the-cache-workshop/scripts/ops/public-readiness-check.sh) now reads JSONL evidence correctly with `jq -s`.
2. [public-summary.sh](/Users/raphaeldelio/Documents/GitHub/workshops/redis-beyond-the-cache-workshop/scripts/loadtest/public-summary.sh) and [public-cleanup-check.sh](/Users/raphaeldelio/Documents/GitHub/workshops/redis-beyond-the-cache-workshop/scripts/loadtest/public-cleanup-check.sh) now choose the newest log by modification time instead of filename sort.
3. [public-rollback-validation.sh](/Users/raphaeldelio/Documents/GitHub/workshops/redis-beyond-the-cache-workshop/scripts/ops/public-rollback-validation.sh) now validates the live release state against the configured rollback target model for the current environment.

## Decision

1. ship recommendation: yes for the public repo surface and local public style validation
2. remaining caveat: this is local evidence, not an internet facing Cloud Run rollout
3. next practical step: run the Cloud Run 100 user window against the real control plane endpoint and store the matching results file
