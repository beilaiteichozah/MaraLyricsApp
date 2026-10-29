#!/usr/bin/env node
// Writes CHANGELOG.md (every version with its full commit log) and, with --tag, creates the annotated git tags.
// The same flow as maralyrics.com's scripts/changelog.mjs, for the Android app.
//
//   node scripts/changelog.mjs                  regenerate CHANGELOG.md from scripts/releases.json + git history
//   node scripts/changelog.mjs --tag            also create any missing annotated tag (vX.Y.Z) at each release's ref
//   node scripts/changelog.mjs --check          exit 1 if CHANGELOG.md is out of date
//   node scripts/changelog.mjs --notes 1.4.0    print the release notes of one version (for a GitHub release body)
//   node scripts/changelog.mjs --title 1.4.0    print the GitHub release title of one version ("v1.4.0 — Title")
//
// GitHub Releases are published from these notes by `scripts/publish-releases.mjs` (the "Publish releases" workflow runs it
// whenever a vX.Y.Z tag is pushed, and on demand from the Actions tab to back-fill or refresh every release).
//
// To cut a new release: raise versionCode and versionName in app/build.gradle.kts and commit, add an entry at the END of
// scripts/releases.json (version, ref = the commit to tag, title, summary, highlights, deploy), run
// `node scripts/changelog.mjs --tag`, commit CHANGELOG.md and scripts/releases.json, then push the commits and the tag.
// A version's ref is a commit that already exists (it cannot be the commit that adds its own changelog entry), so the
// changelog commit shows up in the log of the version after it. Each version's log is every commit since the previous
// version's ref; merge commits are counted but not listed.
import fs from 'node:fs';
import path from 'node:path';
import { execFileSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const ROOT = path.join(path.dirname(fileURLToPath(import.meta.url)), '..');
const git = (...args) => execFileSync('git', args, { cwd: ROOT, encoding: 'utf8', maxBuffer: 64 * 1024 * 1024 }).trim();
const EMPTY_TREE = '4b825dc642cb6eb9a060e54bf8d69288fbee4904';
const REPO = 'beilaiteichozah/MaraLyricsApp';
const BRANCH = 'master';
const GITHUB = `https://github.com/${REPO}`;

const file = JSON.parse(fs.readFileSync(path.join(ROOT, 'scripts/releases.json'), 'utf8'));
const releases = file.releases;
const unreleased = file.unreleased || [];
const args = process.argv.slice(2);
let resolved;
try {
  resolved = releases.map((r) => ({ ...r, sha: git('rev-parse', '--verify', `${r.ref}^{commit}`) }));
} catch (err) {
  // A shallow clone (or no git) can't rebuild the log, so a check has nothing to compare against.
  if (args.includes('--check')) { console.log('changelog check skipped: git history is not available here'); process.exit(0); }
  throw err;
}

function describe(r, i) {
  const prev = i > 0 ? resolved[i - 1].sha : null;
  const range = prev ? `${prev}..${r.sha}` : r.sha;
  // %aN applies .mailmap, so one person is one author however their git name was capitalised.
  const log = git('log', '--reverse', '--no-merges', '--date=short', '--format=%h%x09%ad%x09%aN%x09%s', range)
    .split('\n').filter(Boolean).map((l) => { const [hash, date, author, ...subject] = l.split('\t'); return { hash, date, author, subject: subject.join('\t') }; });
  const merges = Number(git('rev-list', '--count', '--merges', range));
  const shortstat = git('diff', '--shortstat', prev || EMPTY_TREE, r.sha) || 'no file changes';
  const date = git('log', '-1', '--date=short', '--format=%ad', r.sha);
  const authors = [...new Set(log.map((c) => c.author))];
  return { ...r, log, merges, shortstat, date, authors };
}
const described = resolved.map(describe);

function notes(v, { markdown }) {
  const lines = [];
  lines.push(v.summary, '');
  lines.push(markdown ? '**Highlights**' : 'Highlights:');
  for (const h of v.highlights) lines.push(`- ${h}`);
  if (v.deploy) lines.push('', markdown ? `**Deploy notes:** ${v.deploy}` : `Deploy notes: ${v.deploy}`);
  lines.push('', `${v.log.length} commits (${v.merges} merge commits not listed) · ${v.authors.length} author${v.authors.length === 1 ? '' : 's'} · ${v.shortstat}`);
  return lines;
}
const commitLine = (c) => `${c.hash} ${c.date} ${c.subject} — ${c.author}`;

function changelog() {
  const out = [
    '# Changelog', '',
    'Every released version of the MaraLyrics Android app (`com.maralyrics.laitei`), newest first, with its full commit log. Versions follow [semantic versioning](https://semver.org) and each one is a git tag (`vX.Y.Z`) on the commit it names; the website has its own versions at [laiteichozah/maralyrics](https://github.com/laiteichozah/maralyrics/releases).',
    'This file is generated: edit `scripts/releases.json` and run `node scripts/changelog.mjs`. A version\'s log lists every commit since the previous version; merge commits are counted, not listed.',
    '',
  ];
  if (unreleased.length) out.push('## Unreleased', '', 'Merged after the latest version; it ships with the next one.', '', ...unreleased.map((h) => `- ${h}`), '');
  for (const v of [...described].reverse()) {
    out.push(`## v${v.version} — ${v.title} (${v.date})`, '', ...notes(v, { markdown: true }), '');
    out.push(`<details><summary>Full log — ${v.log.length} commits</summary>`, '');
    for (const c of v.log) out.push(`- \`${c.hash}\` ${c.date} ${c.subject} — ${c.author}`);
    out.push('', '</details>', '');
  }
  return `${out.join('\n').replace(/\n{3,}/g, '\n\n').trimEnd()}\n`;
}

const compareUrl = (i) => (i > 0 ? `${GITHUB}/compare/${resolved[i - 1].sha}...${resolved[i].sha}` : `${GITHUB}/commits/${resolved[i].sha}`);
const findVersion = (flag) => {
  const wanted = String(args[args.indexOf(flag) + 1]).replace(/^v/, '');
  const i = described.findIndex((d) => d.version === wanted);
  if (i === -1) { console.error(`No such version: ${wanted}`); process.exit(1); }
  return i;
};
if (args.includes('--title')) {
  const v = described[findVersion('--title')];
  console.log(`v${v.version} — ${v.title}`);
  process.exit(0);
}
if (args.includes('--notes')) {
  const i = findVersion('--notes');
  const v = described[i];
  const sha = (c) => `[\`${c.hash}\`](${GITHUB}/commit/${git('rev-parse', c.hash)})`;
  console.log([
    `Released ${v.date}`, '', ...notes(v, { markdown: true }), '',
    `**[See this version on GitHub](${compareUrl(i)})**`, '',
    `Every release is listed in [CHANGELOG.md](${GITHUB}/blob/${BRANCH}/CHANGELOG.md).`, '',
    '### Full log', ...v.log.map((c) => `- ${sha(c)} ${c.date} ${c.subject} — ${c.author}`),
  ].join('\n'));
  process.exit(0);
}

const files = { 'CHANGELOG.md': changelog() };
if (args.includes('--check')) {
  const stale = Object.entries(files).filter(([name, text]) => !fs.existsSync(path.join(ROOT, name)) || fs.readFileSync(path.join(ROOT, name), 'utf8') !== text).map(([name]) => name);
  if (stale.length) { console.error(`✗ out of date: ${stale.join(', ')} — run: node scripts/changelog.mjs`); process.exit(1); }
  console.log(`✓ CHANGELOG.md is up to date (${described.length} versions)`);
  process.exit(0);
}
for (const [name, text] of Object.entries(files)) fs.writeFileSync(path.join(ROOT, name), text);
console.log(`CHANGELOG.md written: ${described.length} versions, ${described.reduce((n, v) => n + v.log.length, 0)} commits`);

if (args.includes('--tag')) {
  const existing = new Set(git('tag', '-l').split('\n').filter(Boolean));
  for (const v of described) {
    const tag = `v${v.version}`;
    if (existing.has(tag)) { console.log(`  = ${tag} already exists (${git('rev-parse', '--short', `${tag}^{commit}`)})`); continue; }
    const message = [`MaraLyrics app ${tag} — ${v.title}`, `Released ${v.date}`, '', ...notes(v, { markdown: false }), '', `Full log (${v.log.length} commits):`, ...v.log.map(commitLine)].join('\n');
    execFileSync('git', ['tag', '-a', tag, '-F', '-', v.sha], { cwd: ROOT, input: message });
    console.log(`  + ${tag} → ${v.sha.slice(0, 7)} ${v.title} (${v.log.length} commits)`);
  }
}
