import { buildApp } from '../src/server.js';

export async function runPlansTests() {
  console.log('🧪 Running Aura 2.0 Daily Plans & Lock Tests...');
  const app = await buildApp();

  const testUserId = '22222222-2222-2222-2222-222222222222';
  const token = app.jwt.sign({ userId: testUserId, email: 'test-plans@aura.local' });

  // Test 1: Unauthenticated request rejected
  const unauthRes = await app.inject({
    method: 'POST',
    url: '/api/v1/daily-plans/33333333-3333-3333-3333-333333333333/lock',
    payload: { taskIdsInOrder: [] },
  });
  if (unauthRes.statusCode !== 401) {
    throw new Error(`Expected 401 for unauthenticated lock request, got ${unauthRes.statusCode}`);
  }
  console.log('✅ Test P1 Passed: Unauthenticated lock request rejected');

  // Test 2: Invalid body payload validation
  const badBodyRes = await app.inject({
    method: 'POST',
    url: '/api/v1/daily-plans/33333333-3333-3333-3333-333333333333/lock',
    headers: { authorization: `Bearer ${token}` },
    payload: { taskIdsInOrder: ['not-a-uuid'] },
  });
  if (badBodyRes.statusCode !== 422) {
    throw new Error(`Expected 422 for invalid task UUID in lock payload, got ${badBodyRes.statusCode}`);
  }
  console.log('✅ Test P2 Passed: Schema rejects non-UUID task IDs in lock plan request');

  await app.close();
  console.log('🎉 All Daily Plans tests completed successfully!');
}
