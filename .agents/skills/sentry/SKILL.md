# Sentry Skills

**Source:** awesome-agent-skills (VoltAgent)

## Sentry SDK Integration

### JavaScript/Node

```javascript
import * as Sentry from '@sentry/node';

Sentry.init({
  dsn: 'https://xxx@sentry.io/xxx',
  tracesSampleRate: 1.0,
  environment: process.env.NODE_ENV
});

// Error capture
try {
  // code
} catch (e) {
  Sentry.captureException(e);
}

// Message
Sentry.captureMessage('Something happened', 'warning');
```

### Mit Context

```javascript
Sentry.setUser({ id: user.id, email: user.email });
Sentry.setTag('version', '1.0.0');
Sentry.setExtra('details', { orderId: 123 });
```

## Issues lesen

```javascript
const response = await fetch(
  `https://sentry.io/api/0/projects/${org}/${project}/issues/`,
  { headers: { Authorization: `Bearer ${TOKEN}` } }
);
```

## Wann aktivieren

- Error Tracking
- Performance Monitoring
- Release Health
- Sentry API Integration
