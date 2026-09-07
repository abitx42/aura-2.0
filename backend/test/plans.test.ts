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

  // Test 3: Unauthenticated adapt request rejected
  const unauthAdaptRes = await app.inject({
    method: 'POST',
    url: '/api/v1/daily-plans/33333333-3333-3333-3333-333333333333/adapt',
    payload: { taskIdsInOrder: [], reason: 'Rescheduled' },
  });
  if (unauthAdaptRes.statusCode !== 401) {
    throw new Error(`Expected 401 for unauthenticated adapt request, got ${unauthAdaptRes.statusCode}`);
  }
  console.log('✅ Test P3 Passed: Unauthenticated adapt request rejected');

  // Test 4: Invalid task UUID in adapt payload rejected
  const badAdaptRes = await app.inject({
    method: 'POST',
    url: '/api/v1/daily-plans/33333333-3333-3333-3333-333333333333/adapt',
    headers: { authorization: `Bearer ${token}` },
    payload: { taskIdsInOrder: ['invalid-uuid'], reason: 'Rescheduled' },
  });
  if (badAdaptRes.statusCode !== 422) {
    throw new Error(`Expected 422 for invalid task UUID in adapt payload, got ${badAdaptRes.statusCode}`);
  }
  console.log('✅ Test P4 Passed: Schema rejects non-UUID task IDs in adapt plan request');

  // Test 5: Unauthenticated activate request rejected
  const unauthActivateRes = await app.inject({
    method: 'POST',
    url: '/api/v1/daily-plans/33333333-3333-3333-3333-333333333333/activate',
  });
  if (unauthActivateRes.statusCode !== 401) {
    throw new Error(`Expected 401 for unauthenticated activate request, got ${unauthActivateRes.statusCode}`);
  }
  console.log('✅ Test P5 Passed: Unauthenticated activate request rejected');

  await app.close();
  console.log('🎉 All Daily Plans tests completed successfully!');
}
