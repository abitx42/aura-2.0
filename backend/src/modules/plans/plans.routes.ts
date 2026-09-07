import { FastifyInstance } from 'fastify';
import { z } from 'zod';
import { PlansService } from './plans.service.js';

const lockPlanSchema = z.object({
  taskIdsInOrder: z.array(z.string().uuid()),
});

export async function plansRoutes(app: FastifyInstance) {
  app.addHook('onRequest', (app as any).authenticate);

  app.get('/:date', async (request, reply) => {
    const user = (request as any).user;
    const { date } = request.params as { date: string };
    const plan = await PlansService.createOrGetDraft(user.userId, date);
    return reply.send({ success: true, data: plan });
  });

  app.post('/:id/lock', async (request, reply) => {
    const user = (request as any).user;
    const { id } = request.params as { id: string };
    const parseResult = lockPlanSchema.safeParse(request.body);
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

    try {
      const lockedPlan = await PlansService.lockPlan(user.userId, id, parseResult.data.taskIdsInOrder);
      return reply.send({ success: true, data: lockedPlan });
    } catch (err: any) {
      if (err.message === 'PLAN_NOT_FOUND') {
        return reply.status(404).send({
          success: false,
          error: { code: 'NOT_FOUND', message: 'Plan not found' },
        });
      }
      throw err;
    }
  });
}
