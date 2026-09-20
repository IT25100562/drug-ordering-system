import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * MediSys live database viewer (a development tool, not part of the app).
 *
 * Shows every table of MediSysDB in a grid and refreshes it every 2 seconds,
 * so rows the app inserts or updates appear (highlighted) while you click
 * around in the app. It is READ-ONLY: it only runs SELECT queries.
 *
 * It uses the same login as the app (src/main/resources/db.properties).
 * Start it with tools\db-viewer\run.ps1, then open http://localhost:8765
 * (in VS Code: Ctrl+Shift+P, "Simple Browser: Show").
 */
public class DbViewer {

    static final int PORT = 8765;
    static final int MAX_ROWS = 200;

    static String url, user, password;

    public static void main(String[] args) throws Exception {
        String propsFile = args.length > 0 ? args[0] : "src/main/resources/db.properties";
        Properties p = new Properties();
        try (InputStream in = new FileInputStream(propsFile)) {
            p.load(in);
        }
        url = p.getProperty("db.url");
        user = p.getProperty("db.user");
        password = p.getProperty("db.password");

        // Fail early with a clear message if the database can't be reached.
        try (Connection c = connect()) {
            System.out.println("[db-viewer] Connected to " + c.getCatalog());
        }

        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", PORT), 0);
        server.createContext("/", DbViewer::handle);
        server.start();
        System.out.println("[db-viewer] Open http://localhost:" + PORT + "  (Ctrl+C to stop)");
    }

    static Connection connect() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    static void handle(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        try {
            if (path.equals("/api/tables")) {
                send(ex, 200, "application/json", tablesJson());
            } else if (path.equals("/api/table")) {
                send(ex, 200, "application/json", tableJson(query(ex, "name")));
            } else if (path.equals("/")) {
                send(ex, 200, "text/html", PAGE);
            } else {
                send(ex, 404, "text/plain", "Not found");
            }
        } catch (Exception e) {
            send(ex, 500, "application/json", "{\"error\":" + json(e.getMessage()) + "}");
        }
    }

