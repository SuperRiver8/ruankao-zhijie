import { useEffect, useState } from 'react';
import { api, post, setToken, attachmentUrl, Row } from '../api';
import { queryClient } from '../auth/SessionProvider';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
export function Login({ onLogin, admin = false }: { onLogin: () => void; admin?: boolean }) {
  const [register, setRegister] = useState(false),
    [error, setError] = useState(''),
    [busy, setBusy] = useState(false);
  return (
    <main className="login">
      <div className="brand">
        知阶 <small>RUANKAO</small>
      </div>
      <h1>{admin ? '内容与认证管理' : register ? '开始你的软考学习' : '每一步，都有进阶'}</h1>
      <p>学习有章可循，获证真实可查。</p>
      <form
        onSubmit={async (e) => {
          e.preventDefault();
          setBusy(true);
          setError('');
          try {
            const data = Object.fromEntries(new FormData(e.currentTarget));
            setToken(await post(`/admin/auth/${register ? 'register' : 'login'}`, data));
            onLogin();
          } catch (e) {
            setError((e as Error).message);
          } finally {
            setBusy(false);
          }
        }}
      >
        <label>
          用户名
          <input name="username" required autoComplete="username" pattern="[A-Za-z0-9_]{3,40}" />
        </label>
        <label>
          密码
          <input
            name="password"
            required
            type="password"
            autoComplete={register ? 'new-password' : 'current-password'}
            minLength={register ? 10 : 1}
          />
        </label>
        {error && (
          <p role="alert" className="error">
            {error}
          </p>
        )}
        <button disabled={busy}>{busy ? '处理中…' : register ? '注册' : '登录'}</button>
      </form>
      {!admin && (
        <button className="text-button" onClick={() => setRegister(!register)}>
          {register ? '已有账号，登录' : '创建账号'}
        </button>
      )}
    </main>
  );
}
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
