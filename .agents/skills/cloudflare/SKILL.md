# Cloudflare Skills

**Source:** awesome-agent-skills (VoltAgent)

## Verfügbare Skills

### Cloudflare Tunnel
- Tunnel-Management via `cloudflared`
- Port-Mapping, DNS, HTTPS config
-connector status, logs

### Cloudflare Workers
- Workers schreiben (JavaScript/Rust)
- KV, Durable Objects, D1
- Wrangler CLI

### Cloudflare Pages
- Pages deployen
- Functions (Workers auf Pages)
- Analytics

### Cloudflare Zero Trust
- Access policies
- WARP client
- Tunnel config

## Tipps

- Immer `wrangler deploy` für Workers
- Workers brauchen `wrangler.toml`
- KV: `await NAMESPACE.get(key)`
- D1: SQL in .sql files

## Wann aktivieren

- Tunnel/Workers/Pages/DNS/Zero Trust
- Cloudflare API calls
- `*.pages.dev` / `*.workers.dev`
