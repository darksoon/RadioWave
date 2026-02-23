# MCP (Model Context Protocol) Skills

**Source:** awesome-agent-skills (VoltAgent)

## Was ist MCP?

- Standard für AI ↔ Tools Kommunikation
- AI kann externe Services steuern
- JSON-RPC 2.0 basiert

## MCP Builder Skill

### Erstellen eines MCP Servers

```javascript
// server.js
import { Server } from '@modelcontextprotocol/sdk/server/index.js';
import { StdioServerTransport } from '@modelcontextprotocol/sdk/server/stdio.js';

const server = new Server({
  name: 'my-mcp-server',
  version: '1.0.0'
}, {
  capabilities: {
    tools: {}
  }
});

server.setRequestHandler('tools/list', async () => {
  return {
    tools: [{
      name: 'my_tool',
      description: 'Does something useful',
      inputSchema: {
        type: 'object',
        properties: {
          param: { type: 'string' }
        }
      }
    }]
  };
});

const transport = new StdioServerTransport();
await server.connect(transport);
```

### Tools definieren

- `name`: Tool-Name (kebab-case)
- `description`: Was das Tool macht
- `inputSchema`: Parameter-Schema

## MCP Clients

- Claude Desktop
- Claude Code
- Cursor
- Windsurf

## Wann aktivieren

- MCP Server bauen
- External APIs einbinden
- Tool-basierte Automation
