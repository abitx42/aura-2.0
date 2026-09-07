import { FastifyInstance } from 'fastify';
import { z } from 'zod';
import { query } from '../../db/index.js';

const askSchema = z.object({
  prompt: z.string().min(1),
  clientLocalTime: z.string().optional(),
});

export async function auraRoutes(app: FastifyInstance) {
  app.addHook('onRequest', (app as any).authenticate);

  app.post('/ask', async (request, reply) => {
    const user = (request as any).user;
    const parseResult = askSchema.safeParse(request.body);
    if (!parseResult.success) {
      return reply.status(422).send({
        success: false,
        error: {
          code: 'VALIDATION_ERROR',
          message: 'Invalid prompt input',
          details: parseResult.error.issues,
        },
      });
    }

    const { prompt } = parseResult.data;

    // Deterministic SQL Retrieval: Get today's stats
    const today = new Date().toISOString().split('T')[0];
    
    const tasksRes = await query(
      `SELECT id, title, status, priority, due_at
       FROM tasks
       WHERE user_id = $1 AND deleted_at IS NULL AND (due_at::date = $2::date OR status = 'IN_PROGRESS' OR status = 'PENDING')
       ORDER BY priority DESC, created_at ASC`,
      [user.userId, today]
    );

    const pendingTasks = tasksRes.rows.filter(t => t.status === 'PENDING' || t.status === 'IN_PROGRESS');
    const completedTasks = tasksRes.rows.filter(t => t.status === 'COMPLETED');

    const profileRes = await query(
      `SELECT display_name, timezone, typical_wake_time, typical_sleep_time FROM user_profiles WHERE user_id = $1`,
      [user.userId]
    );
    const profile = profileRes.rows[0];

    // Build deterministic narrative
    let message = '';
    let proposedAction = null;

    const lowerPrompt = prompt.toLowerCase();
    if (lowerPrompt.includes('focus') || lowerPrompt.includes('next') || lowerPrompt.includes('do now')) {
      if (pendingTasks.length === 0) {
        message = `Your day is clear right now, ${profile?.display_name || 'friend'}. All ${completedTasks.length} planned items are complete.`;
      } else {
        const topTask = pendingTasks[0];
        message = `Based on your commitments, your immediate focus is **${topTask.title}** (${topTask.priority} priority). You have ${pendingTasks.length} pending items remaining today.`;
      }
    } else if (lowerPrompt.includes('plan') || lowerPrompt.includes('tomorrow')) {
      message = `You have completed ${completedTasks.length} of ${tasksRes.rows.length} commitments today. When you are ready, let's open Plan Tomorrow and lock your schedule.`;
    } else {
      message = `Good day, ${profile?.display_name || 'there'}. You have ${pendingTasks.length} tasks pending and ${completedTasks.length} completed today.`;
    }

    return reply.send({
      success: true,
      data: {
        message,
        deterministicContext: {
          pendingCount: pendingTasks.length,
          completedCount: completedTasks.length,
          totalCount: tasksRes.rows.length,
        },
        proposedAction,
        suggestedQuickPrompts: [
          'What should I focus on next?',
          'How is my daily progress?',
          'Help me prepare tomorrow',
        ],
      },
    });
  });
}
