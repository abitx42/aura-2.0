import { query, withTransaction } from '../../db/index.js';

export class PlansService {
  static async getByDate(userId: string, planDate: string) {
    const planRes = await query(
      `SELECT * FROM daily_plans WHERE user_id = $1 AND plan_date = $2 AND deleted_at IS NULL`,
      [userId, planDate]
    );

    if (planRes.rows.length === 0) {
      return null;
    }

    const plan = planRes.rows[0];
    const itemsRes = await query(
      `SELECT dpi.*, t.title as task_title, t.priority as task_priority, t.status as task_status
       FROM daily_plan_items dpi
       LEFT JOIN tasks t ON dpi.reference_id = t.id
       WHERE dpi.daily_plan_id = $1
       ORDER BY dpi.planned_start ASC, dpi.created_at ASC`,
      [plan.id]
    );

    return {
      ...plan,
      items: itemsRes.rows,
    };
  }

  static async lockPlan(userId: string, planId: string, taskIdsInOrder: string[]) {
    return withTransaction(async (client) => {
      // Verify plan ownership
      const planRes = await client.query(
        `SELECT * FROM daily_plans WHERE id = $1 AND user_id = $2 AND deleted_at IS NULL`,
        [planId, userId]
      );

      if (planRes.rows.length === 0) {
        throw new Error('PLAN_NOT_FOUND');
      }

      // Update plan status to LOCKED
      const updatedPlanRes = await client.query(
        `UPDATE daily_plans
         SET status = 'LOCKED',
             locked_at = NOW(),
             updated_at = NOW(),
             version = version + 1
         WHERE id = $1
         RETURNING *`,
        [planId]
      );

      // Clear existing items and insert fresh locked list
      await client.query('DELETE FROM daily_plan_items WHERE daily_plan_id = $1', [planId]);

      for (let i = 0; i < taskIdsInOrder.length; i++) {
        const taskId = taskIdsInOrder[i];
        await client.query(
          `INSERT INTO daily_plan_items (daily_plan_id, item_type, reference_id, status)
           VALUES ($1, 'TASK', $2, 'PLANNED')`,
          [planId, taskId]
        );
      }

      // Emit Life Event
      await client.query(
        `INSERT INTO life_events (user_id, domain, event_type, payload_json, occurred_at)
         VALUES ($1, 'PLANNING', 'PLAN_LOCKED', $2, NOW())`,
        [userId, JSON.stringify({ planId, taskCount: taskIdsInOrder.length, date: planRes.rows[0].plan_date })]
      );

      return {
        ...updatedPlanRes.rows[0],
        itemCount: taskIdsInOrder.length,
      };
    });
  }

  static async createOrGetDraft(userId: string, planDate: string) {
    const existing = await this.getByDate(userId, planDate);
    if (existing) return existing;

    const res = await query(
      `INSERT INTO daily_plans (user_id, plan_date, status)
       VALUES ($1, $2, 'DRAFT')
       RETURNING *`,
      [userId, planDate]
    );

    return {
      ...res.rows[0],
      items: [],
    };
  }
}
