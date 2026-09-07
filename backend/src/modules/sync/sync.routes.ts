import { FastifyInstance } from 'fastify';
import { z } from 'zod';
import { query, withTransaction } from '../../db/index.js';

const syncChangeSchema = z.object({
  operationId: z.string(),
  entity: z.enum(['task', 'daily_plan', 'plan']),
  action: z.enum(['INSERT', 'UPDATE', 'DELETE', 'CREATE']),
  id: z.string().uuid(),
  version: z.number().int().optional(),
  updatedAt: z.string(),
  data: z.record(z.any()),
});

const syncPushSchema = z.object({
  changes: z.array(syncChangeSchema),
});

export async function syncRoutes(app: FastifyInstance) {
  app.addHook('onRequest', (app as any).authenticate);

  app.post('/push', async (request, reply) => {
    const user = (request as any).user;
    const parseResult = syncPushSchema.safeParse(request.body);
    if (!parseResult.success) {
      return reply.status(422).send({
        success: false,
        error: {
          code: 'VALIDATION_ERROR',
          message: 'Invalid sync payload',
          details: parseResult.error.issues,
        },
      });
    }

    const { changes } = parseResult.data;
    const committedOperations: string[] = [];
    const failedOperations: { operationId: string; error: string }[] = [];

    await withTransaction(async (client) => {
      for (const change of changes) {
        let isCommitted = false;

        if (change.entity === 'task') {
          if (change.action === 'INSERT' || change.action === 'CREATE') {
            await client.query(
              `INSERT INTO tasks (id, user_id, title, priority, status, created_at, updated_at)
               VALUES ($1, $2, $3, COALESCE($4, 'MEDIUM'), COALESCE($5, 'PENDING'), $6, $7)
               ON CONFLICT (id) DO UPDATE
               SET title = EXCLUDED.title, status = EXCLUDED.status, updated_at = EXCLUDED.updated_at, version = tasks.version + 1`,
              [
                change.id,
                user.userId,
                change.data.title || 'Untitled',
                change.data.priority,
                change.data.status,
                change.data.createdAt || change.updatedAt,
                change.updatedAt,
              ]
            );
            isCommitted = true;
          } else if (change.action === 'UPDATE') {
            await client.query(
              `UPDATE tasks
               SET title = COALESCE($1, title),
                   status = COALESCE($2, status),
                   priority = COALESCE($3, priority),
                   completed_at = COALESCE($4, completed_at),
                   updated_at = $5,
                   version = version + 1
               WHERE id = $6 AND user_id = $7`,
              [
                change.data.title,
                change.data.status,
                change.data.priority,
                change.data.completedAt,
                change.updatedAt,
                change.id,
                user.userId,
              ]
            );
            isCommitted = true;
          } else if (change.action === 'DELETE') {
            await client.query(
              `UPDATE tasks SET deleted_at = NOW(), updated_at = NOW(), version = version + 1 WHERE id = $1 AND user_id = $2`,
              [change.id, user.userId]
            );
            isCommitted = true;
          }
        } else if (change.entity === 'plan' || change.entity === 'daily_plan') {
          if (change.action === 'INSERT' || change.action === 'CREATE' || change.action === 'UPDATE') {
            await client.query(
              `INSERT INTO daily_plans (id, user_id, plan_date, status, updated_at)
               VALUES ($1, $2, $3, COALESCE($4, 'DRAFT'), $5)
               ON CONFLICT (id) DO UPDATE
               SET status = EXCLUDED.status, updated_at = EXCLUDED.updated_at, version = daily_plans.version + 1`,
              [change.id, user.userId, change.data.planDate, change.data.status, change.updatedAt]
            );
            isCommitted = true;
          }
        }

        if (isCommitted) {
          committedOperations.push(change.operationId);
        } else {
          failedOperations.push({
            operationId: change.operationId,
            error: `Unhandled entity '${change.entity}' or action '${change.action}'`,
          });
        }
      }
    });

    return reply.send({
      success: true,
      data: {
        committedOperations,
        failedOperations,
        conflicts: [],
        serverTime: new Date().toISOString(),
      },
    });
  });

  app.get('/pull', async (request, reply) => {
    const user = (request as any).user;
    const { since } = request.query as { since?: string };

    const sinceDate = since ? new Date(since).toISOString() : '1970-01-01T00:00:00Z';

    const tasksRes = await query(
      `SELECT * FROM tasks WHERE user_id = $1 AND updated_at > $2`,
      [user.userId, sinceDate]
    );

    const plansRes = await query(
      `SELECT * FROM daily_plans WHERE user_id = $1 AND updated_at > $2`,
      [user.userId, sinceDate]
    );

    return reply.send({
      success: true,
      data: {
        tasks: tasksRes.rows,
        plans: plansRes.rows,
        serverTime: new Date().toISOString(),
      },
    });
  });
}
