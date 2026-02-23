# Vercel Skills

**Source:** awesome-agent-skills (VoltAgent)

## Verfügbare Skills

### Vercel Deployment
- Deploy zu Vercel
- Preview URLs
- Production Deploys
- Alias Management

### React Best Practices
- Next.js patterns
- Server Components
- App Router
- Optimizations

### Web Design Guidelines
- Design System
- Tailwind Config
- Component Library
- Accessibility

## CLI Commands

```bash
# Deploy
vercel

# Production
vercel --prod

# With environment
vercel -e KEY=VALUE

# Secrets
vercel secrets add my-secret "value"
```

## Vercel API

```javascript
// Get deployment status
const response = await fetch(
  `https://api.vercel.com/v6/deployments/${deploymentId}`,
  { headers: { Authorization: `Bearer ${TOKEN}` } }
);
```

## Wann aktivieren

- Next.js / Vercel Deployments
- React/Frontend Development
- Serverless Functions
- Edge Functions
