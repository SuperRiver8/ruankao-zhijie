import { request, documentRequest, sessionSnapshot, updateSession, attachmentUrl } from './client';
export { attachmentUrl };
export type Row = Record<string, any>;
export const loggedIn = () => !!sessionSnapshot();
export function setToken(value: Row) {
  updateSession(value.accessToken ? (value as import('./client').SessionToken) : null);
}
export async function api(path: string, init: RequestInit = {}): Promise<any> {
  if (path === '/admin/imports/schema' || path === '/admin/imports/dictionary')
    return documentRequest(path);
  return request(path, {
    method: init.method,
    data: typeof init.body === 'string' ? JSON.parse(init.body) : init.body,
    headers: Object.fromEntries(new Headers(init.headers)),
    signal: init.signal || undefined,
  });
}
export const post = (path: string, body: Row = {}) =>
  api(path, { method: 'POST', body: JSON.stringify(body) });
export const put = (path: string, body: Row) =>
  api(path, { method: 'PUT', body: JSON.stringify(body) });
export async function upload(file: File, access = 'PRIVATE') {
  const data = new FormData();
  data.append('file', file);
  data.append('access', access);
  return api('/attachments', { method: 'POST', body: data });
}
export const labels: Row = {
  PENDING: '待审核',
  REVIEWING: '审核中',
  SUPPLEMENT: '退回补充',
  APPROVED: '平台审核通过',
  REJECTED: '驳回',
  REVOKED: '已撤销',
  UNCHECKED: '未检查',
  UNREADABLE: '无法识别',
  CONSISTENT: '信息一致',
  DIFFERENCES: '存在差异',
  UNVERIFIED: '未核验',
  MATCH: '匹配',
  MISMATCH: '不匹配',
  UNCERTAIN: '无法确认',
  IN_PROGRESS: '作答中',
  SUBMITTED: '已交卷',
  PUBLISHED: '已发布',
  QUEUED: '排队处理',
  FAILED: '失败，可重试',
  PREVIEW: '待确认',
  COMMITTED: '已提交',
  KNOWLEDGE: '知识点',
  SYLLABUS: '大纲',
  MAPPING: '大纲映射',
  QUESTION: '题目',
  MATERIAL: '资料',
  PAPER: '试卷',
};
export const label = (s: string) => labels[s] || s;
