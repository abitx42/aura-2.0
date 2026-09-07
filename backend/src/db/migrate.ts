import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';
import { pool, withTransaction, query } from './index.js';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

async function runMigrations() {
  console.log('🚀 Starting Aura 2.0 incremental database migration engine...');

  // 1. Ensure schema_migrations tracker table exists
  await query(`
    CREATE TABLE IF NOT EXISTS schema_migrations (
      id SERIAL PRIMARY KEY,
      version VARCHAR(255) UNIQUE NOT NULL,
      name VARCHAR(255) NOT NULL,
      applied_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
    );
  `);

  const migrationsDir = path.join(__dirname, 'migrations');
  if (!fs.existsSync(migrationsDir)) {
    console.error(`❌ Migrations directory not found at: ${migrationsDir}`);
    process.exit(1);
  }

  const files = fs.readdirSync(migrationsDir)
    .filter((f) => f.endsWith('.sql'))
    .sort();

  console.log(`📁 Found ${files.length} migration script(s) in migrations directory.`);

  for (const file of files) {
    const version = file.split('_')[0];
    const checkRes = await query(
      'SELECT id, applied_at FROM schema_migrations WHERE version = $1',
      [version]
    );

    if (checkRes.rows.length > 0) {
      console.log(`⏩ [SKIP] Migration ${file} (Already applied at ${checkRes.rows[0].applied_at})`);
      continue;
    }

    console.log(`⚙️  [APPLYING] Migration ${file}...`);
    const sqlPath = path.join(migrationsDir, file);
    const sql = fs.readFileSync(sqlPath, 'utf8');

    await withTransaction(async (client) => {
      await client.query(sql);
      await client.query(
        'INSERT INTO schema_migrations (version, name) VALUES ($1, $2)',
        [version, file]
      );
    });

    console.log(`✅ [APPLIED] Migration ${file} completed successfully!`);
  }

  console.log('🎉 All database migrations are up to date!');
}

runMigrations()
  .catch((err) => {
    console.error('❌ Migration failed:', err);
    process.exit(1);
  })
  .finally(async () => {
    await pool.end();
  });
