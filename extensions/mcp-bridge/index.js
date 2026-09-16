// ⚔ mcp-bridge — pi extension: connects MCP servers (mcp.json) as native tools.
// Zero dependencies: JSON-RPC 2.0 over stdio. Async factory = registered before session_start.
import { readFileSync } from "node:fs";
import { spawn } from "node:child_process";
import { homedir } from "node:os";
import { join } from "node:path";

const SERVERS_FILES = [
  join(homedir(), ".tekton", "mcp.json"),
  join(homedir(), ".pi", "agent", "mcp.json"),
];
const INIT_TIMEOUT_MS = 60000;

function loadServers() {
  for (const f of SERVERS_FILES) {
    try {
      const j = JSON.parse(readFileSync(f, "utf8"));
      const s = j.mcpServers ?? j.servers;
      if (s && Object.keys(s).length) return { servers: s, from: f };
    } catch {}
  }
  return { servers: {}, from: null };
}

class McpStdioClient {
  constructor(name, command, args) {
    this.name = name; this.command = command; this.args = args ?? [];
    this.pending = new Map(); this.nextId = 1; this.buffer = ""; this.tools = [];
  }
  start() {
    this.proc = spawn(this.command, this.args, { stdio: ["pipe", "pipe", "pipe"] });
    this.proc.unref?.();
    this.proc.stdout?.unref?.(); this.proc.stdin?.unref?.(); this.proc.stderr?.unref?.();
    this.proc.stdout?.unref?.();
    this.proc.on("exit", () => { this.dead = true; });
    this.proc.on("error", () => { this.dead = true; });
    this.proc.stdout.on("data", (d) => this.#onData(d));
    this.proc.stderr.on("data", () => {});
  }
  #onData(chunk) {
    this.buffer += chunk.toString();
    let idx;
    while ((idx = this.buffer.indexOf("\n")) >= 0) {
      const line = this.buffer.slice(0, idx).trim();
      this.buffer = this.buffer.slice(idx + 1);
      if (!line) continue;
      try {
        const msg = JSON.parse(line);
        if (msg.id !== undefined && this.pending.has(msg.id)) {
          const { resolve, reject } = this.pending.get(msg.id);
          this.pending.delete(msg.id);
          msg.error ? reject(new Error(JSON.stringify(msg.error))) : resolve(msg.result);
        }
      } catch {}
    }
  }
  send(method, params, timeoutMs = INIT_TIMEOUT_MS) {
    return new Promise((resolve, reject) => {
      const id = this.nextId++;
      this.pending.set(id, { resolve, reject });
      this.proc.stdin.write(JSON.stringify({ jsonrpc: "2.0", id, method, params }) + "\n");
      setTimeout(() => { if (this.pending.has(id)) { this.pending.delete(id); reject(new Error("mcp timeout: " + method)); } }, timeoutMs);
    });
  }
  async initialize() {
    this.start();
    await this.send("initialize", {
      protocolVersion: "2024-11-05",
      capabilities: {},
      clientInfo: { name: "tekton-mcp-bridge", version: "1.0.0" },
    });
    this.proc.stdin.write(JSON.stringify({ jsonrpc: "2.0", method: "notifications/initialized" }) + "\n");
    const res = await this.send("tools/list", {});
    this.tools = res?.tools ?? [];
    this.ready = true;
  }
  kill() { try { this.proc?.kill(); } catch {} }
  async callTool(name, args) {
    return await this.send("tools/call", { name, arguments: args ?? {} }, 120000);
  }
}

export default async function (pi) {
  const { servers, from } = loadServers();
  if (!from) return;
  const clients = [];
  for (const [name, cfg] of Object.entries(servers)) {
    const client = new McpStdioClient(name, cfg.command, cfg.args);
    try {
      await client.initialize();
    } catch (e) {
      console.error(`[mcp-bridge] ${name}: skipped (${e.message})`);
      continue;
    }
    clients.push(client);
    for (const t of client.tools) {
      const props = {};
      const required = [];
      for (const [k, v] of Object.entries(t.inputSchema?.properties ?? {})) {
        props[k] = v.type === "number" || v.type === "integer" ? { type: "number", description: v.description ?? "" }
          : v.type === "boolean" ? { type: "boolean", description: v.description ?? "" }
          : { type: "string", description: v.description ?? "" };
        if ((t.inputSchema.required ?? []).includes(k)) required.push(k);
      }
      pi.registerTool({
        name: `${name}_${t.name}`.replace(/[^a-zA-Z0-9_]/g, "_").slice(0, 60),
        label: `${name}: ${t.name}`,
        description: `[MCP:${name}] ${t.description ?? t.name}`,
        parameters: { type: "object", properties: props, required },
        async execute(toolCallId, params) {
          const res = await client.callTool(t.name, params);
          const content = res?.content ?? [];
          const text = content.map((c) => (c.type === "text" ? c.text : JSON.stringify(c))).join("\n");
          return { content: [{ type: "text", text: text || "(empty result)" }], details: {} };
        },
      });
    }
    console.error(`[mcp-bridge] ${name}: ${client.tools.length} tools registered`);
  }
  pi.on("session_shutdown", () => { for (const c of clients) c.kill(); });
  process.on("exit", () => { for (const c of clients) { try { c.proc?.kill(); } catch {} } });
}
