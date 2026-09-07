import Fastify from 'fastify';
import cors from '@fastify/cors';
import jwt from '@fastify/jwt';
import { env } from './config/env.js';
import { authRoutes } from './modules/auth/auth.routes.js';
import { profileRoutes } from './modules/profile/profile.routes.js';
import { tasksRoutes } from './modules/tasks/tasks.routes.js';
import { plansRoutes } from './modules/plans/plans.routes.js';
import { actionsRoutes } from './modules/actions/actions.routes.js';
import { syncRoutes } from './modules/sync/sync.routes.js';
import { auraRoutes } from './modules/aura/aura.routes.js';

export async function buildApp() {
  const app = Fastify({
    logger: env.NODE_ENV === 'development',
  });

  // Register CORS
  await app.register(cors, {
    origin: env.CORS_ORIGIN,
    methods: ['GET', 'POST', 'PUT', 'PATCH', 'DELETE'],
  });

  // Register JWT
  await app.register(jwt, {
    secret: env.JWT_SECRET,
    sign: {
      expiresIn: env.JWT_EXPIRES_IN,
    },
  });

  // Decorate with authentication guard
  app.decorate('authenticate', async (request: any, reply: any) => {
    try {
      await request.jwtVerify();
    } catch (err) {
      return reply.status(401).send({
        success: false,
        error: {
          code: 'UNAUTHORIZED',
          message: 'Authentication token is invalid or expired.',
        },
      });
    }
  });

  // Health Check Endpoint
  app.get('/api/v1/health', async () => {
    return {
      status: 'healthy',
      service: 'aura-backend',
      version: '2.0.0',
      timestamp: new Date().toISOString(),
    };
  });

  // Register Module Routes
  await app.register(authRoutes, { prefix: '/api/v1/auth' });
  await app.register(profileRoutes, { prefix: '/api/v1/profile' });
  await app.register(tasksRoutes, { prefix: '/api/v1/tasks' });
  await app.register(plansRoutes, { prefix: '/api/v1/daily-plans' });
  await app.register(actionsRoutes, { prefix: '/api/v1/actions' });
  await app.register(syncRoutes, { prefix: '/api/v1/sync' });
  await app.register(auraRoutes, { prefix: '/api/v1/aura' });

  // Global Error Handler
  app.setErrorHandler((error, request, reply) => {
    app.log.error(error);
    const statusCode = error.statusCode || 500;
    return reply.status(statusCode).send({
      success: false,
      error: {
        code: error.code || 'INTERNAL_SERVER_ERROR',
        message: statusCode === 500 && env.NODE_ENV === 'production'
          ? 'An internal error occurred.'
          : error.message,
      },
    });
  });

  return app;
}

async function start() {
  try {
    const app = await buildApp();
    await app.listen({ port: env.PORT, host: env.HOST });
    console.log(`🌌 Aura 2.0 Backend listening at http://${env.HOST}:${env.PORT}`);
  } catch (err) {
    console.error('Failed to start server:', err);
    process.exit(1);
  }
}

if (process.env.NODE_ENV !== 'test') {
  start();
}
