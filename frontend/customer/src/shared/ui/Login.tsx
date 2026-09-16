import { useEffect, useRef, useState, type FormEvent } from 'react';
import { request, ApiError, updateSession, type SessionToken } from '../api/client';
import type { components } from '../types/generated';

type Challenge = Required<components['schemas']['CaptchaChallengeResponse']>;
type Answer = { challengeId: string; offsetX: number };

export function Login({ onLogin, admin = false }: { onLogin: () => void; admin?: boolean }) {
  const [register, setRegister] = useState(false);
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const [required, setRequired] = useState(false);
  const [challenge, setChallenge] = useState<Challenge | null>(null);
  const [offset, setOffset] = useState(0);
  const locked = useRef(false);
  const version = useRef(0);
  const form = useRef<HTMLFormElement>(null);
  const end = admin ? 'admin' : 'customer';
  const scene = register ? 'CUSTOMER_REGISTER' : admin ? 'ADMIN_LOGIN' : 'CUSTOMER_LOGIN';

  useEffect(
    () => () => {
      version.current++;
    },
    [],
  );
  useEffect(() => {
    if (!challenge) return;
    const timer = window.setTimeout(() => {
      setChallenge(null);
      setOffset(0);
      setError('拼图已过期，请重新获取');
    }, challenge.expiresIn * 1000);
    return () => window.clearTimeout(timer);
  }, [challenge]);

  function reset() {
    version.current++;
    setChallenge(null);
    setOffset(0);
    setRequired(false);
    setError('');
  }

  async function loadChallenge(current: number) {
    setChallenge(null);
    setOffset(0);
    const value = await request<Challenge>('/public/captcha/challenge', {
      method: 'POST',
      data: { scene, username },
    });
    if (version.current === current) setChallenge(value);
  }

  async function refresh() {
    if (locked.current || !form.current?.reportValidity()) return;
    locked.current = true;
    setBusy(true);
    setError('');
    try {
      await loadChallenge(version.current);
    } catch (cause) {
      setError((cause as Error).message);
    } finally {
      locked.current = false;
      setBusy(false);
    }
  }

  async function submit(answer?: Answer) {
    if (locked.current || !form.current?.reportValidity()) return;
    locked.current = true;
    setBusy(true);
    setError('');
    const current = version.current;
    try {
      if ((register || required) && !answer) {
        await loadChallenge(current);
        return;
      }
      // 答案只随业务请求提交，由后端验证并一次性消费。
      if (answer) setChallenge(null);
      const token = await request<SessionToken>(`/${end}/auth/${register ? 'register' : 'login'}`, {
        method: 'POST',
        data: { username, password, ...(answer ? { captcha: answer } : {}) },
      });
      if (version.current !== current) return;
      updateSession(token);
      onLogin();
    } catch (cause) {
      if (version.current !== current) return;
      setError((cause as Error).message);
      const captchaError = cause instanceof ApiError && cause.code.startsWith('CAPTCHA_');
      if (captchaError || answer) {
        setRequired(true);
        // 限流或服务异常时不自动发起更多请求。
        if (!(cause instanceof ApiError) || (cause.status !== 429 && cause.status !== 503)) {
          try {
            await loadChallenge(current);
          } catch (refreshError) {
            setError((refreshError as Error).message);
          }
        }
      }
    } finally {
      locked.current = false;
      setBusy(false);
    }
  }

  function onSubmit(event: FormEvent) {
    event.preventDefault();
    void submit(challenge ? { challengeId: challenge.challengeId, offsetX: offset } : undefined);
  }

  return (
    <main className="login">
      <div className="brand">
        知阶 <small>RUANKAO</small>
      </div>
      <h1>{admin ? '内容与认证管理' : register ? '开始你的软考学习' : '每一步，都有进阶'}</h1>
      <p>学习有章可循，获证真实可查。</p>
      <form ref={form} onSubmit={onSubmit}>
        <label>
          用户名
          <input
            name="username"
            required
            autoComplete="username"
            pattern="[A-Za-z0-9_]{3,40}"
            maxLength={40}
            value={username}
            disabled={busy}
            onChange={(event) => {
              setUsername(event.target.value);
              reset();
            }}
          />
        </label>
        <label>
          密码
          <input
            name="password"
            required
            type="password"
            autoComplete={register ? 'new-password' : 'current-password'}
            minLength={register ? 10 : 1}
            maxLength={100}
            value={password}
            disabled={busy}
            onChange={(event) => setPassword(event.target.value)}
          />
        </label>
        {challenge && (
          <section className="captcha" aria-label="滑块拼图验证">
            <div
              className="captcha-image"
              style={{ aspectRatio: `${challenge.width} / ${challenge.height}` }}
            >
              <img
                className="captcha-background"
                src={`data:image/png;base64,${challenge.background}`}
                alt="请将拼图移至缺口"
                draggable={false}
              />
              <img
                className="captcha-piece"
                src={`data:image/png;base64,${challenge.piece}`}
                alt=""
                draggable={false}
                style={{
                  width: `${(challenge.pieceWidth / challenge.width) * 100}%`,
                  height: `${(challenge.pieceHeight / challenge.height) * 100}%`,
                  left: `${(offset / challenge.width) * 100}%`,
                  top: `${(challenge.pieceY / challenge.height) * 100}%`,
                }}
              />
            </div>
            <label className="captcha-slider-label">
              拖动滑块完成拼图
              <input
                className="captcha-slider"
                type="range"
                min={0}
                max={challenge.width - challenge.pieceWidth}
                step={1}
                value={offset}
                disabled={busy}
                aria-describedby="captcha-help"
                onChange={(event) => setOffset(Number(event.target.value))}
                onPointerUp={(event) => {
                  const value = Number(event.currentTarget.value);
                  void submit({ challengeId: challenge.challengeId, offsetX: value });
                }}
              />
            </label>
            <p id="captcha-help" className="captcha-help">
              松开后提交验证；也可使用方向键调整，再点击下方按钮。
            </p>
            <button
              type="button"
              className="text-button"
              disabled={busy}
              onClick={() => void refresh()}
            >
              换一张拼图
            </button>
          </section>
        )}
        {error && (
          <p role="alert" className="error">
            {error}
          </p>
        )}
        <button disabled={busy}>
          {busy
            ? '处理中…'
            : challenge
              ? `验证并${register ? '注册' : '登录'}`
              : register
                ? '注册'
                : '登录'}
        </button>
      </form>
      {!admin && (
        <button
          className="text-button"
          disabled={busy}
          onClick={() => {
            reset();
            setRegister(!register);
          }}
        >
          {register ? '已有账号，登录' : '创建账号'}
        </button>
      )}
    </main>
  );
}
