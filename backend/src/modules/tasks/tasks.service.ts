import { query } from '../../db/index.js';

export interface Task {
  id: string;
  user_id: string;
  title: string;
  status: 'PENDING' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';
  priority: 'LOW' | 'MEDIUM' | 'HIGH';
  energy_tag?: 'LOW' | 'MEDIUM' | 'HIGH';
  due_at?: string;
  completed_at?: string;
  version: number;
  created_at: string;
  updated_at: string;
}

export class TasksService {
  static async list(userId: string, filters?: { status?: string; date?: string }): Promise<Task[]> {
    let sql = 'SELECT * FROM tasks WHERE user_id = $1 AND deleted_at IS NULL';
    const params: any[] = [userId];

    if (filters?.status) {
      params.push(filters.status);
      sql += ` AND status = $${params.length}`;
    }
    if (filters?.date) {
      params.push(filters.date);
      sql += ` AND due_at::date = $${params.length}::date`;
    }

    sql += ' ORDER BY created_at ASC';
    const res = await query<Task>(sql, params);
    return res.rows;
  }

  static async getById(userId: string, taskId: string): Promise<Task | null> {
    const res = await query<Task>(
      'SELECT * FROM tasks WHERE id = $1 AND user_id = $2 AND deleted_at IS NULL',
      [taskId, userId]
    );
    return res.rows[0] || null;
  }

  static async create(userId: string, data: {
    title: string;
    priority?: 'LOW' | 'MEDIUM' | 'HIGH';
    energyTag?: 'LOW' | 'MEDIUM' | 'HIGH';
    dueAt?: string;
  }): Promise<Task> {
    const res = await query<Task>(
      `INSERT INTO tasks (user_id, title, priority, energy_tag, due_at)
       VALUES ($1, $2, COALESCE($3, 'MEDIUM'), $4, $5)
       RETURNING *`,
      [userId, data.title, data.priority || null, data.energyTag || null, data.dueAt || null]
    );
    return res.rows[0];
  }

  static async complete(userId: string, taskId: string): Promise<Task | null> {
    const res = await query<Task>(
      `UPDATE tasks
       SET status = 'COMPLETED',
           completed_at = NOW(),
           updated_at = NOW(),
           version = version + 1
       WHERE id = $1 AND user_id = $2 AND deleted_at IS NULL
       RETURNING *`,
      [taskId, userId]
    );

    if (res.rows[0]) {
      // Record Life Event
      await query(
        `INSERT INTO life_events (user_id, domain, event_type, reference_table, reference_id, payload_json, occurred_at)
         VALUES ($1, 'PRODUCTIVITY', 'TASK_COMPLETED', 'tasks', $2, $3, NOW())`,
        [userId, taskId, JSON.stringify({ taskId, title: res.rows[0].title })]
      );
    }

    return res.rows[0] || null;
  }

  static async update(userId: string, taskId: string, data: Partial<{
    title: string;
    priority: 'LOW' | 'MEDIUM' | 'HIGH';
    status: 'PENDING' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';
    dueAt: string;
  }>): Promise<Task | null> {
    const res = await query<Task>(
      `UPDATE tasks
       SET title = COALESCE($3, title),
           priority = COALESCE($4, priority),
           status = COALESCE($5, status),
           due_at = COALESCE($6, due_at),
           updated_at = NOW(),
           version = version + 1
       WHERE id = $1 AND user_id = $2 AND deleted_at IS NULL
       RETURNING *`,
      [taskId, userId, data.title || null, data.priority || null, data.status || null, data.dueAt || null]
    );
    return res.rows[0] || null;
  }

  static async delete(userId: string, taskId: string): Promise<boolean> {
    const res = await query(
      `UPDATE tasks
       SET deleted_at = NOW(),
           updated_at = NOW(),
           version = version + 1
       WHERE id = $1 AND user_id = $2 AND deleted_at IS NULL`,
      [taskId, userId]
    );
    return (res.rowCount ?? 0) > 0;
  }
}
