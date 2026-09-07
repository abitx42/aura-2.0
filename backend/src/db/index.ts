import pg from 'pg';
import { env } from '../config/env.js';

const { Pool } = pg;

export const pool = new Pool({
  connectionString: env.DATABASE_URL,
  max: 20,
  idleTimeoutMillis: 30000,
  connectionTimeoutMillis: 5000,
});

export async function query<T extends pg.QueryResultRow = any>(text: string, params?: any[]): Promise<pg.QueryResult<T>> {
  const start = Date.now();
  try {
    const res = await pool.query<T>(text, params);
    if (env.NODE_ENV === 'development') {
      const duration = Date.now() - start;
      const sqlSnippet = text.replace(/\s+/g, ' ').trim().slice(0, 100);
      console.log(`[SQL] (${duration}ms) ${sqlSnippet} | rows: ${res.rowCount}`);
    }
    return res;
  } catch (error) {
    console.error(`[SQL ERROR] on query: ${text}`, error);
    throw error;
  }
}

export async function withTransaction<T>(callback: (client: pg.PoolClient) => Promise<T>): Promise<T> {
  const client = await pool.connect();
  try {
    await client.query('BEGIN');
    const result = await callback(client);
    await client.query('COMMIT');
    return result;
  } catch (err) {
    await client.query('ROLLBACK');
    throw err;
  } finally {
    client.release();
  }
}
