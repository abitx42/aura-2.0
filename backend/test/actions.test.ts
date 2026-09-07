import { buildApp } from '../src/server.js';

export async function runActionsTests() {
  console.log('🧪 Running Aura 2.0 Proposed Actions Safety Tests...');
  const app = await buildApp();

  const testUserId = '44444444-4444-4444-4444-444444444444';
  const token = app.jwt.sign({ userId: testUserId, email: 'actions-user@aura.local' });

  // Test 1: Unauthenticated actions access rejected
  const unauthRes = await app.inject({
    method: 'GET',
    url: '/api/v1/actions',
  });
  if (unauthRes.statusCode !== 401) {
    throw new Error(`Expected 401 for unauthenticated actions fetch, got ${unauthRes.statusCode}`);
  }
  console.log('✅ Test AC1 Passed: Unauthenticated proposed actions access rejected');

  // Test 2: Unauthenticated approve request rejected
  const unauthApproveRes = await app.inject({
    method: 'POST',
    url: '/api/v1/actions/12345678-1234-1234-1234-123456789abc/approve',
  });
  if (unauthApproveRes.statusCode !== 401) {
    throw new Error(`Expected 401 for unauthenticated approve request, got ${unauthApproveRes.statusCode}`);
  }
  console.log('✅ Test AC2 Passed: Unauthenticated approve request rejected');

  await app.close();
  console.log('🎉 All Proposed Actions safety tests completed successfully!');
}