    /** Every user table with its row count. */
    static String tablesJson() throws SQLException {
        String sql = "SELECT t.name, SUM(p.rows) FROM sys.tables t "
                + "JOIN sys.partitions p ON p.object_id = t.object_id AND p.index_id IN (0, 1) "
                + "GROUP BY t.name ORDER BY t.name";
        StringBuilder sb = new StringBuilder("[");
        try (Connection c = connect(); Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                if (sb.length() > 1) sb.append(',');
                sb.append("{\"name\":").append(json(rs.getString(1)))
                  .append(",\"rows\":").append(rs.getLong(2)).append('}');
            }
        }
        return sb.append(']').toString();
    }

    /** The newest MAX_ROWS rows of one table (by its first column, usually the id). */
    static String tableJson(String name) throws SQLException {
        if (!tableNames().contains(name)) {
            throw new SQLException("Unknown table: " + name);
        }
        // The name was checked against sys.tables above, so it is safe to put in the SQL.
        String sql = "SELECT TOP " + MAX_ROWS + " * FROM dbo.[" + name + "] ORDER BY 1 DESC";
        StringBuilder sb = new StringBuilder("{\"columns\":[");
        try (Connection c = connect(); Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            ResultSetMetaData md = rs.getMetaData();
            int n = md.getColumnCount();
            for (int i = 1; i <= n; i++) {
                if (i > 1) sb.append(',');
                sb.append(json(md.getColumnName(i)));
            }
            sb.append("],\"rows\":[");
            boolean first = true;
            while (rs.next()) {
                if (!first) sb.append(',');
                first = false;
                sb.append('[');
                for (int i = 1; i <= n; i++) {
                    if (i > 1) sb.append(',');
                    Object v = rs.getObject(i);
                    if (v instanceof byte[]) v = "(" + ((byte[]) v).length + " bytes)";
                    sb.append(v == null ? "null" : json(v.toString()));
                }
                sb.append(']');
            }
        }
        return sb.append("]}").toString();
    }

    static List<String> tableNames() throws SQLException {
        List<String> names = new ArrayList<>();
        try (Connection c = connect(); Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT name FROM sys.tables")) {
            while (rs.next()) names.add(rs.getString(1));
        }
        return names;
    }

    static String query(HttpExchange ex, String key) {
        String q = ex.getRequestURI().getRawQuery();
        if (q == null) return "";
        for (String part : q.split("&")) {
            String[] kv = part.split("=", 2);
            if (kv[0].equals(key) && kv.length == 2) {
                return URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
            }
        }
        return "";
    }

    static String json(String s) {
        if (s == null) return "null";
        StringBuilder sb = new StringBuilder("\"");
        for (char ch : s.toCharArray()) {
            switch (ch) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (ch < 0x20) sb.append(String.format("\\u%04x", (int) ch));
                    else sb.append(ch);
            }
        }
        return sb.append('"').toString();
    }

    static void send(HttpExchange ex, int status, String type, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", type + "; charset=utf-8");
        ex.getResponseHeaders().set("Cache-Control", "no-store");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    // ------------------------------------------------------------------
    // The page: a table list on the left, the selected table's rows on the
    // right. It asks the server again every 2 seconds and highlights rows
    // that are new or changed since the last refresh.
    // ------------------------------------------------------------------
    static final String PAGE = """
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8">
<title>MediSysDB live</title>
<style>
  :root { --bg:#1e1e1e; --panel:#252526; --line:#3c3c3c; --text:#d4d4d4; --muted:#8a8a8a;
          --accent:#4fc1ff; --new:#264f2e; --changed:#4b4520; --head:#2d2d30; }
  @media (prefers-color-scheme: light) {
    :root { --bg:#ffffff; --panel:#f3f3f3; --line:#dddddd; --text:#1f1f1f; --muted:#6b6b6b;
            --accent:#0066b8; --new:#d7f5dc; --changed:#fff4c2; --head:#ececec; }
  }
  * { box-sizing:border-box; }
  body { margin:0; font:13px/1.4 "Segoe UI", system-ui, sans-serif; background:var(--bg); color:var(--text);
         display:flex; height:100vh; overflow:hidden; }
  aside { width:220px; flex:none; background:var(--panel); border-right:1px solid var(--line); overflow:auto; }
  aside h1 { font-size:13px; margin:0; padding:12px; border-bottom:1px solid var(--line); }
  aside button { display:flex; justify-content:space-between; width:100%; padding:6px 12px; border:0;
                 background:none; color:inherit; font:inherit; text-align:left; cursor:pointer; }
  aside button:hover { background:var(--head); }
  aside button.active { background:var(--head); color:var(--accent); font-weight:600; }
  aside .count { color:var(--muted); font-variant-numeric:tabular-nums; }
  aside .count.bump { color:var(--accent); font-weight:700; }
  main { flex:1; display:flex; flex-direction:column; min-width:0; }
  header { display:flex; gap:12px; align-items:center; padding:10px 14px; border-bottom:1px solid var(--line); }
  header h2 { font-size:15px; margin:0; }
  header .status { margin-left:auto; color:var(--muted); font-size:12px; }
  .dot { display:inline-block; width:8px; height:8px; border-radius:50%; background:#3fb950; margin-right:6px; }
  .dot.off { background:#f85149; }
  label { color:var(--muted); font-size:12px; }
  .wrap { flex:1; overflow:auto; }
  table { border-collapse:collapse; font-family:Consolas, "Cascadia Mono", monospace; font-size:12px; }
  th, td { border:1px solid var(--line); padding:4px 8px; white-space:nowrap; max-width:320px;
           overflow:hidden; text-overflow:ellipsis; text-align:left; }
  th { position:sticky; top:0; background:var(--head); z-index:1; }
  td.null { color:var(--muted); font-style:italic; }
  tr.new td { background:var(--new); }
  tr.changed td { background:var(--changed); }
  .empty, .error { padding:24px; color:var(--muted); }
  .error { color:#f85149; }
</style>
</head>
<body>
<aside><h1>MediSysDB</h1><div id="tables"></div></aside>
<main>
  <header>
    <h2 id="title">Pick a table</h2>
    <label><input type="checkbox" id="live" checked> Live (every 2 s)</label>
    <span class="status"><span class="dot" id="dot"></span><span id="status">connecting…</span></span>
  </header>
  <div class="wrap" id="grid"><div class="empty">Choose a table on the left.</div></div>
</main>
<script>
let current = localStorage.getItem("table") || "orders";
let lastRows = null;          // key -> row JSON, from the previous refresh of this table
let lastCounts = {};

const esc = s => String(s).replace(/[&<>"]/g, c => ({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;"}[c]));

async function getJson(url) {
  const r = await fetch(url);
  const data = await r.json();
  if (!r.ok) throw new Error(data.error || r.statusText);
  return data;
}

async function refreshTables() {
  const list = await getJson("/api/tables");
  document.getElementById("tables").innerHTML = list.map(t => {
    const bump = lastCounts[t.name] !== undefined && lastCounts[t.name] !== t.rows;
    return `<button data-name="${esc(t.name)}" class="${t.name === current ? "active" : ""}">
              <span>${esc(t.name)}</span><span class="count ${bump ? "bump" : ""}">${t.rows}</span></button>`;
  }).join("");
  list.forEach(t => lastCounts[t.name] = t.rows);
}

async function refreshTable() {
  const data = await getJson("/api/table?name=" + encodeURIComponent(current));
  document.getElementById("title").textContent = `${current}  (newest ${data.rows.length} rows)`;
  if (!data.rows.length) {
    document.getElementById("grid").innerHTML = '<div class="empty">No rows yet.</div>';
    lastRows = new Map();
    return;
  }
  const now = new Map(data.rows.map(r => [String(r[0]), JSON.stringify(r)]));
  const head = "<tr>" + data.columns.map(c => `<th>${esc(c)}</th>`).join("") + "</tr>";
  const body = data.rows.map(r => {
    const key = String(r[0]);
    let cls = "";
    if (lastRows && !lastRows.has(key)) cls = "new";
    else if (lastRows && lastRows.get(key) !== now.get(key)) cls = "changed";
    return `<tr class="${cls}">` + r.map(v => v === null
        ? '<td class="null">NULL</td>'
        : `<td title="${esc(v)}">${esc(v)}</td>`).join("") + "</tr>";
  }).join("");
  document.getElementById("grid").innerHTML = `<table><thead>${head}</thead><tbody>${body}</tbody></table>`;
  lastRows = now;
}

async function tick() {
  try {
    await refreshTables();
    await refreshTable();
    document.getElementById("dot").className = "dot";
    document.getElementById("status").textContent = "updated " + new Date().toLocaleTimeString();
  } catch (e) {
    document.getElementById("dot").className = "dot off";
    document.getElementById("status").textContent = "error: " + e.message;
  }
}

document.getElementById("tables").addEventListener("click", e => {
  const b = e.target.closest("button");
  if (!b) return;
  current = b.dataset.name;
  try { localStorage.setItem("table", current); } catch (_) {}
  lastRows = null;
  tick();
});

setInterval(() => { if (document.getElementById("live").checked) tick(); }, 2000);
tick();
</script>
</body>
</html>
""";
}
