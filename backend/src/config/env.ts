import dotenv from 'dotenv';
import { z } from 'zod';

dotenv.config();

const envSchema = z.object({
  PORT: z.coerce.number().default(3000),
  HOST: z.string().default('0.0.0.0'),
  NODE_ENV: z.enum(['development', 'production', 'test']).default('development'),
  DATABASE_URL: z.string().default('postgresql://postgres:postgres@localhost:5432/aura'),
  JWT_SECRET: z.string().min(16).default('aura_super_secure_jwt_secret_change_in_production_key_2026'),
  JWT_EXPIRES_IN: z.string().default('7d'),
  CORS_ORIGIN: z.string().default('*'),
}).refine(
  (data) => {
    if (data.NODE_ENV === 'production' && data.JWT_SECRET === 'aura_super_secure_jwt_secret_change_in_production_key_2026') {
      return false;
    }
    return true;
  },
  {
    message: 'CRITICAL SECURITY: Default JWT_SECRET cannot be used in production environment.',
    path: ['JWT_SECRET'],
  }
);

export const env = envSchema.parse(process.env);
