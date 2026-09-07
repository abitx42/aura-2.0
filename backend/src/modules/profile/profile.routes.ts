import { FastifyInstance } from 'fastify';
import { z } from 'zod';
import { query } from '../../db/index.js';

const updateProfileSchema = z.object({
  displayName: z.string().optional(),
  dateOfBirth: z.string().optional(),
  timezone: z.string().optional(),
  lifestyleType: z.string().optional(),
  typicalWakeTime: z.string().optional(),
  typicalSleepTime: z.string().optional(),
  planningStyle: z.string().optional(),
  onboardingStatus: z.enum(['NOT_STARTED', 'IN_PROGRESS', 'COMPLETE']).optional(),
});

export async function profileRoutes(app: FastifyInstance) {
  app.addHook('onRequest', (app as any).authenticate);

  app.get('/', async (request, reply) => {
    const user = (request as any).user;
    const res = await query('SELECT * FROM user_profiles WHERE user_id = $1', [user.userId]);
    if (res.rows.length === 0) {
      return reply.status(404).send({
        success: false,
        error: { code: 'NOT_FOUND', message: 'Profile not found' },
      });
    }
    return reply.send({ success: true, data: res.rows[0] });
  });

  app.put('/', async (request, reply) => {
    const user = (request as any).user;
    const parseResult = updateProfileSchema.safeParse(request.body);
    if (!parseResult.success) {
      return reply.status(422).send({
        success: false,
        error: {
          code: 'VALIDATION_ERROR',
          message: 'Invalid input',
          details: parseResult.error.issues,
        },
      });
    }

    const { displayName, dateOfBirth, timezone, onboardingStatus } = parseResult.data;

    const res = await query(
      `UPDATE user_profiles
       SET display_name = COALESCE($2, display_name),
           date_of_birth = COALESCE($3, date_of_birth),
           timezone = COALESCE($4, timezone),
           onboarding_status = COALESCE($5, onboarding_status),
           updated_at = NOW()
       WHERE user_id = $1
       RETURNING *`,
      [user.userId, displayName || null, dateOfBirth || null, timezone || null, onboardingStatus || null]
    );

    return reply.send({ success: true, data: res.rows[0] });
  });
}
