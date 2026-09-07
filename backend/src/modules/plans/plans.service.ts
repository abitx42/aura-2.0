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

      if (planRes.rows[0].status === 'LOCKED') {
        throw new Error('PLAN_ALREADY_LOCKED');
      }

      // Verify no duplicate tasks in plan
      const uniqueTaskIds = [...new Set(taskIdsInOrder)];
      if (uniqueTaskIds.length !== taskIdsInOrder.length) {
        throw new Error('DUPLICATE_TASKS_IN_PLAN');
      }

      // Verify all tasks exist and belong to this user
      if (uniqueTaskIds.length > 0) {
        const tasksRes = await client.query(
          `SELECT id FROM tasks WHERE id = ANY($1::uuid[]) AND user_id = $2 AND deleted_at IS NULL`,
          [uniqueTaskIds, userId]
        );
        if (tasksRes.rows.length !== uniqueTaskIds.length) {
          throw new Error('INVALID_TASK_SELECTION');
        }
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
          `INSERT INTO daily_plan_items (daily_plan_id, item_type, reference_id, sort_order, status)
           VALUES ($1, 'TASK', $2, $3, 'PLANNED')`,
          [planId, taskId, i]
        );
      }

      // Emit Life Event
      await client.query(
        `INSERT INTO life_events (user_id, domain, event_type, reference_table, reference_id, payload_json, occurred_at)
         VALUES ($1, 'PLANNING', 'PLAN_LOCKED', 'daily_plans', $2, $3, NOW())`,
        [userId, planId, JSON.stringify({ planId, taskCount: taskIdsInOrder.length, date: planRes.rows[0].plan_date })]
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

  static async adaptPlan(userId: string, planId: string, taskIdsInOrder: string[], reason?: string) {
    return withTransaction(async (client) => {
      // 1. Verify plan ownership
      const planRes = await client.query(
        `SELECT * FROM daily_plans WHERE id = $1 AND user_id = $2 AND deleted_at IS NULL`,
        [planId, userId]
      );

      if (planRes.rows.length === 0) {
        throw new Error('PLAN_NOT_FOUND');
      }

      // 2. Fetch existing items for audit diff
      const existingItemsRes = await client.query(
        `SELECT reference_id FROM daily_plan_items WHERE daily_plan_id = $1 ORDER BY sort_order ASC`,
        [planId]
      );
      const previousTaskIds = existingItemsRes.rows.map((r) => r.reference_id);

      // 3. Verify no duplicate tasks
      const uniqueTaskIds = [...new Set(taskIdsInOrder)];
      if (uniqueTaskIds.length !== taskIdsInOrder.length) {
        throw new Error('DUPLICATE_TASKS_IN_PLAN');
      }

      // 4. Verify tasks exist and belong to user
      if (uniqueTaskIds.length > 0) {
        const tasksRes = await client.query(
          `SELECT id FROM tasks WHERE id = ANY($1::uuid[]) AND user_id = $2 AND deleted_at IS NULL`,
          [uniqueTaskIds, userId]
        );
        if (tasksRes.rows.length !== uniqueTaskIds.length) {
          throw new Error('INVALID_TASK_SELECTION');
        }
      }

      // 5. Update daily_plans status to 'MODIFIED' and bump version
      const updatedPlanRes = await client.query(
        `UPDATE daily_plans
         SET status = CASE WHEN status = 'DRAFT' THEN 'DRAFT' ELSE 'MODIFIED' END,
             lock_reason = COALESCE($2, lock_reason),
             updated_at = NOW(),
             version = version + 1
         WHERE id = $1
         RETURNING *`,
        [planId, reason || null]
      );

      // 6. Clear and insert updated items
      await client.query('DELETE FROM daily_plan_items WHERE daily_plan_id = $1', [planId]);

      for (let i = 0; i < taskIdsInOrder.length; i++) {
        const taskId = taskIdsInOrder[i];
        await client.query(
          `INSERT INTO daily_plan_items (daily_plan_id, item_type, reference_id, sort_order, status)
           VALUES ($1, 'TASK', $2, $3, 'PLANNED')`,
          [planId, taskId, i]
        );
      }

      // 7. Emit non-destructive PLAN_MODIFIED Life Event (preserving Intentionality Audit Trail)
      await client.query(
        `INSERT INTO life_events (user_id, domain, event_type, reference_table, reference_id, payload_json, occurred_at)
         VALUES ($1, 'PLANNING', 'PLAN_MODIFIED', 'daily_plans', $2, $3, NOW())`,
        [
          userId,
          planId,
          JSON.stringify({
            planId,
            date: planRes.rows[0].plan_date,
            reason: reason || 'User adapted plan',
            previousTaskIds,
            newTaskIds: taskIdsInOrder,
            previousTaskCount: previousTaskIds.length,
            newTaskCount: taskIdsInOrder.length,
          }),
        ]
      );

      return {
        ...updatedPlanRes.rows[0],
        itemCount: taskIdsInOrder.length,
      };
    });
  }

  static async activatePlan(userId: string, planId: string) {
    return withTransaction(async (client) => {
      const planRes = await client.query(
        `SELECT * FROM daily_plans WHERE id = $1 AND user_id = $2 AND deleted_at IS NULL`,
        [planId, userId]
      );

      if (planRes.rows.length === 0) {
        throw new Error('PLAN_NOT_FOUND');
      }

      const updatedPlanRes = await client.query(
        `UPDATE daily_plans
         SET status = 'ACTIVE',
             updated_at = NOW(),
             version = version + 1
         WHERE id = $1
         RETURNING *`,
        [planId]
      );

      await client.query(
        `INSERT INTO life_events (user_id, domain, event_type, reference_table, reference_id, payload_json, occurred_at)
         VALUES ($1, 'PLANNING', 'PLAN_ACTIVATED', 'daily_plans', $2, $3, NOW())`,
        [userId, planId, JSON.stringify({ planId, date: planRes.rows[0].plan_date })]
      );

      return updatedPlanRes.rows[0];
    });
  }

  static async updateItemExecution(
    userId: string,
    planId: string,
    itemId: string,
    data: {
      executionState: 'NOT_STARTED' | 'IN_PROGRESS' | 'PAUSED' | 'COMPLETED' | 'SKIPPED';
      actualStart?: string;
      actualDurationSeconds?: number;
    }
  ) {
    return withTransaction(async (client) => {
      // 1. Verify plan ownership
      const planRes = await client.query(
        `SELECT * FROM daily_plans WHERE id = $1 AND user_id = $2 AND deleted_at IS NULL`,
        [planId, userId]
      );

      if (planRes.rows.length === 0) {
        throw new Error('PLAN_NOT_FOUND');
      }

      // 2. Verify item exists on this plan
      const itemRes = await client.query(
        `SELECT * FROM daily_plan_items WHERE id = $1 AND daily_plan_id = $2`,
        [itemId, planId]
      );

      if (itemRes.rows.length === 0) {
        throw new Error('ITEM_NOT_FOUND');
      }

      const item = itemRes.rows[0];
      const actualDuration = data.actualDurationSeconds ?? item.actual_duration_seconds;
      const actualStart = data.actualStart ? new Date(data.actualStart) : item.actual_start;
      const actualEnd =
        data.executionState === 'COMPLETED' || data.executionState === 'SKIPPED'
          ? new Date()
          : item.actual_end;
      const itemStatus =
        data.executionState === 'COMPLETED'
          ? 'DONE'
          : data.executionState === 'SKIPPED'
          ? 'SKIPPED'
          : data.executionState === 'IN_PROGRESS'
          ? 'IN_PROGRESS'
          : 'PLANNED';

      const updatedItemRes = await client.query(
        `UPDATE daily_plan_items
         SET execution_state = $1,
             actual_start = COALESCE($2, actual_start),
             actual_end = COALESCE($3, actual_end),
             actual_duration_seconds = $4,
             status = $5,
             updated_at = NOW()
         WHERE id = $6 AND daily_plan_id = $7
         RETURNING *`,
        [data.executionState, actualStart, actualEnd, actualDuration, itemStatus, itemId, planId]
      );

      // 3. Deterministically update task if reference is a task and completed
      if (data.executionState === 'COMPLETED' && item.item_type === 'TASK' && item.reference_id) {
        await client.query(
          `UPDATE tasks
           SET status = 'COMPLETED',
               completed_at = NOW(),
               updated_at = NOW(),
               version = version + 1
           WHERE id = $1 AND user_id = $2`,
          [item.reference_id, userId]
        );
      }

      // 4. Emit Life Event
      await client.query(
        `INSERT INTO life_events (user_id, domain, event_type, reference_table, reference_id, payload_json, occurred_at)
         VALUES ($1, 'EXECUTION', 'TASK_EXECUTED', 'daily_plan_items', $2, $3, NOW())`,
        [
          userId,
          itemId,
          JSON.stringify({
            planId,
            itemId,
            referenceId: item.reference_id,
            executionState: data.executionState,
            actualDurationSeconds: actualDuration,
          }),
        ]
      );

      return updatedItemRes.rows[0];
    });
  }

  static async reviewPlan(
    userId: string,
    planId: string,
    data: {
      dayMood?: number;
      dayImpactFactors?: string[];
      reviewNotes?: string;
      itemReconciliations?: Array<{
        itemId: string;
        reconciliationAction: 'MOVE_TOMORROW' | 'RESCHEDULE' | 'CANCEL' | 'KEEP_OPEN';
        uncompletedReason?: 'TIME_UNDER_ESTIMATED' | 'LOW_ENERGY' | 'UNEXPECTED_EVENT' | 'PROCRASTINATION' | 'PRIORITY_CHANGED' | 'NO_LONGER_RELEVANT' | 'OTHER';
      }>;
    }
  ) {
    return withTransaction(async (client) => {
      // 1. Verify plan ownership
      const planRes = await client.query(
        `SELECT * FROM daily_plans WHERE id = $1 AND user_id = $2 AND deleted_at IS NULL`,
        [planId, userId]
      );

      if (planRes.rows.length === 0) {
        throw new Error('PLAN_NOT_FOUND');
      }

      const plan = planRes.rows[0];

      // 2. Fetch all items for this plan
      const itemsRes = await client.query(
        `SELECT * FROM daily_plan_items WHERE daily_plan_id = $1 ORDER BY sort_order ASC`,
        [planId]
      );

      const items = itemsRes.rows;
      const totalPlanned = items.length;
      const completedCount = items.filter((i: any) => i.execution_state === 'COMPLETED' || i.status === 'DONE').length;
      const actualFocusSeconds = items.reduce((acc: number, i: any) => acc + (i.actual_duration_seconds || 0), 0);
      const plannedFocusSeconds = items.reduce((acc: number, i: any) => {
        const mins = i.duration_minutes || 30;
        return acc + mins * 60;
      }, 0);

      // 3. Process reconciliations if provided
      if (data.itemReconciliations && data.itemReconciliations.length > 0) {
        const planDateObj = new Date(plan.plan_date);
        const tomorrowObj = new Date(planDateObj);
        tomorrowObj.setDate(tomorrowObj.getDate() + 1);
        const tomorrowStr = tomorrowObj.toISOString().split('T')[0];

        for (const rec of data.itemReconciliations) {
          const matchingItem = items.find((i: any) => i.id === rec.itemId);
          if (matchingItem) {
            await client.query(
              `UPDATE daily_plan_items
               SET uncompleted_reason = $1,
                   reconciliation_action = $2,
                   updated_at = NOW()
               WHERE id = $3 AND daily_plan_id = $4`,
              [rec.uncompletedReason ?? null, rec.reconciliationAction, rec.itemId, planId]
            );

            if (rec.reconciliationAction === 'MOVE_TOMORROW' && matchingItem.item_type === 'TASK' && matchingItem.reference_id) {
              await client.query(
                `UPDATE tasks
                 SET date = $1,
                     updated_at = NOW(),
                     version = version + 1
                 WHERE id = $2 AND user_id = $3`,
                [tomorrowStr, matchingItem.reference_id, userId]
              );
            } else if (rec.reconciliationAction === 'CANCEL' && matchingItem.item_type === 'TASK' && matchingItem.reference_id) {
              await client.query(
                `UPDATE tasks
                 SET status = 'CANCELLED',
                     updated_at = NOW(),
                     version = version + 1
                 WHERE id = $1 AND user_id = $2`,
                [matchingItem.reference_id, userId]
              );
            }
          }
        }
      }

      const uncompletedCount = Math.max(0, totalPlanned - completedCount);
      const planAccuracy = totalPlanned > 0
        ? Math.round((completedCount / totalPlanned) * 100)
        : 100;

      const impactFactorsStr = data.dayImpactFactors && data.dayImpactFactors.length > 0
        ? data.dayImpactFactors.join(',')
        : null;

      // 4. Update daily plan status to REVIEWED
      const updatedPlanRes = await client.query(
        `UPDATE daily_plans
         SET status = 'REVIEWED',
             reviewed_at = NOW(),
             day_mood = $1,
             day_impact_factors = $2,
             review_notes = $3,
             plan_accuracy_percent = $4,
             planned_focus_seconds = $5,
             actual_focus_seconds = $6,
             completed_tasks_count = $7,
             uncompleted_tasks_count = $8,
             updated_at = NOW(),
             version = version + 1
         WHERE id = $9
         RETURNING *`,
        [
          data.dayMood ?? null,
          impactFactorsStr,
          data.reviewNotes ?? null,
          planAccuracy,
          plannedFocusSeconds,
          actualFocusSeconds,
          completedCount,
          uncompletedCount,
          planId,
        ]
      );

      // 5. Emit canonical Life Event REVIEW_COMPLETED
      await client.query(
        `INSERT INTO life_events (user_id, domain, event_type, reference_table, reference_id, payload_json, occurred_at)
         VALUES ($1, 'REFLECTION', 'REVIEW_COMPLETED', 'daily_plans', $2, $3, NOW())`,
        [
          userId,
          planId,
          JSON.stringify({
            planId,
            planDate: plan.plan_date,
            planAccuracyPercent: planAccuracy,
            completedCount,
            uncompletedCount,
            totalPlanned,
            actualFocusSeconds,
            plannedFocusSeconds,
            dayMood: data.dayMood,
          }),
        ]
      );

      return {
        ...updatedPlanRes.rows[0],
        daySummary: {
          planDate: plan.plan_date,
          planAccuracyPercent: planAccuracy,
          completedCount,
          uncompletedCount,
          totalPlanned,
          actualFocusSeconds,
          plannedFocusSeconds,
          dayMood: data.dayMood,
          dayImpactFactors: data.dayImpactFactors ?? [],
          reviewNotes: data.reviewNotes ?? null,
        },
      };
    });
  }
}

