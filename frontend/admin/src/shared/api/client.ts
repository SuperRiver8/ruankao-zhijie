import axios, { AxiosError, AxiosHeaders, type InternalAxiosRequestConfig } from 'axios';

export interface SessionToken {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
}
export interface ApiResponse<T> {
  code: string;
  message: string;
  data: T;
  traceId: string;
}
export class ApiError extends Error {
  constructor(
    message: string,
    public readonly code: string,
    public readonly status?: number,
  ) {
    super(message);
    this.name = 'ApiError';
  }
}
const storageKey = 'zhijie-admin-session-v2';
function restore(): SessionToken | null {
  try {
    const saved: unknown = JSON.parse(sessionStorage.getItem(storageKey) || 'null');
    if (
      saved &&
      typeof saved === 'object' &&
      'accessToken' in saved &&
      'refreshToken' in saved &&
      typeof saved.accessToken === 'string' &&
      typeof saved.refreshToken === 'string'
    )
      return { ...saved, expiresIn: 0 } as SessionToken;
  } catch {
    sessionStorage.removeItem(storageKey);
  }
  return null;
}
let token = restore();
let epoch = 0;
const listeners = new Set<() => void>();
export const sessionSnapshot = () => token;
export const subscribeSession = (listener: () => void) => {
  listeners.add(listener);
  return () => {
    listeners.delete(listener);
  };
};
export function updateSession(value: SessionToken | null) {
  token = value;
  epoch++;
  if (value) sessionStorage.setItem(storageKey, JSON.stringify(value));
  else sessionStorage.removeItem(storageKey);
  listeners.forEach((listener) => listener());
}
export const client = axios.create({ baseURL: '/api', timeout: 30000 });
const refreshClient = axios.create({ baseURL: '/api', timeout: 30000 });
type ReplayConfig = InternalAxiosRequestConfig & { replayed?: boolean; sessionEpoch?: number };
client.interceptors.request.use((config: ReplayConfig) => {
  config.headers = AxiosHeaders.from(config.headers);
  if (
    token &&
    !config.url?.startsWith('/admin/auth/') &&
    !config.url?.startsWith('/public/captcha/')
  )
    config.headers.set('Authorization', `Bearer ${token.accessToken}`);
  config.sessionEpoch = epoch;
  return config;
});
let refreshing: Promise<void> | null = null;
client.interceptors.response.use(
  (response) => response,
  async (error: AxiosError<ApiResponse<null>>) => {
    const config = error.config as ReplayConfig | undefined;
    if (
      error.response?.status === 401 &&
      config &&
      !config.replayed &&
      token &&
      !config.url?.startsWith('/admin/auth/') &&
      !config.url?.startsWith('/public/captcha/')
    ) {
      config.replayed = true;
      if (config.sessionEpoch === epoch) {
        refreshing ??= (async () => {
          const currentEpoch = epoch;
          try {
            const response = await refreshClient.post<ApiResponse<SessionToken>>(
              '/admin/auth/refresh',
              { refreshToken: token?.refreshToken },
            );
            if (currentEpoch !== epoch) throw new Error('会话已变化，请重新登录');
            updateSession(response.data.data);
          } catch (cause) {
            if (currentEpoch === epoch) updateSession(null);
            throw new Error('登录已失效，请重新登录');
          }
        })().finally(() => {
          refreshing = null;
        });
        await refreshing;
      }
      if (!token) throw new Error('登录已失效，请重新登录');
      return client.request(config);
    }
    if (error.response?.status === 401 && config?.replayed) updateSession(null);
    const detail = error.response?.data;
    throw new ApiError(
      detail?.message || `请求失败（${error.response?.status || '网络异常'}）`,
      detail?.code || 'NETWORK_ERROR',
      error.response?.status,
    );
  },
);
export async function request<T>(
  path: string,
  options: {
    method?: string;
    data?: unknown;
    headers?: Record<string, string>;
    signal?: AbortSignal;
  } = {},
): Promise<T> {
  const response = await client.request<ApiResponse<T>>({ url: path, ...options });
  return response.data.data;
}
export async function documentRequest<T>(path: string): Promise<T> {
  return (await client.get<T>(path)).data;
}
export async function attachmentUrl(id: string): Promise<string> {
  return URL.createObjectURL(
    (await client.get<Blob>(`/attachments/${id}`, { responseType: 'blob' })).data,
  );
}
