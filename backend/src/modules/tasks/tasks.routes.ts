import { FastifyInstance } from 'fastify';
import { z } from 'zod';
import { TasksService } from './tasks.service.js';

const createTaskSchema = z.object({
  title: z.string().min(1),
  priority: z.enum(['LOW', 'MEDIUM', 'HIGH']).optional(),
  energyTag: z.enum(['LOW', 'MEDIUM', 'HIGH']).optional(),
  dueAt: z.string().optional(),
});

const updateTaskSchema = z.object({
  title: z.string().min(1).optional(),
  priority: z.enum(['LOW', 'MEDIUM', 'HIGH']).optional(),
  status: z.enum(['PENDING', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED']).optional(),
  dueAt: z.string().optional(),
});

export async function tasksRoutes(app: FastifyInstance) {
  app.addHook('onRequest', (app as any).authenticate);

  app.get('/', async (request, reply) => {
    const user = (request as any).user;
    const query = request.query as any;
    const tasks = await TasksService.list(user.userId, {
      status: query.status,
      date: query.date,
    });
    return reply.send({ success: true, data: tasks });
  });

  app.post('/', async (request, reply) => {
    const user = (request as any).user;
    const parseResult = createTaskSchema.safeParse(request.body);
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

    const task = await TasksService.create(user.userId, parseResult.data);
    return reply.status(201).send({ success: true, data: task });
  });

  app.get('/:id', async (request, reply) => {
    const user = (request as any).user;
    const { id } = request.params as { id: string };
    const task = await TasksService.getById(user.userId, id);
    if (!task) {
      return reply.status(404).send({
        success: false,
        error: { code: 'NOT_FOUND', message: 'Task not found' },
      });
    }
    return reply.send({ success: true, data: task });
  });

  app.post('/:id/complete', async (request, reply) => {
    const user = (request as any).user;
    const { id } = request.params as { id: string };
    const task = await TasksService.complete(user.userId, id);
    if (!task) {
      return reply.status(404).send({
        success: false,
        error: { code: 'NOT_FOUND', message: 'Task not found or already deleted' },
      });
    }
    return reply.send({ success: true, data: task });
  });

  app.patch('/:id', async (request, reply) => {
    const user = (request as any).user;
    const { id } = request.params as { id: string };
    const parseResult = updateTaskSchema.safeParse(request.body);
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

    const task = await TasksService.update(user.userId, id, parseResult.data);
    if (!task) {
      return reply.status(404).send({
        success: false,
        error: { code: 'NOT_FOUND', message: 'Task not found' },
      });
    }
    return reply.send({ success: true, data: task });
  });

  app.delete('/:id', async (request, reply) => {
    const user = (request as any).user;
    const { id } = request.params as { id: string };
    const success = await TasksService.delete(user.userId, id);
    if (!success) {
      return reply.status(404).send({
        success: false,
        error: { code: 'NOT_FOUND', message: 'Task not found' },
      });
    }
    return reply.send({ success: true, data: { deleted: true } });
  });
}
