import { useEffect, useState } from 'react';
import { api, post, setToken, attachmentUrl, Row } from '../api';
import { queryClient } from '../auth/SessionProvider';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
export { Login } from './Login';
export function Markdown({ text }: { text: string }) {
  return (
    <div className="markdown">
      <ReactMarkdown
        remarkPlugins={[remarkGfm]}
        components={{
          img: () => <span>图片请通过下方附件查看</span>,
          a: ({ children, href }) => (
            <a href={href} target="_blank" rel="noopener noreferrer">
              {children}
            </a>
          ),
        }}
      >
        {text}
      </ReactMarkdown>
    </div>
  );
}
export function Attachment({ id, inline = false }: { id: string; inline?: boolean }) {
  const [url, setUrl] = useState(''),
    [error, setError] = useState('');
  useEffect(() => {
    let current = '',
      cancelled = false;
    if (inline)
      attachmentUrl(id)
        .then((u) => {
          current = u;
          if (cancelled) URL.revokeObjectURL(u);
          else setUrl(u);
        })
        .catch((e) => setError(e.message));
    return () => {
      cancelled = true;
      if (current) URL.revokeObjectURL(current);
    };
  }, [id, inline]);
  return (
    <div>
      {inline && url ? (
        <iframe title="证明原件" src={url} className="attachment" />
      ) : (
        <button
          type="button"
          className="text-button"
          onClick={async () => {
            try {
              const u = await attachmentUrl(id);
              setUrl(u);
            } catch (e) {
              setError((e as Error).message);
            }
          }}
        >
          加载附件
        </button>
      )}
      {!inline && url && (
        <a href={url} target="_blank" rel="noreferrer">
          打开附件
        </a>
      )}
      {error && <p className="error">{error}</p>}
    </div>
  );
}
export function JsonView({ data }: { data: unknown }) {
  return <pre className="json">{JSON.stringify(data, null, 2)}</pre>;
}
export async function logout() {
  try {
    await post('/admin/logout');
  } finally {
    setToken({});
    queryClient.clear();
  }
}
