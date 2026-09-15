import { useQuery } from '@tanstack/react-query';
import { useSearchParams } from 'react-router-dom';
import { api, type Row } from './index';

export interface PageData<T> {
  records: T[];
  total: number;
  page: number;
  size: number;
}
export function usePageQuery(path: string, prefix = '') {
  const [search, setSearch] = useSearchParams();
  const current = Math.max(1, Number(search.get(prefix + 'page')) || 1);
  const pageSize = Math.min(100, Math.max(1, Number(search.get(prefix + 'size')) || 20));
  const separator = path.includes('?') ? '&' : '?';
  const result = useQuery<PageData<Row>>({
    queryKey: [path, current, pageSize],
    queryFn: () => api(`${path}${separator}page=${current}&size=${pageSize}`),
    refetchInterval: (query) =>
      path === '/admin/imports' && query.state.data?.records.some((row) => row.status === 'QUEUED')
        ? 4000
        : false,
  });
  const pagination = {
    current,
    pageSize,
    total: result.data?.total || 0,
    onChange: (page: number, size: number) =>
      setSearch((previous) => {
        const next = new URLSearchParams(previous);
        next.set(prefix + 'page', String(page));
        next.set(prefix + 'size', String(size));
        return next;
      }),
  };
  return {
    data: result.data?.records || [],
    error: result.error?.message || '',
    load: result.refetch,
    isLoading: result.isPending,
    pagination,
  };
}
export function useDictionary(path: string) {
  const result = useQuery<Row[]>({ queryKey: [path], queryFn: () => api(path) });
  return { data: result.data || [], error: result.error?.message || '', load: result.refetch };
}
export function PageControls({
  pagination,
}: {
  pagination: ReturnType<typeof usePageQuery>['pagination'];
}) {
  const { current, pageSize, total, onChange } = pagination;
  return (
    <nav className="section-title" aria-label="分页">
      <button disabled={current <= 1} onClick={() => onChange(current - 1, pageSize)}>
        上一页
      </button>
      <span>
        第 {current} 页 · 共 {total} 条
      </span>
      <button
        disabled={current * pageSize >= total}
        onClick={() => onChange(current + 1, pageSize)}
      >
        下一页
      </button>
    </nav>
  );
}
