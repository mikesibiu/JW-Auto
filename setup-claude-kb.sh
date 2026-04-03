#!/usr/bin/env bash
# Claude Code Knowledge Base
# Run once in any project root: bash setup-claude-kb.sh
set -euo pipefail

PROJECT_ROOT="$(pwd)"
CLAUDE_DIR="$PROJECT_ROOT/.claude"

mkdir -p "$CLAUDE_DIR"

# ── 1. kb.py ──────────────────────────────────────────────────────────────────
cat > "$CLAUDE_DIR/kb.py" << 'PYTHON_EOF'
#!/usr/bin/env python3
"""
Usage:
  python3 .claude/kb.py search <query>
  python3 .claude/kb.py learn  <topic> | <content> [| tags]
  python3 .claude/kb.py failed <topic> | <what was tried> | <why it failed> [| tags]
  python3 .claude/kb.py list
"""
import sqlite3, sys, datetime
from pathlib import Path

DB = Path(__file__).parent / "knowledge.db"

SCHEMA = """
CREATE TABLE IF NOT EXISTS knowledge (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    outcome    TEXT NOT NULL DEFAULT 'learned',
    topic      TEXT NOT NULL,
    content    TEXT NOT NULL,
    reason     TEXT DEFAULT '',
    tags       TEXT DEFAULT '',
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL
);
CREATE VIRTUAL TABLE IF NOT EXISTS kb_fts
    USING fts5(topic, content, tags, content=knowledge, content_rowid=id);
CREATE TRIGGER IF NOT EXISTS kb_ai AFTER INSERT ON knowledge BEGIN
    INSERT INTO kb_fts(rowid, topic, content, tags)
    VALUES (new.id, new.topic, new.content, new.tags);
END;
CREATE TRIGGER IF NOT EXISTS kb_au AFTER UPDATE ON knowledge BEGIN
    INSERT INTO kb_fts(kb_fts, rowid, topic, content, tags)
    VALUES ('delete', old.id, old.topic, old.content, old.tags);
    INSERT INTO kb_fts(rowid, topic, content, tags)
    VALUES (new.id, new.topic, new.content, new.tags);
END;
"""

ICONS = {'learned': '✅', 'failed': '❌'}

def now():
    return datetime.datetime.now(datetime.timezone.utc).isoformat(timespec='seconds')

def db():
    c = sqlite3.connect(DB)
    c.row_factory = sqlite3.Row
    c.executescript(SCHEMA)
    return c

def search(args):
    if not args:
        print("Usage: kb.py search <query>"); return
    query = ' '.join(args)
    rows = db().execute("""
        SELECT k.id, k.outcome, k.topic, k.content, k.reason, k.tags, k.updated_at
        FROM kb_fts f JOIN knowledge k ON f.rowid = k.id
        WHERE kb_fts MATCH ? ORDER BY rank LIMIT 10
    """, (query,)).fetchall()
    if not rows:
        print("(no results)"); return
    for r in rows:
        icon = ICONS.get(r['outcome'], '•')
        print(f"{icon} [{r['outcome'].upper()}] {r['topic']}")
        print(f"    {r['content']}")
        if r['reason']:
            print(f"    why it failed: {r['reason']}")
        if r['tags']:
            print(f"    tags: {r['tags']}")
        print(f"    updated: {r['updated_at'][:10]}")
        print()

def learn(args):
    raw = ' '.join(args)
    parts = [p.strip() for p in raw.split('|')]
    if len(parts) < 2:
        print("Usage: kb.py learn <topic> | <content> [| tags]"); sys.exit(1)
    _save('learned', parts[0], parts[1], '', parts[2] if len(parts) > 2 else '')

def failed(args):
    raw = ' '.join(args)
    parts = [p.strip() for p in raw.split('|')]
    if len(parts) < 3:
        print("Usage: kb.py failed <topic> | <what was tried> | <why it failed> [| tags]"); sys.exit(1)
    _save('failed', parts[0], parts[1], parts[2], parts[3] if len(parts) > 3 else '')

def _save(outcome, topic, content, reason, tags):
    ts = now()
    c = db()
    existing = c.execute(
        "SELECT id FROM knowledge WHERE topic = ? COLLATE NOCASE AND outcome = ? LIMIT 1",
        (topic, outcome)
    ).fetchone()
    if existing:
        c.execute(
            "UPDATE knowledge SET content=?, reason=?, tags=?, updated_at=? WHERE id=?",
            (content, reason, tags, ts, existing['id'])
        )
        c.commit()
        print(f"{ICONS[outcome]} Updated [{topic}]")
    else:
        c.execute(
            "INSERT INTO knowledge(outcome,topic,content,reason,tags,created_at,updated_at) VALUES(?,?,?,?,?,?,?)",
            (outcome, topic, content, reason, tags, ts, ts)
        )
        c.commit()
        print(f"{ICONS[outcome]} Saved [{outcome}] {topic}")

