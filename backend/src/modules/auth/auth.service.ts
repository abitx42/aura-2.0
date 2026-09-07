import bcrypt from 'bcryptjs';
import { query, withTransaction } from '../../db/index.js';

export interface User {
  id: string;
  email: string;
  preferred_name: string | null;
  created_at: string;
  updated_at: string;
}

export interface UserWithPassword extends User {
  password_hash: string;
}

export interface UserProfile {
  user_id: string;
  display_name: string | null;
  date_of_birth: string | null;
  timezone: string;
  typical_wake_time: string | null;
  typical_sleep_time: string | null;
  planning_style: string;
  onboarding_status: string;
}

export class AuthService {
  static async createUser(email: string, passwordPlain: string, displayName: string) {
    const salt = await bcrypt.genSalt(10);
    const passwordHash = await bcrypt.hash(passwordPlain, salt);

    return withTransaction(async (client) => {
      // Check existing
      const existing = await client.query('SELECT id FROM users WHERE email = $1', [email]);
      if (existing.rows.length > 0) {
        throw new Error('EMAIL_ALREADY_EXISTS');
      }

      // Insert user with password_hash and preferred_name
      const userRes = await client.query<User>(
        `INSERT INTO users (email, password_hash, preferred_name, auth_provider)
         VALUES ($1, $2, $3, 'password')
         RETURNING id, email, preferred_name, created_at, updated_at`,
        [email, passwordHash, displayName]
      );
      const user = userRes.rows[0];

      // Insert profile
      await client.query(
        `INSERT INTO user_profiles (user_id, display_name, onboarding_status)
         VALUES ($1, $2, 'NOT_STARTED')`,
        [user.id, displayName]
      );

      return {
        user: {
          id: user.id,
          email: user.email,
          displayName: user.preferred_name || displayName,
        },
      };
    });
  }

  static async findByEmailWithPassword(email: string): Promise<UserWithPassword | null> {
    const res = await query<UserWithPassword>(
      'SELECT id, email, password_hash, preferred_name, created_at, updated_at FROM users WHERE email = $1',
      [email]
    );
    return res.rows[0] || null;
  }

  static async findByEmail(email: string): Promise<User | null> {
    const res = await query<User>(
      'SELECT id, email, preferred_name, created_at, updated_at FROM users WHERE email = $1',
      [email]
    );
    return res.rows[0] || null;
  }

  static async getProfile(userId: string) {
    const res = await query(
      `SELECT u.id, u.email, p.display_name, p.timezone, p.onboarding_status, p.created_at
       FROM users u
       LEFT JOIN user_profiles p ON u.id = p.user_id
       WHERE u.id = $1`,
      [userId]
    );
    return res.rows[0] || null;
  }
}
