# Claude Code Knowledge Base

A lightweight, per-project memory system for Claude Code. The longer you work with Claude on a project, the smarter it gets about that project — and it stops repeating the same mistakes.

---

## How it works

Claude Code automatically reads `CLAUDE.md` at the start of every session. This file contains rules that tell Claude to search a local SQLite database before responding to anything, and to save what it learns after every non-trivial interaction.

You don't type any commands. You don't manage the database. You just work with Claude as normal — it handles the memory automatically in the background.

---

## Setup

Run once in any project directory:

```bash
bash setup-claude-kb.sh
```

Then restart Claude Code. That's all.

### What gets created

```
your-project/
├── CLAUDE.md               ← workflow rules Claude reads every session
└── .claude/
    ├── kb.py               ← the database manager (pure Python stdlib)
    └── knowledge.db        ← SQLite knowledge base (commit this to git)
```

---

## ⚠️ Critical: The Pipe Syntax Problem

The `kb.py` commands use `|` as a delimiter between topic, content, and tags.
**The shell treats unquoted `|` as a pipe operator and will break the command.**

### WRONG (shell interprets | as pipe — command fails):
```bash
python3 .claude/kb.py learn chicken breed | ISA Brown chosen for eggs | livestock
```

### CORRECT — wrap the whole thing in a quoted string:
```bash
python3 .claude/kb.py learn "chicken breed | ISA Brown chosen for eggs | livestock"
```

### Also correct — escape each pipe:
```bash
python3 .claude/kb.py learn chicken breed \| ISA Brown chosen for eggs \| livestock
```

**Always use the quoted string form.** It's the simplest and most readable.

The same applies to `failed` entries:
```bash
python3 .claude/kb.py failed "3D model east-west orientation | camera looking in +z puts east on LEFT | use n2z = D-N and camera south of property looking in -z | 3d,coordinates"
```

---

## Seeding the KB at project start

The KB is only useful if it has content. At the start of a new project, **manually seed it** with the most important facts before Claude can learn them organically. Good candidates:

- What the project is and what it does
- Key files and what they contain
- Critical decisions already made
- Things that are always true (e.g. "chickens are always in the plan")
- Git workflow for this repo
- Any non-obvious conventions

Use a Python heredoc to avoid pipe issues when seeding many entries at once:

```bash
python3 - <<'PYEOF'
import sqlite3, datetime
from pathlib import Path

DB = Path('.claude/knowledge.db')

def now():
    return datetime.datetime.now(datetime.timezone.utc).isoformat(timespec='seconds')

def save(outcome, topic, content, reason='', tags=''):
    c = sqlite3.connect(DB)
    c.row_factory = sqlite3.Row
    ts = now()
    existing = c.execute(
        "SELECT id FROM knowledge WHERE topic = ? COLLATE NOCASE AND outcome = ? LIMIT 1",
        (topic, outcome)
    ).fetchone()
    if existing:
        c.execute("UPDATE knowledge SET content=?, reason=?, tags=?, updated_at=? WHERE id=?",
                  (content, reason, tags, ts, existing['id']))
        print(f"Updated [{topic}]")
    else:
        c.execute("INSERT INTO knowledge(outcome,topic,content,reason,tags,created_at,updated_at) VALUES(?,?,?,?,?,?,?)",
                  (outcome, topic, content, reason, tags, ts, ts))
        print(f"Saved [{topic}]")
    c.commit()

save('learned', 'project overview', 'What this project is and does', '', 'overview')
save('learned', 'git workflow', 'How commits and branches work in this repo', '', 'git')
# ... add as many as you need

print('Done')
PYEOF
```

**Important:** Run this from the project root directory (where `.claude/` lives).
If you run it from a different directory, `knowledge.db` will be created in the wrong place.

---

## What Claude saves automatically

Every entry has one of two outcomes:

| Outcome | Meaning | Example |
|---------|---------|---------|
| ✅ **learned** | Something that is true and works in this project | "Migrations must run via `npm run migrate`, not directly — it triggers seed hooks" |
| ❌ **failed** | Something that was tried and did not work | "JWT in cookies fails on Safari due to ITP — use Authorization header instead" |

### Good candidates for saving

- A bug and its fix
- A convention or pattern specific to this project
- A config quirk or non-obvious setting
- A design decision and the reasoning behind it
- An approach that was tried and rejected, and why
- Something that wasted time — so it never wastes time again
- User corrections ("no, chickens are always in the plan" → save as learned fact)

### What does NOT get saved

- General knowledge true of any project of this type
- Anything instantly findable in official documentation
- Temporary or session-specific context

---

## The automatic workflow (what CLAUDE.md should instruct)

Claude follows this on every interaction, without being asked:

1. **Session start** — broad orientation search:
   ```bash
   python3 .claude/kb.py search project overview
   python3 .claude/kb.py search file index
   ```

2. **Before responding** — searches the KB for anything relevant to the current task

3. **On a `learned` result** — treats it as ground truth for this project and uses it directly

4. **On a `failed` result** — explicitly does not suggest that approach, tells you it was already tried and why it failed, and moves on to something else

5. **When the user corrects a mistake** — saves a `failed` entry *immediately*, not at end of session

6. **After resolving anything non-trivial** — saves what was learned or what failed

---

## Add a file index to the KB

For projects with many files, add a "file index" entry (or several, by topic) so Claude knows exactly which file to read for each kind of question. This dramatically reduces time spent searching:

```bash
python3 .claude/kb.py learn "file index - planning files | master_plan.txt: THE master document. timeline.txt: year by year. MONTHLY_CHECKLIST.md: monthly tasks. | files,planning"
```

Then add a rule to your `CLAUDE.md` and `MASTER_FILE_INDEX.md` (or equivalent) requiring the index to be updated in the same commit whenever a new file is created.

---

## The database commands

You won't normally need these, but they're there if you want to inspect or manually add entries.

```bash
# See everything in the KB
python3 .claude/kb.py list

# Search manually
python3 .claude/kb.py search <query>

# Add a learned fact (use quoted string for | delimiter)
python3 .claude/kb.py learn "topic | what is true / what works | tags"

# Record a failed attempt
python3 .claude/kb.py failed "topic | what was tried | why it failed | tags"
```

---

## Git

Commit `knowledge.db` to git. It is the accumulated knowledge of the project and should travel with it. Include it in commits whenever new KB entries were added during a session — don't let it drift.

The WAL temporary files are automatically excluded from git via `.gitignore`.

```
.claude/knowledge.db        ← commit this
.claude/knowledge.db-wal    ← excluded automatically
.claude/knowledge.db-shm    ← excluded automatically
```

---

## Works for any kind of project

The KB is plain text — it adapts to whatever you are building.

**Ansible / DevOps** — remembers inventory quirks, vault locations, which playbooks are safe to run ad-hoc, failed approaches to idempotency problems.

**Software development** — remembers architecture decisions, which refactors broke things, dependency version constraints, deployment gotchas.

**Long-term planning projects** — remembers what's always in scope, decisions already made, files that are authoritative for each topic, things that "were always the plan" so Claude doesn't treat them as new ideas.

**Translation / content** — remembers client terminology preferences, strings that are intentionally left untranslated, tone guidelines, previously rejected phrasings.

**Data / ML** — remembers dataset quirks, which preprocessing steps caused leakage, failed model architectures, environment setup issues.

Any domain where the same mistakes can recur across sessions benefits from this.
