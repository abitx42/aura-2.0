import { FastifyInstance } from 'fastify';
import { z } from 'zod';
import { PlansService } from './plans.service.js';

const lockPlanSchema = z.object({
  taskIdsInOrder: z.array(z.string().uuid()),
});

const adaptPlanSchema = z.object({
  taskIdsInOrder: z.array(z.string().uuid()),
  reason: z.string().optional(),
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
      if (err.message === 'PLAN_ALREADY_LOCKED') {
        return reply.status(409).send({
          success: false,
          error: { code: 'PLAN_ALREADY_LOCKED', message: 'Plan is already locked' },
        });
      }
      if (err.message === 'DUPLICATE_TASKS_IN_PLAN') {
        return reply.status(422).send({
          success: false,
          error: { code: 'DUPLICATE_TASKS_IN_PLAN', message: 'Duplicate tasks cannot be added to a daily plan' },
        });
      }
      if (err.message === 'INVALID_TASK_SELECTION') {
        return reply.status(403).send({
          success: false,
          error: { code: 'INVALID_TASK_SELECTION', message: 'One or more tasks are invalid or belong to another account' },
        });
      }
      throw err;
    }
  });

  app.post('/:id/adapt', async (request, reply) => {
    const user = (request as any).user;
    const { id } = request.params as { id: string };
    const parseResult = adaptPlanSchema.safeParse(request.body);
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
      const adaptedPlan = await PlansService.adaptPlan(
        user.userId,
        id,
        parseResult.data.taskIdsInOrder,
        parseResult.data.reason
      );
      return reply.send({ success: true, data: adaptedPlan });
    } catch (err: any) {
      if (err.message === 'PLAN_NOT_FOUND') {
        return reply.status(404).send({
          success: false,
          error: { code: 'NOT_FOUND', message: 'Plan not found' },
        });
      }
      if (err.message === 'DUPLICATE_TASKS_IN_PLAN') {
        return reply.status(422).send({
          success: false,
          error: { code: 'DUPLICATE_TASKS_IN_PLAN', message: 'Duplicate tasks cannot be added to a daily plan' },
        });
      }
      if (err.message === 'INVALID_TASK_SELECTION') {
        return reply.status(403).send({
          success: false,
          error: { code: 'INVALID_TASK_SELECTION', message: 'One or more tasks are invalid or belong to another account' },
        });
      }
      throw err;
    }
  });

  app.post('/:id/activate', async (request, reply) => {
    const user = (request as any).user;
    const { id } = request.params as { id: string };

    try {
      const activePlan = await PlansService.activatePlan(user.userId, id);
      return reply.send({ success: true, data: activePlan });
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
