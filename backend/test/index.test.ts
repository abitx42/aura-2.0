import { buildApp } from '../src/server.js';

async function runTests() {
  console.log('🧪 Running Aura 2.0 Backend Tests...');
  const app = await buildApp();

  // Test 1: Health Check
  const healthRes = await app.inject({
    method: 'GET',
    url: '/api/v1/health',
  });

  if (healthRes.statusCode !== 200) {
    throw new Error(`Health check failed with status ${healthRes.statusCode}`);
  }
  const healthJson = JSON.parse(healthRes.body);
  if (healthJson.status !== 'healthy') {
    throw new Error(`Unexpected health status: ${healthJson.status}`);
  }
  console.log('✅ Test 1 Passed: Health Check endpoint responds with healthy status');

  // Test 2: Unauthenticated Route Guard
  const tasksRes = await app.inject({
    method: 'GET',
    url: '/api/v1/tasks',
  });

  if (tasksRes.statusCode !== 401) {
    throw new Error(`Expected 401 on unauthenticated route, got ${tasksRes.statusCode}`);
  }
  const tasksJson = JSON.parse(tasksRes.body);
  if (tasksJson.error.code !== 'UNAUTHORIZED') {
    throw new Error(`Expected UNAUTHORIZED code, got ${tasksJson.error.code}`);
  }
  console.log('✅ Test 2 Passed: Protected route properly rejects unauthenticated request');

  // Test 3: Validation Error on Invalid Signup Body
  const signupRes = await app.inject({
    method: 'POST',
    url: '/api/v1/auth/signup',
    payload: {
      email: 'not-an-email',
      password: '123',
    },
  });

  if (signupRes.statusCode !== 422) {
    throw new Error(`Expected 422 validation error, got ${signupRes.statusCode}`);
  }
  const signupJson = JSON.parse(signupRes.body);
  if (signupJson.error.code !== 'VALIDATION_ERROR') {
    throw new Error(`Expected VALIDATION_ERROR, got ${signupJson.error.code}`);
  }
  console.log('✅ Test 3 Passed: Zod schema rejects malformed signup request');

  console.log('🎉 All backend tests passed successfully!');
  await app.close();
  process.exit(0);
}

runTests().catch((err) => {
  console.error('❌ Test execution failed:', err);
  process.exit(1);
});
