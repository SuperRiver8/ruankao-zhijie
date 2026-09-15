import { createContext, useContext, useSyncExternalStore, useEffect, type ReactNode } from 'react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import {
  request,
  sessionSnapshot,
  subscribeSession,
  updateSession,
  type SessionToken,
} from '../api/client';

export const queryClient = new QueryClient({
  defaultOptions: { queries: { retry: false, staleTime: 15000 }, mutations: { retry: false } },
});
const SessionContext = createContext<{ token: SessionToken | null; logout: () => Promise<void> }>({
  token: null,
  logout: async () => {},
});
export function SessionProvider({ children }: { children: ReactNode }) {
  const token = useSyncExternalStore(subscribeSession, sessionSnapshot);
  useEffect(
    () =>
      subscribeSession(() => {
        if (!sessionSnapshot()) queryClient.clear();
      }),
    [],
  );
  const logout = async () => {
    try {
      await request('/customer/logout', { method: 'POST' });
    } finally {
      updateSession(null);
      queryClient.clear();
    }
  };
  return (
    <QueryClientProvider client={queryClient}>
      <SessionContext.Provider value={{ token, logout }}>{children}</SessionContext.Provider>
    </QueryClientProvider>
  );
}
export const useSession = () => useContext(SessionContext);
