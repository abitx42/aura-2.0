import bcrypt from 'bcryptjs';
import { buildApp } from '../src/server.js';
import { AuthService } from '../src/modules/auth/auth.service.js';

export async function runAuthTests() {
  console.log('🧪 Running Aura 2.0 Authentication Tests...');
  const app = await buildApp();

  // Test 1: Password hashing and validation invariant
  const plainPassword = 'SuperSecretPassword2026!';
  const salt = await bcrypt.genSalt(10);
  const hash = await bcrypt.hash(plainPassword, salt);

  const isMatch = await bcrypt.compare(plainPassword, hash);
  if (!isMatch) {
    throw new Error('bcrypt.compare failed on matching password');
  }

  const isMismatch = await bcrypt.compare('WrongPassword!', hash);
  if (isMismatch) {
    throw new Error('bcrypt.compare succeeded on incorrect password');
  }
  console.log('✅ Test A1 Passed: Bcrypt hashing and comparison operate correctly');

  // Test 2: AuthService methods exist and have proper signatures
  if (typeof AuthService.createUser !== 'function' || typeof AuthService.findByEmailWithPassword !== 'function') {
    throw new Error('AuthService is missing required credential methods');
  }
  console.log('✅ Test A2 Passed: AuthService interface supports credential persistence');

  // Test 3: Malformed login request validation
  const badLoginRes = await app.inject({
    method: 'POST',
    url: '/api/v1/auth/login',
    payload: {
      email: 'invalid-email',
      password: '',
    },
  });

  if (badLoginRes.statusCode !== 422) {
    throw new Error(`Expected 422 for invalid login payload, got ${badLoginRes.statusCode}`);
  }
  console.log('✅ Test A3 Passed: Login validation rejects empty passwords and invalid emails');

  // Test 4: JWT Verification with app.jwt
  const testPayload = { userId: '11111111-1111-1111-1111-111111111111', email: 'test@aura.local' };
  const token = app.jwt.sign(testPayload);
  const decoded = app.jwt.verify(token) as any;

  if (decoded.userId !== testPayload.userId || decoded.email !== testPayload.email) {
    throw new Error('JWT token did not encode or decode correctly');
  }
  console.log('✅ Test A4 Passed: JWT issuance and verification work securely');

  await app.close();
  console.log('🎉 All Auth tests completed successfully!');
}