def list_all(_=None):
    rows = db().execute(
        "SELECT id, outcome, topic, tags, updated_at FROM knowledge ORDER BY outcome, updated_at DESC"
    ).fetchall()
    if not rows:
        print("(empty)"); return
    print(f"{'':2} {'ID':<5} {'Outcome':<10} {'Topic':<38} {'Tags':<20} Updated")
    print("─" * 82)
    for r in rows:
        icon = ICONS.get(r['outcome'], '•')
        print(f"{icon}  {r['id']:<5} {r['outcome']:<10} {r['topic'][:37]:<38} {(r['tags'] or '')[:19]:<20} {r['updated_at'][:10]}")

CMDS = {
    'search': search,
    'learn':  learn,
    'failed': failed,
    'list':   list_all,
}

if __name__ == '__main__':
    if len(sys.argv) < 2 or sys.argv[1] not in CMDS:
        print(__doc__); sys.exit(0)
    CMDS[sys.argv[1]](sys.argv[2:])
PYTHON_EOF

chmod +x "$CLAUDE_DIR/kb.py"
python3 "$CLAUDE_DIR/kb.py" list > /dev/null
echo "✅ knowledge.db initialised"

# ── 2. CLAUDE.md ──────────────────────────────────────────────────────────────
CLAUDE_MD="$PROJECT_ROOT/CLAUDE.md"

if [ ! -f "$CLAUDE_MD" ]; then
    cat > "$CLAUDE_MD" << 'CLAUDE_EOF'
# Project Knowledge Base

## About this project
<!-- Describe this project briefly on your first session -->

---

## Workflow — follow this on every interaction, without being asked

### Step 1 — Search before doing anything
Before answering, suggesting an approach, writing code, or making changes:

```bash
python3 .claude/kb.py search <keywords relevant to the task>
```

Read the results and act on them:

- **outcome: LEARNED** — this is established knowledge for this project. Use it.
  Don't re-derive it. Don't second-guess it. Don't ask the user to confirm it.

- **outcome: FAILED** — this approach has already been tried and failed in this project.
  Do NOT suggest it again. Explicitly tell the user "we already tried X and it failed
  because Y" and move on to a different approach.

If the KB is empty on a topic, proceed with general knowledge and note it.

### Step 2 — Save what you learn
After any non-trivial interaction, save durable knowledge. Ask yourself:
*"If I had amnesia right now, what would I wish I knew about this project?"*

**Save a learned fact:**
```bash
python3 .claude/kb.py learn <topic> | <what is true / what works> | <tags>
```

**Save a failed attempt:**
```bash
python3 .claude/kb.py failed <topic> | <what was tried> | <why it failed> | <tags>
```

### What is worth saving

| Situation | What to save | Command |
|-----------|-------------|---------|
| Something works a specific way in this project | The fact | `learn` |
| A bug was fixed | The symptom and the fix | `learn` |
| An approach was tried and didn't work | What was tried and why it failed | `failed` |
| A suggestion was rejected by the user | What was suggested and why it was wrong | `failed` |
| A design decision was made | The choice and the reasoning | `learn` |
| A config quirk or non-obvious setting | The detail | `learn` |
| A pattern or convention specific to this project | The pattern | `learn` |

### What is NOT worth saving
- General knowledge true of all projects of this type
- Anything instantly findable in official documentation
- Temporary or session-specific context

### Topic naming
Keep topics short and specific. Good: `webpack hmr config`, `auth token expiry`,
`vault password file location`. Bad: `bug`, `issue`, `thing we discussed`.
CLAUDE_EOF
    echo "✅ Created CLAUDE.md"
else
    echo "ℹ️  CLAUDE.md already exists — add the workflow block manually if needed"
fi

# ── 3. .gitignore ─────────────────────────────────────────────────────────────
GITIGNORE="$PROJECT_ROOT/.gitignore"
ENTRY="# Claude KB temp files\n.claude/knowledge.db-wal\n.claude/knowledge.db-shm"
if [ -f "$GITIGNORE" ]; then
    grep -q "knowledge.db-wal" "$GITIGNORE" || printf "\n$ENTRY\n" >> "$GITIGNORE"
else
    printf "$ENTRY\n" > "$GITIGNORE"
fi
echo "✅ .gitignore updated"

# ── Done ──────────────────────────────────────────────────────────────────────
echo ""
echo "Files created:"
echo "  .claude/kb.py          ← search / learn / failed / list"
echo "  .claude/knowledge.db   ← SQLite (commit this to git)"
echo "  CLAUDE.md              ← workflow rules Claude reads every session"
echo ""
echo "Commands:"
echo "  python3 .claude/kb.py search <query>"
echo "  python3 .claude/kb.py learn  <topic> | <what works> | <tags>"
echo "  python3 .claude/kb.py failed <topic> | <what was tried> | <why it failed> | <tags>"
echo "  python3 .claude/kb.py list"
