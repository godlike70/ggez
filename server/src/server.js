'use strict';
require('dotenv').config();
const crypto = require('node:crypto');
const fs = require('node:fs');
const path = require('node:path');
const express = require('express');
const helmet = require('helmet');
const cors = require('cors');
const rateLimit = require('express-rate-limit');
const Database = require('better-sqlite3');
const { OAuth2Client } = require('google-auth-library');
const jwt = require('jsonwebtoken');

const required = ['JWT_SECRET', 'OWNER_ADMIN_KEY', 'GOOGLE_CLIENT_ID'];
for (const key of required) if (!process.env[key] || process.env[key].startsWith('REPLACE_') || process.env[key].startsWith('YOUR_')) {
  console.warn(`[GGEZ] ${key} is not configured. Related authentication routes will reject requests.`);
}
if (process.env.JWT_SECRET && process.env.JWT_SECRET.length < 32) throw new Error('JWT_SECRET must be at least 32 characters.');
const port = Number(process.env.PORT || 8787);
const dbFile = path.resolve(__dirname, '..', process.env.DATABASE_FILE || './data/ggez.sqlite');
fs.mkdirSync(path.dirname(dbFile), { recursive: true });
const db = new Database(dbFile);
db.pragma('journal_mode = WAL');
db.exec(`
CREATE TABLE IF NOT EXISTS users (id TEXT PRIMARY KEY, email TEXT NOT NULL UNIQUE, name TEXT, created_at TEXT NOT NULL);
CREATE TABLE IF NOT EXISTS claims (
 id TEXT PRIMARY KEY, user_id TEXT NOT NULL, email TEXT NOT NULL, plan TEXT NOT NULL,
 payment_method TEXT NOT NULL, payer_reference TEXT NOT NULL, payer_name TEXT,
 note TEXT, status TEXT NOT NULL DEFAULT 'pending', submitted_at TEXT NOT NULL,
 reviewed_at TEXT, reviewer_note TEXT, entitlement_id TEXT,
 FOREIGN KEY(user_id) REFERENCES users(id)
);
CREATE TABLE IF NOT EXISTS entitlements (
 id TEXT PRIMARY KEY, user_id TEXT NOT NULL, plan TEXT NOT NULL, status TEXT NOT NULL,
 starts_at TEXT NOT NULL, expires_at TEXT, created_at TEXT NOT NULL,
 FOREIGN KEY(user_id) REFERENCES users(id)
);
CREATE INDEX IF NOT EXISTS idx_claims_status_time ON claims(status, submitted_at);
CREATE INDEX IF NOT EXISTS idx_entitlements_user ON entitlements(user_id, status);
`);
const app = express();
app.use(helmet({ contentSecurityPolicy: false }));
app.use(cors({ origin: process.env.ALLOWED_ORIGINS ? process.env.ALLOWED_ORIGINS.split(',') : false }));
app.use(express.json({ limit: '24kb' }));
app.use('/v1/', rateLimit({ windowMs: 60_000, limit: 50, standardHeaders: true, legacyHeaders: false }));
app.use(express.static(path.join(__dirname, '..', 'public')));
const google = new OAuth2Client(process.env.GOOGLE_CLIENT_ID);
const now = () => new Date().toISOString();
const randomId = (prefix) => `${prefix}_${crypto.randomBytes(12).toString('hex')}`;
const clean = (v, max = 160) => typeof v === 'string' ? v.trim().slice(0, max) : '';
function userAuth(req, res, next) {
  try {
    const token = (req.headers.authorization || '').replace(/^Bearer\s+/i, '');
    const payload = jwt.verify(token, process.env.JWT_SECRET);
    req.user = { id: payload.sub, email: payload.email };
    next();
  } catch { res.status(401).json({ error: 'Sign in with Google to continue.' }); }
}
function ownerAuth(req, res, next) {
  const supplied = (req.headers.authorization || '').replace(/^Bearer\s+/i, '');
  const expected = process.env.OWNER_ADMIN_KEY || '';
  const a = Buffer.from(supplied); const b = Buffer.from(expected);
  if (!expected || a.length !== b.length || !crypto.timingSafeEqual(a, b)) return res.status(401).json({ error: 'Owner authorization required.' });
  next();
}
function paymentConfig() {
  const upiId = clean(process.env.PAYMENT_UPI_ID, 120);
  const bank = {
    bankName: clean(process.env.PAYMENT_BANK_NAME, 120),
    accountName: clean(process.env.PAYMENT_ACCOUNT_NAME, 120),
    accountNumber: clean(process.env.PAYMENT_ACCOUNT_NUMBER, 80),
    ifsc: clean(process.env.PAYMENT_IFSC, 20),
    branch: clean(process.env.PAYMENT_BANK_BRANCH, 120)
  };
  const hasBank = Boolean(bank.bankName && bank.accountName && bank.accountNumber && bank.ifsc);
  return {
    currency: clean(process.env.CURRENCY || 'INR', 8),
    instructions: clean(process.env.PAYMENT_INSTRUCTIONS, 600),
    methods: [
      ...(upiId ? [{ id: 'upi', label: 'UPI', upiId, payeeName: clean(process.env.PAYMENT_UPI_NAME, 120) }] : []),
      ...(hasBank ? [{ id: 'bank_transfer', label: 'Bank transfer', ...bank }] : [])
    ],
    plans: {
      monthly: { label: 'Plugin Studio Monthly', price: clean(process.env.PLUGIN_STUDIO_MONTHLY_PRICE, 30) },
      yearly: { label: 'Plugin Studio Yearly', price: clean(process.env.PLUGIN_STUDIO_YEARLY_PRICE, 30) },
      lifetime: { label: 'Plugin Studio Lifetime', price: clean(process.env.PLUGIN_STUDIO_LIFETIME_PRICE, 30) }
    }
  };
}
app.get('/health', (_req, res) => res.json({ ok: true, service: 'ggez-entitlements' }));
app.get('/v1/store/config', (_req, res) => res.json(paymentConfig()));
app.post('/v1/auth/google', async (req, res) => {
  try {
    const credential = clean(req.body?.credential, 10000);
    if (!credential || !process.env.GOOGLE_CLIENT_ID) return res.status(503).json({ error: 'Google sign-in is not configured on this server.' });
    const ticket = await google.verifyIdToken({ idToken: credential, audience: process.env.GOOGLE_CLIENT_ID });
    const p = ticket.getPayload();
    if (!p?.sub || !p.email || !p.email_verified) return res.status(401).json({ error: 'A verified Google account is required.' });
    const existing = db.prepare('SELECT id FROM users WHERE email = ?').get(p.email.toLowerCase());
    const id = existing?.id || randomId('usr');
    db.prepare(`INSERT INTO users(id,email,name,created_at) VALUES(?,?,?,?)
      ON CONFLICT(email) DO UPDATE SET name=excluded.name`).run(id, p.email.toLowerCase(), clean(p.name, 120), now());
    const token = jwt.sign({ email: p.email.toLowerCase() }, process.env.JWT_SECRET, { subject: id, expiresIn: '7d', issuer: 'ggez' });
    res.json({ token, user: { id, email: p.email.toLowerCase(), name: clean(p.name, 120) } });
  } catch { res.status(401).json({ error: 'Google credential could not be verified.' }); }
});
app.post('/v1/claims', userAuth, (req, res) => {
  const plan = clean(req.body?.plan, 20);
  const method = clean(req.body?.paymentMethod, 30);
  const reference = clean(req.body?.paymentReference, 120);
  const payerName = clean(req.body?.payerName, 120);
  const note = clean(req.body?.note, 500);
  const cfg = paymentConfig();
  if (!['monthly', 'yearly', 'lifetime'].includes(plan)) return res.status(400).json({ error: 'Choose a valid plan.' });
  if (!cfg.methods.some(m => m.id === method)) return res.status(400).json({ error: 'That payment method is not currently configured.' });
  if (reference.length < 5) return res.status(400).json({ error: 'Enter the payment transaction/reference ID.' });
  const duplicate = db.prepare('SELECT id FROM claims WHERE payment_method=? AND payer_reference=?').get(method, reference);
  if (duplicate) return res.status(409).json({ error: 'This payment reference has already been submitted.' });
  const id = randomId('claim');
  db.prepare(`INSERT INTO claims(id,user_id,email,plan,payment_method,payer_reference,payer_name,note,status,submitted_at)
    VALUES(?,?,?,?,?,?,?,?, 'pending',?)`).run(id, req.user.id, req.user.email, plan, method, reference, payerName, note, now());
  res.status(201).json({ claimId: id, status: 'pending', message: 'Your claim is awaiting manual review. Payment does not unlock access until approved.' });
});
app.get('/v1/claims/mine', userAuth, (req, res) => {
  const rows = db.prepare('SELECT id,plan,payment_method,payer_reference,status,submitted_at,reviewed_at,reviewer_note FROM claims WHERE user_id=? ORDER BY submitted_at DESC LIMIT 30').all(req.user.id);
  res.json({ claims: rows });
});
app.get('/v1/entitlements/me', userAuth, (req, res) => {
  const rows = db.prepare(`SELECT id,plan,status,starts_at,expires_at FROM entitlements
    WHERE user_id=? AND status='active' AND (expires_at IS NULL OR expires_at > ?) ORDER BY created_at DESC`).all(req.user.id, now());
  res.json({ entitlements: rows, pluginStudioUnlocked: rows.length > 0 });
});
app.get('/v1/admin/session', ownerAuth, (_req, res) => res.json({ ok: true, role: 'owner', service: 'ggez' }));
app.get('/v1/admin/claims', ownerAuth, (_req, res) => {
  const rows = db.prepare(`SELECT id,email,plan,payment_method,payer_reference,payer_name,note,status,submitted_at
    FROM claims WHERE status='pending' ORDER BY submitted_at ASC LIMIT 100`).all();
  res.json({ pending: rows });
});
app.post('/v1/admin/claims/:id/review', ownerAuth, (req, res) => {
  const decision = clean(req.body?.decision, 12);
  const reviewerNote = clean(req.body?.note, 300);
  if (!['approve', 'deny'].includes(decision)) return res.status(400).json({ error: 'Decision must be approve or deny.' });
  const claim = db.prepare('SELECT * FROM claims WHERE id=?').get(req.params.id);
  if (!claim) return res.status(404).json({ error: 'Claim not found.' });
  if (claim.status !== 'pending') return res.status(409).json({ error: 'This claim has already been reviewed.' });
  const stamp = now();
  if (decision === 'deny') {
    db.prepare('UPDATE claims SET status=?,reviewed_at=?,reviewer_note=? WHERE id=? AND status=\'pending\'').run('denied', stamp, reviewerNote, claim.id);
    return res.json({ ok: true, status: 'denied' });
  }
  const entitlementId = randomId('ent');
  let expiresAt = null;
  if (claim.plan === 'monthly') { const d = new Date(); d.setMonth(d.getMonth() + 1); expiresAt = d.toISOString(); }
  if (claim.plan === 'yearly') { const d = new Date(); d.setFullYear(d.getFullYear() + 1); expiresAt = d.toISOString(); }
  const tx = db.transaction(() => {
    db.prepare('INSERT INTO entitlements(id,user_id,plan,status,starts_at,expires_at,created_at) VALUES(?,?,?,\'active\',?,?,?)')
      .run(entitlementId, claim.user_id, claim.plan, stamp, expiresAt, stamp);
    db.prepare('UPDATE claims SET status=\'approved\',reviewed_at=?,reviewer_note=?,entitlement_id=? WHERE id=? AND status=\'pending\'')
      .run(stamp, reviewerNote, entitlementId, claim.id);
  });
  tx();
  res.json({ ok: true, status: 'approved', entitlementId, email: claim.email });
});
app.get('/admin', (_req, res) => res.sendFile(path.join(__dirname, '..', 'public', 'admin.html')));
app.use((err, _req, res, _next) => { console.error(err); res.status(500).json({ error: 'Unexpected server error.' }); });
app.listen(port, () => console.log(`GGEZ entitlement service listening on ${port}`));
