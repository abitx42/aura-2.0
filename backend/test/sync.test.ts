import { buildApp } from '../src/server.js';

export async function runSyncTests() {
  console.log('🧪 Running Aura 2.0 Sync Engine Tests...');
  const app = await buildApp();

  const testUserId = '33333333-3333-3333-3333-333333333333';
  const token = app.jwt.sign({ userId: testUserId, email: 'sync-user@aura.local' });

  // Test 1: Unauthenticated sync rejected
  const unauthRes = await app.inject({
    method: 'POST',
    url: '/api/v1/sync/push',
    payload: { changes: [] },
  });
  if (unauthRes.statusCode !== 401) {
    throw new Error(`Expected 401 for unauthenticated sync push, got ${unauthRes.statusCode}`);
  }
  console.log('✅ Test S1 Passed: Unauthenticated sync push rejected');

  // Test 2: Validation rejection on malformed sync payload
  const badSyncRes = await app.inject({
    method: 'POST',
    url: '/api/v1/sync/push',
    headers: { authorization: `Bearer ${token}` },
    payload: {
      changes: [
        {
          operationId: '1',
          entity: '', // empty entity
          action: 'INSERT',
        },
      ],
    },
  });
  if (badSyncRes.statusCode !== 422) {
    throw new Error(`Expected 422 for malformed sync change payload, got ${badSyncRes.statusCode}`);
  }
  console.log('✅ Test S2 Passed: Zod schema rejects malformed sync change item');

  // Test 3: Unauthenticated sync pull rejected
  const unauthPullRes = await app.inject({
    method: 'GET',
    url: '/api/v1/sync/pull',
  });
  if (unauthPullRes.statusCode !== 401) {
    throw new Error(`Expected 401 for unauthenticated sync pull, got ${unauthPullRes.statusCode}`);
  }
  console.log('✅ Test S3 Passed: Unauthenticated sync pull rejected');

  await app.close();
  console.log('🎉 All Sync Engine tests completed successfully!');
}
