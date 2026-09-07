import { FastifyInstance } from 'fastify';
import { query, withTransaction } from '../../db/index.js';

export async function actionsRoutes(app: FastifyInstance) {
  app.addHook('onRequest', (app as any).authenticate);

  app.get('/', async (request, reply) => {
    const user = (request as any).user;
    const res = await query(
      `SELECT * FROM proposed_actions WHERE user_id = $1 ORDER BY created_at DESC LIMIT 20`,
      [user.userId]
    );
    return reply.send({ success: true, data: res.rows });
  });

  app.post('/:id/approve', async (request, reply) => {
    const user = (request as any).user;
    const { id } = request.params as { id: string };

    const actionRes = await query(
      `SELECT * FROM proposed_actions WHERE id = $1 AND user_id = $2`,
      [id, user.userId]
    );

    if (actionRes.rows.length === 0) {
      return reply.status(404).send({
        success: false,
        error: { code: 'NOT_FOUND', message: 'Proposed action not found' },
      });
    }

    const action = actionRes.rows[0];
    if (action.status !== 'PROPOSED') {
      return reply.status(400).send({
        success: false,
        error: { code: 'INVALID_STATE', message: `Action is already ${action.status}` },
      });
    }

    // Execute deterministically based on action_type (Fail-Closed Architecture)
    if (action.action_type !== 'RESCHEDULE_TASK') {
      return reply.status(422).send({
        success: false,
        error: {
          code: 'UNSUPPORTED_ACTION_TYPE',
          message: `Action type '${action.action_type}' is not supported by this deterministic executor.`,
        },
      });
    }

    await withTransaction(async (client) => {
      const payload = action.payload_json;
      if (!payload?.taskId || !payload?.newDueAt) {
        throw new Error('INVALID_PAYLOAD');
      }

      await client.query(
        `UPDATE tasks SET due_at = $1, updated_at = NOW(), version = version + 1 WHERE id = $2 AND user_id = $3`,
        [payload.newDueAt, payload.taskId, user.userId]
      );

      await client.query(
        `UPDATE proposed_actions SET status = 'EXECUTED', resolved_at = NOW() WHERE id = $1`,
        [id]
      );

      // Record audit life event
      await client.query(
        `INSERT INTO life_events (user_id, domain, event_type, reference_table, reference_id, payload_json, occurred_at)
         VALUES ($1, 'GENERAL', 'ACTION_EXECUTED', 'proposed_actions', $2, $3, NOW())`,
        [user.userId, id, JSON.stringify({ actionType: action.action_type, payload })]
      );
    });

    return reply.send({ success: true, data: { id, status: 'EXECUTED' } });
  });

  app.post('/:id/reject', async (request, reply) => {
    const user = (request as any).user;
    const { id } = request.params as { id: string };

    const res = await query(
      `UPDATE proposed_actions
       SET status = 'REJECTED', resolved_at = NOW()
       WHERE id = $1 AND user_id = $2 AND status = 'PROPOSED'
       RETURNING *`,
      [id, user.userId]
    );

    if (res.rows.length === 0) {
      return reply.status(404).send({
        success: false,
        error: { code: 'NOT_FOUND', message: 'Proposed action not found or already resolved' },
      });
    }

    return reply.send({ success: true, data: res.rows[0] });
  });
}
