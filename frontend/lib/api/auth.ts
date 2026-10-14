import type { z } from 'zod';
import { GetCurrentUserResponse, LoginResponse } from './generated/sbi.zod.ts';
import { ApiError, type ApiClient } from './http.ts';
import { LOGIN_FAILED_MESSAGE } from './messages.ts';

export type UserSession = z.infer<typeof GetCurrentUserResponse>;

export function authApi(client: ApiClient) {
  return {
    me: (signal?: AbortSignal) => client.request('/auth/me', { schema: GetCurrentUserResponse, ...(signal ? { signal } : {}) }),
    async login(email: string, password: string): Promise<UserSession> {
      client.resetCsrf();
      try {
        const result = await client.request('/auth/login', { method: 'POST', body: { email, password }, schema: LoginResponse });
        return result.user;
      } catch (error) {
        // Mesma mensagem para senha errada, conta bloqueada, inativa ou inexistente (RF-18).
        if (error instanceof ApiError && error.status === 401) throw new ApiError(401, error.code, LOGIN_FAILED_MESSAGE);
        throw error;
      } finally {
        client.resetCsrf();
      }
    },
    async logout(): Promise<void> {
      try {
        await client.request('/auth/logout', { method: 'POST' });
      } finally {
        client.resetCsrf();
      }
    },
  };
}
