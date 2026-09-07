import { FastifyInstance } from 'fastify';
import { z } from 'zod';
import bcrypt from 'bcryptjs';
import { AuthService } from './auth.service.js';

const signupSchema = z.object({
  email: z.string().email(),
  password: z.string().min(8),
  preferredName: z.string().min(1),
});

const loginSchema = z.object({
  email: z.string().email(),
  password: z.string().min(1),
});

export async function authRoutes(app: FastifyInstance) {
  app.post('/signup', async (request, reply) => {
    const parseResult = signupSchema.safeParse(request.body);
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
      const { email, password, preferredName } = parseResult.data;
      const result = await AuthService.createUser(email, password, preferredName);
      
      const token = app.jwt.sign({
        userId: result.user.id,
        email: result.user.email,
      });

      return reply.status(201).send({
        success: true,
        data: {
          user: result.user,
          accessToken: token,
        },
      });
    } catch (err: any) {
      if (err.message === 'EMAIL_ALREADY_EXISTS') {
        return reply.status(409).send({
          success: false,
          error: {
            code: 'EMAIL_ALREADY_EXISTS',
            message: 'An account with this email already exists.',
          },
        });
      }
      throw err;
    }
  });

  app.post('/login', async (request, reply) => {
    const parseResult = loginSchema.safeParse(request.body);
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

    const { email, password } = parseResult.data;
    const user = await AuthService.findByEmailWithPassword(email);
    if (!user || !user.password_hash) {
      return reply.status(401).send({
        success: false,
        error: {
          code: 'UNAUTHORIZED',
          message: 'Incorrect email or password.',
        },
      });
    }

    const isMatch = await bcrypt.compare(password, user.password_hash);
    if (!isMatch) {
      return reply.status(401).send({
        success: false,
        error: {
          code: 'UNAUTHORIZED',
          message: 'Incorrect email or password.',
        },
      });
    }

    const token = app.jwt.sign({
      userId: user.id,
      email: user.email,
    });

    const profile = await AuthService.getProfile(user.id);

    return reply.send({
      success: true,
      data: {
        user: {
          id: user.id,
          email: user.email,
          displayName: profile?.display_name || '',
        },
        accessToken: token,
      },
    });
  });

  app.get('/me', { onRequest: [(app as any).authenticate] }, async (request, reply) => {
    const user = (request as any).user;
    const profile = await AuthService.getProfile(user.userId);
    return reply.send({
      success: true,
      data: { profile },
    });
  });
}
